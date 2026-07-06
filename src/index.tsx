import { NativeModules, Platform } from 'react-native';
import {
  type RequestOneTimePaymentOptions,
  type RequestBillingAgreementOptions,
  type BTPayPalAccountNonceResult,
  type BTPayPalError,
  type BTPayPalGetDeviceDataResult,
  type BTCardTokenizationNonceResult,
  type TokenizeCardOptions,
  type ApplePayOptions,
  type ApplePayNonceResult,
  type ApplePayCanMakePaymentsOptions,
  type GooglePayOptions,
  type GooglePayNonceResult,
  type ThreeDSecureRequestOptions,
  type ThreeDSecureNonceResult,
} from './types';

const LINKING_ERROR =
  `The package 'expo-braintree' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const ExpoBraintree = NativeModules.ExpoBraintree
  ? NativeModules.ExpoBraintree
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const PRICE_PATTERN = /^\d+(\.\d+)?$/;

type ShippingEntryFieldName = 'shippingMethods' | 'shippingOptions';

/** Shared shape of ApplePayShippingMethod and GooglePayShippingOption. */
type ShippingEntry = {
  id: string;
  label: string;
  description?: string;
  price: string;
};

function createShippingValidationError(
  fieldName: ShippingEntryFieldName,
  message: string
): BTPayPalError {
  return {
    code:
      fieldName === 'shippingMethods'
        ? 'INVALID_SHIPPING_METHODS'
        : 'INVALID_SHIPPING_OPTIONS',
    message,
    domain: 'expo-braintree',
  };
}

/**
 * Validates express-checkout delivery entries before they cross the bridge.
 * Native code on both platforms would otherwise skip malformed entries or
 * treat an unparseable price as zero, silently dropping the delivery fee
 * from the total.
 */
function validateShippingEntries(
  fieldName: ShippingEntryFieldName,
  entries: ShippingEntry[] | undefined,
  defaultFieldName: 'defaultShippingMethodId' | 'defaultShippingOptionId',
  defaultId: string | undefined
): BTPayPalError | undefined {
  if (entries === undefined) {
    return undefined;
  }
  if (!Array.isArray(entries)) {
    return createShippingValidationError(
      fieldName,
      `${fieldName} must be an array`
    );
  }
  if (entries.length === 0) {
    return undefined;
  }

  const seenIds = new Set<string>();
  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i];
    if (typeof entry !== 'object' || entry === null) {
      return createShippingValidationError(
        fieldName,
        `${fieldName}[${i}] must be an object`
      );
    }
    const { id, label, description, price } = entry;
    if (typeof id !== 'string' || id.length === 0) {
      return createShippingValidationError(
        fieldName,
        `${fieldName}[${i}].id must be a non-empty string`
      );
    }
    if (seenIds.has(id)) {
      return createShippingValidationError(
        fieldName,
        `${fieldName}[${i}].id "${id}" is already used by another entry`
      );
    }
    seenIds.add(id);
    if (typeof label !== 'string' || label.length === 0) {
      return createShippingValidationError(
        fieldName,
        `${fieldName}[${i}].label must be a non-empty string`
      );
    }
    if (description !== undefined && typeof description !== 'string') {
      return createShippingValidationError(
        fieldName,
        `${fieldName}[${i}].description must be a string`
      );
    }
    if (typeof price !== 'string' || !PRICE_PATTERN.test(price)) {
      return createShippingValidationError(
        fieldName,
        `${fieldName}[${i}].price must be a decimal string like "5.00" (got ${JSON.stringify(price)})`
      );
    }
  }

  if (defaultId !== undefined && !seenIds.has(defaultId)) {
    return createShippingValidationError(
      fieldName,
      `${defaultFieldName} "${defaultId}" does not match any ${fieldName} entry id`
    );
  }

  return undefined;
}

export async function requestBillingAgreement(
  options: RequestBillingAgreementOptions
): Promise<BTPayPalAccountNonceResult | BTPayPalError> {
  try {
    const result: BTPayPalAccountNonceResult =
      ExpoBraintree.requestBillingAgreement(options);
    return result;
  } catch (ex: unknown) {
    return ex as BTPayPalError;
  }
}

export async function requestOneTimePayment(
  options: RequestOneTimePaymentOptions
): Promise<BTPayPalAccountNonceResult | BTPayPalError> {
  try {
    const result: BTPayPalAccountNonceResult =
      await ExpoBraintree.requestOneTimePayment(options);
    return result;
  } catch (ex: unknown) {
    return ex as BTPayPalError;
  }
}

export async function handlePayPalReturnToApp(): Promise<
  BTPayPalAccountNonceResult | BTPayPalError
> {
  try {
    const result: BTPayPalAccountNonceResult =
      await ExpoBraintree.handlePayPalReturnToApp();
    return result;
  } catch (ex: unknown) {
    return ex as BTPayPalError;
  }
}

export async function getDeviceDataFromDataCollector(
  clientToken: string
): Promise<BTPayPalGetDeviceDataResult | BTPayPalError> {
  try {
    const result: BTPayPalGetDeviceDataResult =
      await ExpoBraintree.getDeviceDataFromDataCollector(clientToken);
    return result;
  } catch (ex: unknown) {
    return ex as BTPayPalError;
  }
}

export async function tokenizeCardData(
  options: TokenizeCardOptions
): Promise<BTCardTokenizationNonceResult | BTPayPalError> {
  try {
    const result: BTCardTokenizationNonceResult =
      await ExpoBraintree.tokenizeCardData(options);
    return result;
  } catch (ex: unknown) {
    return ex as BTPayPalError;
  }
}

// Apple Pay Functions
export async function isApplePayAvailable(): Promise<boolean> {
  if (Platform.OS !== 'ios') {
    return false;
  }
  try {
    const result: boolean = await ExpoBraintree.isApplePayAvailable();
    return result;
  } catch (ex: unknown) {
    return false;
  }
}

export async function canMakeApplePayPayments(
  options?: ApplePayCanMakePaymentsOptions
): Promise<boolean> {
  if (Platform.OS !== 'ios') {
    return false;
  }
  try {
    const result: boolean = await ExpoBraintree.canMakeApplePayPayments(
      options || {}
    );
    return result;
  } catch (ex: unknown) {
    return false;
  }
}

export async function presentApplePaymentSheet(
  options: ApplePayOptions
): Promise<ApplePayNonceResult | BTPayPalError> {
  if (Platform.OS !== 'ios') {
    return {
      code: undefined,
      message: 'Apple Pay is only available on iOS',
      domain: undefined,
    } as BTPayPalError;
  }
  const validationError = validateShippingEntries(
    'shippingMethods',
    options.shippingMethods,
    'defaultShippingMethodId',
    options.defaultShippingMethodId
  );
  if (validationError) {
    return validationError;
  }
  try {
    const result: ApplePayNonceResult =
      await ExpoBraintree.presentApplePaymentSheet(options);
    return result;
  } catch (ex: unknown) {
    return ex as BTPayPalError;
  }
}

export async function tokenizeApplePayPayment(options: {
  clientToken: string;
}): Promise<ApplePayNonceResult | BTPayPalError> {
  if (Platform.OS !== 'ios') {
    return {
      code: undefined,
      message: 'Apple Pay is only available on iOS',
      domain: undefined,
    } as BTPayPalError;
  }
  try {
    const result: ApplePayNonceResult =
      await ExpoBraintree.tokenizeApplePayPayment(options);
    return result;
  } catch (ex: unknown) {
    return ex as BTPayPalError;
  }
}

// Google Pay Functions
export async function isGooglePayAvailable(
  clientToken: string
): Promise<boolean> {
  if (Platform.OS !== 'android') {
    return false;
  }
  try {
    const result: boolean =
      await ExpoBraintree.isGooglePayAvailable(clientToken);
    return result;
  } catch (ex: unknown) {
    return false;
  }
}

export async function requestGooglePayPayment(
  options: GooglePayOptions
): Promise<GooglePayNonceResult | BTPayPalError> {
  if (Platform.OS !== 'android') {
    return {
      code: undefined,
      message: 'Google Pay is only available on Android',
      domain: undefined,
    } as BTPayPalError;
  }
  const validationError = validateShippingEntries(
    'shippingOptions',
    options.shippingOptions,
    'defaultShippingOptionId',
    options.defaultShippingOptionId
  );
  if (validationError) {
    return validationError;
  }
  try {
    const result: GooglePayNonceResult =
      await ExpoBraintree.requestGooglePayPayment(options);
    return result;
  } catch (ex: unknown) {
    return ex as BTPayPalError;
  }
}

export async function verifyThreeDSecure(
  options: ThreeDSecureRequestOptions
): Promise<ThreeDSecureNonceResult | BTPayPalError> {
  try {
    const result: ThreeDSecureNonceResult =
      await ExpoBraintree.verifyThreeDSecure(options);
    return result;
  } catch (ex: unknown) {
    return ex as BTPayPalError;
  }
}

export * from './types';
