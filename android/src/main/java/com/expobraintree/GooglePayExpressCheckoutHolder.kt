package com.expobraintree

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Process-wide holder for the Google Pay express-checkout configuration.
 *
 * The Google Pay dynamic-price-update callback runs in a [GooglePayCallbackService]
 * that Google Play Services binds to in this app's own process. The service has no
 * direct reference to the payment request, so the executor seeds this singleton with
 * the base price and delivery options, and the service reads it back to recompute the
 * total when the user changes a delivery method. Seeding happens in the executor's
 * init (not only before launch) so the holder is restored when the host activity is
 * recreated behind an open sheet (rotation, process death) — the executor is rebuilt
 * from the persisted payment args while [seed] is otherwise never called again.
 */
object GooglePayExpressCheckoutHolder {

    @Volatile
    var basePrice: String = "0"
        private set

    @Volatile
    var currencyCode: String = "USD"
        private set

    @Volatile
    var shippingOptions: List<GooglePayShippingOption> = emptyList()
        private set

    @Volatile
    var defaultShippingOptionId: String? = null
        private set

    @Volatile
    var totalPriceLabel: String? = null
        private set

    fun seed(
        basePrice: String,
        currencyCode: String,
        shippingOptions: List<GooglePayShippingOption>,
        defaultShippingOptionId: String?,
        totalPriceLabel: String? = null
    ) {
        this.basePrice = basePrice
        this.currencyCode = currencyCode
        this.shippingOptions = shippingOptions
        this.defaultShippingOptionId = defaultShippingOptionId ?: shippingOptions.firstOrNull()?.id
        this.totalPriceLabel = totalPriceLabel
    }

    fun clear() {
        basePrice = "0"
        currencyCode = "USD"
        shippingOptions = emptyList()
        defaultShippingOptionId = null
        totalPriceLabel = null
    }

    fun optionById(id: String?): GooglePayShippingOption? =
        shippingOptions.firstOrNull { it.id == id }

    /** Total shown in the sheet: base price + the selected delivery option's price. */
    fun totalForOption(id: String?): String = totalFor(basePrice, shippingOptions, id)

    /**
     * Pure variant for callers that hold the payment args themselves (the executor),
     * so the returned amount never depends on this holder's mutable state.
     */
    fun totalFor(
        basePrice: String,
        options: List<GooglePayShippingOption>,
        id: String?
    ): String {
        val base = basePrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val delivery = options.firstOrNull { it.id == id }?.price?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return base.add(delivery).setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
}
