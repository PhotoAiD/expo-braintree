import Braintree
import Foundation

func prepareThreeDSecureRequest(options: [String: Any]) -> BTThreeDSecureRequest {
  let threeDSecureRequest = BTThreeDSecureRequest()

  let formatter = NumberFormatter()
  formatter.generatesDecimalNumbers = true

  if let amountString = options["amount"] as? String {
    threeDSecureRequest.amount = formatter.number(from: amountString) as? NSDecimalNumber ?? 0
  }

  threeDSecureRequest.nonce = options["nonce"] as? String
  threeDSecureRequest.email = options["email"] as? String

  if let versionRequested = options["versionRequested"] as? String {
    if versionRequested == "2" {
      threeDSecureRequest.versionRequested = .version2
    } else if versionRequested == "1" {
      threeDSecureRequest.versionRequested = .version1
    }
  }

  if let accountType = options["accountType"] as? String {
    if accountType == "credit" {
      threeDSecureRequest.accountType = .credit
    } else if accountType == "debit" {
      threeDSecureRequest.accountType = .debit
    }
  }

  if let challengeRequested = options["challengeRequested"] as? Bool {
    threeDSecureRequest.challengeRequested = challengeRequested
  }

  if let exemptionRequested = options["exemptionRequested"] as? Bool {
    threeDSecureRequest.exemptionRequested = exemptionRequested
  }

  if let mobilePhoneNumber = options["mobilePhoneNumber"] as? String {
    threeDSecureRequest.mobilePhoneNumber = mobilePhoneNumber
  }

  if let cardAddChallenge = options["cardAddChallenge"] as? String {
    if cardAddChallenge == "requested" {
      threeDSecureRequest.cardAddChallenge = .requested
    } else if cardAddChallenge == "not_requested" {
      threeDSecureRequest.cardAddChallenge = .notRequested
    }
  }

  if let billingAddressDict = options["billingAddress"] as? [String: String] {
    let billingAddress = BTThreeDSecurePostalAddress()
    billingAddress.givenName = billingAddressDict["givenName"]
    billingAddress.surname = billingAddressDict["surname"]
    billingAddress.phoneNumber = billingAddressDict["phoneNumber"]
    billingAddress.streetAddress = billingAddressDict["streetAddress"]
    billingAddress.extendedAddress = billingAddressDict["extendedAddress"]
    billingAddress.locality = billingAddressDict["locality"]
    billingAddress.region = billingAddressDict["region"]
    billingAddress.postalCode = billingAddressDict["postalCode"]
    billingAddress.countryCodeAlpha2 = billingAddressDict["countryCodeAlpha2"]
    threeDSecureRequest.billingAddress = billingAddress
  }

  if let additionalInfoDict = options["additionalInformation"] as? [String: Any] {
    let additionalInfo = BTThreeDSecureAdditionalInformation()

    if let shippingAddressDict = additionalInfoDict["shippingAddress"] as? [String: String] {
      let shippingAddress = BTThreeDSecurePostalAddress()
      shippingAddress.givenName = shippingAddressDict["givenName"]
      shippingAddress.surname = shippingAddressDict["surname"]
      shippingAddress.phoneNumber = shippingAddressDict["phoneNumber"]
      shippingAddress.streetAddress = shippingAddressDict["streetAddress"]
      shippingAddress.extendedAddress = shippingAddressDict["extendedAddress"]
      shippingAddress.locality = shippingAddressDict["locality"]
      shippingAddress.region = shippingAddressDict["region"]
      shippingAddress.postalCode = shippingAddressDict["postalCode"]
      shippingAddress.countryCodeAlpha2 = shippingAddressDict["countryCodeAlpha2"]
      additionalInfo.shippingAddress = shippingAddress
    }

    threeDSecureRequest.additionalInformation = additionalInfo
  }

  return threeDSecureRequest
}

func prepareThreeDSecureNonceResult(nonce: BTThreeDSecureResult) -> NSDictionary {
  let result = NSMutableDictionary()

  if let tokenizedCard = nonce.tokenizedCard {
    result["nonce"] = tokenizedCard.nonce
    result["cardNetwork"] = tokenizedCard.cardNetwork
    result["lastTwo"] = tokenizedCard.lastTwo
    result["lastFour"] = tokenizedCard.lastFour
    result["expirationMonth"] = tokenizedCard.expirationMonth
    result["expirationYear"] = tokenizedCard.expirationYear
    result["bin"] = tokenizedCard.bin

    let threeDSecureInfo = NSMutableDictionary()
    threeDSecureInfo["liabilityShifted"] = tokenizedCard.threeDSecureInfo.liabilityShifted
    threeDSecureInfo["liabilityShiftPossible"] = tokenizedCard.threeDSecureInfo.liabilityShiftPossible
    threeDSecureInfo["wasVerified"] = tokenizedCard.threeDSecureInfo.wasVerified

    result["threeDSecureInfo"] = threeDSecureInfo
  }

  return result
}
