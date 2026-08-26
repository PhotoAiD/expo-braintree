package com.expobraintree

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.googlepay.GooglePayCardNonce
import com.braintreepayments.api.googlepay.GooglePayClient
import com.braintreepayments.api.googlepay.GooglePayPaymentAuthRequest
import com.braintreepayments.api.googlepay.GooglePayReadinessResult
import com.braintreepayments.api.googlepay.GooglePayRequest
import com.braintreepayments.api.googlepay.GooglePayResult
import com.braintreepayments.api.googlepay.GooglePayShippingAddressParameters
import com.braintreepayments.api.googlepay.GooglePayTotalPriceStatus
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import org.json.JSONObject

/**
 * Google Pay executor.
 *
 * Drives Google Pay through Google's [PaymentsClient.loadPaymentData] rather than Braintree's
 * GooglePayLauncher: the launcher calls AutoResolveHelper.resolveTask, which was REMOVED in
 * play-services-wallet 20.0.0 — the version required for the express dynamic-price callback API
 * ([GooglePayCallbackService]). Braintree is still used to BUILD the request (gateway tokenization
 * spec via createPaymentAuthRequest) and to TOKENIZE the returned PaymentData.
 *
 * Standard and express checkout share the same launch + result path; express additionally augments
 * the request with selectable delivery options + callbackIntents. The result returns through the
 * host activity's onActivityResult -> [handleExpressResult].
 */
class GooglePayPaymentExecutor(
    private val context: Context,
    listener: PaymentExecutorListener,
    args: BasePaymentArgs
) : BasePaymentExecutor(args, listener) {

    private val googlePayClient: GooglePayClient
    private var hostActivity: FragmentActivity? = null
    private var selectedShippingOptionId: String? = null

    init {
        Log.d(TAG, "[init] Creating GooglePayClient, tokenPresent=${args.clientToken.isNotEmpty()}")
        googlePayClient = GooglePayClient(
            context = context,
            authorization = args.clientToken
        )
        // Seed here rather than at launch: if the host activity is recreated behind an
        // open sheet (process death), launch never runs again, so init is the only
        // point that can restore the holder for the callback service — otherwise it
        // would price every option from an empty holder (total 0.00).
        (args.paymentMethod as? PaymentMethod.GooglePay)
            ?.takeIf { it.shippingOptions.isNotEmpty() }
            ?.let { paymentMethod ->
                GooglePayExpressCheckoutHolder.seed(
                    basePrice = paymentMethod.amount,
                    currencyCode = paymentMethod.currency,
                    shippingOptions = paymentMethod.shippingOptions,
                    defaultShippingOptionId = paymentMethod.defaultShippingOptionId,
                    totalPriceLabel = paymentMethod.totalPriceLabel
                )
            }
    }

    override fun requestPayment(activity: FragmentActivity) {
        val paymentMethod = args.paymentMethod as? PaymentMethod.GooglePay
        if (paymentMethod == null) {
            Log.e(TAG, "[requestPayment] Invalid payment method")
            handleError("Invalid payment method", "Expected GooglePay")
            return
        }

        Log.d(TAG, "[requestPayment] Checking Google Pay readiness")
        hostActivity = activity

        googlePayClient.isReadyToPay(context) { readinessResult ->
            when (readinessResult) {
                is GooglePayReadinessResult.ReadyToPay -> {
                    Log.d(TAG, "[requestPayment] Google Pay ready, creating payment request")
                    createAndLaunchGooglePayRequest(paymentMethod)
                }
                is GooglePayReadinessResult.NotReadyToPay -> {
                    Log.e(TAG, "[requestPayment] Google Pay not ready")
                    handleError("Google Pay not available", "Google Pay is not available on this device")
                }
                null -> {
                    Log.e(TAG, "[requestPayment] Google Pay readiness check failed")
                    handleError("Google Pay check failed", "Could not check Google Pay availability")
                }
            }
        }
    }

    private fun buildGooglePayRequest(paymentMethod: PaymentMethod.GooglePay): GooglePayRequest {
        return GooglePayRequest(
            currencyCode = paymentMethod.currency,
            totalPrice = paymentMethod.amount,
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL
        ).apply {
            isShippingAddressRequired = paymentMethod.isShippingAddressRequired
            isBillingAddressRequired = paymentMethod.isBillingAddressRequired
            isEmailRequired = paymentMethod.isEmailRequired
            isPhoneNumberRequired = paymentMethod.isPhoneNumberRequired
            if (paymentMethod.isShippingAddressRequired) {
                shippingAddressParameters = GooglePayShippingAddressParameters().also {
                    it.isPhoneNumberRequired = paymentMethod.isPhoneNumberRequired
                }
            }
            if (paymentMethod.merchantName.isNotEmpty()) {
                googleMerchantName = paymentMethod.merchantName
            }
        }
    }

    private fun createAndLaunchGooglePayRequest(paymentMethod: PaymentMethod.GooglePay) {
        Log.d(
            TAG,
            "[createAndLaunchGooglePayRequest] amount=${paymentMethod.amount}, currency=${paymentMethod.currency}, expressOptions=${paymentMethod.shippingOptions.size}"
        )

        val googlePayRequest = buildGooglePayRequest(paymentMethod)

        googlePayClient.createPaymentAuthRequest(googlePayRequest) { paymentAuthRequest ->
            when (paymentAuthRequest) {
                is GooglePayPaymentAuthRequest.ReadyToLaunch -> launchGooglePay(paymentMethod, paymentAuthRequest)
                is GooglePayPaymentAuthRequest.Failure -> {
                    Log.e(TAG, "[createAndLaunchGooglePayRequest] Auth request failure: ${paymentAuthRequest.error.message}")
                    handleError(paymentAuthRequest.error.message, paymentAuthRequest.error.localizedMessage)
                }
            }
        }
    }

    private fun launchGooglePay(
        paymentMethod: PaymentMethod.GooglePay,
        readyToLaunch: GooglePayPaymentAuthRequest.ReadyToLaunch
    ) {
        val activity = hostActivity
        if (activity == null) {
            Log.e(TAG, "[launchGooglePay] No host activity")
            handleError("Google Pay launch failed", "No host activity")
            return
        }

        val params = readyToLaunch.requestParams
        val environment = params.googlePayEnvironment
        val isExpress = paymentMethod.shippingOptions.isNotEmpty()

        val requestJson = if (isExpress) {
            val defaultOptionId = paymentMethod.defaultShippingOptionId ?: paymentMethod.shippingOptions.first().id
            GooglePayExpressRequestBuilder.augment(
                baseRequestJson = params.paymentDataRequest.toJson(),
                options = paymentMethod.shippingOptions,
                defaultShippingOptionId = defaultOptionId,
                currencyCode = paymentMethod.currency,
                totalPrice = GooglePayExpressCheckoutHolder.totalFor(
                    paymentMethod.amount,
                    paymentMethod.shippingOptions,
                    defaultOptionId
                ),
                totalPriceLabel = paymentMethod.totalPriceLabel
            )
        } else {
            params.paymentDataRequest.toJson()
        }

        val paymentsClient: PaymentsClient = Wallet.getPaymentsClient(
            activity,
            Wallet.WalletOptions.Builder().setEnvironment(environment).build()
        )

        Log.d(TAG, "[launchGooglePay] loadPaymentData, env=$environment, express=$isExpress")
        paymentsClient.loadPaymentData(PaymentDataRequest.fromJson(requestJson))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val paymentData = task.result
                    if (paymentData != null) {
                        handlePaymentData(paymentData)
                    } else {
                        handleError("Google Pay failed", "No payment data returned")
                    }
                } else {
                    when (val ex = task.exception) {
                        is ResolvableApiException -> try {
                            ex.startResolutionForResult(activity, REQUEST_CODE_GOOGLE_PAY_EXPRESS)
                        } catch (sendEx: IntentSender.SendIntentException) {
                            Log.e(TAG, "[launchGooglePay] startResolutionForResult failed: ${sendEx.message}")
                            handleError("Google Pay error", sendEx.message ?: "Could not launch Google Pay")
                        }
                        else -> handleError("Google Pay error", ex?.message ?: "loadPaymentData failed")
                    }
                }
            }
    }

    /** Called by [BraintreePaymentActivity.onActivityResult] for the Google Pay sheet result. */
    fun handleExpressResult(resultCode: Int, data: Intent?) {
        Log.d(TAG, "[handleExpressResult] resultCode=$resultCode, dataPresent=${data != null}")
        when (resultCode) {
            Activity.RESULT_OK -> {
                val paymentData = data?.let { PaymentData.getFromIntent(it) }
                if (paymentData == null) {
                    handleError("Google Pay failed", "No payment data returned")
                    return
                }
                handlePaymentData(paymentData)
            }
            Activity.RESULT_CANCELED -> handleCancel()
            AutoResolveHelper.RESULT_ERROR -> {
                val status = data?.let { AutoResolveHelper.getStatusFromIntent(it) }
                Log.e(TAG, "[handleExpressResult] Google Pay error: ${status?.statusCode} ${status?.statusMessage}")
                handleError("Google Pay error", status?.statusMessage ?: "Google Pay returned an error")
            }
            else -> handleError("Google Pay failed", "Unexpected result code: $resultCode")
        }
    }

    private fun handlePaymentData(paymentData: PaymentData) {
        selectedShippingOptionId = parseSelectedShippingOptionId(paymentData)
        tokenizePaymentData(paymentData)
    }

    private fun parseSelectedShippingOptionId(paymentData: PaymentData): String? {
        return try {
            JSONObject(paymentData.toJson())
                .optJSONObject("shippingOptionData")
                ?.optString("id")
                ?.takeIf { it.isNotEmpty() }
        } catch (ex: Exception) {
            Log.e(TAG, "[parseSelectedShippingOptionId] ${ex.message}")
            null
        }
    }

    private fun tokenizePaymentData(paymentData: PaymentData) {
        Log.d(TAG, "[tokenizePaymentData] Tokenizing PaymentData via Braintree")
        googlePayClient.tokenize(paymentData) { googlePayResult ->
            when (googlePayResult) {
                is GooglePayResult.Success -> finishWithNonce(googlePayResult.nonce)
                is GooglePayResult.Failure -> {
                    Log.e(TAG, "[tokenizePaymentData] failure: ${googlePayResult.error.message}")
                    handleError(googlePayResult.error.message, googlePayResult.error.localizedMessage)
                }
                is GooglePayResult.Cancel -> handleCancel()
            }
        }
    }

    private fun finishWithNonce(nonce: com.braintreepayments.api.core.PaymentMethodNonce) {
        val paymentMethod = args.paymentMethod as PaymentMethod.GooglePay
        val cardNonce = nonce as? GooglePayCardNonce        
        val effectiveOptionId = when {
            selectedShippingOptionId != null -> selectedShippingOptionId
            paymentMethod.shippingOptions.isNotEmpty() ->
                paymentMethod.defaultShippingOptionId ?: paymentMethod.shippingOptions.first().id
            else -> null
        }
        val finalAmount = effectiveOptionId
            ?.let { GooglePayExpressCheckoutHolder.totalFor(paymentMethod.amount, paymentMethod.shippingOptions, it) }
            ?: paymentMethod.amount

        handleSuccess(
            nonce = nonce.string,
            amount = finalAmount,
            currency = paymentMethod.currency,
            paymentType = "GooglePay",
            shippingAddress = cardNonce?.shippingAddress.toPaymentAddress(),
            billingAddress = cardNonce?.billingAddress.toPaymentAddress(),
            googlePayEmail = cardNonce?.email,
            shippingOptionId = effectiveOptionId
        )
    }

    override fun onResume(intent: Intent) {
        Log.d(TAG, "[onResume] Activity resumed")
    }

    override fun onDestroy(isFinishing: Boolean) {
        Log.d(TAG, "[onDestroy] Cleaning up, isFinishing=$isFinishing")
        hostActivity = null
        if (isFinishing) {
            GooglePayExpressCheckoutHolder.clear()
        }
    }

    companion object {
        const val REQUEST_CODE_GOOGLE_PAY_EXPRESS = 9911
    }
}
