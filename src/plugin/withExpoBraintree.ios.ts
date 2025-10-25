/* eslint-disable no-bitwise */
import {
  withAppDelegate,
  withInfoPlist,
  type ConfigPlugin,
  IOSConfig,
} from '@expo/config-plugins';
import eol from 'eol';
import type { ExpoBraintreePluginProps } from './withExpoBraintree';

export const withExpoBraintreeAppDelegate: ConfigPlugin<
  ExpoBraintreePluginProps
> = (expoConfig, { xCodeProjectAppName }) => {
  return withAppDelegate(expoConfig, (config) => {
    const appDelegate = config.modResults;
    const isSwift = appDelegate.language === 'swift';
    let contents = eol.split(appDelegate.contents);

    if (isSwift) {
      // Handle Swift AppDelegate
      // Step 1: Remove any Objective-C imports that may have been added
      contents = contents.filter(
        (line) =>
          !line.includes('#import "ExpoModulesCore-Swift.h"') &&
          !line.includes('#import "undefined-Swift.h"') &&
          !line.includes(`#import "${xCodeProjectAppName}-Swift.h"`)
      );

      // Step 2: Add configure method in didFinishLaunchingWithOptions
      const didFinishLaunchingWithOptions = 'didFinishLaunchingWithOptions';
      const expoBraintreeConfigureLineSwift =
        '    BraintreeExpoConfig.configure()';
      const expoBraintreeConfigureLineObjC =
        '  [BraintreeExpoConfig configure];';

      let didFinishLaunchingWithOptionsElementIndex = contents.findIndex(
        (content) => content.includes(didFinishLaunchingWithOptions)
      );

      // Remove any existing Objective-C style configuration
      const objCConfigIndex = contents.findIndex(
        (content) =>
          content.includes(expoBraintreeConfigureLineObjC) ||
          content.includes('[BraintreeExpoConfig configure]')
      );
      if (~objCConfigIndex) {
        contents.splice(objCConfigIndex, 1);
      }

      const swiftConfigIndex = contents.findIndex((content) =>
        content.includes(expoBraintreeConfigureLineSwift)
      );

      // If didFinishLaunchingWithOptions exist and Swift configure line doesn't exist
      if (!~swiftConfigIndex && !!~didFinishLaunchingWithOptionsElementIndex) {
        contents.splice(
          // We are adding +2 to the index to insert content after '{' block
          didFinishLaunchingWithOptionsElementIndex + 2,
          0,
          expoBraintreeConfigureLineSwift
        );
      }

      // Step 3: Add method to properly handle openUrl method in Swift
      const openUrlMethod = 'open url: URL';
      const expoBraintreeOpenUrlLinesSwift = [
        '    if url.scheme?.localizedCaseInsensitiveCompare(BraintreeExpoConfig.getPaymentUrlScheme()) == .orderedSame {',
        '      return BraintreeExpoConfig.handleUrl(url: url)',
        '    }',
      ];

      // Find the openURL method
      let openUrlMethodElementIndex = contents.findIndex((content) =>
        content.includes(openUrlMethod)
      );

      // If we found the method signature, look for the opening brace
      if (openUrlMethodElementIndex !== -1) {
        // Find the opening brace of the function body after the method signature
        let braceIndex = -1;
        for (let i = openUrlMethodElementIndex; i < contents.length; i++) {
          const line = contents[i];
          if (line && line.includes('{')) {
            braceIndex = i;
            break;
          }
        }

        const expoBraintreeOpenUrlLineIndex = contents.findIndex((content) =>
          content.includes('BraintreeExpoConfig.getPaymentUrlScheme()')
        );

        // If openUrlMethod exists and Braintree handling doesn't exist
        if (!~expoBraintreeOpenUrlLineIndex && braceIndex !== -1) {
          contents.splice(braceIndex + 1, 0, ...expoBraintreeOpenUrlLinesSwift);
        }
      }
    } else {
      // Handle Objective-C AppDelegate (existing logic)
      // Step 1 Edit Import part
      // Editing import part for -swift.h file to be able to use Braintree
      const importSwiftHeaderFileContent = `#import "${xCodeProjectAppName}-Swift.h"`;
      const importSwiftHeaderFileIndex = contents.findIndex((content) =>
        content.includes(importSwiftHeaderFileContent)
      );
      // If importSwiftHeaderFileContent do not exist in AppDelegate.mm
      if (!~importSwiftHeaderFileIndex) {
        contents = [importSwiftHeaderFileContent, ...contents];
      }
      const importExpoModulesSwiftHeader = `#import "ExpoModulesCore-Swift.h"`;
      const importExpoModulesSwiftHeaderFileIndex = contents.findIndex(
        (content) => content.includes(importExpoModulesSwiftHeader)
      );
      // If importExpoModulesSwiftHeader do not exist in AppDelegate.mm
      if (!~importExpoModulesSwiftHeaderFileIndex) {
        contents = [importExpoModulesSwiftHeader, ...contents];
      }
      // Step 2 Add configure method in didFinishLaunchingWithOptions
      const didFinishLaunchingWithOptions = 'didFinishLaunchingWithOptions';
      const expoBraintreeConfigureLine = '  [BraintreeExpoConfig configure];';
      let didFinishLaunchingWithOptionsElementIndex = contents.findIndex(
        (content) => content.includes(didFinishLaunchingWithOptions)
      );
      const expoBraintreeConfigureLineIndex = contents.findIndex((content) =>
        content.includes(expoBraintreeConfigureLine)
      );
      // If didFinishLaunchingWithOptions exist in AppDelegate.mm and expoBraintreeConfigureLine do not exist
      if (
        !~expoBraintreeConfigureLineIndex &&
        !!~didFinishLaunchingWithOptionsElementIndex
      ) {
        contents.splice(
          // We are adding +2 to the index to insert content after '{' block
          didFinishLaunchingWithOptionsElementIndex + 2,
          0,
          expoBraintreeConfigureLine
        );
      }
      // Step 3 Add method to properly handle openUrl method in AppDelegate.m
      const openUrlMethod =
        '- (BOOL)application:(UIApplication *)application openURL';
      const expoBraintreeOpenUrlLines = [
        '  if ([url.scheme localizedCaseInsensitiveCompare:[BraintreeExpoConfig getPaymentUrlScheme]] == NSOrderedSame) {',
        '    return [BraintreeExpoConfig handleUrl:url];',
        '  }',
      ];
      const openUrlMethodElementIndex = contents.findIndex((content) =>
        content.includes(openUrlMethod)
      );
      const expoBraintreeOpenUrlLineIndex = contents.findIndex((content) =>
        content.includes(expoBraintreeOpenUrlLines?.[0] ?? '')
      );
      // If openUrlMethodElementIndex exist in AppDelegate.mm and expoBraintreeOpenUrlLineIndex do not exist
      if (!~expoBraintreeOpenUrlLineIndex && !!~openUrlMethodElementIndex) {
        contents.splice(
          // We are adding +1 to the index to insert content after '{' block
          openUrlMethodElementIndex + 1,
          0,
          ...expoBraintreeOpenUrlLines
        );
      }
    }

    config.modResults.contents = contents.join('\n');
    return config;
  });
};

/**
 * Add a new wrapper Swift file to the Xcode project for Swift compatibility.
 */
export const withSwiftBraintreeWrapperFile: ConfigPlugin = (config) => {
  return IOSConfig.XcodeProjectFile.withBuildSourceFile(config, {
    filePath: 'BraintreeExpoConfig.swift',
    contents: [
      'import Braintree',
      'import Foundation',
      '',
      '@objc public class BraintreeExpoConfig: NSObject {',
      '',
      '@objc(configure)',
      'public static func configure() {',
      '  BTAppContextSwitcher.sharedInstance.returnURLScheme = self.getPaymentUrlScheme()',
      '}',
      '',
      '@objc(getPaymentUrlScheme)',
      'public static func getPaymentUrlScheme() -> String {',
      '  let bundleIdentifier = Bundle.main.bundleIdentifier ?? ""',
      '  return bundleIdentifier + ".braintree"',
      '}',
      '',
      '@objc(handleUrl:)',
      'public static func handleUrl(url: URL) -> Bool {',
      '  return BTAppContextSwitcher.sharedInstance.handleOpen(url)',
      '}',
      '}',
    ].join('\n'),
  });
};

export const withExpoBraintreePlist: ConfigPlugin = (expoConfig) => {
  return withInfoPlist(expoConfig, (config) => {
    const bundleIdentifier = config.ios?.bundleIdentifier ?? '';
    const bundleIdentifierWithBraintreeSchema = `${bundleIdentifier}.braintree`;
    const bundleUrlTypes = config.modResults.CFBundleURLTypes;
    const isBraintreeSchemaNotExist = !bundleUrlTypes?.find((urlTypes) => {
      urlTypes.CFBundleURLSchemes.includes(bundleIdentifierWithBraintreeSchema);
    });
    // If Braintree url schema for specific bundle id not exist then add this entry
    if (isBraintreeSchemaNotExist) {
      config.modResults.CFBundleURLTypes = bundleUrlTypes?.map(
        (bundleUrlType) => {
          const isUrlSchemaContainBundleIdentifier =
            bundleUrlType.CFBundleURLSchemes.includes(bundleIdentifier);
          if (isUrlSchemaContainBundleIdentifier) {
            bundleUrlType.CFBundleURLSchemes.push(
              bundleIdentifierWithBraintreeSchema
            );
          }
          return bundleUrlType;
        }
      );
    }
    return config;
  });
};
