package com.expobraintree

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import android.net.Uri
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.card.CardClient
import com.braintreepayments.api.card.CardNonce
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.datacollector.DataCollectorRequest
import com.braintreepayments.api.datacollector.DataCollectorResult
import com.braintreepayments.api.paypal.PayPalAccountNonce
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalVaultRequest
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalResult
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap

class ExpoBraintreeModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), ActivityEventListener, LifecycleEventListener {
  val NAME = "ExpoBraintree"
  private lateinit var promiseRef: Promise
  private lateinit var currentActivityRef: FragmentActivity
  private var reactContextRef: Context
  private var payPalClientRef: PayPalClient? = null
  private var cardClientRef: CardClient? = null
  private var dataCollectorRef: DataCollector? = null
  private var pendingPayPalRequest: String? = null
  private val paypalRebornModuleHandlers: PaypalRebornModuleHandlers = PaypalRebornModuleHandlers()

  init {
    this.reactContextRef = reactContext
    reactContext.addLifecycleEventListener(this)
    reactContext.addActivityEventListener(this)
  }

  @ReactMethod
  fun requestBillingAgreement(data: ReadableMap, localPromise: Promise) {
    try {
      promiseRef = localPromise
      currentActivityRef = getCurrentActivity() as FragmentActivity
      val clientToken = data.getString("clientToken") ?: ""

      // Get launcher from MainActivity bridge
      val launcherBridge = PayPalLauncherBridge.getInstance()
      if (launcherBridge == null) {
        throw Exception("PayPalLauncher not initialized. MainActivity setup required.")
      }

      // Initialize PayPalClient with app link and deep link
      val appLinkUri = Uri.parse("https://photoaid.com")
      val deepLinkScheme = "${currentActivityRef.packageName}.braintree"

      payPalClientRef = PayPalClient(
        context = currentActivityRef,
        authorization = clientToken,
        appLinkReturnUrl = appLinkUri,
        deepLinkFallbackUrlScheme = deepLinkScheme
      )

      val vaultRequest: PayPalVaultRequest = PaypalDataConverter.createVaultRequest(data)

      // Step 1: Create payment auth request
      payPalClientRef!!.createPaymentAuthRequest(currentActivityRef, vaultRequest) { paymentAuthRequest ->
        when (paymentAuthRequest) {
          is PayPalPaymentAuthRequest.ReadyToLaunch -> {
            // Step 2: Launch PayPal flow
            val pendingRequest = launcherBridge.launch(currentActivityRef, paymentAuthRequest)
            when (pendingRequest) {
              is PayPalPendingRequest.Started -> {
                // Store pending request for later completion
                pendingPayPalRequest = pendingRequest.pendingRequestString
              }
              is PayPalPendingRequest.Failure -> {
                handlePayPalError(pendingRequest.error)
              }
            }
          }
          is PayPalPaymentAuthRequest.Failure -> {
            handlePayPalError(paymentAuthRequest.error)
          }
        }
      }

    } catch (ex: Exception) {
      localPromise.reject(
          EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
          ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.value,
          PaypalDataConverter.createError(EXCEPTION_TYPES.KOTLIN_EXCEPTION.value, ex.message)
      )
    }
  }

  @ReactMethod
  fun getDeviceDataFromDataCollector(clientToken: String?, localPromise: Promise) {
    try {
      promiseRef = localPromise

      // V5: Direct initialization without BraintreeClient
      dataCollectorRef = DataCollector(
        context = reactContextRef,
        authorization = clientToken ?: ""
      )

      // V5: collectDeviceData with DataCollectorRequest and sealed class result
      val dataCollectorRequest = DataCollectorRequest(hasUserLocationConsent = false)
      dataCollectorRef!!.collectDeviceData(
        context = reactContextRef,
        request = dataCollectorRequest
      ) { dataCollectorResult ->
        when (dataCollectorResult) {
          is DataCollectorResult.Success -> {
            paypalRebornModuleHandlers.handleGetDeviceDataFromDataCollectorResult(
              dataCollectorResult.deviceData,
              null,
              promiseRef
            )
          }
          is DataCollectorResult.Failure -> {
            paypalRebornModuleHandlers.handleGetDeviceDataFromDataCollectorResult(
              null,
              dataCollectorResult.error,
              promiseRef
            )
          }
        }
      }
    } catch (ex: Exception) {
      promiseRef.reject(
          EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
          ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.value,
          PaypalDataConverter.createError(EXCEPTION_TYPES.KOTLIN_EXCEPTION.value, ex.message)
      )
    }
  }

  @ReactMethod
  fun requestOneTimePayment(data: ReadableMap, localPromise: Promise) {
    try {
      promiseRef = localPromise
      currentActivityRef = getCurrentActivity() as FragmentActivity
      val clientToken = data.getString("clientToken") ?: ""

      // Get launcher from MainActivity bridge
      val launcherBridge = PayPalLauncherBridge.getInstance()
      if (launcherBridge == null) {
        throw Exception("PayPalLauncher not initialized. MainActivity setup required.")
      }

      // Initialize PayPalClient with app link and deep link
      val appLinkUri = Uri.parse("https://photoaid.com")
      val deepLinkScheme = "${currentActivityRef.packageName}.braintree"

      payPalClientRef = PayPalClient(
        context = currentActivityRef,
        authorization = clientToken,
        appLinkReturnUrl = appLinkUri,
        deepLinkFallbackUrlScheme = deepLinkScheme
      )

      val checkoutRequest: PayPalCheckoutRequest = PaypalDataConverter.createCheckoutRequest(data)

      // Step 1: Create payment auth request
      payPalClientRef!!.createPaymentAuthRequest(currentActivityRef, checkoutRequest) { paymentAuthRequest ->
        when (paymentAuthRequest) {
          is PayPalPaymentAuthRequest.ReadyToLaunch -> {
            // Step 2: Launch PayPal flow
            val pendingRequest = launcherBridge.launch(currentActivityRef, paymentAuthRequest)
            when (pendingRequest) {
              is PayPalPendingRequest.Started -> {
                // Store pending request for later completion
                pendingPayPalRequest = pendingRequest.pendingRequestString
              }
              is PayPalPendingRequest.Failure -> {
                handlePayPalError(pendingRequest.error)
              }
            }
          }
          is PayPalPaymentAuthRequest.Failure -> {
            handlePayPalError(paymentAuthRequest.error)
          }
        }
      }

    } catch (ex: Exception) {
      localPromise.reject(
          EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
          ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.value,
          PaypalDataConverter.createError(EXCEPTION_TYPES.KOTLIN_EXCEPTION.value, ex.message)
      )
    }
  }

  @ReactMethod
  fun tokenizeCardData(data: ReadableMap, localPromise: Promise) {
    try {
      promiseRef = localPromise
      currentActivityRef = getCurrentActivity() as FragmentActivity
      val clientToken = data.getString("clientToken") ?: ""

      // V5: Direct initialization without BraintreeClient
      cardClientRef = CardClient(
        context = currentActivityRef,
        authorization = clientToken
      )

      val cardRequest: Card = PaypalDataConverter.createTokenizeCardRequest(data)
      // V5: CardResult sealed class instead of two parameters
      cardClientRef!!.tokenize(cardRequest) { cardResult ->
        when (cardResult) {
          is com.braintreepayments.api.card.CardResult.Success -> {
            handleCardTokenizeResult(cardResult.nonce, null)
          }
          is com.braintreepayments.api.card.CardResult.Failure -> {
            handleCardTokenizeResult(null, cardResult.error)
          }
        }
      }
    } catch (ex: Exception) {
      localPromise.reject(
          EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
          ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.value,
          PaypalDataConverter.createError(EXCEPTION_TYPES.KOTLIN_EXCEPTION.value, ex.message)
      )
    }
  }

  public fun handleCardTokenizeResult(
      cardNonce: CardNonce?,
      error: Exception?,
  ) {
    if (error != null) {
      paypalRebornModuleHandlers.onCardTokenizeFailure(error, promiseRef)
      return
    }
    if (cardNonce != null) {
      paypalRebornModuleHandlers.onCardTokenizeSuccessHandler(cardNonce, promiseRef)
    }
  }

  public fun handlePayPalAccountNonceResult(
      payPalAccountNonce: PayPalAccountNonce?,
      error: Exception?,
  ) {
    if (error != null) {
      paypalRebornModuleHandlers.onPayPalFailure(error, promiseRef)
      return
    }
    if (payPalAccountNonce != null) {
      paypalRebornModuleHandlers.onPayPalSuccessHandler(payPalAccountNonce, promiseRef)
    }
  }

  @ReactMethod
  fun handlePayPalReturnToApp(localPromise: Promise) {
    try {
      val pendingRequestString = pendingPayPalRequest
      if (pendingRequestString == null) {
        localPromise.reject("NO_PENDING_REQUEST", "No pending PayPal request found")
        return
      }

      val launcherBridge = PayPalLauncherBridge.getInstance()
      if (launcherBridge == null) {
        throw Exception("PayPalLauncher not available")
      }

      val intent = currentActivityRef.intent
      val pendingRequest = PayPalPendingRequest.Started(pendingRequestString)

      when (val result = launcherBridge.handleReturnToApp(pendingRequest, intent)) {
        is PayPalPaymentAuthResult.Success -> {
          // Step 3: Tokenize the successful authorization
          // V5: Pass PayPalPaymentAuthResult.Success directly, not individual parameters
          payPalClientRef?.tokenize(result) { tokenizeResult ->
            when (tokenizeResult) {
              is PayPalResult.Success -> {
                pendingPayPalRequest = null
                handlePayPalAccountNonceResult(tokenizeResult.nonce, null)
              }
              is PayPalResult.Failure -> {
                pendingPayPalRequest = null
                handlePayPalAccountNonceResult(null, tokenizeResult.error)
              }
              is PayPalResult.Cancel -> {
                pendingPayPalRequest = null
                localPromise.reject(
                  EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.value,
                  ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.value,
                  PaypalDataConverter.createError(EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.value, "User cancelled")
                )
              }
            }
          }
        }
        is PayPalPaymentAuthResult.NoResult -> {
          // User returned to app without completing - keep pending request
          localPromise.reject("NO_RESULT", "User returned without completing PayPal flow")
        }
        is PayPalPaymentAuthResult.Failure -> {
          pendingPayPalRequest = null
          handlePayPalError(result.error)
        }
      }

    } catch (ex: Exception) {
      localPromise.reject(
        EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
        "HANDLE_RETURN_ERROR",
        PaypalDataConverter.createError(EXCEPTION_TYPES.KOTLIN_EXCEPTION.value, ex.message)
      )
    }
  }

  private fun handlePayPalError(error: Exception) {
    paypalRebornModuleHandlers.onPayPalFailure(error, promiseRef)
  }

  override fun onHostResume() {
    // V5: Browser switch handling now done via PayPalLauncher and handlePayPalReturnToApp
    // This method kept empty for backward compatibility
  }

  override fun onNewIntent(intent: Intent) {
    android.util.Log.d("ExpoBraintreeModule", "[DeepLink] onNewIntent called")
    android.util.Log.d("ExpoBraintreeModule", "[DeepLink] currentActivityRef initialized: ${this::currentActivityRef.isInitialized}")
    android.util.Log.d("ExpoBraintreeModule", "[DeepLink] pendingPayPalRequest: $pendingPayPalRequest")
    android.util.Log.d("ExpoBraintreeModule", "[DeepLink] intent.data: ${intent.data}")

    if (this::currentActivityRef.isInitialized) {
      // Auto-handle PayPal return if there's a pending request
      if (pendingPayPalRequest != null && intent.data != null) {
        val uri = intent.data.toString()
        android.util.Log.d("ExpoBraintreeModule", "[DeepLink] Checking URI: $uri")
        // Check if this is a PayPal return (onetouch or braintree scheme)
        if (uri.contains("onetouch") || uri.contains("braintree")) {
          android.util.Log.d("ExpoBraintreeModule", "[DeepLink] PayPal return detected, handling...")
          // Set intent temporarily for PayPalLauncher to process
          currentActivityRef.setIntent(intent)
          handlePayPalReturn(intent)
          // Clear the intent data to prevent Expo Router from processing it
          currentActivityRef.setIntent(Intent())
          android.util.Log.d("ExpoBraintreeModule", "[DeepLink] PayPal return handled and intent cleared")
          return
        }
      }
      // For other intents, set normally
      android.util.Log.d("ExpoBraintreeModule", "[DeepLink] Not a PayPal return, setting intent normally")
      currentActivityRef.setIntent(intent)
    }
  }

  private fun handlePayPalReturn(intent: Intent) {
    val pendingRequestString = pendingPayPalRequest ?: return
    val launcherBridge = PayPalLauncherBridge.getInstance() ?: return

    val pendingRequest = PayPalPendingRequest.Started(pendingRequestString)

    when (val result = launcherBridge.handleReturnToApp(pendingRequest, intent)) {
      is PayPalPaymentAuthResult.Success -> {
        // Tokenize the successful authorization
        payPalClientRef?.tokenize(result) { tokenizeResult ->
          when (tokenizeResult) {
            is PayPalResult.Success -> {
              pendingPayPalRequest = null
              handlePayPalAccountNonceResult(tokenizeResult.nonce, null)
            }
            is PayPalResult.Failure -> {
              pendingPayPalRequest = null
              handlePayPalAccountNonceResult(null, tokenizeResult.error)
            }
            is PayPalResult.Cancel -> {
              pendingPayPalRequest = null
              if (this::promiseRef.isInitialized) {
                promiseRef.reject(
                  EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.value,
                  ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.value,
                  PaypalDataConverter.createError(EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.value, "User cancelled")
                )
              }
            }
          }
        }
      }
      is PayPalPaymentAuthResult.Failure -> {
        pendingPayPalRequest = null
        handlePayPalError(result.error)
      }
      is PayPalPaymentAuthResult.NoResult -> {
        // User canceled or no result - treat as cancellation
        pendingPayPalRequest = null
        if (this::promiseRef.isInitialized) {
          promiseRef.reject(
            EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.value,
            ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.value,
            PaypalDataConverter.createError(EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.value, "User cancelled")
          )
        }
      }
    }
  }

  override fun getName(): String {
    return NAME
  }

  override fun getConstants(): Map<String, Any>? {
    return emptyMap()
  }

  // empty required Implementations from interfaces
  override fun onHostPause() {}
  override fun onHostDestroy() {}
  override fun onActivityResult(
      activity: Activity,
      requestCode: Int,
      resultCode: Int,
      data: Intent?
  ) {}
}
