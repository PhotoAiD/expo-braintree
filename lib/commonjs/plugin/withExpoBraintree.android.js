"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.withExpoBraintreeAndroid = void 0;
var _configPlugins = require("@expo/config-plugins");
const {
  getMainActivityOrThrow,
  getMainApplication
} = _configPlugins.AndroidConfig.Manifest;
const withExpoBraintreeAndroid = (expoConfig, props) => {
  const domain = props?.androidDeepLinkDomain || 'photoaid.com';
  expoConfig = (0, _configPlugins.withAndroidManifest)(expoConfig, config => {
    const packageName = config.android?.package || 'com.example.app';
    config.modResults = addBraintreePaymentActivity(config.modResults, packageName, domain);
    config.modResults = setMainActivityLaunchMode(config.modResults);
    return config;
  });
  expoConfig = (0, _configPlugins.withAndroidStyles)(expoConfig, config => {
    config.modResults = addBraintreeTransparentTheme(config.modResults);
    return config;
  });
  return expoConfig;
};

// Set MainActivity launch mode to singleTop
exports.withExpoBraintreeAndroid = withExpoBraintreeAndroid;
const setMainActivityLaunchMode = modResults => {
  const mainActivity = getMainActivityOrThrow(modResults);
  mainActivity.$['android:launchMode'] = 'singleTop';
  return modResults;
};

// Add BraintreePaymentActivity to AndroidManifest
const addBraintreePaymentActivity = (modResults, packageName, domain) => {
  const mainApplication = getMainApplication(modResults);
  if (!mainApplication) {
    console.warn('withExpoBraintreeAndroid: No main application found');
    return modResults;
  }
  if (!mainApplication.activity) {
    mainApplication.activity = [];
  }

  // Check if BraintreePaymentActivity already exists
  const existingActivity = mainApplication.activity.find(activity => activity.$?.['android:name'] === 'com.expobraintree.BraintreePaymentActivity');
  if (existingActivity) {
    console.log('withExpoBraintreeAndroid: BraintreePaymentActivity already exists');
    return modResults;
  }

  // Add BraintreePaymentActivity
  mainApplication.activity.push({
    '$': {
      'android:name': 'com.expobraintree.BraintreePaymentActivity',
      'android:configChanges': 'keyboard|keyboardHidden|orientation|screenSize|screenLayout|uiMode',
      'android:launchMode': 'singleTask',
      'android:windowSoftInputMode': 'adjustResize',
      'android:theme': '@style/Theme.Braintree.Transparent',
      'android:exported': 'true'
    },
    'intent-filter': [{
      action: [{
        $: {
          'android:name': 'android.intent.action.VIEW'
        }
      }],
      category: [{
        $: {
          'android:name': 'android.intent.category.DEFAULT'
        }
      }, {
        $: {
          'android:name': 'android.intent.category.BROWSABLE'
        }
      }],
      data: [{
        $: {
          'android:scheme': `${packageName}.braintree`
        }
      }]
    }, {
      action: [{
        $: {
          'android:name': 'android.intent.action.VIEW'
        }
      }],
      category: [{
        $: {
          'android:name': 'android.intent.category.DEFAULT'
        }
      }, {
        $: {
          'android:name': 'android.intent.category.BROWSABLE'
        }
      }],
      data: [{
        $: {
          'android:scheme': 'https',
          'android:host': domain,
          'android:pathPrefix': '/braintree/return'
        }
      }]
    }]
  });
  return modResults;
};

// Add transparent theme to styles.xml
const addBraintreeTransparentTheme = styles => {
  if (!styles.resources) {
    styles.resources = {};
  }
  if (!styles.resources.style) {
    styles.resources.style = [];
  }

  // Check if theme already exists
  const existingTheme = styles.resources.style.find(style => style.$ && style.$.name === 'Theme.Braintree.Transparent');
  if (existingTheme) {
    console.log('withExpoBraintreeAndroid: Theme.Braintree.Transparent already exists');
    return styles;
  }

  // Add transparent theme
  styles.resources.style.push({
    $: {
      name: 'Theme.Braintree.Transparent',
      parent: 'Theme.AppCompat.NoActionBar'
    },
    item: [{
      _: 'true',
      $: {
        name: 'android:windowIsTranslucent'
      }
    }, {
      _: '@android:color/transparent',
      $: {
        name: 'android:windowBackground'
      }
    }, {
      _: '@null',
      $: {
        name: 'android:windowContentOverlay'
      }
    }, {
      _: 'true',
      $: {
        name: 'android:windowNoTitle'
      }
    }, {
      _: 'false',
      $: {
        name: 'android:windowIsFloating'
      }
    }, {
      _: 'false',
      $: {
        name: 'android:backgroundDimEnabled'
      }
    }]
  });
  return styles;
};
//# sourceMappingURL=withExpoBraintree.android.js.map