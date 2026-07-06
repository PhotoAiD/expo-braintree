//
//  BTApplePay.swift
//  expo-braintree
//
//  Apple Pay integration for Braintree
//

import Braintree
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

    // Express checkout: selectable delivery methods. Apple Pay pre-selects the
    // first method in the array, so the default is moved to the front.
    let shippingMethods = prepareShippingMethods(options: options)
    if !shippingMethods.isEmpty {
      request.shippingMethods = shippingMethods
      request.shippingType = shippingType(from: options["shippingType"] as? String)
    }

    request.paymentSummaryItems = prepareSummaryItems(
      options: options,
      shippingMethod: shippingMethods.first
    )

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

    // Express checkout needs the shipping address to be collected
    if !shippingMethods.isEmpty {
      request.requiredShippingContactFields.insert(.postalAddress)
    }

    return request
  }

  // Build PKShippingMethods from the `shippingMethods` option. The method matching
  // `defaultShippingMethodId` is moved to the front because Apple Pay always
  // pre-selects the first method in the array.
  static func prepareShippingMethods(options: [String: Any]) -> [PKShippingMethod] {
    guard let methods = options["shippingMethods"] as? [[String: Any]] else {
      return []
    }

    var shippingMethods: [PKShippingMethod] = []
    for method in methods {
      guard let id = method["id"] as? String,
            let label = method["label"] as? String,
            let price = method["price"] as? String else {
        continue
      }
      let amount = NSDecimalNumber(string: price)
      if amount == NSDecimalNumber.notANumber {
        continue
      }
      let shippingMethod = PKShippingMethod(label: label, amount: amount)
      shippingMethod.identifier = id
      shippingMethod.detail = method["description"] as? String ?? ""
      shippingMethods.append(shippingMethod)
    }

    if let defaultId = options["defaultShippingMethodId"] as? String,
       let index = shippingMethods.firstIndex(where: { $0.identifier == defaultId }),
       index > 0 {
      let defaultMethod = shippingMethods.remove(at: index)
      shippingMethods.insert(defaultMethod, at: 0)
    }

    return shippingMethods
  }

  // Build the payment summary items, adding the selected delivery method as a
  // line item and folding its price into the grand total. Also used by the
  // executor to refresh the sheet when the user picks another method.
  static func prepareSummaryItems(
    options: [String: Any],
    shippingMethod: PKShippingMethod?
  ) -> [PKPaymentSummaryItem] {
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

    if let shippingMethod = shippingMethod {
      paymentSummaryItems.append(
        PKPaymentSummaryItem(label: shippingMethod.label, amount: shippingMethod.amount)
      )
    }

    // Total (must be the last summary item)
    if let companyName = options["companyName"] as? String,
       let totalAmount = options["totalAmount"] as? String {
      let totalItem = PKPaymentSummaryItem(
        label: companyName,
        amount: totalWithShipping(baseAmount: totalAmount, shippingMethod: shippingMethod)
      )
      paymentSummaryItems.append(totalItem)
    }

    return paymentSummaryItems
  }

  // `baseAmount + shipping price`, falling back to the base amount when there is
  // no delivery method or the base amount is not a valid decimal
  static func totalWithShipping(
    baseAmount: String,
    shippingMethod: PKShippingMethod?
  ) -> NSDecimalNumber {
    let base = NSDecimalNumber(string: baseAmount)
    guard let shippingMethod = shippingMethod, base != NSDecimalNumber.notANumber else {
      return base
    }
    return base.adding(shippingMethod.amount)
  }

  static func shippingType(from value: String?) -> PKShippingType {
    switch value {
    case "delivery":
      return .delivery
    case "storePickup":
      return .storePickup
    case "servicePickup":
      return .servicePickup
    default:
      return .shipping
    }
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

      // Add bin data
      let binData = nonce.binData
      var binInfo: [String: Any] = [:]
      binInfo["prepaid"] = binData.prepaid ?? "Unknown"
      binInfo["healthcare"] = binData.healthcare ?? "Unknown"
      binInfo["debit"] = binData.debit ?? "Unknown"
      binInfo["durbinRegulated"] = binData.durbinRegulated ?? "Unknown"
      binInfo["commercial"] = binData.commercial ?? "Unknown"
      binInfo["payroll"] = binData.payroll ?? "Unknown"
      binInfo["countryOfIssuance"] = binData.countryOfIssuance
      binInfo["issuingBank"] = binData.issuingBank
      binInfo["productId"] = binData.productID
      response["binData"] = binInfo

      // Add payment information from PKPayment
      if let token = payment.token as PKPaymentToken? {
        response["paymentMethodDisplayName"] = token.paymentMethod.displayName ?? ""
        response["paymentMethodNetwork"] = token.paymentMethod.network?.rawValue ?? ""
        response["transactionIdentifier"] = token.transactionIdentifier
      }

      // Add selected delivery method (express checkout)
      if let shippingMethodId = payment.shippingMethod?.identifier {
        response["shippingMethodId"] = shippingMethodId
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