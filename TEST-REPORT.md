# LongPaste Keyboard v1.0.1 Test Report

## Source-level validation

- 1 MiB ASCII reconstruction: PASS
- Unicode reconstruction including Bangla, emoji, accented Latin, and CJK: PASS
- Chunk-count boundary tests: PASS
- XML/resource structure: PASS
- Workflow YAML structure: PASS
- Manual-only `workflow_dispatch` trigger: PASS
- No Internet permission in AndroidManifest: PASS

## GitHub Actions fix

The previous GitHub run stopped during Android SDK setup before Gradle started. The cause was the old `android-actions/setup-android@v3` flow requesting the deprecated `tools` SDK package. The workflow now uses `android-actions/setup-android@v4`, requests only `platform-tools` from that action, and installs the required Android platform/build-tools explicitly with `sdkmanager`.

## Build status

The actual Android APK build must be executed on GitHub Actions because the local validation environment does not contain an Android SDK. This ZIP is therefore not labeled as locally APK-build-verified.
