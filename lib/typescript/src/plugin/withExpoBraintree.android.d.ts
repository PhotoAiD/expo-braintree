import { type ConfigPlugin } from '@expo/config-plugins';
export type ExpoBraintreePluginProps = {
    /**
     * Custom domain for deep linking (default: 'photoaid.com')
     * This is used for PayPal return URL deep linking
     */
    androidDeepLinkDomain?: string;
};
export declare const withExpoBraintreeAndroid: ConfigPlugin<ExpoBraintreePluginProps | void>;
//# sourceMappingURL=withExpoBraintree.android.d.ts.map