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
        self.handleError(
          message: ERROR_TYPES.APPLE_PAY_TOKENIZATION_ERROR.rawValue,
          localizedMessage: error.localizedDescription
        )
      } else if let response = response, let nonce = response["nonce"] as? String {
        let result = PKPaymentAuthorizationResult(status: .success, errors: nil)
        completion(result)

        var amount = "0"
        var currency = "USD"

        if case .applePay(let options) = self.args.paymentMethod {
          amount = options["totalAmount"] as? String ?? "0"
          currency = options["currencyCode"] as? String ?? "USD"
        }

        self.handleSuccess(
          nonce: nonce,
          amount: amount,
          currency: currency,
          paymentType: "ApplePay",
          additionalData: response
        )
      } else {
        let result = PKPaymentAuthorizationResult(status: .failure, errors: nil)
        completion(result)
        self.handleError(
          message: ERROR_TYPES.APPLE_PAY_TOKENIZATION_ERROR.rawValue,
          localizedMessage: "Failed to tokenize Apple Pay payment"
        )
      }
    }
  }

  func paymentAuthorizationViewControllerDidFinish(_ controller: PKPaymentAuthorizationViewController) {
    controller.dismiss(animated: true) { [weak self] in
      self?.handleCancel()
    }
  }

  func onResume() {
  }

  func onDestroy() {
    apiClient = nil
    applePayClient = nil
    paymentAuthorizationViewController = nil
  }
}
