package com.expobraintree

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ThreeDSecureInfo(
    val liabilityShifted: Boolean,
    val liabilityShiftPossible: Boolean,
    val wasVerified: Boolean,
    val challengeRequired: Boolean
) : Parcelable

@Parcelize
data class GooglePayAddress(
    val recipientName: String?,
    val phoneNumber: String?,
    val streetAddress: String?,
    val extendedAddress: String?,
    val locality: String?,
    val region: String?,
    val postalCode: String?,
    val countryCodeAlpha2: String?
) : Parcelable

sealed class PaymentResultModel : Parcelable {
    @Parcelize
    data class Success(
        val nonce: String,
        val amount: String,
        val currency: String,
        val deviceData: String,
        val email: String,
        val paymentType: String,
        val threeDSecureInfo: ThreeDSecureInfo? = null,
        val shippingAddress: GooglePayAddress? = null,
        val billingAddress: GooglePayAddress? = null,
        val googlePayEmail: String? = null,
        val shippingOptionId: String? = null
    ) : PaymentResultModel()

    @Parcelize
    data class Error(
        val message: String?,
        val localizedMessage: String?
    ) : PaymentResultModel()

    @Parcelize
    object Cancel : PaymentResultModel()
}
