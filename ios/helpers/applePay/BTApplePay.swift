//
//  BTApplePay.swift
//  expo-braintree
//
//  Apple Pay integration for Braintree
//

import Braintree
import BraintreeApplePay
import Foundation
import PassKit

class BTApplePayHelper: NSObject {

  // Check if Apple Pay is available on the device
  @objc static func isApplePayAvailable() -> Bool {
    return PKPaymentAuthorizationViewController.canMakePayments()
  }

  // Check if the device supports specific payment networks
  @objc static func canMakePayments(usingNetworks networks: [PKPaymentNetwork]) -> Bool {
    return PKPaymentAuthorizationViewController.canMakePayments(usingNetworks: networks)
  }

  // Prepare Apple Pay payment request
  static func preparePaymentRequest(options: [String: Any]) -> PKPaymentRequest {
    let request = PKPaymentRequest()

    // Set merchant identifier
    if let merchantId = options["merchantId"] as? String {
      request.merchantIdentifier = merchantId
    }

    // Set supported networks
    request.supportedNetworks = [.visa, .masterCard, .amex, .discover]

    // Set merchant capabilities
    request.merchantCapabilities = .threeDSecure

    // Set country code
    if let countryCode = options["countryCode"] as? String {
      request.countryCode = countryCode
    } else {
      request.countryCode = "US"
    }

    // Set currency code
    if let currencyCode = options["currencyCode"] as? String {
      request.currencyCode = currencyCode
    } else {
      request.currencyCode = "USD"
    }

    // Set payment summary items
    var paymentSummaryItems: [PKPaymentSummaryItem] = []

    if let items = options["items"] as? [[String: Any]] {
      for item in items {
        if let label = item["label"] as? String,
           let amountString = item["amount"] as? String,
           let amount = NSDecimalNumber(string: amountString) as NSDecimalNumber? {
          let summaryItem = PKPaymentSummaryItem(label: label, amount: amount)
          paymentSummaryItems.append(summaryItem)
        }
      }
    }

    // Add total amount
    if let companyName = options["companyName"] as? String,
       let totalAmount = options["totalAmount"] as? String,
       let amount = NSDecimalNumber(string: totalAmount) as NSDecimalNumber? {
      let totalItem = PKPaymentSummaryItem(label: companyName, amount: amount)
      paymentSummaryItems.append(totalItem)
    }

    request.paymentSummaryItems = paymentSummaryItems

    // Set additional options
    if let requiredBillingContactFields = options["requiredBillingContactFields"] as? [String] {
      var contactFields: Set<PKContactField> = []
      for field in requiredBillingContactFields {
        switch field {
        case "postalAddress":
          contactFields.insert(.postalAddress)
        case "phone":
          contactFields.insert(.phoneNumber)
        case "email":
          contactFields.insert(.emailAddress)
        case "name":
          contactFields.insert(.name)
        default:
          break
        }
      }
      request.requiredBillingContactFields = contactFields
    }

    if let requiredShippingContactFields = options["requiredShippingContactFields"] as? [String] {
      var contactFields: Set<PKContactField> = []
      for field in requiredShippingContactFields {
        switch field {
        case "postalAddress":
          contactFields.insert(.postalAddress)
        case "phone":
          contactFields.insert(.phoneNumber)
        case "email":
          contactFields.insert(.emailAddress)
        case "name":
          contactFields.insert(.name)
        default:
          break
        }
      }
      request.requiredShippingContactFields = contactFields
    }

    return request
  }

  // Tokenize Apple Pay payment
  static func tokenizeApplePayment(
    apiClient: BTAPIClient,
    payment: PKPayment,
    completion: @escaping ([String: Any]?, Error?) -> Void
  ) {
    let applePayClient = BTApplePayClient(apiClient: apiClient)

    applePayClient.tokenize(payment) { (tokenizedApplePayNonce, error) in
      if let error = error {
        completion(nil, error)
        return
      }

      guard let nonce = tokenizedApplePayNonce else {
        completion(nil, NSError(domain: "BTApplePay", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to tokenize Apple Pay payment"]))
        return
      }

      // Prepare response
      var response: [String: Any] = [
        "nonce": nonce.nonce,
        "type": nonce.type,
        "isDefault": nonce.isDefault
      ]

      // Add bin data if available
      if let binData = nonce.binData {
        var binInfo: [String: Any] = [:]
        binInfo["prepaid"] = binData.prepaid ?? "Unknown"
        binInfo["healthcare"] = binData.healthcare ?? "Unknown"
        binInfo["debit"] = binData.debit ?? "Unknown"
        binInfo["durbinRegulated"] = binData.durbinRegulated ?? "Unknown"
        binInfo["commercial"] = binData.commercial ?? "Unknown"
        binInfo["payroll"] = binData.payroll ?? "Unknown"
        if let countryOfIssuance = binData.countryOfIssuance {
          binInfo["countryOfIssuance"] = countryOfIssuance
        }
        if let issuingBank = binData.issuingBank {
          binInfo["issuingBank"] = issuingBank
        }
        if let productId = binData.productID {
          binInfo["productId"] = productId
        }
        response["binData"] = binInfo
      }

      // Add payment information from PKPayment
      if let token = payment.token as PKPaymentToken? {
        response["paymentMethodDisplayName"] = token.paymentMethod.displayName ?? ""
        response["paymentMethodNetwork"] = token.paymentMethod.network?.rawValue ?? ""
        response["transactionIdentifier"] = token.transactionIdentifier
      }

      // Add billing contact if available
      if let billingContact = payment.billingContact {
        var contactInfo: [String: Any] = [:]
        if let name = billingContact.name {
          contactInfo["givenName"] = name.givenName ?? ""
          contactInfo["familyName"] = name.familyName ?? ""
        }
        if let emailAddress = billingContact.emailAddress {
          contactInfo["emailAddress"] = emailAddress
        }
        if let phoneNumber = billingContact.phoneNumber {
          contactInfo["phoneNumber"] = phoneNumber.stringValue
        }
        if let postalAddress = billingContact.postalAddress {
          contactInfo["street"] = postalAddress.street
          contactInfo["city"] = postalAddress.city
          contactInfo["state"] = postalAddress.state
          contactInfo["postalCode"] = postalAddress.postalCode
          contactInfo["country"] = postalAddress.country
          contactInfo["isoCountryCode"] = postalAddress.isoCountryCode
        }
        response["billingContact"] = contactInfo
      }

      // Add shipping contact if available
      if let shippingContact = payment.shippingContact {
        var contactInfo: [String: Any] = [:]
        if let name = shippingContact.name {
          contactInfo["givenName"] = name.givenName ?? ""
          contactInfo["familyName"] = name.familyName ?? ""
        }
        if let emailAddress = shippingContact.emailAddress {
          contactInfo["emailAddress"] = emailAddress
        }
        if let phoneNumber = shippingContact.phoneNumber {
          contactInfo["phoneNumber"] = phoneNumber.stringValue
        }
        if let postalAddress = shippingContact.postalAddress {
          contactInfo["street"] = postalAddress.street
          contactInfo["city"] = postalAddress.city
          contactInfo["state"] = postalAddress.state
          contactInfo["postalCode"] = postalAddress.postalCode
          contactInfo["country"] = postalAddress.country
          contactInfo["isoCountryCode"] = postalAddress.isoCountryCode
        }
        response["shippingContact"] = contactInfo
      }

      completion(response, nil)
    }
  }
}