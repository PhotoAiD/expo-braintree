package com.expobraintree

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class BraintreePaymentActivity : AppCompatActivity(), PaymentExecutorListener {

    private var paymentExecutor: BasePaymentExecutor? = null
    private var paymentArgs: BasePaymentArgs? = null
    private var isPaymentFlowStarted = false
    private var pendingPayPalRequest: String? = null

    companion object {
        private const val TAG = "[BraintreePayment]"
        const val ARG_PAYMENT_DATA = "payment_data"
        const val PAYMENT_RESULT = "payment_result"
        const val RESULT_CODE_SUCCESS = 123
        const val RESULT_CODE_ERROR = 234
        const val RESULT_CODE_CANCEL = 345
        private const val STATE_PAYMENT_ARGS = "state_payment_args"
        private const val STATE_FLOW_STARTED = "state_flow_started"
        private const val STATE_PAYPAL_PENDING = "state_paypal_pending"

        fun getIntent(context: Context, data: BasePaymentArgs): Intent {
            return Intent(context, BraintreePaymentActivity::class.java).apply {
                putExtra(ARG_PAYMENT_DATA, data)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "[onCreate] Activity created, savedInstanceState=${savedInstanceState != null}")

        if (savedInstanceState != null) {
            paymentArgs = savedInstanceState.getParcelable(STATE_PAYMENT_ARGS)
            isPaymentFlowStarted = savedInstanceState.getBoolean(STATE_FLOW_STARTED, false)
            pendingPayPalRequest = savedInstanceState.getString(STATE_PAYPAL_PENDING)
            Log.d(TAG, "[onCreate] State restored, isPaymentFlowStarted=$isPaymentFlowStarted, pendingPayPalPresent=${pendingPayPalRequest != null}")
        } else {
            paymentArgs = intent.getParcelableExtra(ARG_PAYMENT_DATA)
            isPaymentFlowStarted = false
            pendingPayPalRequest = null
        }

        val args = paymentArgs
        if (args == null) {
            Log.e(TAG, "[onCreate] No payment args provided, cancelling")
            finishWithCancel()
            return
        }

        val paymentMethod = args.paymentMethod
        Log.d(TAG, "[onCreate] Payment method: ${paymentMethod::class.simpleName}")

        paymentExecutor = when (paymentMethod) {
            is PaymentMethod.PayPalCheckout -> {
                Log.d(TAG, "[onCreate] Creating PayPalPaymentExecutor (checkout)")
                PayPalPaymentExecutor(this, this, args, isVault = false)
            }
            is PaymentMethod.PayPalVault -> {
                Log.d(TAG, "[onCreate] Creating PayPalPaymentExecutor (vault)")
                PayPalPaymentExecutor(this, this, args, isVault = true)
            }
            is PaymentMethod.Card -> {
                Log.d(TAG, "[onCreate] Creating CardPaymentExecutor")
                CardPaymentExecutor(this, this, args)
            }
            is PaymentMethod.GooglePay -> {
                Log.d(TAG, "[onCreate] Creating GooglePayPaymentExecutor")
                GooglePayPaymentExecutor(this, this, args)
            }
            is PaymentMethod.ThreeDSecure -> {
                Log.d(TAG, "[onCreate] Creating ThreeDSecureExecutor")
                ThreeDSecureExecutor(this, this, args)
            }
        }

        if (pendingPayPalRequest != null && paymentExecutor is PayPalPaymentExecutor) {
            Log.d(TAG, "[onCreate] Restoring PayPal pending request")
            (paymentExecutor as PayPalPaymentExecutor).pendingRequestString = pendingPayPalRequest
        }

        if (!isPaymentFlowStarted) {
            Log.d(TAG, "[onCreate] Starting payment flow")
            isPaymentFlowStarted = true
            paymentExecutor?.requestPayment(this)
        } else {
            Log.d(TAG, "[onCreate] Payment flow already started, skipping requestPayment")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (paymentExecutor is PayPalPaymentExecutor) {
            pendingPayPalRequest = (paymentExecutor as PayPalPaymentExecutor).pendingRequestString
        }

        Log.d(TAG, "[onSaveInstanceState] Saving state, pendingPayPalPresent=${pendingPayPalRequest != null}")
        outState.putParcelable(STATE_PAYMENT_ARGS, paymentArgs)
        outState.putBoolean(STATE_FLOW_STARTED, isPaymentFlowStarted)
        outState.putString(STATE_PAYPAL_PENDING, pendingPayPalRequest)
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        Log.d(TAG, "[onNewIntent] Intent received, data=${newIntent.data}")

        val extras = intent.extras
        newIntent.extras?.putAll(extras)
        intent = newIntent
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "[onResume] Activity resumed")
        paymentExecutor?.onResume(intent)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "[onActivityResult] requestCode=$requestCode, resultCode=$resultCode")
        if (requestCode == GooglePayPaymentExecutor.REQUEST_CODE_GOOGLE_PAY_EXPRESS) {
            (paymentExecutor as? GooglePayPaymentExecutor)?.handleExpressResult(resultCode, data)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "[onDestroy] Activity destroyed, isFinishing=$isFinishing")
        paymentExecutor?.onDestroy(isFinishing)
        super.onDestroy()
    }

    override fun onPaymentResult(result: PaymentResultModel) {
        Log.d(TAG, "[onPaymentResult] Result type: ${result::class.simpleName}")
        when (result) {
            is PaymentResultModel.Cancel -> finishWithCancel()
            is PaymentResultModel.Error -> finishWithError(result)
            is PaymentResultModel.Success -> finishWithSuccess(result)
        }
    }

    private fun finishWithSuccess(result: PaymentResultModel.Success) {
        Log.d(TAG, "[finishWithSuccess] Returning success result")
        setResult(
            RESULT_CODE_SUCCESS,
            Intent().apply {
                putExtra(PAYMENT_RESULT, result)
            }
        )
        finish()
    }

    private fun finishWithError(result: PaymentResultModel.Error) {
        Log.e(TAG, "[finishWithError] Returning error result: ${result.message}")
        setResult(
            RESULT_CODE_ERROR,
            Intent().apply {
                putExtra(PAYMENT_RESULT, result)
            }
        )
        finish()
    }

    private fun finishWithCancel() {
        Log.d(TAG, "[finishWithCancel] Returning cancel result")
        setResult(
            RESULT_CODE_CANCEL,
            Intent().apply {
                putExtra(PAYMENT_RESULT, PaymentResultModel.Cancel)
            }
        )
        finish()
    }
}
