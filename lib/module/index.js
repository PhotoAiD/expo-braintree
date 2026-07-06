import { NativeModules, Platform } from 'react-native';
const LINKING_ERROR = `The package 'expo-braintree' doesn't seem to be linked. Make sure: \n\n` + Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo Go\n';
const ExpoBraintree = NativeModules.ExpoBraintree ? NativeModules.ExpoBraintree : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});
const PRICE_PATTERN = /^\d+(\.\d+)?$/;

/** Shared shape of ApplePayShippingMethod and GooglePayShippingOption. */

function createShippingValidationError(fieldName, message) {
  return {
    code: fieldName === 'shippingMethods' ? 'INVALID_SHIPPING_METHODS' : 'INVALID_SHIPPING_OPTIONS',
    message,
    domain: 'expo-braintree'
  };
}

/**
 * Validates express-checkout delivery entries before they cross the bridge.
 * Native code on both platforms would otherwise skip malformed entries or
 * treat an unparseable price as zero, silently dropping the delivery fee
 * from the total.
 */
function validateShippingEntries(fieldName, entries, defaultFieldName, defaultId) {
  if (entries === undefined) {
    return undefined;
  }
  if (!Array.isArray(entries)) {
    return createShippingValidationError(fieldName, `${fieldName} must be an array`);
  }
  if (entries.length === 0) {
    return undefined;
  }
  const seenIds = new Set();
  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i];
    if (typeof entry !== 'object' || entry === null) {
      return createShippingValidationError(fieldName, `${fieldName}[${i}] must be an object`);
    }
    const {
      id,
      label,
      description,
      price
    } = entry;
    if (typeof id !== 'string' || id.length === 0) {
      return createShippingValidationError(fieldName, `${fieldName}[${i}].id must be a non-empty string`);
    }
    if (seenIds.has(id)) {
      return createShippingValidationError(fieldName, `${fieldName}[${i}].id "${id}" is already used by another entry`);
    }
    seenIds.add(id);
    if (typeof label !== 'string' || label.length === 0) {
      return createShippingValidationError(fieldName, `${fieldName}[${i}].label must be a non-empty string`);
    }
    if (description !== undefined && typeof description !== 'string') {
      return createShippingValidationError(fieldName, `${fieldName}[${i}].description must be a string`);
    }
    if (typeof price !== 'string' || !PRICE_PATTERN.test(price)) {
      return createShippingValidationError(fieldName, `${fieldName}[${i}].price must be a decimal string like "5.00" (got ${JSON.stringify(price)})`);
    }
  }
  if (defaultId !== undefined && !seenIds.has(defaultId)) {
    return createShippingValidationError(fieldName, `${defaultFieldName} "${defaultId}" does not match any ${fieldName} entry id`);
  }
  return undefined;
}
export async function requestBillingAgreement(options) {
  try {
    const result = ExpoBraintree.requestBillingAgreement(options);
    return result;
  } catch (ex) {
    return ex;
  }
}
export async function requestOneTimePayment(options) {
  try {
    const result = await ExpoBraintree.requestOneTimePayment(options);
    return result;
  } catch (ex) {
    return ex;
  }
}
export async function handlePayPalReturnToApp() {
  try {
    const result = await ExpoBraintree.handlePayPalReturnToApp();
    return result;
  } catch (ex) {
    return ex;
  }
}
export async function getDeviceDataFromDataCollector(clientToken) {
  try {
    const result = await ExpoBraintree.getDeviceDataFromDataCollector(clientToken);
    return result;
  } catch (ex) {
    return ex;
  }
}
export async function tokenizeCardData(options) {
  try {
    const result = await ExpoBraintree.tokenizeCardData(options);
    return result;
  } catch (ex) {
    return ex;
  }
}

// Apple Pay Functions
export async function isApplePayAvailable() {
  if (Platform.OS !== 'ios') {
    return false;
  }
  try {
    const result = await ExpoBraintree.isApplePayAvailable();
    return result;
  } catch (ex) {
    return false;
  }
}
export async function canMakeApplePayPayments(options) {
  if (Platform.OS !== 'ios') {
    return false;
  }
  try {
    const result = await ExpoBraintree.canMakeApplePayPayments(options || {});
    return result;
  } catch (ex) {
    return false;
  }
}
export async function presentApplePaymentSheet(options) {
  if (Platform.OS !== 'ios') {
    return {
      code: undefined,
      message: 'Apple Pay is only available on iOS',
      domain: undefined
    };
  }
  const validationError = validateShippingEntries('shippingMethods', options.shippingMethods, 'defaultShippingMethodId', options.defaultShippingMethodId);
  if (validationError) {
    return validationError;
  }
  try {
    const result = await ExpoBraintree.presentApplePaymentSheet(options);
    return result;
  } catch (ex) {
    return ex;
  }
}
export async function tokenizeApplePayPayment(options) {
  if (Platform.OS !== 'ios') {
    return {
      code: undefined,
      message: 'Apple Pay is only available on iOS',
      domain: undefined
    };
  }
  try {
    const result = await ExpoBraintree.tokenizeApplePayPayment(options);
    return result;
  } catch (ex) {
    return ex;
  }
}

// Google Pay Functions
export async function isGooglePayAvailable(clientToken) {
  if (Platform.OS !== 'android') {
    return false;
  }
  try {
    const result = await ExpoBraintree.isGooglePayAvailable(clientToken);
    return result;
  } catch (ex) {
    return false;
  }
}
export async function requestGooglePayPayment(options) {
  if (Platform.OS !== 'android') {
    return {
      code: undefined,
      message: 'Google Pay is only available on Android',
      domain: undefined
    };
  }
  const validationError = validateShippingEntries('shippingOptions', options.shippingOptions, 'defaultShippingOptionId', options.defaultShippingOptionId);
  if (validationError) {
    return validationError;
  }
  try {
    const result = await ExpoBraintree.requestGooglePayPayment(options);
    return result;
  } catch (ex) {
    return ex;
  }
}
export async function verifyThreeDSecure(options) {
  try {
    const result = await ExpoBraintree.verifyThreeDSecure(options);
    return result;
  } catch (ex) {
    return ex;
  }
}
export * from './types';
//# sourceMappingURL=index.js.map