import { createRunOncePlugin, type ConfigPlugin } from '@expo/config-plugins';
import { withExpoBraintreeAndroid } from './withExpoBraintree.android';
import {
  withExpoBraintreeAppDelegate,
  withExpoBraintreePlist,
  withSwiftBraintreeWrapperFile,
} from './withExpoBraintree.ios';
import * as path from 'path';
import * as fs from 'fs';

// Find package.json by traversing up from compiled location
const findPackageJson = () => {
  let currentDir = __dirname;
  for (let i = 0; i < 5; i++) {
    const pkgPath = path.join(currentDir, 'package.json');
    if (fs.existsSync(pkgPath)) {
      return require(pkgPath);
    }
    currentDir = path.dirname(currentDir);
  }
  return { name: 'react-native-expo-braintree', version: '3.0.0' };
};

const pkg = findPackageJson();

export type ExpoBraintreePluginProps = {
  /**
   * xCode project name, used for importing the swift expo braintree config header
   */
  xCodeProjectAppName: string;
};

export const withExpoBraintreePlugin: ConfigPlugin<ExpoBraintreePluginProps> = (
  config,
  props
) => {
  // Android mods
  config = withExpoBraintreeAndroid(config);
  // IOS mods
  config = withSwiftBraintreeWrapperFile(config);
  config = withExpoBraintreeAppDelegate(config, props);
  config = withExpoBraintreePlist(config);
  return config;
};

export default createRunOncePlugin(
  withExpoBraintreePlugin,
  pkg.name,
  pkg.version
);
