package com.expobraintree

import com.braintreepayments.api.threedsecure.ThreeDSecureLauncher
import com.braintreepayments.api.threedsecure.ThreeDSecurePaymentAuthRequest

interface ThreeDSecureLauncherBridge {
    fun launch(paymentAuthRequest: ThreeDSecurePaymentAuthRequest.ReadyToLaunch)

    companion object {
        private var instance: ThreeDSecureLauncherBridge? = null

        fun getInstance(): ThreeDSecureLauncherBridge? = instance

        fun setInstance(bridge: ThreeDSecureLauncherBridge) {
            instance = bridge
        }
    }
}

class ThreeDSecureLauncherBridgeImpl(
    private val launcher: ThreeDSecureLauncher
) : ThreeDSecureLauncherBridge {

    override fun launch(paymentAuthRequest: ThreeDSecurePaymentAuthRequest.ReadyToLaunch) {
        launcher.launch(paymentAuthRequest)
    }
}
