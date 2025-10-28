//
//  PaymentResultModel.swift
//  expo-braintree
//
//  Result models for payment operations
//

import Foundation

enum PaymentResultModel {
  case success(PaymentSuccessResult)
  case error(PaymentErrorResult)
  case cancel
}

struct PaymentSuccessResult {
  let nonce: String
  let amount: String
  let currency: String
  let deviceData: String
  let email: String
  let paymentType: String
  let additionalData: [String: Any]?

  init(
    nonce: String,
    amount: String = "0",
    currency: String = "USD",
    deviceData: String = "",
    email: String = "",
    paymentType: String,
    additionalData: [String: Any]? = nil
  ) {
    self.nonce = nonce
    self.amount = amount
    self.currency = currency
    self.deviceData = deviceData
    self.email = email
    self.paymentType = paymentType
    self.additionalData = additionalData
  }

  func toNSDictionary() -> NSDictionary {
    let result = NSMutableDictionary()
    result["nonce"] = nonce
    result["deviceData"] = deviceData

    if !email.isEmpty {
      result["email"] = email
    }

    if let additional = additionalData {
      for (key, value) in additional {
        result[key] = value
      }
    }

    return result
  }
}

struct PaymentErrorResult {
  let message: String?
  let localizedMessage: String?

  init(message: String?, localizedMessage: String? = nil) {
    self.message = message
    self.localizedMessage = localizedMessage
  }
}
