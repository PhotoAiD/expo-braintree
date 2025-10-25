package com.expobraintreeexample

import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import com.expobraintree.PayPalLauncherBridge
import com.expobraintree.GooglePayLauncherBridge
import com.expobraintree.ThreeDSecureLauncherBridge
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.googlepay.GooglePayLauncher
import com.braintreepayments.api.threeDSecure.ThreeDSecureLauncher

class MainActivity : ReactActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val paypalLauncher = PayPalLauncher(this) { paypalPaymentAuthResult ->
      val expoBraintreeModule = reactInstanceManager.currentReactContext
        ?.getNativeModule(com.expobraintree.ExpoBraintreeModule::class.java)
      expoBraintreeModule?.handlePayPalAccountNonceResult(
        paypalPaymentAuthResult.nonce,
        paypalPaymentAuthResult.error
      )
    }
    PayPalLauncherBridge.initialize(paypalLauncher)

    val googlePayLauncher = GooglePayLauncher(this) { googlePayPaymentAuthResult ->
      val expoBraintreeModule = reactInstanceManager.currentReactContext
        ?.getNativeModule(com.expobraintree.ExpoBraintreeModule::class.java)
      expoBraintreeModule?.handleGooglePayAuthResult(googlePayPaymentAuthResult)
    }
    GooglePayLauncherBridge.initialize(googlePayLauncher)

    val threeDSecureLauncher = ThreeDSecureLauncher(this) { threeDSecurePaymentAuthResult ->
      val expoBraintreeModule = reactInstanceManager.currentReactContext
        ?.getNativeModule(com.expobraintree.ExpoBraintreeModule::class.java)
      expoBraintreeModule?.handleThreeDSecureAuthResult(threeDSecurePaymentAuthResult)
    }
    ThreeDSecureLauncherBridge.initialize(threeDSecureLauncher)
  }

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  override fun getMainComponentName(): String = "ExpoBraintreeExample"

  /**
   * Returns the instance of the [ReactActivityDelegate]. We use [DefaultReactActivityDelegate]
   * which allows you to enable New Architecture with a single boolean flags [fabricEnabled]
   */
  override fun createReactActivityDelegate(): ReactActivityDelegate =
      DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)
}
