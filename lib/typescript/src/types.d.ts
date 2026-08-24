export declare enum BTPayPalCheckoutIntent {
    authorize = "authorize",
    order = "order",
    sale = "sale"
}
export declare enum BTPayPalRequestUserAction {
    none = "none",
    payNow = "payNow"
}
export declare enum BoolValue {
    true = "true",
    false = "false"
}
export type RequestBillingAgreementOptions = {
    clientToken: string;
    billingAgreementDescription?: string;
    displayName?: string;
    localeCode?: string;
    userAuthenticationEmail?: string;
    offerCredit?: BoolValue;
    isShippingAddressRequired?: BoolValue;
    isShippingAddressEditable?: BoolValue;
    isAccessibilityElement?: BoolValue;
};
export type RequestOneTimePaymentOptions = {
    amount: string;
    intent?: BTPayPalCheckoutIntent;
    /**
     * Controls the final button in the PayPal sheet. The default shows
     * "Continue", meaning the final amount is confirmed in-app after return.
     * Use `payNow` when the sheet total is the final charge — mandatory with
     * `shippingCallbackUrl`: PayPal fires shipping callbacks only in the
     * Pay Now flow, and the sheet total is then amount + selected shipping.
     */
    userAction?: BTPayPalRequestUserAction;
    offerPayLater?: BoolValue;
    currencyCode?: string;
    requestBillingAgreement?: BoolValue;
    clientToken: string;
    /**
     * When true, the buyer's shipping address is collected in the PayPal sheet
     * and returned as `shippingAddress` in the result (express checkout).
     */
    isShippingAddressRequired?: boolean;
    /**
     * Lets the buyer pick a different shipping address inside the PayPal sheet.
     * Only relevant when isShippingAddressRequired is true.
     */
    isShippingAddressEditable?: boolean;
    /**
     * HTTPS URL of a server endpoint PayPal calls when the buyer changes the
     * shipping address or shipping option inside the PayPal sheet, so shipping
     * options and updated amounts can be served directly in the sheet.
     * The endpoint must implement Braintree's PayPal shipping-callback contract
     * (public HTTPS, no redirects, 200 with the documented JSON schema) and its
     * domain must be registered per environment in the Braintree Control Panel
     * (Settings → Processing → PayPal → Options → Shipping Callback Domains).
     * One-time checkout only, and only in the Pay Now flow with
     * `isShippingAddressRequired` and `isShippingAddressEditable` set — under
     * `userAction: none` (Continue) or with shipping disabled, PayPal silently
     * sends no callbacks. Invalid URLs are silently ignored.
     */
    shippingCallbackUrl?: string;
};
export type TokenizeCardOptions = {
    number: string;
    expirationMonth: string;
    expirationYear: string;
    cvv: string;
    postalCode?: string;
    clientToken: string;
    amount?: string;
    currency?: string;
    use3DSecure?: boolean;
    /**
     * The URL scheme for OOB (Out-of-Band) 3DS authentication.
     * Example: "photoaid://3ds-return"
     */
    requestorAppURL?: string;
};
export type BTPayPalAccountNonceAddressResult = {
    recipientName?: string;
    streetAddress?: string;
    extendedAddress?: string;
    locality?: string;
    countryCodeAlpha2?: string;
    postalCode?: string;
    region?: string;
};
export type BTPayPalAccountNonceResult = {
    email?: string;
    payerID?: string;
    nonce: string;
    firstName?: string;
    lastName?: string;
    phone?: string;
    billingAddress?: BTPayPalAccountNonceAddressResult;
    shippingAddress?: BTPayPalAccountNonceAddressResult;
};
export type BTCardTokenizationNonceResult = {
    nonce: string;
    cardNetwork?: string;
    lastTwo?: string;
    lastFour?: string;
    expirationMonth?: string;
    expirationYear?: string;
    /** Present when use3DSecure is true */
    threeDSecureInfo?: ThreeDSecureInfo;
};
export type BTPayPalGetDeviceDataResult = string;
export type BTPayPalError = {
    code?: string;
    message?: string;
    domain?: string;
};
export type ApplePaySummaryItem = {
    label: string;
    amount: string;
};
export type ApplePayContactField = 'postalAddress' | 'phone' | 'email' | 'name';
export type ApplePayNetwork = 'visa' | 'masterCard' | 'amex' | 'discover';
/**
 * A selectable delivery method shown inside the Apple Pay sheet during express
 * checkout. The sheet total recalculates live as the user picks a method.
 */
export type ApplePayShippingMethod = {
    id: string;
    label: string;
    description?: string;
    price: string;
};
/** How the Apple Pay sheet labels the shipping section. */
export type ApplePayShippingType = 'shipping' | 'delivery' | 'storePickup' | 'servicePickup';
export type ApplePayOptions = {
    clientToken: string;
    merchantId: string;
    countryCode?: string;
    currencyCode?: string;
    companyName: string;
    totalAmount: string;
    items?: ApplePaySummaryItem[];
    requiredBillingContactFields?: ApplePayContactField[];
    requiredShippingContactFields?: ApplePayContactField[];
    shippingMethods?: ApplePayShippingMethod[];
    defaultShippingMethodId?: string;
    shippingType?: ApplePayShippingType;
};
export type ApplePayCanMakePaymentsOptions = {
    networks?: ApplePayNetwork[];
};
export type ApplePayContactInfo = {
    givenName?: string;
    familyName?: string;
    emailAddress?: string;
    phoneNumber?: string;
    street?: string;
    city?: string;
    state?: string;
    postalCode?: string;
    country?: string;
    isoCountryCode?: string;
};
export type ApplePayBinData = {
    prepaid?: string;
    healthcare?: string;
    debit?: string;
    durbinRegulated?: string;
    commercial?: string;
    payroll?: string;
    countryOfIssuance?: string;
    issuingBank?: string;
    productId?: string;
};
export type ApplePayNonceResult = {
    nonce: string;
    type: string;
    isDefault: boolean;
    binData?: ApplePayBinData;
    paymentMethodDisplayName?: string;
    paymentMethodNetwork?: string;
    transactionIdentifier?: string;
    billingContact?: ApplePayContactInfo;
    shippingContact?: ApplePayContactInfo;
    shippingMethodId?: string;
};
/**
 * A selectable delivery method shown inside the Google Pay sheet during express
 * checkout. The sheet total recalculates live as the user picks an option.
 */
export type GooglePayShippingOption = {
    /** Stable identifier returned as `shippingOptionId` in the result. */
    id: string;
    /** Text shown to the user, e.g. "Standard — 3-5 days". */
    label: string;
    /** Optional secondary line shown under the label. */
    description?: string;
    /** Delivery price added to the base amount, e.g. "5.00". Use "0" for free. */
    price: string;
};
export type GooglePayOptions = {
    clientToken: string;
    amount: string;
    currencyCode?: string;
    merchantName?: string;
    isShippingAddressRequired?: boolean;
    isPhoneNumberRequired?: boolean;
    isBillingAddressRequired?: boolean;
    isEmailRequired?: boolean;
    /**
     * Delivery methods to present inside the Google Pay sheet (express checkout).
     * When provided, the sheet shows a selectable list and the total updates live
     * as `amount + selected option price`.
     */
    shippingOptions?: GooglePayShippingOption[];
    /** Id of the shipping option selected by default. */
    defaultShippingOptionId?: string;
    /**
     * Label shown next to the total in the itemized sheet (express checkout).
     * Without it Google Pay renders a non-localized default ("Final"). Only
     * applies together with `shippingOptions` — the compact sheet has no total row.
     */
    totalPriceLabel?: string;
};
export type GooglePayAddressResult = BTPayPalAccountNonceAddressResult & {
    /** Present when isPhoneNumberRequired is true. */
    phoneNumber?: string;
};
export type GooglePayNonceResult = {
    nonce: string;
    type?: string;
    description?: string;
    isDefault?: boolean;
    cardNetwork?: string;
    email?: string;
    shippingAddress?: GooglePayAddressResult;
    billingAddress?: GooglePayAddressResult;
    /** Id of the delivery method the user selected (express checkout only). */
    shippingOptionId?: string;
};
export type ThreeDSecurePostalAddress = {
    givenName?: string;
    surname?: string;
    phoneNumber?: string;
    streetAddress?: string;
    extendedAddress?: string;
    locality?: string;
    region?: string;
    postalCode?: string;
    countryCodeAlpha2?: string;
};
export type ThreeDSecureAdditionalInformation = {
    shippingGivenName?: string;
    shippingSurname?: string;
    shippingPhone?: string;
    shippingAddress?: ThreeDSecurePostalAddress;
};
export type ThreeDSecureRequestOptions = {
    clientToken: string;
    amount: string;
    nonce: string;
    email?: string;
    currencyCode?: string;
    billingAddress?: ThreeDSecurePostalAddress;
    additionalInformation?: ThreeDSecureAdditionalInformation;
    versionRequested?: '1' | '2';
    accountType?: 'credit' | 'debit';
    challengeRequested?: boolean;
    exemptionRequested?: boolean;
    mobilePhoneNumber?: string;
    cardAddChallenge?: 'requested' | 'not_requested';
    /**
     * The URL scheme for OOB (Out-of-Band) authentication to return to this app.
     * Required for EMV 3DS 2.2+ when bank app redirects are used.
     * Example: "photoaid://3ds-return"
     */
    requestorAppURL?: string;
};
export type ThreeDSecureInfo = {
    liabilityShifted: boolean;
    liabilityShiftPossible: boolean;
    wasVerified: boolean;
    /** True if user had to complete a 3DS challenge, false if authentication was frictionless */
    challengeRequired: boolean;
};
export type ThreeDSecureNonceResult = {
    nonce: string;
    threeDSecureInfo: ThreeDSecureInfo;
    cardNetwork?: string;
    lastTwo?: string;
    lastFour?: string;
    expirationMonth?: string;
    expirationYear?: string;
    bin?: string;
    cardType?: string;
};
//# sourceMappingURL=types.d.ts.map