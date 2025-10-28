//
//  BasePaymentExecutor.swift
//  expo-braintree
//
//  Base protocol for payment executors
//

import Foundation
import UIKit

protocol PaymentExecutorListener: AnyObject {
  func onPaymentResult(_ result: PaymentResultModel)
}

protocol BasePaymentExecutor {
  var args: BasePaymentArgs { get }
  var listener: PaymentExecutorListener? { get }

  func requestPayment(viewController: UIViewController)
  func onResume()
  func onDestroy()
}

extension BasePaymentExecutor {
  func handleError(message: String?, localizedMessage: String? = nil) {
    listener?.onPaymentResult(.error(PaymentErrorResult(
      message: message,
      localizedMessage: localizedMessage
    )))
  }

  func handleCancel() {
    listener?.onPaymentResult(.cancel)
  }

  func handleSuccess(
    nonce: String,
    amount: String = "0",
    currency: String = "USD",
    paymentType: String,
    additionalData: [String: Any]? = nil
  ) {
    listener?.onPaymentResult(.success(PaymentSuccessResult(
      nonce: nonce,
      amount: amount,
      currency: currency,
      deviceData: args.deviceData,
      email: args.email,
      paymentType: paymentType,
      additionalData: additionalData
    )))
  }
}
