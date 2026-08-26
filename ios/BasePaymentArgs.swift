//
//  BasePaymentArgs.swift
//  expo-braintree
//
//  Payment arguments data model
//

import Foundation

enum PaymentMethod {
  case payPalVault(billingAgreementDescription: String?)
  case payPalCheckout(
    amount: String,
    currency: String,
    intent: String?,
    userAction: String?,
    offerPayLater: Bool,
    requestBillingAgreement: Bool,
    isShippingAddressRequired: Bool,
    isShippingAddressEditable: Bool,
    shippingCallbackUrl: String?
  )
  case card(
    cardNumber: String,
    expirationMonth: String,
    expirationYear: String,
    cvv: String?,
    postalCode: String?,
    use3DSecure: Bool,
    amount: String,
    currency: String,
    requestorAppURL: String?
  )
  case applePay(options: [String: Any])
  case threeDSecure(nonce: String, amount: String, options: [String: Any])
}

struct BasePaymentArgs {
  let clientToken: String
  let paymentMethod: PaymentMethod
  let email: String
  let deviceData: String

  init(
    clientToken: String,
    paymentMethod: PaymentMethod,
    email: String = "",
    deviceData: String = ""
  ) {
    self.clientToken = clientToken
    self.paymentMethod = paymentMethod
    self.email = email
    self.deviceData = deviceData
  }
}
