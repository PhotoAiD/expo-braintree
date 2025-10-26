package com.expobraintree

import androidx.activity.result.ActivityResultLauncher
import com.braintreepayments.api.threedsecure.ThreeDSecurePaymentAuthRequest
import com.braintreepayments.api.threedsecure.ThreeDSecurePaymentAuthResult

class ThreeDSecureLauncherBridge private constructor() {

  private var threeDSecureLauncher: ActivityResultLauncher<ThreeDSecurePaymentAuthRequest.ReadyToLaunch>? = null

  companion object {
    @Volatile
    private var INSTANCE: ThreeDSecureLauncherBridge? = null

    fun getInstance(): ThreeDSecureLauncherBridge? {
      return INSTANCE
    }

    fun initialize(launcher: ActivityResultLauncher<ThreeDSecurePaymentAuthRequest.ReadyToLaunch>): ThreeDSecureLauncherBridge {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: ThreeDSecureLauncherBridge().also {
          it.threeDSecureLauncher = launcher
          INSTANCE = it
        }
      }
    }
  }

  fun launch(paymentAuthRequest: ThreeDSecurePaymentAuthRequest.ReadyToLaunch) {
    threeDSecureLauncher?.launch(paymentAuthRequest)
  }
}
