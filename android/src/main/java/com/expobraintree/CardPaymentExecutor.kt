package com.expobraintree

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.card.CardClient
import com.braintreepayments.api.card.CardResult
import com.braintreepayments.api.threedsecure.ThreeDSecureClient
import com.braintreepayments.api.threedsecure.ThreeDSecureLauncher
import com.braintreepayments.api.threedsecure.ThreeDSecureLauncherCallback
import com.braintreepayments.api.threedsecure.ThreeDSecurePaymentAuthRequest
import com.braintreepayments.api.threedsecure.ThreeDSecurePaymentAuthResult
import com.braintreepayments.api.threedsecure.ThreeDSecureRequest
import com.braintreepayments.api.threedsecure.ThreeDSecureResult

class CardPaymentExecutor(
    private val context: Context,
    listener: PaymentExecutorListener,
    args: BasePaymentArgs
) : BasePaymentExecutor(args, listener), ThreeDSecureLauncherCallback {

    private val cardClient: CardClient
    private var threeDSecureClient: ThreeDSecureClient? = null
    private var threeDSecureLauncher: ThreeDSecureLauncher? = null
    private var currentCardNonce: String? = null

    init {
        Log.d(TAG, "[init] Creating CardClient, tokenPresent=${args.clientToken.isNotEmpty()}")
        cardClient = CardClient(
            context = context,
            authorization = args.clientToken
        )

        // Initialize 3DS client and launcher early if 3DS is enabled
        val paymentMethod = args.paymentMethod as? PaymentMethod.Card
        if (paymentMethod?.use3DSecure == true && context is FragmentActivity) {
            Log.d(TAG, "[init] Initializing 3DS client and launcher early")
            threeDSecureClient = ThreeDSecureClient(
                context = context,
                authorization = args.clientToken
            )
            threeDSecureLauncher = ThreeDSecureLauncher(context, this)
        }
    }

    override fun requestPayment(activity: FragmentActivity) {
        val paymentMethod = args.paymentMethod as? PaymentMethod.Card
        if (paymentMethod == null) {
            Log.e(TAG, "[requestPayment] Invalid payment method")
            handleError("Invalid payment method", "Expected Card")
            return
        }

        Log.d(TAG, "[requestPayment] Starting card tokenization, use3DSecure=${paymentMethod.use3DSecure}")

        val card = Card().apply {
            number = paymentMethod.cardNumber
            expirationMonth = paymentMethod.expirationMonth
            expirationYear = paymentMethod.expirationYear
            if (!paymentMethod.cvv.isNullOrEmpty()) {
                cvv = paymentMethod.cvv
            }
            if (!paymentMethod.postalCode.isNullOrEmpty()) {
                postalCode = paymentMethod.postalCode
            }
        }

        Log.d(TAG, "[requestPayment] Card prepared, tokenizing...")

        cardClient.tokenize(card) { cardResult ->
            Log.d(TAG, "[requestPayment] tokenize callback, result type: ${cardResult::class.simpleName}")

            when (cardResult) {
                is CardResult.Success -> {
                    val nonce = cardResult.nonce
                    Log.d(TAG, "[requestPayment] Card tokenization success, cardType=${nonce.cardType}")

                    if (paymentMethod.use3DSecure) {
                        Log.d(TAG, "[requestPayment] 3DS enabled, starting verification")
                        currentCardNonce = nonce.string
                        requestThreeDSecure(activity, nonce)
                    } else {
                        Log.d(TAG, "[requestPayment] 3DS not required, returning nonce")
                        handleSuccessWithCardMethod(nonce.string)
                    }
                }
                is CardResult.Failure -> {
                    Log.e(TAG, "[requestPayment] Card tokenization failure: ${cardResult.error.message}")
                    handleError(cardResult.error.message, cardResult.error.localizedMessage)
                }
            }
        }
    }

    private fun requestThreeDSecure(activity: FragmentActivity, cardNonce: com.braintreepayments.api.card.CardNonce) {
        val paymentMethod = args.paymentMethod as PaymentMethod.Card
        Log.d(TAG, "[requestThreeDSecure] Starting 3DS verification")

        // Ensure 3DS client and launcher are initialized
        if (threeDSecureClient == null) {
            Log.d(TAG, "[requestThreeDSecure] Late initialization of 3DS client")
            threeDSecureClient = ThreeDSecureClient(
                context = context,
                authorization = args.clientToken
            )
        }

        if (threeDSecureLauncher == null) {
            Log.e(TAG, "[requestThreeDSecure] ThreeDSecureLauncher not initialized - this should not happen!")
            handleError("3D Secure initialization error", "Launcher not initialized early enough")
            return
        }

        Log.d(TAG, "[requestThreeDSecure] cardType: ${cardNonce.cardType}, amount: ${paymentMethod.amount}")

        val threeDSecureRequest = ThreeDSecureRequest(
            nonce = cardNonce.string,
            amount = paymentMethod.amount,
            email = args.email
        )

        Log.d(TAG, "[requestThreeDSecure] Creating 3DS payment auth request")

        threeDSecureClient!!.createPaymentAuthRequest(
            context = activity,
            request = threeDSecureRequest
        ) { paymentAuthRequest ->
            Log.d(TAG, "[requestThreeDSecure] createPaymentAuthRequest callback, type: ${paymentAuthRequest::class.simpleName}")

            when (paymentAuthRequest) {
                is ThreeDSecurePaymentAuthRequest.Failure -> {
                    Log.e(TAG, "[requestThreeDSecure] 3DS auth request failure: ${paymentAuthRequest.error.message}")
                    handleError(paymentAuthRequest.error.message, paymentAuthRequest.error.localizedMessage)
                }
                is ThreeDSecurePaymentAuthRequest.LaunchNotRequired -> {
                    Log.d(TAG, "[requestThreeDSecure] 3DS launch not required, using nonce directly")
                    handleSuccessWithCardMethod(paymentAuthRequest.nonce.string)
                }
                is ThreeDSecurePaymentAuthRequest.ReadyToLaunch -> {
                    Log.d(TAG, "[requestThreeDSecure] 3DS ready to launch, launching...")
                    threeDSecureLauncher?.launch(paymentAuthRequest)
                }
            }
        }
    }

    override fun onThreeDSecurePaymentAuthResult(paymentAuthResult: ThreeDSecurePaymentAuthResult) {
        Log.d(TAG, "[onThreeDSecurePaymentAuthResult] Received 3DS auth result, tokenizing...")

        threeDSecureClient?.tokenize(paymentAuthResult) { threeDSecureResult ->
            Log.d(TAG, "[onThreeDSecurePaymentAuthResult] tokenize callback, type: ${threeDSecureResult::class.simpleName}")

            when (threeDSecureResult) {
                is ThreeDSecureResult.Success -> {
                    val threeDSecureNonce = threeDSecureResult.nonce
                    val threeDSecureInfo = threeDSecureNonce.threeDSecureInfo

                    Log.d(TAG, "[onThreeDSecurePaymentAuthResult] 3DS success, liabilityShiftPossible=${threeDSecureInfo.liabilityShiftPossible}, liabilityShifted=${threeDSecureInfo.liabilityShifted}")

                    if (!threeDSecureInfo.liabilityShiftPossible) {
                        Log.e(TAG, "[onThreeDSecurePaymentAuthResult] Liability shift not possible")
                        handleError("3D Secure liability shift not possible", "3D Secure liability shift not possible")
                        return@tokenize
                    }

                    if (!threeDSecureInfo.liabilityShifted) {
                        Log.e(TAG, "[onThreeDSecurePaymentAuthResult] Liability not shifted")
                        handleError("3D Secure liability not shifted", "3D Secure liability not shifted")
                        return@tokenize
                    }

                    Log.d(TAG, "[onThreeDSecurePaymentAuthResult] 3DS verification successful")
                    handleSuccessWithCardMethod(threeDSecureNonce.string)
                }
                is ThreeDSecureResult.Failure -> {
                    Log.e(TAG, "[onThreeDSecurePaymentAuthResult] 3DS failure: ${threeDSecureResult.error.message}")
                    handleError(threeDSecureResult.error.message, threeDSecureResult.error.localizedMessage)
                }
                is ThreeDSecureResult.Cancel -> {
                    Log.d(TAG, "[onThreeDSecurePaymentAuthResult] 3DS cancelled by user")
                    handleCancel()
                }
            }
        }
    }

    private fun handleSuccessWithCardMethod(nonce: String) {
        val paymentMethod = args.paymentMethod as PaymentMethod.Card
        handleSuccess(
            nonce = nonce,
            amount = paymentMethod.amount,
            currency = if (paymentMethod.currency.isNotEmpty()) paymentMethod.currency else "USD",
            paymentType = "Card"
        )
    }

    override fun onResume(intent: Intent) {
        Log.d(TAG, "[onResume] Activity resumed")
    }

    override fun onDestroy() {
        Log.d(TAG, "[onDestroy] Cleaning up")
        threeDSecureLauncher = null
        threeDSecureClient = null
    }
}
