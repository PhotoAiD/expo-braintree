package com.expobraintree

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.datacollector.DataCollectorRequest
import com.braintreepayments.api.datacollector.DataCollectorResult
import com.braintreepayments.api.googlepay.GooglePayClient
import com.braintreepayments.api.googlepay.GooglePayReadinessResult
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments

class ExpoBraintreeModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), ActivityEventListener {

    private val TAG = "[ExpoBraintree]"
    private var paymentPromise: Promise? = null
    private var dataCollectorRef: DataCollector? = null
    private val paypalRebornModuleHandlers: PaypalRebornModuleHandlers = PaypalRebornModuleHandlers()

    init {
        Log.d(TAG, "[init] Module initialized")
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String = "ExpoBraintree"

    @ReactMethod
    fun getDeviceDataFromDataCollector(clientToken: String?, localPromise: Promise) {
        try {
            Log.d(TAG, "[getDeviceDataFromDataCollector] Starting, tokenPresent=${!clientToken.isNullOrEmpty()}")

            dataCollectorRef = DataCollector(
                context = reactContext,
                authorization = clientToken ?: ""
            )

            val dataCollectorRequest = DataCollectorRequest(hasUserLocationConsent = false)
            dataCollectorRef!!.collectDeviceData(
                context = reactContext,
                request = dataCollectorRequest
            ) { dataCollectorResult ->
                Log.d(TAG, "[getDeviceDataFromDataCollector] callback, type: ${dataCollectorResult::class.simpleName}")

                when (dataCollectorResult) {
                    is DataCollectorResult.Success -> {
                        Log.d(TAG, "[getDeviceDataFromDataCollector] Success")
                        paypalRebornModuleHandlers.handleGetDeviceDataFromDataCollectorResult(
                            dataCollectorResult.deviceData,
                            null,
                            localPromise
                        )
                    }
                    is DataCollectorResult.Failure -> {
                        Log.e(TAG, "[getDeviceDataFromDataCollector] Failure: ${dataCollectorResult.error.message}")
                        paypalRebornModuleHandlers.handleGetDeviceDataFromDataCollectorResult(
                            null,
                            dataCollectorResult.error,
                            localPromise
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "[getDeviceDataFromDataCollector] Exception: ${ex.message}")
            localPromise.reject(
                EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
                ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.value,
                PaypalDataConverter.createError(EXCEPTION_TYPES.KOTLIN_EXCEPTION.value, ex.message)
            )
        }
    }

    @ReactMethod
    fun isGooglePayAvailable(clientToken: String?, localPromise: Promise) {
        try {
            Log.d(TAG, "[isGooglePayAvailable] Checking availability, tokenPresent=${!clientToken.isNullOrEmpty()}")

            if (clientToken.isNullOrEmpty()) {
                localPromise.resolve(false)
                return
            }

            val googlePayClient = GooglePayClient(
                context = reactContext,
                authorization = clientToken
            )

            googlePayClient.isReadyToPay(reactContext) { googlePayReadinessResult ->
                Log.d(TAG, "[isGooglePayAvailable] callback, type: ${googlePayReadinessResult?.let { it::class.simpleName } ?: "null"}")

                when (googlePayReadinessResult) {
                    is GooglePayReadinessResult.ReadyToPay -> {
                        Log.d(TAG, "[isGooglePayAvailable] Google Pay available")
                        localPromise.resolve(true)
                    }
                    is GooglePayReadinessResult.NotReadyToPay -> {
                        Log.d(TAG, "[isGooglePayAvailable] Google Pay not available")
                        localPromise.resolve(false)
                    }
                    else -> {
                        Log.d(TAG, "[isGooglePayAvailable] Google Pay check returned null")
                        localPromise.resolve(false)
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "[isGooglePayAvailable] Exception: ${ex.message}")
            localPromise.resolve(false)
        }
    }

    @ReactMethod
    fun requestBillingAgreement(data: ReadableMap, localPromise: Promise) {
        try {
            Log.d(TAG, "[requestBillingAgreement] Starting PayPal vault flow")
            val clientToken = data.getString("clientToken") ?: ""
            val deviceData = data.getString("deviceData") ?: ""
            val email = data.getString("email") ?: ""

            val paymentArgs = BasePaymentArgs(
                clientToken = clientToken,
                paymentMethod = PaymentMethod.PayPalVault(
                    billingAgreementDescription = data.getString("billingAgreementDescription")
                ),
                email = email,
                deviceData = deviceData
            )

            launchBraintreePaymentActivity(paymentArgs, localPromise)
        } catch (ex: Exception) {
            Log.e(TAG, "[requestBillingAgreement] Exception: ${ex.message}")
            localPromise.reject(
                EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
                ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.value,
                PaypalDataConverter.createError(EXCEPTION_TYPES.KOTLIN_EXCEPTION.value, ex.message)
            )
        }
    }

    @ReactMethod
    fun requestOneTimePayment(data: ReadableMap, localPromise: Promise) {
        try {
            Log.d(TAG, "[requestOneTimePayment] Starting PayPal checkout flow")
            val clientToken = data.getString("clientToken") ?: ""
            val deviceData = data.getString("deviceData") ?: ""
            val email = data.getString("email") ?: ""
            val amount = data.getString("amount") ?: ""
            val currency = data.getString("currencyCode") ?: "USD"

            val paymentArgs = BasePaymentArgs(
                clientToken = clientToken,
                paymentMethod = PaymentMethod.PayPalCheckout(
                    amount = amount,
                    currency = currency
                ),
                email = email,
                deviceData = deviceData
            )

            launchBraintreePaymentActivity(paymentArgs, localPromise)
        } catch (ex: Exception) {
            Log.e(TAG, "[requestOneTimePayment] Exception: ${ex.message}")
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
            Log.d(TAG, "[tokenizeCardData] Starting card tokenization")
            val clientToken = data.getString("clientToken") ?: ""
            val deviceData = data.getString("deviceData") ?: ""
            val email = data.getString("email") ?: ""
            val amount = data.getString("amount") ?: "0"
            val currency = data.getString("currency") ?: "USD"
            val use3DSecure = data.getBoolean("use3DSecure")

            Log.d(TAG, "[tokenizeCardData] Parameters - amount: $amount, currency: $currency, use3DSecure: $use3DSecure")

            val cardData = data.getMap("card")
            Log.d(TAG, "[tokenizeCardData] Card data present: ${cardData != null}, has number: ${cardData?.hasKey("number")}")
            val paymentArgs = BasePaymentArgs(
                clientToken = clientToken,
                paymentMethod = PaymentMethod.Card(
                    cardNumber = cardData?.getString("number") ?: "",
                    expirationMonth = cardData?.getString("expirationMonth") ?: "",
                    expirationYear = cardData?.getString("expirationYear") ?: "",
                    cvv = cardData?.getString("cvv"),
                    postalCode = cardData?.getString("postalCode"),
                    use3DSecure = use3DSecure,
                    amount = amount,
                    currency = currency
                ),
                email = email,
                deviceData = deviceData
            )

            launchBraintreePaymentActivity(paymentArgs, localPromise)
        } catch (ex: Exception) {
            Log.e(TAG, "[tokenizeCardData] Exception: ${ex.message}")
            localPromise.reject(
                EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
                ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.value,
                PaypalDataConverter.createError(EXCEPTION_TYPES.KOTLIN_EXCEPTION.value, ex.message)
            )
        }
    }

    @ReactMethod
    fun requestGooglePayPayment(data: ReadableMap, localPromise: Promise) {
        try {
            Log.d(TAG, "[requestGooglePayPayment] Starting Google Pay flow")
            val clientToken = data.getString("clientToken") ?: ""
            val deviceData = data.getString("deviceData") ?: ""
            val email = data.getString("email") ?: ""
            val amount = data.getString("totalPrice") ?: ""
            val currency = data.getString("currencyCode") ?: "USD"

            val paymentArgs = BasePaymentArgs(
                clientToken = clientToken,
                paymentMethod = PaymentMethod.GooglePay(
                    amount = amount,
                    currency = currency,
                    merchantName = "PhotoAiD"
                ),
                email = email,
                deviceData = deviceData
            )

            launchBraintreePaymentActivity(paymentArgs, localPromise)
        } catch (ex: Exception) {
            Log.e(TAG, "[requestGooglePayPayment] Exception: ${ex.message}")
            localPromise.reject(
                EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
                ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.value,
                PaypalDataConverter.createError(EXCEPTION_TYPES.KOTLIN_EXCEPTION.value, ex.message)
            )
        }
    }

    @ReactMethod
    fun verifyThreeDSecure(data: ReadableMap, localPromise: Promise) {
        try {
            Log.d(TAG, "[verifyThreeDSecure] Starting 3DS verification")
            val clientToken = data.getString("clientToken") ?: ""
            val deviceData = data.getString("deviceData") ?: ""
            val email = data.getString("email") ?: ""
            val nonce = data.getString("nonce") ?: ""
            val amount = data.getString("amount") ?: ""

            val paymentArgs = BasePaymentArgs(
                clientToken = clientToken,
                paymentMethod = PaymentMethod.ThreeDSecure(
                    nonce = nonce,
                    amount = amount,
                    email = email
                ),
                email = email,
                deviceData = deviceData
            )

            launchBraintreePaymentActivity(paymentArgs, localPromise)
        } catch (ex: Exception) {
            Log.e(TAG, "[verifyThreeDSecure] Exception: ${ex.message}")
            localPromise.reject(
                EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
                ERROR_TYPES.API_CLIENT_INITIALIZATION_ERROR.value,
                PaypalDataConverter.createError(EXCEPTION_TYPES.KOTLIN_EXCEPTION.value, ex.message)
            )
        }
    }

    private fun launchBraintreePaymentActivity(args: BasePaymentArgs, promise: Promise) {
        Log.d(TAG, "[launchBraintreePaymentActivity] Launching payment activity")
        val activity = getCurrentActivity()
        if (activity == null) {
            Log.e(TAG, "[launchBraintreePaymentActivity] No current activity")
            promise.reject("NO_ACTIVITY", "No current activity")
            return
        }

        paymentPromise = promise
        val intent = BraintreePaymentActivity.getIntent(activity, args)
        activity.startActivityForResult(intent, BRAINTREE_PAYMENT_REQUEST_CODE)
    }

    override fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        Log.d(TAG, "[onActivityResult] requestCode=$requestCode, resultCode=$resultCode, dataPresent=${data != null}")

        if (requestCode != BRAINTREE_PAYMENT_REQUEST_CODE) {
            return
        }

        val promise = paymentPromise
        if (promise == null) {
            Log.e(TAG, "[onActivityResult] No promise stored")
            return
        }

        paymentPromise = null

        when (resultCode) {
            BraintreePaymentActivity.RESULT_CODE_SUCCESS -> {
                Log.d(TAG, "[onActivityResult] Payment success")
                val result = data?.getParcelableExtra<PaymentResultModel.Success>(
                    BraintreePaymentActivity.PAYMENT_RESULT
                )
                if (result != null) {
                    handleSuccessResult(result, promise)
                } else {
                    Log.e(TAG, "[onActivityResult] Success but no result data")
                    promise.reject("NO_RESULT", "Payment succeeded but no result data")
                }
            }
            BraintreePaymentActivity.RESULT_CODE_ERROR -> {
                Log.e(TAG, "[onActivityResult] Payment error")
                val result = data?.getParcelableExtra<PaymentResultModel.Error>(
                    BraintreePaymentActivity.PAYMENT_RESULT
                )
                if (result != null) {
                    promise.reject(
                        EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
                        result.message ?: "Payment error",
                        PaypalDataConverter.createError(
                            EXCEPTION_TYPES.KOTLIN_EXCEPTION.value,
                            result.localizedMessage
                        )
                    )
                } else {
                    Log.e(TAG, "[onActivityResult] Error but no result data")
                    promise.reject("PAYMENT_ERROR", "Payment failed")
                }
            }
            BraintreePaymentActivity.RESULT_CODE_CANCEL -> {
                Log.d(TAG, "[onActivityResult] Payment cancelled")
                promise.reject(
                    EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.value,
                    ERROR_TYPES.USER_CANCEL_TRANSACTION_ERROR.value,
                    PaypalDataConverter.createError(
                        EXCEPTION_TYPES.USER_CANCEL_EXCEPTION.value,
                        "User cancelled"
                    )
                )
            }
            else -> {
                Log.e(TAG, "[onActivityResult] Unknown result code: $resultCode")
                promise.reject("UNKNOWN_RESULT", "Unknown payment result")
            }
        }
    }

    private fun handleSuccessResult(result: PaymentResultModel.Success, promise: Promise) {
        Log.d(TAG, "[handleSuccessResult] paymentType=${result.paymentType}")

        when (result.paymentType) {
            "PayPal" -> {
                val map = Arguments.createMap().apply {
                    putString("nonce", result.nonce)
                    putString("email", result.email)
                    putString("deviceData", result.deviceData)
                }
                promise.resolve(map)
            }
            "Card", "Card3DS" -> {
                val map = Arguments.createMap().apply {
                    putString("nonce", result.nonce)
                    putString("deviceData", result.deviceData)
                }
                promise.resolve(map)
            }
            "GooglePay" -> {
                val map = Arguments.createMap().apply {
                    putString("nonce", result.nonce)
                    putString("deviceData", result.deviceData)
                }
                promise.resolve(map)
            }
            else -> {
                Log.w(TAG, "[handleSuccessResult] Unknown payment type: ${result.paymentType}")
                val map = Arguments.createMap().apply {
                    putString("nonce", result.nonce)
                    putString("deviceData", result.deviceData)
                }
                promise.resolve(map)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        Log.d(TAG, "[onNewIntent] Intent received (no-op in new architecture)")
    }

    override fun getConstants(): Map<String, Any> {
        return emptyMap()
    }

    companion object {
        private const val BRAINTREE_PAYMENT_REQUEST_CODE = 8877
    }
}
