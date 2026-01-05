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
    userAction?: BTPayPalRequestUserAction;
    offerPayLater?: BoolValue;
    currencyCode?: string;
    requestBillingAgreement?: BoolValue;
    clientToken: string;
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
};
export type GooglePayOptions = {
    clientToken: string;
    amount: string;
    currencyCode?: string;
    merchantName?: string;
};
export type GooglePayNonceResult = {
    nonce: string;
    type?: string;
    description?: string;
    isDefault?: boolean;
    cardNetwork?: string;
    email?: string;
    shippingAddress?: BTPayPalAccountNonceAddressResult;
    billingAddress?: BTPayPalAccountNonceAddressResult;
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