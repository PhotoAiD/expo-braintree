"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.withExpoBraintreePlugin = exports.default = void 0;
var _configPlugins = require("@expo/config-plugins");
var _withExpoBraintree = require("./withExpoBraintree.android");
var _withExpoBraintree2 = require("./withExpoBraintree.ios");
var path = _interopRequireWildcard(require("path"));
var fs = _interopRequireWildcard(require("fs"));
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
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
  return {
    name: 'react-native-expo-braintree',
    version: '3.0.0'
  };
};
const pkg = findPackageJson();
const withExpoBraintreePlugin = (config, props) => {
  // Android mods
  config = (0, _withExpoBraintree.withExpoBraintreeAndroid)(config, props);
  // IOS mods
  config = (0, _withExpoBraintree2.withSwiftBraintreeWrapperFile)(config);
  config = (0, _withExpoBraintree2.withExpoBraintreeAppDelegate)(config, props);
  config = (0, _withExpoBraintree2.withExpoBraintreePlist)(config);
  return config;
};
exports.withExpoBraintreePlugin = withExpoBraintreePlugin;
var _default = exports.default = (0, _configPlugins.createRunOncePlugin)(withExpoBraintreePlugin, pkg.name, pkg.version);
//# sourceMappingURL=withExpoBraintree.js.map