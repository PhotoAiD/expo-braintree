import { type ConfigPlugin } from '@expo/config-plugins';
export type ExpoBraintreePluginProps = {
    /**
     * xCode project name, used for importing the swift expo braintree config header
     */
    xCodeProjectAppName: string;
};
export declare const withExpoBraintreePlugin: ConfigPlugin<ExpoBraintreePluginProps>;
declare const _default: ConfigPlugin<ExpoBraintreePluginProps>;
export default _default;
//# sourceMappingURL=withExpoBraintree.d.ts.map