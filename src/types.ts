export enum EXCEPTION_TYPES {
  SWIFT_EXCEPTION = 'ExpoBraintree:`SwiftException',
  USER_CANCEL_EXCEPTION = 'ExpoBraintree:`UserCancelException',
  PAYPAL_DISABLED_IN_CONFIGURATION = 'ExpoBraintree:`Paypal disabled in configuration',
  TOKENIZE_EXCEPTION = 'ExpoBraintree:`TokenizeException',
}

export enum ERROR_TYPES {
  API_CLIENT_INITIALIZATION_ERROR = 'API_CLIENT_INITIALIZATION_ERROR',
  TOKENIZE_VAULT_PAYMENT_ERROR = 'TOKENIZE_VAULT_PAYMENT_ERROR',
  USER_CANCEL_TRANSACTION_ERROR = 'USER_CANCEL_TRANSACTION_ERROR',
  PAYPAL_DISABLED_IN_CONFIGURATION_ERROR = 'PAYPAL_DISABLED_IN_CONFIGURATION_ERROR',
  DATA_COLLECTOR_ERROR = 'DATA_COLLECTOR_ERROR',
  CARD_TOKENIZATION_ERROR = 'CARD_TOKENIZATION_ERROR',
  APPLE_PAY_NOT_AVAILABLE = 'APPLE_PAY_NOT_AVAILABLE',
  APPLE_PAY_TOKENIZATION_ERROR = 'APPLE_PAY_TOKENIZATION_ERROR',
}

export enum BTPayPalCheckoutIntent {
  authorize = 'authorize',
  order = 'order',
  sale = 'sale',
}
export enum BTPayPalRequestUserAction {
  none = 'none',
  payNow = 'payNow',
}

export enum BoolValue {
  true = 'true',
  false = 'false',
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
};

export type BTPayPalGetDeviceDataResult = string;

export type BTPayPalError = {
  code?: EXCEPTION_TYPES;
  message?: ERROR_TYPES | string;
  domain?: ERROR_TYPES;
};

// Apple Pay Types
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
