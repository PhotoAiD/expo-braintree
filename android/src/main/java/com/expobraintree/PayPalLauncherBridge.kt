package com.expobraintree

import android.content.Intent
import androidx.activity.ComponentActivity
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult

interface PayPalLauncherBridge {
    fun launch(activity: ComponentActivity, paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch): PayPalPendingRequest
    fun handleReturnToApp(pendingRequest: PayPalPendingRequest.Started, intent: Intent): PayPalPaymentAuthResult

    companion object {
        private var instance: PayPalLauncherBridge? = null

        fun getInstance(): PayPalLauncherBridge? = instance

        fun setInstance(bridge: PayPalLauncherBridge) {
            instance = bridge
        }
    }
}

class PayPalLauncherBridgeImpl(
    private val launcher: PayPalLauncher
) : PayPalLauncherBridge {

    override fun launch(activity: ComponentActivity, paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch): PayPalPendingRequest {
        return launcher.launch(activity, paymentAuthRequest)
    }

    override fun handleReturnToApp(
        pendingRequest: PayPalPendingRequest.Started,
        intent: Intent
    ): PayPalPaymentAuthResult {
        return launcher.handleReturnToApp(pendingRequest, intent)
    }
}
