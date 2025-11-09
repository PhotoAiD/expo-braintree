//
//  PayPalPaymentExecutor.swift
//  expo-braintree
//
//  PayPal payment executor
//

import Braintree
import Foundation
import UIKit

class PayPalPaymentExecutor: BasePaymentExecutor {
  let args: BasePaymentArgs
  weak var listener: PaymentExecutorListener?
  private let isVault: Bool
  private var payPalClient: BTPayPalClient?

  init(args: BasePaymentArgs, listener: PaymentExecutorListener?, isVault: Bool) {
    self.args = args
    self.listener = listener
    self.isVault = isVault
  }

  func requestPayment(viewController: UIViewController) {
    guard let apiClient = BTAPIClient(authorization: args.clientToken) else {
      handleError(
        message: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        localizedMessage: "Failed to initialize Braintree API client"
      )
      return
    }

    payPalClient = BTPayPalClient(apiClient: apiClient)

    if isVault {
      requestVaultPayment()
    } else {
      requestCheckoutPayment()
    }
  }

  private func requestVaultPayment() {
    guard case .payPalVault(let billingAgreementDescription) = args.paymentMethod else {
      handleError(message: "Invalid payment method", localizedMessage: "Expected PayPalVault")
      return
    }

    let vaultRequest = BTPayPalVaultRequest()
    if let description = billingAgreementDescription, !description.isEmpty {
      vaultRequest.billingAgreementDescription = description
    }

    payPalClient?.tokenize(vaultRequest) { [weak self] (accountNonce, error) in
      self?.handleTokenizeResult(accountNonce: accountNonce, error: error)
    }
  }

  private func requestCheckoutPayment() {
    guard case .payPalCheckout(let amount, let currency) = args.paymentMethod else {
      handleError(message: "Invalid payment method", localizedMessage: "Expected PayPalCheckout")
      return
    }

    let checkoutRequest = BTPayPalCheckoutRequest(amount: amount)
    checkoutRequest.currencyCode = currency

    payPalClient?.tokenize(checkoutRequest) { [weak self] (accountNonce, error) in
      self?.handleTokenizeResult(accountNonce: accountNonce, error: error)
    }
  }

  private func handleTokenizeResult(accountNonce: BTPayPalAccountNonce?, error: Error?) {
    if let accountNonce = accountNonce {
      let additionalData = prepareBTPayPalAccountNonceResult(accountNonce: accountNonce) as? [String: Any]

      let amount: String
      let currency: String

      switch args.paymentMethod {
      case .payPalCheckout(let amt, let curr):
        amount = amt
        currency = curr
      default:
        amount = "0"
        currency = "USD"
      }

      handleSuccess(
        nonce: accountNonce.nonce,
        amount: amount,
        currency: currency,
        paymentType: "PayPal",
        additionalData: additionalData
      )
    } else if let error = error as? BTPayPalError {
      switch error.errorCode {
      case BTPayPalError.disabled.errorCode:
        handleError(
          message: ERROR_TYPES.PAYPAL_DISABLED_IN_CONFIGURATION_ERROR.rawValue,
          localizedMessage: "PayPal is disabled in configuration"
        )
      case BTPayPalError.canceled.errorCode:
        handleCancel()
      default:
        handleError(
          message: ERROR_TYPES.TOKENIZE_VAULT_PAYMENT_ERROR.rawValue,
          localizedMessage: error.localizedDescription
        )
      }
    } else {
      handleError(
        message: ERROR_TYPES.TOKENIZE_VAULT_PAYMENT_ERROR.rawValue,
        localizedMessage: "Unknown error occurred"
      )
    }
  }

  func onResume() {
  }

  func onDestroy() {
    payPalClient = nil
  }
}
