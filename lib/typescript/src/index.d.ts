import { type RequestOneTimePaymentOptions, type RequestBillingAgreementOptions, type BTPayPalAccountNonceResult, type BTPayPalError, type BTPayPalGetDeviceDataResult, type BTCardTokenizationNonceResult, type TokenizeCardOptions, type ApplePayOptions, type ApplePayNonceResult, type ApplePayCanMakePaymentsOptions, type GooglePayOptions, type GooglePayNonceResult, type ThreeDSecureRequestOptions, type ThreeDSecureNonceResult } from './types';
export declare function requestBillingAgreement(options: RequestBillingAgreementOptions): Promise<BTPayPalAccountNonceResult | BTPayPalError>;
export declare function requestOneTimePayment(options: RequestOneTimePaymentOptions): Promise<BTPayPalAccountNonceResult | BTPayPalError>;
export declare function handlePayPalReturnToApp(): Promise<BTPayPalAccountNonceResult | BTPayPalError>;
export declare function getDeviceDataFromDataCollector(clientToken: string): Promise<BTPayPalGetDeviceDataResult | BTPayPalError>;
export declare function tokenizeCardData(options: TokenizeCardOptions): Promise<BTCardTokenizationNonceResult | BTPayPalError>;
export declare function isApplePayAvailable(): Promise<boolean>;
export declare function canMakeApplePayPayments(options?: ApplePayCanMakePaymentsOptions): Promise<boolean>;
export declare function presentApplePaymentSheet(options: ApplePayOptions): Promise<ApplePayNonceResult | BTPayPalError>;
export declare function tokenizeApplePayPayment(options: {
    clientToken: string;
}): Promise<ApplePayNonceResult | BTPayPalError>;
export declare function isGooglePayAvailable(clientToken: string): Promise<boolean>;
export declare function requestGooglePayPayment(options: GooglePayOptions): Promise<GooglePayNonceResult | BTPayPalError>;
export declare function verifyThreeDSecure(options: ThreeDSecureRequestOptions): Promise<ThreeDSecureNonceResult | BTPayPalError>;
export * from './types';
//# sourceMappingURL=index.d.ts.map