# Validation Report

Date: 2026-10-05

## Passed locally
- Kotlin utility compilation with `kotlinc`
- 1 MiB chunk reconstruction test
- Unicode chunk reconstruction test
- Chunk-count boundary test
- UTF-8 byte-size helper test
- XML parsing for all Android XML resources
- GitHub Actions YAML parsing and policy checks
- Manual-only `workflow_dispatch` trigger verified
- No `push` or `pull_request` build trigger
- No `android.permission.INTERNET` permission
- IME service and `BIND_INPUT_METHOD` manifest wiring present

## Not runnable in this environment
The container does not include an Android SDK, Android platform JARs, Gradle distribution, or an emulator/device. Therefore a genuine `assembleDebug`, `testDebugUnitTest`, APK installation test, and POCO F3 paste benchmark could not be executed locally.

## GitHub validation
The included workflow installs Android API 36 and Build Tools 36.0.0, then runs the same Gradle build and unit-test commands intended for CI.

## Important
The ZIP is therefore a **source-ready CI project**, not an APK that has been falsely marked as locally build-verified.
