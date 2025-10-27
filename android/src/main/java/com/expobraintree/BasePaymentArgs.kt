package com.expobraintree

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class PaymentMethod : Parcelable {
    @Parcelize
    data class PayPalCheckout(
        val amount: String,
        val currency: String
    ) : PaymentMethod()

    @Parcelize
    data class PayPalVault(
        val billingAgreementDescription: String? = null
    ) : PaymentMethod()

    @Parcelize
    data class Card(
        val cardNumber: String,
        val expirationMonth: String,
        val expirationYear: String,
        val cvv: String?,
        val postalCode: String?,
        val use3DSecure: Boolean,
        val amount: String = "0",
        val currency: String = ""
    ) : PaymentMethod()

    @Parcelize
    data class GooglePay(
        val amount: String,
        val currency: String,
        val merchantName: String
    ) : PaymentMethod()

    @Parcelize
    data class ThreeDSecure(
        val nonce: String,
        val amount: String,
        val email: String
    ) : PaymentMethod()
}

@Parcelize
data class BasePaymentArgs(
    val clientToken: String,
    val paymentMethod: PaymentMethod,
    val email: String,
    val deviceData: String
) : Parcelable
