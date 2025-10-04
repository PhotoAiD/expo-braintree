import {
  withAndroidManifest,
  AndroidConfig,
  type ConfigPlugin,
} from '@expo/config-plugins';

const { getMainActivityOrThrow } = AndroidConfig.Manifest;

export const withExpoBraintreeAndroid: ConfigPlugin = (expoConfig) => {
  return withAndroidManifest(expoConfig, (config) => {
    config.modResults = addPaypalIntentFilter(config.modResults);
    return config;
  });
};

type ManifestData = {
  $: {
    [key: string]: string | undefined;
    'android:host'?: string;
    'android:pathPrefix'?: string;
    'android:scheme'?: string;
  };
};

// Add new intent filter
// <activity>
//   ...
//   <intent-filter>
//     <action android:name="android.intent.action.VIEW" />
//     <category android:name="android.intent.category.DEFAULT" />
//     <category android:name="android.intent.category.BROWSABLE" />
//     <data android:scheme="${applicationId}.braintree" />
//   </intent-filter>
// </activity>;
const intentActionView = 'android.intent.action.VIEW';
const intentCategoryDefault = 'android.intent.category.DEFAULT';
const intentCategoryBrowsable = 'android.intent.category.BROWSABLE';
const intentDataBraintree = '${applicationId}.braintree';

export const addPaypalIntentFilter = (
  modResults: AndroidConfig.Manifest.AndroidManifest
): AndroidConfig.Manifest.AndroidManifest => {
  const mainActivity = getMainActivityOrThrow(modResults);
  // We want always to add the data to the first intent filter
  const intentFilters = mainActivity['intent-filter'];
  if (!intentFilters?.length) {
    console.warn(
      'withExpoBraintreeAndroid.addPaypalIntentFilter: No .Intent Filters'
    );
    return modResults;
  }
  const {
    isIntentActionExist,
    isIntentCategoryBrowsableExist,
    isIntentCategoryDefaultExist,
    isIntentDataBraintreeExist,
    isIntentDataAppLinkExist,
  } = checkAndroidManifestData(intentFilters);

  // Add deep link intent filter (fallback for v5)
  if (
    !isIntentActionExist ||
    !isIntentCategoryBrowsableExist ||
    !isIntentCategoryDefaultExist ||
    !isIntentDataBraintreeExist
  ) {
    intentFilters.push({
      action: [
        {
          $: { 'android:name': intentActionView },
        },
      ],
      category: [
        { $: { 'android:name': intentCategoryDefault } },
        { $: { 'android:name': intentCategoryBrowsable } },
      ],
      data: [{ $: { 'android:scheme': '${applicationId}.braintree' } }],
    });
  }

  // Add App Link intent filter for v5 (HTTPS scheme)
  if (!isIntentDataAppLinkExist) {
    intentFilters.push({
      action: [
        {
          $: { 'android:name': intentActionView },
        },
      ],
      category: [
        { $: { 'android:name': intentCategoryDefault } },
        { $: { 'android:name': intentCategoryBrowsable } },
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
    });
  }

  return modResults;
};

const checkAndroidManifestData = (
  intentFilters: AndroidConfig.Manifest.ManifestIntentFilter[]
) => ({
  isIntentActionExist: isElementInAndroidManifestExist(
    intentFilters,
    intentActionView,
    'action'
  ),
  isIntentCategoryDefaultExist: isElementInAndroidManifestExist(
    intentFilters,
    intentCategoryDefault,
    'category'
  ),
  isIntentCategoryBrowsableExist: isElementInAndroidManifestExist(
    intentFilters,
    intentCategoryBrowsable,
    'category'
  ),
  isIntentDataBraintreeExist: isElementInAndroidManifestExist(
    intentFilters,
    intentDataBraintree,
    'data'
  ),
  isIntentDataAppLinkExist: isAppLinkInAndroidManifestExist(
    intentFilters,
    'https',
    'photoaid.com',
    '/braintree/return'
  ),
});

const isElementInAndroidManifestExist = (
  intentFilters: AndroidConfig.Manifest.ManifestIntentFilter[] | undefined,
  value: string,
  type: 'action' | 'data' | 'category'
) =>
  !!intentFilters?.some((intentFilter) =>
    intentFilter[type]?.find((item) => {
      switch (type) {
        case 'action':
        case 'category':
          return item.$['android:name'] === value;
        case 'data':
          const typedItem = item as ManifestData;
          return typedItem.$['android:scheme'] === value;
      }
    })
  );

const isAppLinkInAndroidManifestExist = (
  intentFilters: AndroidConfig.Manifest.ManifestIntentFilter[] | undefined,
  scheme: string,
  host: string,
  pathPrefix: string
) =>
  !!intentFilters?.some((intentFilter) =>
    intentFilter.data?.find((item) => {
      const typedItem = item as ManifestData;
      return (
        typedItem.$['android:scheme'] === scheme &&
        typedItem.$['android:host'] === host &&
        typedItem.$['android:pathPrefix'] === pathPrefix
      );
    })
  );
