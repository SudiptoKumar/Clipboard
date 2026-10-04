# GitHub Actions SDK Setup Fix

## Previous failure

The GitHub Actions run failed before Gradle started while `android-actions/setup-android@v3` attempted to install the deprecated Android SDK `tools` package. The job then exited from `sdkmanager` with code 1.

## Current fix

The workflow now:

1. Uses `android-actions/setup-android@v4`.
2. Requests only `platform-tools` from the setup action.
3. Lets the setup action accept Android SDK licenses.
4. Installs `platforms;android-36` and `build-tools;36.0.0` explicitly with `sdkmanager`.
5. Runs Gradle 9.6.0 with JDK 17.
6. Runs unit tests before assembling the APK.
7. Verifies the APK file exists before uploading it.

## Expected order

```text
Checkout
  -> JDK 17
  -> Android SDK setup
  -> Android platform/build-tools installation
  -> Gradle configuration validation
  -> Unit tests
  -> Debug APK build
  -> APK existence check
  -> Artifact upload
```

The workflow remains `workflow_dispatch` only, so APK builds are manual.
