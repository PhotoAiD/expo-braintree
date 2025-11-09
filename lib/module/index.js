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