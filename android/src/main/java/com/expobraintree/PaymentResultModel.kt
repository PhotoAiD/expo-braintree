package com.expobraintree

import android.os.Parcelable
import com.braintreepayments.api.core.PostalAddress
import kotlinx.parcelize.Parcelize

@Parcelize
data class ThreeDSecureInfo(
    val liabilityShifted: Boolean,
    val liabilityShiftPossible: Boolean,
    val wasVerified: Boolean,
    val challengeRequired: Boolean
) : Parcelable

@Parcelize
data class PaymentAddress(
    val recipientName: String?,
    val phoneNumber: String?,
    val streetAddress: String?,
    val extendedAddress: String?,
    val locality: String?,
    val region: String?,
    val postalCode: String?,
    val countryCodeAlpha2: String?
) : Parcelable

fun PostalAddress?.toPaymentAddress(): PaymentAddress? {
    if (this == null) return null
    return PaymentAddress(
        recipientName = recipientName,
        phoneNumber = phoneNumber,
        streetAddress = streetAddress,
        extendedAddress = extendedAddress,
        locality = locality,
        region = region,
        postalCode = postalCode,
        countryCodeAlpha2 = countryCodeAlpha2
    )
}

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
        val shippingAddress: PaymentAddress? = null,
        val billingAddress: PaymentAddress? = null,
        val googlePayEmail: String? = null,
        val shippingOptionId: String? = null,
        val payPalEmail: String? = null,
        val payerId: String? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val phone: String? = null
    ) : PaymentResultModel()

    @Parcelize
    data class Error(
        val message: String?,
        val localizedMessage: String?
    ) : PaymentResultModel()

    @Parcelize
    object Cancel : PaymentResultModel()
}
