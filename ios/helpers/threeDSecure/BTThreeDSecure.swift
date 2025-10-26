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

  let threeDsRequestPostalAddress = BTThreeDSecurePostalAddress()
  threeDsRequestPostalAddress.givenName = options["givenName"] as? String
  threeDsRequestPostalAddress.surname = options["surName"] as? String
  threeDsRequestPostalAddress.phoneNumber = options["phoneNumber"] as? String
  threeDsRequestPostalAddress.streetAddress = options["streetAddress"] as? String
  threeDsRequestPostalAddress.extendedAddress = options["extendedAddress"] as? String
  threeDsRequestPostalAddress.locality = options["city"] as? String
  threeDsRequestPostalAddress.postalCode = options["postalCode"] as? String
  threeDsRequestPostalAddress.region = options["region"] as? String
  threeDsRequestPostalAddress.countryCodeAlpha2 = options["countryCodeAlpha2"] as? String

  let threeDsRequestAdditionalInformation = BTThreeDSecureAdditionalInformation()
  threeDsRequestAdditionalInformation.shippingAddress = threeDsRequestPostalAddress

  threeDSecureRequest.additionalInformation = threeDsRequestAdditionalInformation
  threeDSecureRequest.billingAddress = threeDsRequestPostalAddress

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
