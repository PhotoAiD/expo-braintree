package com.expobraintree

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPaymentIntent
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalVaultRequest

class PayPalPaymentExecutor(
    private val context: Context,
    listener: PaymentExecutorListener,
    args: BasePaymentArgs,
    private val isVault: Boolean
) : BasePaymentExecutor(args, listener) {

    private val payPalLauncher: PayPalLauncher = PayPalLauncher()
    private val payPalClient: PayPalClient
    var pendingRequestString: String? = null

    init {
        Log.d(TAG, "[init] Creating PayPalClient, isVault=$isVault, tokenPresent=${args.clientToken.isNotEmpty()}")
        val appLinkUri = "https://photoaid.com".toUri()
        val deepLinkScheme = "${context.packageName}.braintree"
        Log.d(TAG, "[init] appLinkUri=$appLinkUri, deepLinkScheme=$deepLinkScheme")

        payPalClient = PayPalClient(
            context = context,
            authorization = args.clientToken,
            appLinkReturnUrl = appLinkUri,
            deepLinkFallbackUrlScheme = deepLinkScheme
        )
    }

    override fun requestPayment(activity: FragmentActivity) {
        Log.d(TAG, "[requestPayment] Starting PayPal payment flow")

        if (isVault) {
            requestVaultPayment(activity)
        } else {
            requestCheckoutPayment(activity)
        }
    }

    private fun requestCheckoutPayment(activity: FragmentActivity) {
        val paymentMethod = args.paymentMethod as? PaymentMethod.PayPalCheckout
        if (paymentMethod == null) {
            Log.e(TAG, "[requestCheckoutPayment] Invalid payment method")
            handleError("Invalid payment method", "Expected PayPalCheckout")
            return
        }

        Log.d(TAG, "[requestCheckoutPayment] amount=${paymentMethod.amount}, currency=${paymentMethod.currency}")

        val request = PayPalCheckoutRequest(
            amount = paymentMethod.amount,
            hasUserLocationConsent = false
        )
        request.currencyCode = paymentMethod.currency
        request.intent = PayPalPaymentIntent.AUTHORIZE

        payPalClient.createPaymentAuthRequest(activity, request) { paymentAuthRequest ->
            Log.d(TAG, "[requestCheckoutPayment] createPaymentAuthRequest callback")
            handlePaymentAuthRequest(activity, paymentAuthRequest)
        }
    }

    private fun requestVaultPayment(activity: FragmentActivity) {
        val paymentMethod = args.paymentMethod as? PaymentMethod.PayPalVault
        if (paymentMethod == null) {
            Log.e(TAG, "[requestVaultPayment] Invalid payment method")
            handleError("Invalid payment method", "Expected PayPalVault")
            return
        }

        Log.d(TAG, "[requestVaultPayment] billingAgreementDescription=${paymentMethod.billingAgreementDescription}")

        val request = PayPalVaultRequest(hasUserLocationConsent = false).apply {
            billingAgreementDescription = paymentMethod.billingAgreementDescription
        }

        payPalClient.createPaymentAuthRequest(activity, request) { paymentAuthRequest ->
            Log.d(TAG, "[requestVaultPayment] createPaymentAuthRequest callback")
            handlePaymentAuthRequest(activity, paymentAuthRequest)
        }
    }

    private fun handlePaymentAuthRequest(
        activity: FragmentActivity,
        paymentAuthRequest: PayPalPaymentAuthRequest
    ) {
        when (paymentAuthRequest) {
            is PayPalPaymentAuthRequest.Failure -> {
                Log.e(TAG, "[handlePaymentAuthRequest] Failure: ${paymentAuthRequest.error.message}")
                handleError(paymentAuthRequest.error.message, paymentAuthRequest.error.localizedMessage)
            }
            is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                Log.d(TAG, "[handlePaymentAuthRequest] ReadyToLaunch, launching PayPal flow")
                val pendingRequest = payPalLauncher.launch(activity, paymentAuthRequest)

                when (pendingRequest) {
                    is PayPalPendingRequest.Started -> {
                        pendingRequestString = pendingRequest.pendingRequestString
                        Log.d(TAG, "[handlePaymentAuthRequest] PayPal flow started, pendingRequest stored")
                    }
                    is PayPalPendingRequest.Failure -> {
                        Log.e(TAG, "[handlePaymentAuthRequest] Launch failure: ${pendingRequest.error.message}")
                        handleError(pendingRequest.error.message, pendingRequest.error.localizedMessage)
                    }
                }
            }
        }
    }

    override fun onResume(intent: Intent) {
        Log.d(TAG, "[onResume] Checking for PayPal return, pendingRequestPresent=${pendingRequestString != null}")

        val pendingString = pendingRequestString ?: return
        val intentData = intent.data?.toString()
        Log.d(TAG, "[onResume] intentData=$intentData")

        val pendingRequest = PayPalPendingRequest.Started(pendingString)
        val paymentAuthResult = payPalLauncher.handleReturnToApp(pendingRequest, intent)

        Log.d(TAG, "[onResume] paymentAuthResult type: ${paymentAuthResult::class.simpleName}")

        when (paymentAuthResult) {
            is PayPalPaymentAuthResult.Failure -> {
                Log.e(TAG, "[onResume] Payment auth failure: ${paymentAuthResult.error.message}")
                pendingRequestString = null
                handleError(paymentAuthResult.error.message, paymentAuthResult.error.localizedMessage)
            }
            is PayPalPaymentAuthResult.Success -> {
                Log.d(TAG, "[onResume] Payment auth success, tokenizing...")
                tokenizePayPalResult(paymentAuthResult)
            }
            is PayPalPaymentAuthResult.NoResult -> {
                Log.d(TAG, "[onResume] No result - user cancelled or incomplete flow")
                pendingRequestString = null
                handleCancel()
            }
        }
    }

    private fun tokenizePayPalResult(paymentAuthResult: PayPalPaymentAuthResult.Success) {
        payPalClient.tokenize(paymentAuthResult) { payPalResult ->
            Log.d(TAG, "[tokenizePayPalResult] tokenize callback, result type: ${payPalResult::class.simpleName}")

            when (payPalResult) {
                is PayPalResult.Success -> {
                    Log.d(TAG, "[tokenizePayPalResult] Tokenization success")
                    pendingRequestString = null

                    val paymentMethod = args.paymentMethod
                    val amount = when (paymentMethod) {
                        is PaymentMethod.PayPalCheckout -> paymentMethod.amount
                        is PaymentMethod.PayPalVault -> "0"
                        else -> "0"
                    }
                    val currency = when (paymentMethod) {
                        is PaymentMethod.PayPalCheckout -> paymentMethod.currency
                        else -> "USD"
                    }

                    handleSuccess(
                        nonce = payPalResult.nonce.string,
                        amount = amount,
                        currency = currency,
                        paymentType = "PayPal"
                    )
                }
                is PayPalResult.Failure -> {
                    Log.e(TAG, "[tokenizePayPalResult] Tokenization failure: ${payPalResult.error.message}")
                    pendingRequestString = null
                    handleError(payPalResult.error.message, payPalResult.error.localizedMessage)
                }
                is PayPalResult.Cancel -> {
                    Log.d(TAG, "[tokenizePayPalResult] Tokenization cancelled")
                    pendingRequestString = null
                    handleCancel()
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "[onDestroy] Cleaning up")
    }
}
