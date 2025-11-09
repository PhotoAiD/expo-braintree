//
//  ExpoBraintreeRefactored.swift
//  expo-braintree
//
//  Refactored version using executor pattern
//

import Braintree
import Foundation
import PassKit
import React

@objc(ExpoBraintree)
class ExpoBraintree: NSObject, PaymentExecutorListener {
  private var currentExecutor: BasePaymentExecutor?
  private var currentResolve: RCTPromiseResolveBlock?
  private var currentReject: RCTPromiseRejectBlock?

  @objc(requestBillingAgreement:withResolver:withRejecter:)
  func requestBillingAgreement(
    options: [String: Any],
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let clientToken = options["clientToken"] as? String ?? ""
    let billingAgreementDescription = options["billingAgreementDescription"] as? String

    let args = BasePaymentArgs(
      clientToken: clientToken,
      paymentMethod: .payPalVault(billingAgreementDescription: billingAgreementDescription),
      email: options["email"] as? String ?? "",
      deviceData: options["deviceData"] as? String ?? ""
    )

    executePayment(args: args, resolve: resolve, reject: reject)
  }

  @objc(requestOneTimePayment:withResolver:withRejecter:)
  func requestOneTimePayment(
    options: [String: Any],
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let clientToken = options["clientToken"] as? String ?? ""
    let amount = options["amount"] as? String ?? ""
    let currency = options["currencyCode"] as? String ?? "USD"

    let args = BasePaymentArgs(
      clientToken: clientToken,
      paymentMethod: .payPalCheckout(amount: amount, currency: currency),
      email: options["email"] as? String ?? "",
      deviceData: options["deviceData"] as? String ?? ""
    )

    executePayment(args: args, resolve: resolve, reject: reject)
  }

  @objc(getDeviceDataFromDataCollector:withResolver:withRejecter:)
  func getDeviceDataFromDataCollector(
    clientToken: String,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    guard let apiClient = BTAPIClient(authorization: clientToken) else {
      return reject(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        NSError(domain: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue, code: -1)
      )
    }

    let dataCollector = BTDataCollector(apiClient: apiClient)

    dataCollector.collectDeviceData { correlationId, dataCollectorError in
      if let correlationId = correlationId {
        return resolve(correlationId)
      } else {
        return reject(
          EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
          ERROR_TYPES.DATA_COLLECTOR_ERROR.rawValue,
          NSError(domain: ERROR_TYPES.DATA_COLLECTOR_ERROR.rawValue, code: -1)
        )
      }
    }
  }

  @objc(tokenizeCardData:withResolver:withRejecter:)
  func tokenizeCardData(
    options: [String: Any],
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let clientToken = options["clientToken"] as? String ?? ""
    let cardData = options["card"] as? [String: Any] ?? [:]
    let cardNumber = cardData["number"] as? String ?? ""
    let expirationMonth = cardData["expirationMonth"] as? String ?? ""
    let expirationYear = cardData["expirationYear"] as? String ?? ""
    let cvv = cardData["cvv"] as? String
    let postalCode = cardData["postalCode"] as? String
    let use3DSecure = options["use3DSecure"] as? Bool ?? false
    let amount = options["amount"] as? String ?? "0"
    let currency = options["currency"] as? String ?? "USD"

    let args = BasePaymentArgs(
      clientToken: clientToken,
      paymentMethod: .card(
        cardNumber: cardNumber,
        expirationMonth: expirationMonth,
        expirationYear: expirationYear,
        cvv: cvv,
        postalCode: postalCode,
        use3DSecure: use3DSecure,
        amount: amount,
        currency: currency
      ),
      email: options["email"] as? String ?? "",
      deviceData: options["deviceData"] as? String ?? ""
    )

    executePayment(args: args, resolve: resolve, reject: reject)
  }

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
    guard BTApplePayHelper.isApplePayAvailable() else {
      return reject(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        ERROR_TYPES.APPLE_PAY_NOT_AVAILABLE.rawValue,
        NSError(domain: ERROR_TYPES.APPLE_PAY_NOT_AVAILABLE.rawValue, code: -1)
      )
    }

    let clientToken = options["clientToken"] as? String ?? ""

    let args = BasePaymentArgs(
      clientToken: clientToken,
      paymentMethod: .applePay(options: options),
      email: options["email"] as? String ?? "",
      deviceData: options["deviceData"] as? String ?? ""
    )

    executePayment(args: args, resolve: resolve, reject: reject)
  }

  @objc(verifyThreeDSecure:withResolver:withRejecter:)
  func verifyThreeDSecure(
    options: [String: Any],
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let clientToken = options["clientToken"] as? String ?? ""
    let nonce = options["nonce"] as? String ?? ""
    let amount = options["amount"] as? String ?? ""

    let args = BasePaymentArgs(
      clientToken: clientToken,
      paymentMethod: .threeDSecure(nonce: nonce, amount: amount, options: options),
      email: options["email"] as? String ?? "",
      deviceData: options["deviceData"] as? String ?? ""
    )

    executePayment(args: args, resolve: resolve, reject: reject)
  }

  private func executePayment(
    args: BasePaymentArgs,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    currentResolve = resolve
    currentReject = reject

    let executor: BasePaymentExecutor

    switch args.paymentMethod {
    case .payPalVault:
      executor = PayPalPaymentExecutor(args: args, listener: self, isVault: true)
    case .payPalCheckout:
      executor = PayPalPaymentExecutor(args: args, listener: self, isVault: false)
    case .card:
      executor = CardPaymentExecutor(args: args, listener: self)
    case .applePay:
      executor = ApplePayPaymentExecutor(args: args, listener: self)
    case .threeDSecure:
      executor = ThreeDSecureExecutor(args: args, listener: self)
    }

    currentExecutor = executor

    DispatchQueue.main.async {
      guard let rootViewController = UIApplication.shared.delegate?.window??.rootViewController else {
        reject(
          EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
          "NO_ROOT_VIEW_CONTROLLER",
          NSError(domain: "NO_ROOT_VIEW_CONTROLLER", code: -1)
        )
        return
      }

      executor.requestPayment(viewController: rootViewController)
    }
  }

  func onPaymentResult(_ result: PaymentResultModel) {
    defer {
      currentExecutor?.onDestroy()
      currentExecutor = nil
      currentResolve = nil
      currentReject = nil
    }

    switch result {
    case .success(let successResult):
      if successResult.paymentType == "Card", let cardData = successResult.additionalData {
        currentResolve?(cardData as NSDictionary)
      } else {
        currentResolve?(successResult.toNSDictionary())
      }

    case .error(let errorResult):
      currentReject?(
        EXCEPTION_TYPES.SWIFT_EXCEPTION.rawValue,
        errorResult.message ?? "Unknown error",
        NSError(
          domain: errorResult.message ?? "Unknown error",
          code: -1,
          userInfo: [NSLocalizedDescriptionKey: errorResult.localizedMessage ?? ""]
        )
      )

    case .cancel:
      currentReject?(
        EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.rawValue,
        ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.rawValue,
        NSError(domain: ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.rawValue, code: -1)
      )
    }
  }
}
