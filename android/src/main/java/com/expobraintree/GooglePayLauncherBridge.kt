package com.expobraintree

import androidx.activity.ComponentActivity
import com.braintreepayments.api.googlepay.GooglePayLauncher
import com.braintreepayments.api.googlepay.GooglePayPaymentAuthRequest
import com.braintreepayments.api.googlepay.GooglePayPaymentAuthResult

interface GooglePayLauncherBridge {
    fun launch(activity: ComponentActivity, paymentAuthRequest: GooglePayPaymentAuthRequest.ReadyToLaunch)

    companion object {
        private var instance: GooglePayLauncherBridge? = null

        fun getInstance(): GooglePayLauncherBridge? = instance

        fun setInstance(bridge: GooglePayLauncherBridge) {
            instance = bridge
        }
    }
}

class GooglePayLauncherBridgeImpl(
    private val launcher: GooglePayLauncher
) : GooglePayLauncherBridge {

    override fun launch(activity: ComponentActivity, paymentAuthRequest: GooglePayPaymentAuthRequest.ReadyToLaunch) {
        launcher.launch(activity, paymentAuthRequest)
    }
}
