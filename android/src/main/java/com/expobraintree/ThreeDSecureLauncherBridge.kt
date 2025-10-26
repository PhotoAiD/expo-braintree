package com.expobraintree

import android.os.Handler
import android.os.Looper
import com.braintreepayments.api.threedsecure.ThreeDSecureLauncher
import com.braintreepayments.api.threedsecure.ThreeDSecurePaymentAuthRequest

class ThreeDSecureLauncherBridge private constructor() {

  private var threeDSecureLauncher: ThreeDSecureLauncher? = null

  companion object {
    @Volatile
    private var INSTANCE: ThreeDSecureLauncherBridge? = null

    fun getInstance(): ThreeDSecureLauncherBridge? {
      return INSTANCE
    }

    fun initialize(launcher: ThreeDSecureLauncher): ThreeDSecureLauncherBridge {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: ThreeDSecureLauncherBridge().also {
          it.threeDSecureLauncher = launcher
          INSTANCE = it
        }
      }
    }
  }

  fun launch(paymentAuthRequest: ThreeDSecurePaymentAuthRequest.ReadyToLaunch) {
    // Ensure launch happens on UI thread
    Handler(Looper.getMainLooper()).post {
      threeDSecureLauncher?.launch(paymentAuthRequest)
    }
  }
}
