//
//  CardPaymentExecutor.swift
//  expo-braintree
//
//  Card payment executor with 3DS support
//

import Braintree
import Foundation
import UIKit

class CardPaymentExecutor: NSObject, BasePaymentExecutor, BTThreeDSecureRequestDelegate {
  let args: BasePaymentArgs
  weak var listener: PaymentExecutorListener?
  private var cardClient: BTCardClient?
  private var threeDSecureClient: BTThreeDSecureClient?
  private var currentCardNonce: String?
  private weak var viewController: UIViewController?

  init(args: BasePaymentArgs, listener: PaymentExecutorListener?) {
    self.args = args
    self.listener = listener
    super.init()
  }

  func requestPayment(viewController: UIViewController) {
    self.viewController = viewController

    guard case .card(
      let cardNumber,
      let expirationMonth,
      let expirationYear,
      let cvv,
      let postalCode,
      let use3DSecure,
      _,
      _,
      _
    ) = args.paymentMethod else {
      handleError(message: "Invalid payment method", localizedMessage: "Expected Card")
      return
    }

    guard let apiClient = BTAPIClient(authorization: args.clientToken) else {
      handleError(
        message: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        localizedMessage: "Failed to initialize Braintree API client"
      )
      return
    }

    cardClient = BTCardClient(apiClient: apiClient)

    if use3DSecure {
      threeDSecureClient = BTThreeDSecureClient(apiClient: apiClient)
    }

    let card = BTCard()
    card.number = cardNumber
    card.expirationMonth = expirationMonth
    card.expirationYear = expirationYear
    if let cvvValue = cvv, !cvvValue.isEmpty {
      card.cvv = cvvValue
    }
    if let postalCodeValue = postalCode, !postalCodeValue.isEmpty {
      card.postalCode = postalCodeValue
    }
    card.shouldValidate = false

    cardClient?.tokenize(card) { [weak self] (cardNonce, error) in
      guard let self = self else { return }

      if let cardNonce = cardNonce {
        if use3DSecure {
          self.currentCardNonce = cardNonce.nonce
          self.requestThreeDSecure(cardNonce: cardNonce)
        } else {
          self.handleSuccessWithCardNonce(cardNonce: cardNonce)
        }
      } else if let error = error {
        self.handleError(
          message: ERROR_TYPES.CARD_TOKENIZATION_ERROR.rawValue,
          localizedMessage: error.localizedDescription
        )
      }
    }
  }

  private func requestThreeDSecure(cardNonce: BTCardNonce) {
    guard case .card(_, _, _, _, _, _, let amount, _, let requestorAppURL) = args.paymentMethod else {
      handleError(message: "Invalid payment method", localizedMessage: "Expected Card")
      return
    }

    guard let secureClient = threeDSecureClient else {
      handleError(
        message: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        localizedMessage: "3D Secure client not initialized"
      )
      return
    }

    let threeDSecureRequest = BTThreeDSecureRequest()
    let formatter = NumberFormatter()
    formatter.generatesDecimalNumbers = true
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.numberStyle = .decimal

    threeDSecureRequest.amount = formatter.number(from: amount) as? NSDecimalNumber ?? 0
    threeDSecureRequest.nonce = cardNonce.nonce
    threeDSecureRequest.email = args.email
    threeDSecureRequest.threeDSecureRequestDelegate = self
    threeDSecureRequest.requestorAppURL = requestorAppURL

    secureClient.startPaymentFlow(threeDSecureRequest) { [weak self] (threeDSecureResult, error) in
      guard let self = self else { return }

      if let threeDSecureResult = threeDSecureResult,
         let tokenizedCard = threeDSecureResult.tokenizedCard {

        let threeDSecureInfo = tokenizedCard.threeDSecureInfo

        if !threeDSecureInfo.liabilityShiftPossible {
          self.handleError(
            message: ERROR_TYPES.THREE_D_SECURE_NOT_ABLE_TO_SHIFT_LIABILITY.rawValue,
            localizedMessage: "3D Secure liability shift not possible"
          )
          return
        }

        if !threeDSecureInfo.liabilityShifted {
          self.handleError(
            message: ERROR_TYPES.THREE_D_SECURE_LIABILITY_NOT_SHIFTED.rawValue,
            localizedMessage: "3D Secure liability not shifted"
          )
          return
        }

        self.handleSuccessWithCardNonce(cardNonce: tokenizedCard)
      } else if let error = error {
        if let threeDSecureError = error as? BTThreeDSecureError, threeDSecureError == .canceled {
          self.handleCancel()
        } else if (error as NSError).code == BTThreeDSecureError.canceled.errorCode {
          self.handleCancel()
        } else {
          self.handleError(
            message: ERROR_TYPES.THREE_D_SECURE_AUTHENTICATION_FAILED.rawValue,
            localizedMessage: error.localizedDescription
          )
        }
      } else {
        self.handleError(
          message: ERROR_TYPES.THREE_D_SECURE_VERIFICATION_FAILED.rawValue,
          localizedMessage: "3D Secure verification failed"
        )
      }
    }
  }

  private func handleSuccessWithCardNonce(cardNonce: BTCardNonce) {
    guard case .card(_, _, _, _, _, _, let amount, let currency, _) = args.paymentMethod else {
      handleError(message: "Invalid payment method", localizedMessage: "Expected Card")
      return
    }

    let additionalData = prepareBTCardNonceResult(cardNonce: cardNonce) as? [String: Any]

    handleSuccess(
      nonce: cardNonce.nonce,
      amount: amount,
      currency: currency.isEmpty ? "USD" : currency,
      paymentType: "Card",
      additionalData: additionalData
    )
  }

  func onLookupComplete(_ request: BTThreeDSecureRequest, lookupResult: BTThreeDSecureResult, next: @escaping () -> Void) {
    next()
  }

  func onResume() {
  }

  func onDestroy() {
    cardClient = nil
    threeDSecureClient = nil
    currentCardNonce = nil
  }
}
