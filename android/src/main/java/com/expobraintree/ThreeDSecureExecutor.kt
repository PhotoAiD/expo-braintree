package com.expobraintree

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.threedsecure.ThreeDSecureClient
import com.braintreepayments.api.threedsecure.ThreeDSecureLauncher
import com.braintreepayments.api.threedsecure.ThreeDSecureLauncherCallback
import com.braintreepayments.api.threedsecure.ThreeDSecurePaymentAuthRequest
import com.braintreepayments.api.threedsecure.ThreeDSecurePaymentAuthResult
import com.braintreepayments.api.threedsecure.ThreeDSecureRequest
import com.braintreepayments.api.threedsecure.ThreeDSecureResult

class ThreeDSecureExecutor(
    private val context: Context,
    listener: PaymentExecutorListener,
    args: BasePaymentArgs
) : BasePaymentExecutor(args, listener), ThreeDSecureLauncherCallback {

    private val threeDSecureClient: ThreeDSecureClient
    private var threeDSecureLauncher: ThreeDSecureLauncher? = null

    init {
        Log.d(TAG, "[init] Creating ThreeDSecureClient, tokenPresent=${args.clientToken.isNotEmpty()}")
        threeDSecureClient = ThreeDSecureClient(
            context = context,
            authorization = args.clientToken
        )
    }

    override fun requestPayment(activity: FragmentActivity) {
        val paymentMethod = args.paymentMethod as? PaymentMethod.ThreeDSecure
        if (paymentMethod == null) {
            Log.e(TAG, "[requestPayment] Invalid payment method")
            handleError("Invalid payment method", "Expected ThreeDSecure")
            return
        }

        Log.d(TAG, "[requestPayment] Starting 3DS verification, amount=${paymentMethod.amount}")

        threeDSecureLauncher = ThreeDSecureLauncher(activity, this)

        val threeDSecureRequest = ThreeDSecureRequest(
            nonce = paymentMethod.nonce,
            amount = paymentMethod.amount,
            email = paymentMethod.email
        )

        Log.d(TAG, "[requestPayment] Creating 3DS payment auth request")

        threeDSecureClient.createPaymentAuthRequest(
            context = activity,
            request = threeDSecureRequest
        ) { paymentAuthRequest ->
            Log.d(TAG, "[requestPayment] createPaymentAuthRequest callback, type: ${paymentAuthRequest::class.simpleName}")

            when (paymentAuthRequest) {
                is ThreeDSecurePaymentAuthRequest.ReadyToLaunch -> {
                    Log.d(TAG, "[requestPayment] Ready to launch 3DS, launching...")
                    threeDSecureLauncher?.launch(paymentAuthRequest)
                }
                is ThreeDSecurePaymentAuthRequest.LaunchNotRequired -> {
                    val nonce = paymentAuthRequest.nonce
                    val info = nonce.threeDSecureInfo
                    Log.d(TAG, "[requestPayment] 3DS launch not required, liabilityShifted=${info.liabilityShifted}, liabilityShiftPossible=${info.liabilityShiftPossible}")
                    handleSuccess(
                        nonce = nonce.string,
                        amount = paymentMethod.amount,
                        currency = paymentMethod.currency,
                        paymentType = "Card3DS",
                        threeDSecureInfo = ThreeDSecureInfo(
                            liabilityShifted = info.liabilityShifted,
                            liabilityShiftPossible = info.liabilityShiftPossible,
                            wasVerified = info.wasVerified,
                            challengeRequired = false
                        )
                    )
                }
                is ThreeDSecurePaymentAuthRequest.Failure -> {
                    Log.e(TAG, "[requestPayment] 3DS auth request failure: ${paymentAuthRequest.error.message}")
                    handleError(paymentAuthRequest.error.message, paymentAuthRequest.error.localizedMessage)
                }
            }
        }
    }

    override fun onThreeDSecurePaymentAuthResult(paymentAuthResult: ThreeDSecurePaymentAuthResult) {
        Log.d(TAG, "[onThreeDSecurePaymentAuthResult] Received 3DS auth result, tokenizing...")

        threeDSecureClient.tokenize(paymentAuthResult) { threeDSecureResult ->
            Log.d(TAG, "[onThreeDSecurePaymentAuthResult] tokenize callback, type: ${threeDSecureResult::class.simpleName}")

            when (threeDSecureResult) {
                is ThreeDSecureResult.Success -> {
                    val threeDSecureNonce = threeDSecureResult.nonce
                    val info = threeDSecureNonce.threeDSecureInfo

                    Log.d(TAG, "[onThreeDSecurePaymentAuthResult] 3DS success after challenge, liabilityShiftPossible=${info.liabilityShiftPossible}, liabilityShifted=${info.liabilityShifted}, wasVerified=${info.wasVerified}")

                    val paymentMethod = args.paymentMethod as PaymentMethod.ThreeDSecure
                    handleSuccess(
                        nonce = threeDSecureNonce.string,
                        amount = paymentMethod.amount,
                        currency = paymentMethod.currency,
                        paymentType = "Card3DS",
                        threeDSecureInfo = ThreeDSecureInfo(
                            liabilityShifted = info.liabilityShifted,
                            liabilityShiftPossible = info.liabilityShiftPossible,
                            wasVerified = info.wasVerified,
                            challengeRequired = true
                        )
                    )
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

    override fun onResume(intent: Intent) {
        Log.d(TAG, "[onResume] Activity resumed")
    }

    override fun onDestroy() {
        Log.d(TAG, "[onDestroy] Cleaning up")
        threeDSecureLauncher = null
    }
}
