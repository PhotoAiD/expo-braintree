package com.expobraintree

import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentActivity

interface PaymentExecutorListener {
    fun onPaymentResult(result: PaymentResultModel)
}

abstract class BasePaymentExecutor(
    protected val args: BasePaymentArgs,
    protected val listener: PaymentExecutorListener
) {
    protected val TAG: String = this::class.java.simpleName

    abstract fun requestPayment(activity: FragmentActivity)
    abstract fun onResume(intent: Intent)

    /**
     * Called from the host activity's onDestroy. [isFinishing] distinguishes a terminal
     * destroy (result delivered, back press) from a non-finishing one (config change,
     * system-initiated destruction behind an open payment sheet) — process-wide state
     * must survive the latter.
     */
    abstract fun onDestroy(isFinishing: Boolean)

    protected fun handleError(message: String?, localizedMessage: String?) {
        Log.e(TAG, "[Error] message=$message, localizedMessage=$localizedMessage")
        listener.onPaymentResult(
            PaymentResultModel.Error(
                message = message,
                localizedMessage = localizedMessage
            )
        )
    }

    protected fun handleCancel() {
        Log.d(TAG, "[Cancel] Payment cancelled by user")
        listener.onPaymentResult(PaymentResultModel.Cancel)
    }

    protected fun handleSuccess(
        nonce: String,
        amount: String,
        currency: String,
        paymentType: String,
        threeDSecureInfo: ThreeDSecureInfo? = null,
        shippingAddress: PaymentAddress? = null,
        billingAddress: PaymentAddress? = null,
        googlePayEmail: String? = null,
        shippingOptionId: String? = null,
        payPalEmail: String? = null,
        payerId: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        phone: String? = null
    ) {
        Log.d(TAG, "[Success] paymentType=$paymentType, amount=$amount, currency=$currency, noncePresent=${nonce.isNotEmpty()}, 3dsInfo=$threeDSecureInfo")
        listener.onPaymentResult(
            PaymentResultModel.Success(
                nonce = nonce,
                amount = amount,
                currency = currency,
                deviceData = args.deviceData,
                email = args.email,
                paymentType = paymentType,
                threeDSecureInfo = threeDSecureInfo,
                shippingAddress = shippingAddress,
                billingAddress = billingAddress,
                googlePayEmail = googlePayEmail,
                shippingOptionId = shippingOptionId,
                payPalEmail = payPalEmail,
                payerId = payerId,
                firstName = firstName,
                lastName = lastName,
                phone = phone
            )
        )
    }
}
