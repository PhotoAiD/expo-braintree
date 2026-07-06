//
//  ApplePayPaymentExecutor.swift
//  expo-braintree
//
//  Apple Pay payment executor
//

import Braintree
import Foundation
import PassKit
import UIKit

class ApplePayPaymentExecutor: NSObject, BasePaymentExecutor, PKPaymentAuthorizationViewControllerDelegate {
  let args: BasePaymentArgs
  weak var listener: PaymentExecutorListener?
  private var apiClient: BTAPIClient?
  private var applePayClient: BTApplePayClient?
  private var paymentAuthorizationViewController: PKPaymentAuthorizationViewController?
  private var didHandleResult = false
  private var pendingSuccessData: (nonce: String, amount: String, currency: String, additionalData: [String: Any])?
  private var pendingError: (message: String, localizedMessage: String)?
  private var shippingMethods: [PKShippingMethod] = []
  private var selectedShippingMethod: PKShippingMethod?

  init(args: BasePaymentArgs, listener: PaymentExecutorListener?) {
    self.args = args
    self.listener = listener
    super.init()
  }

  func requestPayment(viewController: UIViewController) {
    guard case .applePay(let options) = args.paymentMethod else {
      handleError(message: "Invalid payment method", localizedMessage: "Expected ApplePay")
      return
    }

    guard BTApplePayHelper.isApplePayAvailable() else {
      handleError(
        message: ERROR_TYPES.APPLE_PAY_NOT_AVAILABLE.rawValue,
        localizedMessage: "Apple Pay is not available on this device"
      )
      return
    }

    guard let client = BTAPIClient(authorization: args.clientToken) else {
      handleError(
        message: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        localizedMessage: "Failed to initialize Braintree API client"
      )
      return
    }

    apiClient = client
    applePayClient = BTApplePayClient(apiClient: client)

    let paymentRequest = BTApplePayHelper.preparePaymentRequest(options: options)
    shippingMethods = paymentRequest.shippingMethods ?? []
    selectedShippingMethod = shippingMethods.first

    guard let paymentVC = PKPaymentAuthorizationViewController(paymentRequest: paymentRequest) else {
      handleError(
        message: ERROR_TYPES.APPLE_PAY_NOT_AVAILABLE.rawValue,
        localizedMessage: "Failed to create Apple Pay view controller"
      )
      return
    }

    paymentAuthorizationViewController = paymentVC
    paymentVC.delegate = self

    DispatchQueue.main.async {
      viewController.present(paymentVC, animated: true, completion: nil)
    }
  }

  // Express checkout: recalculate the sheet total when the user picks another
  // delivery method
  func paymentAuthorizationViewController(
    _ controller: PKPaymentAuthorizationViewController,
    didSelect shippingMethod: PKShippingMethod,
    handler completion: @escaping (PKPaymentRequestShippingMethodUpdate) -> Void
  ) {
    selectedShippingMethod = shippingMethod
    completion(PKPaymentRequestShippingMethodUpdate(paymentSummaryItems: currentSummaryItems()))
  }

  // Express checkout: the delivery method list is static, so an address change
  // keeps the same methods and total. This is the hook for address-dependent
  // pricing if it is ever needed. The contact is redacted by iOS before
  // authorization (only city/state/zip/country are available here).
  func paymentAuthorizationViewController(
    _ controller: PKPaymentAuthorizationViewController,
    didSelectShippingContact contact: PKContact,
    handler completion: @escaping (PKPaymentRequestShippingContactUpdate) -> Void
  ) {
    completion(PKPaymentRequestShippingContactUpdate(
      errors: nil,
      paymentSummaryItems: currentSummaryItems(),
      shippingMethods: shippingMethods
    ))
  }

  private func currentSummaryItems() -> [PKPaymentSummaryItem] {
    guard case .applePay(let options) = args.paymentMethod else {
      return []
    }
    return BTApplePayHelper.prepareSummaryItems(
      options: options,
      shippingMethod: selectedShippingMethod
    )
  }

  func paymentAuthorizationViewController(
    _ controller: PKPaymentAuthorizationViewController,
    didAuthorizePayment payment: PKPayment,
    handler completion: @escaping (PKPaymentAuthorizationResult) -> Void
  ) {
    guard let client = apiClient else {
      let result = PKPaymentAuthorizationResult(status: .failure, errors: nil)
      completion(result)
      handleError(
        message: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        localizedMessage: "Apple Pay client not initialized"
      )
      return
    }

    BTApplePayHelper.tokenizeApplePayment(
      apiClient: client,
      payment: payment
    ) { [weak self] (response, error) in
      guard let self = self else { return }

      if let error = error {
        let result = PKPaymentAuthorizationResult(status: .failure, errors: nil)
        completion(result)
        self.didHandleResult = true
        self.pendingError = (
          message: ERROR_TYPES.APPLE_PAY_TOKENIZATION_ERROR.rawValue,
          localizedMessage: error.localizedDescription
        )
      } else if let response = response, let nonce = response["nonce"] as? String {
        let result = PKPaymentAuthorizationResult(status: .success, errors: nil)
        completion(result)
        self.didHandleResult = true

        // Default to "0" for amount - this is intentional as the JavaScript interface
        // allows nil values, and falling back to "0" will trigger a clear validation error
        // in the success callback rather than silently failing
        var amount = "0"
        var currency = "USD"

        if case .applePay(let options) = self.args.paymentMethod {
          amount = options["totalAmount"] as? String ?? "0"
          currency = options["currencyCode"] as? String ?? "USD"

          // Express checkout: the authorized total includes the delivery price
          if let shippingMethod = payment.shippingMethod ?? self.selectedShippingMethod {
            amount = BTApplePayHelper.totalWithShipping(
              baseAmount: amount,
              shippingMethod: shippingMethod
            ).stringValue
          }
        }

        // Store success data to send after sheet dismisses
        self.pendingSuccessData = (nonce: nonce, amount: amount, currency: currency, additionalData: response)
      } else {
        let result = PKPaymentAuthorizationResult(status: .failure, errors: nil)
        completion(result)
        self.didHandleResult = true
        self.pendingError = (
          message: ERROR_TYPES.APPLE_PAY_TOKENIZATION_ERROR.rawValue,
          localizedMessage: "Failed to tokenize Apple Pay payment"
        )
      }
    }
  }

  func paymentAuthorizationViewControllerDidFinish(_ controller: PKPaymentAuthorizationViewController) {
    // Dismiss the Apple Pay sheet
    controller.dismiss(animated: true) { [weak self] in
      guard let self = self else { return }

      // Send the result to React Native after the sheet has been dismissed
      if let successData = self.pendingSuccessData {
        self.handleSuccess(
          nonce: successData.nonce,
          amount: successData.amount,
          currency: successData.currency,
          paymentType: "ApplePay",
          additionalData: successData.additionalData
        )
      } else if let error = self.pendingError {
        self.handleError(
          message: error.message,
          localizedMessage: error.localizedMessage
        )
      } else if !self.didHandleResult {
        self.handleCancel()
      }
    }
  }

  func onResume() {
  }

  func onDestroy() {
    apiClient = nil
    applePayClient = nil
    paymentAuthorizationViewController = nil
    didHandleResult = false
    pendingSuccessData = nil
    pendingError = nil
    shippingMethods = []
    selectedShippingMethod = nil
  }
}
