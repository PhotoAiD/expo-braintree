//
//  ThreeDSecureExecutor.swift
//  expo-braintree
//
//  Standalone 3D Secure verification executor
//

import Braintree
import Foundation
import UIKit

class ThreeDSecureExecutor: NSObject, BasePaymentExecutor, BTThreeDSecureRequestDelegate {
  let args: BasePaymentArgs
  weak var listener: PaymentExecutorListener?
  private var threeDSecureClient: BTThreeDSecureClient?
  private weak var viewController: UIViewController?

  init(args: BasePaymentArgs, listener: PaymentExecutorListener?) {
    self.args = args
    self.listener = listener
    super.init()
  }

  func requestPayment(viewController: UIViewController) {
    self.viewController = viewController

    guard case .threeDSecure(let nonce, let amount, let options) = args.paymentMethod else {
      handleError(message: "Invalid payment method", localizedMessage: "Expected ThreeDSecure")
      return
    }

    guard let apiClient = BTAPIClient(authorization: args.clientToken) else {
      handleError(
        message: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        localizedMessage: "Failed to initialize Braintree API client"
      )
      return
    }

    threeDSecureClient = BTThreeDSecureClient(apiClient: apiClient)

    guard let secureClient = threeDSecureClient else {
      handleError(
        message: ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.rawValue,
        localizedMessage: "Failed to initialize 3D Secure client"
      )
      return
    }

    var requestOptions = options
    requestOptions["nonce"] = nonce
    requestOptions["amount"] = amount
    requestOptions["email"] = args.email

    let threeDSecureRequest = prepareThreeDSecureRequest(options: requestOptions)
    threeDSecureRequest.threeDSecureRequestDelegate = self

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

        let additionalData = prepareThreeDSecureNonceResult(nonce: threeDSecureResult) as? [String: Any]

        self.handleSuccess(
          nonce: tokenizedCard.nonce,
          amount: amount,
          currency: "USD",
          paymentType: "Card3DS",
          additionalData: additionalData
        )
      } else if let error = error {
        self.handleError(
          message: ERROR_TYPES.THREE_D_SECURE_AUTHENTICATION_FAILED.rawValue,
          localizedMessage: error.localizedDescription
        )
      } else {
        self.handleError(
          message: ERROR_TYPES.THREE_D_SECURE_VERIFICATION_FAILED.rawValue,
          localizedMessage: "3D Secure verification failed"
        )
      }
    }
  }

  func onLookupComplete(_ request: BTThreeDSecureRequest, lookupResult: BTThreeDSecureResult, next: @escaping () -> Void) {
    next()
  }

  func onResume() {
  }

  func onDestroy() {
    threeDSecureClient = nil
  }
}
