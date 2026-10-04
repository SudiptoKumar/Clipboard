# LongPaste Keyboard

A privacy-first Android IME with persistent, large-text clipboard history.

## What it does

LongPaste Keyboard does not modify Gboard. It becomes the selected Android keyboard and maintains its own local clipboard history, search, pin/delete controls, system-clipboard copy-back, and chunked large-text insertion.

## MVP targets

- Android IME
- Local SQLite clipboard history
- Search, pin/unpin, copy, paste, delete, clear
- 1 MiB plain-text target
- 16 KiB chunked `InputConnection.commitText()` insertion
- Sensitive/password-field clipboard capture suppression
- No Internet permission
- No cloud, analytics, or ads

## Build on GitHub

The included workflow is manual-only and installs its Android SDK requirements:

1. Push the repository to GitHub.
2. Open **Actions**.
3. Select **Build Android APK**.
4. Click **Run workflow**.
5. Download **LongPaste-debug-apk**.

The workflow uses Gradle 9.6.0, JDK 17, Android Gradle Plugin 9.4.0, Kotlin 2.2.10, compileSdk 36, and targetSdk 36.

## Important limitation

The app controls its own clipboard storage and insertion strategy, but the destination application may impose its own input constraints. Therefore the product does not claim an absolute unlimited-paste guarantee.

## Validation

See `TEST-REPORT.md` for the exact checks completed in the build environment and the tests that must be completed in GitHub/device CI.
