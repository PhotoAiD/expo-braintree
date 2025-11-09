package com.expobraintree

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.googlepay.GooglePayClient
import com.braintreepayments.api.googlepay.GooglePayLauncher
import com.braintreepayments.api.googlepay.GooglePayLauncherCallback
import com.braintreepayments.api.googlepay.GooglePayPaymentAuthRequest
import com.braintreepayments.api.googlepay.GooglePayPaymentAuthResult
import com.braintreepayments.api.googlepay.GooglePayReadinessResult
import com.braintreepayments.api.googlepay.GooglePayRequest
import com.braintreepayments.api.googlepay.GooglePayResult
import com.braintreepayments.api.googlepay.GooglePayTotalPriceStatus

class GooglePayPaymentExecutor(
    private val context: Context,
    listener: PaymentExecutorListener,
    args: BasePaymentArgs
) : BasePaymentExecutor(args, listener), GooglePayLauncherCallback {

    private val googlePayClient: GooglePayClient
    private var googlePayLauncher: GooglePayLauncher? = null

    init {
        Log.d(TAG, "[init] Creating GooglePayClient, tokenPresent=${args.clientToken.isNotEmpty()}")
        googlePayClient = GooglePayClient(
            context = context,
            authorization = args.clientToken
        )
    }

    override fun requestPayment(activity: FragmentActivity) {
        val paymentMethod = args.paymentMethod as? PaymentMethod.GooglePay
        if (paymentMethod == null) {
            Log.e(TAG, "[requestPayment] Invalid payment method")
            handleError("Invalid payment method", "Expected GooglePay")
            return
        }

        Log.d(TAG, "[requestPayment] Checking Google Pay readiness")

        googlePayLauncher = GooglePayLauncher(activity, this)

        googlePayClient.isReadyToPay(context) { readinessResult ->
            Log.d(TAG, "[requestPayment] readiness check callback, type: ${readinessResult?.let { it::class.simpleName } ?: "null"}")

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

    private fun createAndLaunchGooglePayRequest(paymentMethod: PaymentMethod.GooglePay) {
        Log.d(TAG, "[createAndLaunchGooglePayRequest] amount=${paymentMethod.amount}, currency=${paymentMethod.currency}")

        val googlePayRequest = GooglePayRequest(
            currencyCode = paymentMethod.currency,
            totalPrice = paymentMethod.amount,
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL
        ).apply {
            isBillingAddressRequired = false
            isEmailRequired = false
        }

        Log.d(TAG, "[createAndLaunchGooglePayRequest] Creating payment auth request")

        googlePayClient.createPaymentAuthRequest(googlePayRequest) { paymentAuthRequest ->
            Log.d(TAG, "[createAndLaunchGooglePayRequest] createPaymentAuthRequest callback, type: ${paymentAuthRequest::class.simpleName}")

            when (paymentAuthRequest) {
                is GooglePayPaymentAuthRequest.ReadyToLaunch -> {
                    Log.d(TAG, "[createAndLaunchGooglePayRequest] Ready to launch, launching Google Pay")
                    googlePayLauncher?.launch(paymentAuthRequest)
                }
                is GooglePayPaymentAuthRequest.Failure -> {
                    Log.e(TAG, "[createAndLaunchGooglePayRequest] Auth request failure: ${paymentAuthRequest.error.message}")
                    handleError(paymentAuthRequest.error.message, paymentAuthRequest.error.localizedMessage)
                }
            }
        }
    }

    override fun onGooglePayLauncherResult(result: GooglePayPaymentAuthResult) {
        Log.d(TAG, "[onGooglePayLauncherResult] Received Google Pay result, tokenizing...")

        googlePayClient.tokenize(result) { googlePayResult ->
            Log.d(TAG, "[onGooglePayLauncherResult] tokenize callback, type: ${googlePayResult::class.simpleName}")

            when (googlePayResult) {
                is GooglePayResult.Success -> {
                    Log.d(TAG, "[onGooglePayLauncherResult] Google Pay tokenization success")
                    val paymentMethod = args.paymentMethod as PaymentMethod.GooglePay
                    handleSuccess(
                        nonce = googlePayResult.nonce.string,
                        amount = paymentMethod.amount,
                        currency = paymentMethod.currency,
                        paymentType = "GooglePay"
                    )
                }
                is GooglePayResult.Failure -> {
                    Log.e(TAG, "[onGooglePayLauncherResult] Google Pay tokenization failure: ${googlePayResult.error.message}")
                    handleError(googlePayResult.error.message, googlePayResult.error.localizedMessage)
                }
                is GooglePayResult.Cancel -> {
                    Log.d(TAG, "[onGooglePayLauncherResult] Google Pay cancelled by user")
                    handleCancel()
                }
            }
        }
    }

    override fun onResume(intent: Intent) {
        Log.d(TAG, "[onResume] Activity resumed")
    }

    override fun onDestroy() {
        Log.d(TAG, "[onDestroy] Cleaning up")
        googlePayLauncher = null
    }
}
