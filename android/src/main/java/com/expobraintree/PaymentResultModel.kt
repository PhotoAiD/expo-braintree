package com.expobraintree

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class PaymentResultModel : Parcelable {
    @Parcelize
    data class Success(
        val nonce: String,
        val amount: String,
        val currency: String,
        val deviceData: String,
        val email: String,
        val paymentType: String
    ) : PaymentResultModel()

    @Parcelize
    data class Error(
        val message: String?,
        val localizedMessage: String?
    ) : PaymentResultModel()

    @Parcelize
    object Cancel : PaymentResultModel()
}
