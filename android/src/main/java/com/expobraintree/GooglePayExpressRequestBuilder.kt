package com.expobraintree

import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds the Google Pay JSON fragments needed for express checkout (delivery
 * methods + dynamic price updates).
 *
 * Braintree's [com.braintreepayments.api.googlepay.GooglePayClient.createPaymentAuthRequest]
 * produces a fully-formed `PaymentDataRequest` (correct gateway tokenization spec,
 * allowed card networks, merchant info, environment). We only *augment* that JSON
 * with the shipping-option parameters and callback intents that Braintree's
 * `GooglePayRequest` cannot express, so tokenization stays correct.
 */
object GooglePayExpressRequestBuilder {

    /**
     * Adds shipping-option parameters, callback intents, and an ESTIMATED total to
     * an existing PaymentDataRequest JSON string.
     *
     * @param baseRequestJson `PaymentDataRequest.toJson()` from Braintree's auth request.
     */
    fun augment(
        baseRequestJson: String,
        options: List<GooglePayShippingOption>,
        defaultShippingOptionId: String?,
        currencyCode: String,
        totalPrice: String
    ): String {
        val root = JSONObject(baseRequestJson)

        root.put("shippingAddressRequired", true)
        root.put("shippingOptionRequired", true)
        root.put("shippingOptionParameters", shippingOptionParameters(options, defaultShippingOptionId))
        root.put("callbackIntents", JSONArray(listOf("SHIPPING_ADDRESS", "SHIPPING_OPTION")))
        root.put(
            "transactionInfo",
            transactionInfo(currencyCode, totalPrice, "ESTIMATED")
        )

        return root.toString()
    }

    /**
     * `shippingOptionParameters` object:
     * { defaultSelectedOptionId, shippingOptions: [ { id, label, description } ] }
     */
    fun shippingOptionParameters(
        options: List<GooglePayShippingOption>,
        defaultShippingOptionId: String?
    ): JSONObject {
        val array = JSONArray()
        for (option in options) {
            val item = JSONObject()
                .put("id", option.id)
                .put("label", option.label)
            if (!option.description.isNullOrEmpty()) {
                item.put("description", option.description)
            }
            array.put(item)
        }
        val default = defaultShippingOptionId ?: options.firstOrNull()?.id
        return JSONObject()
            .put("defaultSelectedOptionId", default)
            .put("shippingOptions", array)
    }

    /** `transactionInfo` object used both in the request and in callback responses. */
    fun transactionInfo(
        currencyCode: String,
        totalPrice: String,
        totalPriceStatus: String
    ): JSONObject = JSONObject()
        .put("totalPriceStatus", totalPriceStatus)
        .put("totalPrice", totalPrice)
        .put("currencyCode", currencyCode)
}
