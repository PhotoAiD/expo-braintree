"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.BoolValue = exports.BTPayPalRequestUserAction = exports.BTPayPalCheckoutIntent = void 0;
let BTPayPalCheckoutIntent = exports.BTPayPalCheckoutIntent = /*#__PURE__*/function (BTPayPalCheckoutIntent) {
  BTPayPalCheckoutIntent["authorize"] = "authorize";
  BTPayPalCheckoutIntent["order"] = "order";
  BTPayPalCheckoutIntent["sale"] = "sale";
  return BTPayPalCheckoutIntent;
}({});
let BTPayPalRequestUserAction = exports.BTPayPalRequestUserAction = /*#__PURE__*/function (BTPayPalRequestUserAction) {
  BTPayPalRequestUserAction["none"] = "none";
  BTPayPalRequestUserAction["payNow"] = "payNow";
  return BTPayPalRequestUserAction;
}({});
let BoolValue = exports.BoolValue = /*#__PURE__*/function (BoolValue) {
  BoolValue["true"] = "true";
  BoolValue["false"] = "false";
  return BoolValue;
}({}); // Apple Pay Types
// Google Pay Types
/**
 * A selectable delivery method shown inside the Google Pay sheet during express
 * checkout. The sheet total recalculates live as the user picks an option.
 */
// 3D Secure Types
//# sourceMappingURL=types.js.map