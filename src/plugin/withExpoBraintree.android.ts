import {
  withAndroidManifest,
  withAndroidStyles,
  AndroidConfig,
  type ConfigPlugin,
} from '@expo/config-plugins';

const { getMainActivityOrThrow, getMainApplication } = AndroidConfig.Manifest;

export const withExpoBraintreeAndroid: ConfigPlugin = (expoConfig) => {
  expoConfig = withAndroidManifest(expoConfig, (config) => {
    config.modResults = addBraintreePaymentActivity(config.modResults);
    config.modResults = setMainActivityLaunchMode(config.modResults);
    return config;
  });

  expoConfig = withAndroidStyles(expoConfig, (config) => {
    config.modResults = addBraintreeTransparentTheme(config.modResults);
    return config;
  });

  return expoConfig;
};

// Set MainActivity launch mode to singleTop
const setMainActivityLaunchMode = (
  modResults: AndroidConfig.Manifest.AndroidManifest
): AndroidConfig.Manifest.AndroidManifest => {
  const mainActivity = getMainActivityOrThrow(modResults);

  mainActivity.$['android:launchMode'] = 'singleTop';

  return modResults;
};

// Add BraintreePaymentActivity to AndroidManifest
const addBraintreePaymentActivity = (
  modResults: AndroidConfig.Manifest.AndroidManifest
): AndroidConfig.Manifest.AndroidManifest => {
  const mainApplication = getMainApplication(modResults);

  if (!mainApplication) {
    console.warn('withExpoBraintreeAndroid: No main application found');
    return modResults;
  }

  if (!mainApplication.activity) {
    mainApplication.activity = [];
  }

  // Check if BraintreePaymentActivity already exists
  const existingActivity = mainApplication.activity.find(
    (activity) =>
      activity.$?.['android:name'] ===
      'com.expobraintree.BraintreePaymentActivity'
  );

  if (existingActivity) {
    console.log(
      'withExpoBraintreeAndroid: BraintreePaymentActivity already exists'
    );
    return modResults;
  }

  // Get package name for deep link scheme
  const packageName = modResults.manifest.$.package || 'com.example.app';

  // Add BraintreePaymentActivity
  mainApplication.activity.push({
    '$': {
      'android:name': 'com.expobraintree.BraintreePaymentActivity',
      'android:configChanges':
        'keyboard|keyboardHidden|orientation|screenSize|screenLayout|uiMode',
      'android:launchMode': 'singleTask',
      'android:windowSoftInputMode': 'adjustResize',
      'android:theme': '@style/Theme.Braintree.Transparent',
      'android:exported': 'true',
    },
    'intent-filter': [
      {
        action: [{ $: { 'android:name': 'android.intent.action.VIEW' } }],
        category: [
          { $: { 'android:name': 'android.intent.category.DEFAULT' } },
          { $: { 'android:name': 'android.intent.category.BROWSABLE' } },
        ],
        data: [{ $: { 'android:scheme': `${packageName}.braintree` } }],
      },
      {
        action: [{ $: { 'android:name': 'android.intent.action.VIEW' } }],
        category: [
          { $: { 'android:name': 'android.intent.category.DEFAULT' } },
          { $: { 'android:name': 'android.intent.category.BROWSABLE' } },
        ],
        data: [
          {
            $: {
              'android:scheme': 'https',
              'android:host': 'photoaid.com',
              'android:pathPrefix': '/braintree/return',
            },
          },
        ],
      },
    ],
  });

  return modResults;
};

// Add transparent theme to styles.xml
const addBraintreeTransparentTheme = (
  styles: AndroidConfig.Resources.ResourceXML
): AndroidConfig.Resources.ResourceXML => {
  if (!styles.resources) {
    styles.resources = {};
  }

  if (!styles.resources.style) {
    styles.resources.style = [];
  }

  // Check if theme already exists
  const existingTheme = styles.resources.style.find(
    (style) => style.$ && style.$.name === 'Theme.Braintree.Transparent'
  );

  if (existingTheme) {
    console.log(
      'withExpoBraintreeAndroid: Theme.Braintree.Transparent already exists'
    );
    return styles;
  }

  // Add transparent theme
  styles.resources.style.push({
    $: {
      name: 'Theme.Braintree.Transparent',
      parent: 'Theme.AppCompat.NoActionBar',
    },
    item: [
      {
        _: 'true',
        $: { name: 'android:windowIsTranslucent' },
      },
      {
        _: '@android:color/transparent',
        $: { name: 'android:windowBackground' },
      },
      {
        _: '@null',
        $: { name: 'android:windowContentOverlay' },
      },
      {
        _: 'true',
        $: { name: 'android:windowNoTitle' },
      },
      {
        _: 'false',
        $: { name: 'android:windowIsFloating' },
      },
      {
        _: 'false',
        $: { name: 'android:backgroundDimEnabled' },
      },
    ],
  });

  return styles;
};
