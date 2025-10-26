package com.expobraintree

import com.braintreepayments.api.threedsecure.ThreeDSecureAdditionalInformation
import com.braintreepayments.api.threedsecure.ThreeDSecurePostalAddress
import com.braintreepayments.api.threedsecure.ThreeDSecureRequest
import com.braintreepayments.api.threedsecure.ThreeDSecureV2ButtonType
import com.braintreepayments.api.threedsecure.ThreeDSecureV2UiCustomization
import com.braintreepayments.api.card.CardNonce
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

object ThreeDSecureDataConverter {

  fun createThreeDSecureRequest(data: ReadableMap): ThreeDSecureRequest {
    val amount = data.getString("amount") ?: ""
    val nonce = data.getString("nonce") ?: ""
    val email = data.getString("email")

    val request = ThreeDSecureRequest()
    request.amount = amount
    request.nonce = nonce
    request.email = email

    if (data.hasKey("accountType")) {
      val accountTypeStr = data.getString("accountType")
      request.accountType = when (accountTypeStr) {
        "CREDIT" -> com.braintreepayments.api.threedsecure.ThreeDSecureAccountType.CREDIT
        "DEBIT" -> com.braintreepayments.api.threedsecure.ThreeDSecureAccountType.DEBIT
        else -> com.braintreepayments.api.threedsecure.ThreeDSecureAccountType.UNSPECIFIED
      }
    }

    if (data.hasKey("challengeRequested")) {
      request.challengeRequested = data.getBoolean("challengeRequested")
    }

    if (data.hasKey("exemptionRequested")) {
      request.exemptionRequested = data.getBoolean("exemptionRequested")
    }

    if (data.hasKey("mobilePhoneNumber")) {
      request.mobilePhoneNumber = data.getString("mobilePhoneNumber")
    }

    if (data.hasKey("billingAddress")) {
      val billingAddressMap = data.getMap("billingAddress")
      if (billingAddressMap != null) {
        val billingAddress = ThreeDSecurePostalAddress()
        billingAddress.givenName = billingAddressMap.getString("givenName")
        billingAddress.surname = billingAddressMap.getString("surname")
        billingAddress.phoneNumber = billingAddressMap.getString("phoneNumber")
        billingAddress.streetAddress = billingAddressMap.getString("streetAddress")
        billingAddress.extendedAddress = billingAddressMap.getString("extendedAddress")
        billingAddress.locality = billingAddressMap.getString("locality")
        billingAddress.region = billingAddressMap.getString("region")
        billingAddress.postalCode = billingAddressMap.getString("postalCode")
        billingAddress.countryCodeAlpha2 = billingAddressMap.getString("countryCodeAlpha2")
        request.billingAddress = billingAddress
      }
    }

    if (data.hasKey("additionalInformation")) {
      val additionalInfoMap = data.getMap("additionalInformation")
      if (additionalInfoMap != null) {
        val additionalInfo = ThreeDSecureAdditionalInformation()

        if (additionalInfoMap.hasKey("shippingAddress")) {
          val shippingAddressMap = additionalInfoMap.getMap("shippingAddress")
          if (shippingAddressMap != null) {
            val shippingAddress = ThreeDSecurePostalAddress()
            shippingAddress.givenName = shippingAddressMap.getString("givenName")
            shippingAddress.surname = shippingAddressMap.getString("surname")
            shippingAddress.phoneNumber = shippingAddressMap.getString("phoneNumber")
            shippingAddress.streetAddress = shippingAddressMap.getString("streetAddress")
            shippingAddress.extendedAddress = shippingAddressMap.getString("extendedAddress")
            shippingAddress.locality = shippingAddressMap.getString("locality")
            shippingAddress.region = shippingAddressMap.getString("region")
            shippingAddress.postalCode = shippingAddressMap.getString("postalCode")
            shippingAddress.countryCodeAlpha2 = shippingAddressMap.getString("countryCodeAlpha2")
            additionalInfo.shippingAddress = shippingAddress
          }
        }

        request.additionalInformation = additionalInfo
      }
    }

    return request
  }

  fun createThreeDSecureNonceResult(cardNonce: CardNonce): WritableMap {
    val result = Arguments.createMap()
    result.putString("nonce", cardNonce.string)
    result.putString("cardNetwork", cardNonce.cardType)
    result.putString("lastTwo", cardNonce.lastTwo)
    result.putString("lastFour", cardNonce.lastFour)
    result.putString("expirationMonth", cardNonce.expirationMonth)
    result.putString("expirationYear", cardNonce.expirationYear)
    result.putString("bin", cardNonce.bin)
    result.putString("cardType", cardNonce.cardType)

    val threeDSecureInfo = Arguments.createMap()
    val info = cardNonce.threeDSecureInfo
    if (info != null) {
      threeDSecureInfo.putBoolean("liabilityShifted", info.liabilityShifted)
      threeDSecureInfo.putBoolean("liabilityShiftPossible", info.liabilityShiftPossible)
      threeDSecureInfo.putBoolean("wasVerified", info.wasVerified)
    } else {
      threeDSecureInfo.putBoolean("liabilityShifted", false)
      threeDSecureInfo.putBoolean("liabilityShiftPossible", false)
      threeDSecureInfo.putBoolean("wasVerified", false)
    }

    result.putMap("threeDSecureInfo", threeDSecureInfo)
    return result
  }
}
