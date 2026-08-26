package com.expobraintree

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class PaymentMethod : Parcelable {
    @Parcelize
    data class PayPalCheckout(
        val amount: String,
        val currency: String,
        val intent: String? = null,
        val userAction: String? = null,
        val offerPayLater: Boolean = false,
        val requestBillingAgreement: Boolean = false,
        val isShippingAddressRequired: Boolean = false,
        val isShippingAddressEditable: Boolean = false,
        val shippingCallbackUrl: String? = null
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
        val merchantName: String,
        val isShippingAddressRequired: Boolean = false,
        val isPhoneNumberRequired: Boolean = false,
        val isBillingAddressRequired: Boolean = false,
        val isEmailRequired: Boolean = false,
        val shippingOptions: List<GooglePayShippingOption> = emptyList(),
        val defaultShippingOptionId: String? = null,
        val totalPriceLabel: String? = null,
    ) : PaymentMethod()

    @Parcelize
    data class ThreeDSecure(
        val nonce: String,
        val amount: String,
        val email: String,
        val currency: String = ""
    ) : PaymentMethod()
}

@Parcelize
data class GooglePayShippingOption(
    val id: String,
    val label: String,
    val description: String?,
    val price: String
) : Parcelable

@Parcelize
data class BasePaymentArgs(
    val clientToken: String,
    val paymentMethod: PaymentMethod,
    val email: String,
    val deviceData: String
) : Parcelable
