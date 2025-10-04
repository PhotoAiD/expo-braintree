package com.expobraintree

import com.braintreepayments.api.paypal.PayPalAccountNonce
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalPaymentIntent
import com.braintreepayments.api.paypal.PayPalVaultRequest
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.card.Card;
import com.braintreepayments.api.card.CardNonce;
import com.braintreepayments.api.googlepay.GooglePayRequest
import com.braintreepayments.api.googlepay.GooglePayNonce
import com.braintreepayments.api.googlepay.GooglePayTotalPriceStatus

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap


class PaypalDataConverter {

  companion object {
    fun convertAddressData(address: PostalAddress): WritableMap {
      val result: WritableMap = Arguments.createMap();
      result.putString("recipientName", address.recipientName)
      result.putString("streetAddress", address.streetAddress)
      result.putString("extendedAddress", address.extendedAddress)
      result.putString("locality", address.locality)
      result.putString("countryCodeAlpha2", address.countryCodeAlpha2)
      result.putString("postalCode", address.postalCode)
      result.putString("region", address.region)
      return result
    }

    fun convertPaypalDataAccountNonce(payPalAccountNonce: PayPalAccountNonce): WritableMap {
      val result: WritableMap = Arguments.createMap()
      result.putString("nonce", payPalAccountNonce.string)
      result.putString("payerId", payPalAccountNonce.payerId)
      result.putString("email", payPalAccountNonce.email)
      result.putString("firstName", payPalAccountNonce.firstName)
      result.putString("lastName", payPalAccountNonce.lastName)
      result.putString("phone", payPalAccountNonce.phone)
      return result
    }

    fun createTokenizeCardDataNonce(cardNonce: CardNonce): WritableMap {
      val result: WritableMap = Arguments.createMap()
      result.putString("nonce", cardNonce.string)
      if (cardNonce.cardType == "Unknown") {
        result.putString("cardNetwork", "")
      } else {
        result.putString("cardNetwork", cardNonce.cardType)
      }
      // V5: Properties instead of getters
      result.putString("lastFour", cardNonce.lastFour)
      result.putString("lastTwo", cardNonce.lastTwo)
      result.putString("expirationMonth", cardNonce.expirationMonth)
      result.putString("expirationYear", cardNonce.expirationYear)
      return result
    }

    fun createError(domain: String, details: String?): WritableMap {
      val result: WritableMap = Arguments.createMap();
      result.putString("domain", domain)
      result.putString("details", details)
      return result
    }

    fun createVaultRequest(options: ReadableMap): PayPalVaultRequest {
      // V5: Constructor requires hasUserLocationConsent
      val request: PayPalVaultRequest = PayPalVaultRequest(hasUserLocationConsent = false)

      // V5: Direct property assignment instead of setters
      if (options.hasKey("billingAgreementDescription")) {
        request.billingAgreementDescription = options.getString("billingAgreementDescription")
      }
      if (options.hasKey("localeCode")) {
        request.localeCode = options.getString("localeCode")
      }
      if (options.hasKey("displayName")) {
        request.displayName = options.getString("displayName")
      }
      if (options.hasKey("offerCredit")) {
        val offerCredit: String = options.getString("offerCredit") ?: ""
        when (offerCredit) {
          "true" -> request.shouldOfferCredit = true
        }
      }
      if (options.hasKey("isShippingAddressRequired")) {
        val isShippingAddressRequired: String = options.getString("isShippingAddressRequired") ?: ""
        when (isShippingAddressRequired) {
          "true" -> request.isShippingAddressRequired = true
        }
      }
      if (options.hasKey("isShippingAddressEditable")) {
        val isShippingAddressEditable: String = options.getString("isShippingAddressEditable") ?: ""
        when (isShippingAddressEditable) {
          "true" -> request.isShippingAddressEditable = true
        }
      }
      return request
    }

    fun createCheckoutRequest(options: ReadableMap): PayPalCheckoutRequest {
      // V5: Constructor requires amount and hasUserLocationConsent
      val request = PayPalCheckoutRequest(
        amount = options.getString("amount") ?: "",
        hasUserLocationConsent = false
      )

      // V5: Direct property assignment
      if (options.hasKey("billingAgreementDescription")) {
        request.billingAgreementDescription = options.getString("billingAgreementDescription")
      }
      if (options.hasKey("localeCode")) {
        request.localeCode = options.getString("localeCode") ?: "en-US"
      }
      if (options.hasKey("displayName")) {
        request.displayName = options.getString("displayName")
      }
      if (options.hasKey("currencyCode")) {
        request.currencyCode = options.getString("currencyCode")
      }
      if (options.hasKey("isShippingAddressRequired")) {
        val isShippingAddressRequired: String = options.getString("isShippingAddressRequired") ?: ""
        when (isShippingAddressRequired) {
          "false" -> request.isShippingAddressRequired = false
          "true" -> request.isShippingAddressRequired = true
        }
      }
      if (options.hasKey("intent")) {
        val intent: String = options.getString("intent") ?: ""
        when (intent) {
          "sale" -> request.intent = PayPalPaymentIntent.SALE
          "order" -> request.intent = PayPalPaymentIntent.ORDER
        }
      } else {
        request.intent = PayPalPaymentIntent.AUTHORIZE
      }
      return request
    }

    fun createTokenizeCardRequest(options: ReadableMap): Card {
      // V5: Direct property assignment instead of setters
      val card: Card = Card()
      if (options.hasKey("number")) {
        card.number = options.getString("number")
      }
      if (options.hasKey("expirationMonth")) {
        card.expirationMonth = options.getString("expirationMonth")
      }
      if (options.hasKey("expirationYear")) {
        card.expirationYear = options.getString("expirationYear")
      }
      if (options.hasKey("cvv")) {
        card.cvv = options.getString("cvv")
      }
      if (options.hasKey("postalCode")) {
        card.postalCode = options.getString("postalCode")
      }
      return card
    }

    fun createGooglePayRequest(options: ReadableMap): GooglePayRequest {
      val amount = options.getString("amount") ?: ""
      val currencyCode = options.getString("currencyCode") ?: "USD"

      val request = GooglePayRequest(
        currencyCode = currencyCode,
        totalPrice = amount,
        totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL
      )

      if (options.hasKey("merchantName")) {
        request.googleMerchantName = options.getString("merchantName")
      }

      return request
    }

    fun createGooglePayNonceResult(googlePayNonce: GooglePayNonce): WritableMap {
      val result: WritableMap = Arguments.createMap()
      result.putString("nonce", googlePayNonce.string)
      result.putString("type", googlePayNonce.type)
      result.putString("description", googlePayNonce.description)
      result.putBoolean("isDefault", googlePayNonce.isDefault)

      googlePayNonce.cardNetwork?.let {
        result.putString("cardNetwork", it)
      }

      googlePayNonce.email?.let {
        result.putString("email", it)
      }

      googlePayNonce.shippingAddress?.let {
        result.putMap("shippingAddress", convertAddressData(it))
      }

      googlePayNonce.billingAddress?.let {
        result.putMap("billingAddress", convertAddressData(it))
      }

      return result
    }

  }
}
