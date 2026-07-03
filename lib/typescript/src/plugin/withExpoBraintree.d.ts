import { type ConfigPlugin } from '@expo/config-plugins';
import { type ExpoBraintreePluginProps as AndroidPluginProps } from './withExpoBraintree.android';
export type ExpoBraintreePluginProps = {
    /**
     * xCode project name, used for importing the swift expo braintree config header
     */
    xCodeProjectAppName: string;
} & AndroidPluginProps;
export declare const withExpoBraintreePlugin: ConfigPlugin<ExpoBraintreePluginProps>;
declare const _default: ConfigPlugin<ExpoBraintreePluginProps>;
export default _default;
//# sourceMappingURL=withExpoBraintree.d.ts.map