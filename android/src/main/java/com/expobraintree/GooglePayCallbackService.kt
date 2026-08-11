package com.expobraintree

import android.util.Log
import com.google.android.gms.wallet.callback.BasePaymentDataCallbacks
import com.google.android.gms.wallet.callback.BasePaymentDataCallbacksService
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.OnCompleteListener
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import org.json.JSONObject

/**
 * Google Pay dynamic-price-update callback service (express checkout).
 *
 * Google Play Services binds to this service during the Google Pay sheet when the
 * `PaymentDataRequest` declares `callbackIntents`. It must be declared in the
 * AndroidManifest with the `PAYMENT_DATA_CALLBACKS` intent-filter action and the
 * `BIND_PAYMENTS_CALLBACK_SERVICE` permission. Requires play-services-wallet 20.0.0+.
 *
 * It recomputes the sheet total as `basePrice + selected delivery price`, reading the
 * configuration the executor stored in [GooglePayExpressCheckoutHolder].
 */
class GooglePayCallbackService : BasePaymentDataCallbacksService() {
    override fun createPaymentDataCallbacks(): BasePaymentDataCallbacks =
        GooglePayPaymentDataCallbacks()
}

private class GooglePayPaymentDataCallbacks : BasePaymentDataCallbacks() {

    override fun onPaymentDataChanged(
        request: IntermediatePaymentData?,
        onCompleteListener: OnCompleteListener<PaymentDataRequestUpdate>
    ) {
        // Google's contract requires exactly one complete() call; an uncaught
        // exception here would leave the sheet spinning forever, so any failure
        // must resolve into an error update.
        var errorIntent = "SHIPPING_ADDRESS"
        try {
            val holder = GooglePayExpressCheckoutHolder
            val json = JSONObject(request?.toJson() ?: "{}")
            val trigger = json.optString("callbackTrigger", "")
            if (trigger == "SHIPPING_OPTION") {
                errorIntent = "SHIPPING_OPTION"
            }
            val selectedId = json.optJSONObject("shippingOptionData")?.optString("id")

            // No option chosen yet on INITIALIZE / SHIPPING_ADDRESS -> fall back to default.
            val effectiveId = if (!selectedId.isNullOrEmpty()) selectedId else holder.defaultShippingOptionId
            val total = holder.totalForOption(effectiveId)

            Log.d(TAG, "[onPaymentDataChanged] trigger=$trigger selected=$selectedId total=$total")

            val update = JSONObject().put(
                "newTransactionInfo",
                GooglePayExpressRequestBuilder.transactionInfo(holder.currencyCode, total, "ESTIMATED")
            )

            // Refresh the available options on init / address change; on a pure option
            // change only the total needs updating.
            if (trigger != "SHIPPING_OPTION") {
                update.put(
                    "newShippingOptionParameters",
                    GooglePayExpressRequestBuilder.shippingOptionParameters(
                        holder.shippingOptions,
                        effectiveId
                    )
                )
            }

            onCompleteListener.complete(PaymentDataRequestUpdate.fromJson(update.toString()))
        } catch (ex: Exception) {
            Log.e(TAG, "[onPaymentDataChanged] Failed to build update: ${ex.message}", ex)
            onCompleteListener.complete(errorUpdate(errorIntent))
        }
    }

    /** Surfaces the failure on the sheet instead of hanging it (built from static, valid JSON). */
    private fun errorUpdate(intent: String): PaymentDataRequestUpdate =
        PaymentDataRequestUpdate.fromJson(
            """{"error":{"reason":"OTHER_ERROR","message":"Unable to update delivery options. Please try again.","intent":"$intent"}}"""
        )

    companion object {
        private const val TAG = "[GPayCallback]"
    }
}
