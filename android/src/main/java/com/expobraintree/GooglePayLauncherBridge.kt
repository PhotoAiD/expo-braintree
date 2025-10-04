package com.expobraintree

import com.braintreepayments.api.googlepay.GooglePayLauncher
import com.braintreepayments.api.googlepay.GooglePayPaymentAuthRequest

interface GooglePayLauncherBridge {
    fun launch(paymentAuthRequest: GooglePayPaymentAuthRequest.ReadyToLaunch)

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

    override fun launch(paymentAuthRequest: GooglePayPaymentAuthRequest.ReadyToLaunch) {
        launcher.launch(paymentAuthRequest)
    }
}
