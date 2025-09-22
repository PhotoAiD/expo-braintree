//
//  BTPayPalVaultRequest.swift
//  expo-braintree
//
//  Created by Maciej Sasinowski on 28/04/2024.
//

import Braintree
import BraintreeApplePay
import Foundation
import PassKit
import React

enum EXCEPTION_TYPES: String {
  case SWIFT_EXCEPTION = "ReactNativeExpoBraintree:`SwiftException"
  case USER_CANCEL_EXCEPTION = "ReactNativeExpoBraintree:`UserCancelException"
  case TOKENIZE_EXCEPTION = "ReactNativeExpoBraintree:`TokenizeException"
  case PAYPAL_DISABLED_IN_CONFIGURATION =
    "ReactNativeExpoBraintree:`Paypal disabled in configuration"
}

enum ERROR_TYPES: String {
  case API_CLIENT_INITIALIZATION_ERROR = "API_CLIENT_INITIALIZATION_ERROR"
  case TOKENIZE_VAULT_PAYMENT_ERROR = "TOKENIZE_VAULT_PAYMENT_ERROR"
  case USER_CANCEL_TRANSACTION_ERROR = "USER_CANCEL_TRANSACTION_ERROR"
  case PAYPAL_DISABLED_IN_CONFIGURATION_ERROR = "PAYPAL_DISABLED_IN_CONFIGURATION_ERROR"
  case DATA_COLLECTOR_ERROR = "DATA_COLLECTOR_ERROR"
  case CARD_TOKENIZATION_ERROR = "CARD_TOKENIZATION_ERROR"
  case APPLE_PAY_NOT_AVAILABLE = "APPLE_PAY_NOT_AVAILABLE"
  case APPLE_PAY_TOKENIZATION_ERROR = "APPLE_PAY_TOKENIZATION_ERROR"
}

@objc(ExpoBraintree)
class ExpoBraintree: NSObject {

  @objc(requestBillingAgreement:withResolver:withRejecter:)
  func requestBillingAgreement(
    options: [String: String], resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let clientToken = options["clientToken"] ?? ""
    // Step 1: Initialize Braintree API Client
    let apiClientOptional = BTAPIClient(authorization: clientToken)
    guard let apiClient = apiClientOptional else {
      return reject(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        NSError(domain: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue, code: -1))
    }
    // Step 2: Initialize BPayPal API Client
    let payPalClient = BTPayPalClient(apiClient: apiClient)
    let vaultRequest = prepareBTPayPalVaultRequest(options: options)
    payPalClient.tokenize(vaultRequest) {
      (accountNonce, error) -> Void in
      if let accountNonce = accountNonce {
        // Step 3: Handle Success: Paypal Nonce Created resolved
        return resolve(
          prepareBTPayPalAccountNonceResult(
            accountNonce: accountNonce
          ))
      } else if let error = error as? BTPayPalError {
        // Step 3: Handle Error: Tokenize error
        switch error.errorCode {
        case BTPayPalError.disabled.errorCode:
          return reject(
            EXCEPTION_TYPES.PAYPAL_DISABLED_IN_CONFIGURATION.rawValue,
            ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.rawValue,
            NSError(
              domain: ERROR_TYPES.PAYPAL_DISABLED_IN_CONFIGURATION_ERROR.rawValue,
              code: BTPayPalError.disabled.errorCode)
          )
        case BTPayPalError.canceled.errorCode:
          return reject(
            EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.rawValue,
            ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.rawValue,
            NSError(
              domain: ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.rawValue,
              code: BTPayPalError.canceled.errorCode)
          )
        default:
          return reject(
            EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
            ERROR_TYPES.TOKENIZE_VAULT_PAYMENT_ERROR.rawValue,
            NSError(
              domain: error.localizedDescription,
              code: -1
            )
          )
        }
      }
    }
  }

  @objc(requestOneTimePayment:withResolver:withRejecter:)
  func requestOneTimePayment(
    options: [String: String], resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let clientToken = options["clientToken"] ?? ""
    // Step 1: Initialize Braintree API Client
    let apiClientOptional = BTAPIClient(authorization: clientToken)
    guard let apiClient = apiClientOptional else {
      return reject(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        NSError(domain: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue, code: -1))
    }
    // Step 2: Initialize BPayPal API Client
    let payPalClient = BTPayPalClient(apiClient: apiClient)
    let checkoutRequest = prepareBTPayPalCheckoutRequest(options: options)
    payPalClient.tokenize(checkoutRequest) {
      (accountNonce, error) -> Void in
      if let accountNonce = accountNonce {
        // Step 3: Handle Success: Paypal Nonce Created resolved
        return resolve(
          prepareBTPayPalAccountNonceResult(
            accountNonce: accountNonce
          ))
      } else if let error = error as? BTPayPalError {
        // Step 3: Handle Error: Tokenize error
        switch error.errorCode {
        case BTPayPalError.disabled.errorCode:
          return reject(
            EXCEPTION_TYPES.PAYPAL_DISABLED_IN_CONFIGURATION.rawValue,
            ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.rawValue,
            NSError(
              domain: ERROR_TYPES.PAYPAL_DISABLED_IN_CONFIGURATION_ERROR.rawValue,
              code: BTPayPalError.disabled.errorCode)
          )
        case BTPayPalError.canceled.errorCode:
          return reject(
            EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.rawValue,
            ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.rawValue,
            NSError(
              domain: ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.rawValue,
              code: BTPayPalError.canceled.errorCode)
          )
        default:
          return reject(
            EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
            ERROR_TYPES.TOKENIZE_VAULT_PAYMENT_ERROR.rawValue,
            NSError(
              domain: error.localizedDescription,
              code: -1
            )
          )
        }
      }
    }
  }

  @objc(getDeviceDataFromDataCollector:withResolver:withRejecter:)
  func getDeviceDataFromDataCollector(
    clientToken: String, resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    // Step 1: Initialize Braintree API Client
    let apiClientOptional = BTAPIClient(authorization: clientToken)
    guard let apiClient = apiClientOptional else {
      return reject(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        NSError(domain: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue, code: -1))
    }
    // Step 2: Initialize DataCollerctor
    let dataCollector = BTDataCollector(apiClient: apiClient)
    // Step 3: Try To Collect Device Data and make a corelation Id if that is possible
    dataCollector.collectDeviceData { corelationId, dataCollectorError in
      if let corelationId = corelationId {
        // Step 4: Return corelation id
        return resolve(corelationId)
      } else if let dataCollectorError = dataCollectorError {
        // Step 4: Handle Error: DataCollector error
        return reject(
          EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
          ERROR_TYPES.DATA_COLLECTOR_ERROR.rawValue,
          NSError(
            domain: ERROR_TYPES.DATA_COLLECTOR_ERROR.rawValue,
            code: -1)
        )
      }
    }
  }

  @objc(tokenizeCardData:withResolver:withRejecter:)
  func tokenizeCardData(
    options: [String: String], resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let clientToken = options["clientToken"] ?? ""
    // Step 1: Initialize Braintree API Client
    let apiClientOptional = BTAPIClient(authorization: clientToken)
    guard let apiClient = apiClientOptional else {
      return reject(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        NSError(domain: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue, code: -1))
    }
    // Step 2: Initialize DataCollerctor
    let cardClient = BTCardClient(apiClient: apiClient)
    let card = prepareCardData(options: options)
    // Step 3: Try To Collect Device Data and make a corelation Id if that is possible
    cardClient.tokenize(card) {
      (cardNonce, error) -> Void in
      if let cardNonce = cardNonce {
        // Step 4: Return corelation id
        return resolve(prepareBTCardNonceResult(cardNonce: cardNonce))
      } else if let error = error {
        // Step 4: Handle Error: DataCollector error
        return reject(
          EXCEPTION_TYPES.TOKENIZE_EXCEPTION.rawValue,
          ERROR_TYPES.CARD_TOKENIZATION_ERROR.rawValue,
          NSError(
            domain: ERROR_TYPES.CARD_TOKENIZATION_ERROR.rawValue,
            code: -1)
        )
      }
    }
  }

  // MARK: - Apple Pay Methods

  @objc(isApplePayAvailable:withRejecter:)
  func isApplePayAvailable(
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let available = BTApplePayHelper.isApplePayAvailable()
    resolve(available)
  }

  @objc(canMakeApplePayPayments:withResolver:withRejecter:)
  func canMakeApplePayPayments(
    options: [String: Any]?,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    var networks: [PKPaymentNetwork] = [.visa, .masterCard, .amex, .discover]

    if let options = options, let requestedNetworks = options["networks"] as? [String] {
      networks = []
      for network in requestedNetworks {
        switch network.lowercased() {
        case "visa":
          networks.append(.visa)
        case "mastercard":
          networks.append(.masterCard)
        case "amex", "americanexpress":
          networks.append(.amex)
        case "discover":
          networks.append(.discover)
        default:
          break
        }
      }
    }

    let canMakePayments = BTApplePayHelper.canMakePayments(usingNetworks: networks)
    resolve(canMakePayments)
  }

  @objc(presentApplePaymentSheet:withResolver:withRejecter:)
  func presentApplePaymentSheet(
    options: [String: Any],
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    // Check if Apple Pay is available
    guard BTApplePayHelper.isApplePayAvailable() else {
      return reject(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        ERROR_TYPES.APPLE_PAY_NOT_AVAILABLE.rawValue,
        NSError(domain: ERROR_TYPES.APPLE_PAY_NOT_AVAILABLE.rawValue, code: -1)
      )
    }

    // Store the client token for later use
    self.applePayClientToken = options["clientToken"] as? String

    // Prepare payment request
    let paymentRequest = BTApplePayHelper.preparePaymentRequest(options: options)

    // Create payment authorization view controller
    guard let paymentAuthorizationViewController = PKPaymentAuthorizationViewController(paymentRequest: paymentRequest) else {
      return reject(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        ERROR_TYPES.APPLE_PAY_NOT_AVAILABLE.rawValue,
        NSError(domain: ERROR_TYPES.APPLE_PAY_NOT_AVAILABLE.rawValue, code: -1)
      )
    }

    // Store the promise callbacks to use them in the delegate methods
    self.applePayResolve = resolve
    self.applePayReject = reject

    paymentAuthorizationViewController.delegate = self

    // Present the payment sheet
    DispatchQueue.main.async {
      if let rootViewController = UIApplication.shared.delegate?.window??.rootViewController {
        rootViewController.present(paymentAuthorizationViewController, animated: true, completion: nil)
      } else {
        reject(
          EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
          "NO_ROOT_VIEW_CONTROLLER",
          NSError(domain: "NO_ROOT_VIEW_CONTROLLER", code: -1)
        )
      }
    }
  }

  @objc(tokenizeApplePayPayment:withResolver:withRejecter:)
  func tokenizeApplePayPayment(
    options: [String: Any],
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let clientToken = options["clientToken"] as? String ?? ""

    // Step 1: Initialize Braintree API Client
    let apiClientOptional = BTAPIClient(authorization: clientToken)
    guard let apiClient = apiClientOptional else {
      return reject(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        NSError(domain: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue, code: -1)
      )
    }

    // This method would be called after receiving PKPayment from Apple Pay sheet
    // For now, we'll return an error since we can't directly tokenize without PKPayment
    reject(
      EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
      "DIRECT_TOKENIZATION_NOT_SUPPORTED",
      NSError(domain: "Use presentApplePaymentSheet instead", code: -1)
    )
  }

  // Properties to store promise callbacks for Apple Pay
  private var applePayResolve: RCTPromiseResolveBlock?
  private var applePayReject: RCTPromiseRejectBlock?

}

// MARK: - PKPaymentAuthorizationViewControllerDelegate

extension ExpoBraintree: PKPaymentAuthorizationViewControllerDelegate {

  func paymentAuthorizationViewController(
    _ controller: PKPaymentAuthorizationViewController,
    didAuthorizePayment payment: PKPayment,
    handler completion: @escaping (PKPaymentAuthorizationResult) -> Void
  ) {
    // Get the client token from stored options or use a method to retrieve it
    guard let clientToken = self.applePayClientToken else {
      let result = PKPaymentAuthorizationResult(status: .failure, errors: nil)
      completion(result)
      self.applePayReject?(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        NSError(domain: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue, code: -1)
      )
      return
    }

    let apiClientOptional = BTAPIClient(authorization: clientToken)
    guard let apiClient = apiClientOptional else {
      let result = PKPaymentAuthorizationResult(status: .failure, errors: nil)
      completion(result)
      self.applePayReject?(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        NSError(domain: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue, code: -1)
      )
      return
    }

    BTApplePayHelper.tokenizeApplePayment(apiClient: apiClient, payment: payment) { [weak self] (response, error) in
      if let error = error {
        let result = PKPaymentAuthorizationResult(status: .failure, errors: nil)
        completion(result)
        self?.applePayReject?(
          EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
          ERROR_TYPES.APPLE_PAY_TOKENIZATION_ERROR.rawValue,
          error
        )
      } else if let response = response {
        let result = PKPaymentAuthorizationResult(status: .success, errors: nil)
        completion(result)
        self?.applePayResolve?(response)
      } else {
        let result = PKPaymentAuthorizationResult(status: .failure, errors: nil)
        completion(result)
        self?.applePayReject?(
          EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
          ERROR_TYPES.APPLE_PAY_TOKENIZATION_ERROR.rawValue,
          NSError(domain: ERROR_TYPES.APPLE_PAY_TOKENIZATION_ERROR.rawValue, code: -1)
        )
      }
    }
  }

  func paymentAuthorizationViewControllerDidFinish(_ controller: PKPaymentAuthorizationViewController) {
    controller.dismiss(animated: true) {
      // If we haven't resolved yet, it means the user cancelled
      if self.applePayResolve != nil {
        self.applePayReject?(
          EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.rawValue,
          ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.rawValue,
          NSError(domain: ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.rawValue, code: -1)
        )
      }
      // Clear the callbacks
      self.applePayResolve = nil
      self.applePayReject = nil
      self.applePayClientToken = nil
    }
  }

  // Property to store client token for Apple Pay
  private var applePayClientToken: String?

}
