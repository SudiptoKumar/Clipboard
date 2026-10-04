# LongPaste Keyboard v1.2 Test Report

## Changes verified by source inspection

- Launcher UI rebuilt as a real setup/control screen.
- IME UI uses a compact Gboard-like QWERTY layout with toolbar and recent clipboard strip.
- Platform `Button` widgets removed from the keyboard and launcher action surface.
- Clipboard mode includes search, pin/unpin, copy, paste, delete, and clear.
- Shift, number/symbol mode, backspace, space, comma, period, editor-action Enter, and long-press Space paste are implemented.
- IME forces non-fullscreen input mode.
- IME metadata declares next-IME switching support and points settings to `MainActivity`.
- No Internet permission is present.
- No cloud/analytics/ads dependencies are present.
- GitHub workflow remains manual-only with `workflow_dispatch`.

## Local validation

### PASS

- XML/resource parsing.
- Workflow YAML parsing.
- Workflow regression checks.
- Kotlin utility compilation.
- 1 MiB-class chunk reconstruction.
- Unicode chunk reconstruction with Bangla, emoji, accented Latin, and CJK.
- UTF-8 byte-count helper.
- No deprecated Kotlin Android plugin configuration in live Gradle files.

## Build fix in v1.2

- Fixed the `HorizontalScrollView.LayoutParams` compilation failure reported by GitHub Actions. The child now uses compatible generic view layout parameters.
- Large-text chunking now protects UTF-16 surrogate pairs at chunk boundaries.

## Android build limitation

A full Android APK build was not executed in this preparation environment because Android SDK packages and a Gradle installation are not available locally and outbound network resolution is unavailable. The repository is therefore not labeled as device/CI-build verified here.

## Required device verification

Use GitHub Actions for the APK build, then test on the target Android device:

1. LongPaste appears in Android's enabled keyboard list.
2. Selecting LongPaste shows the complete QWERTY layout.
3. Clipboard shelf entries are visible and tappable.
4. Tapping a saved clip inserts the full content into a text field.
5. 1 MiB plain-text insertion completes without truncation in a destination app that accepts the payload.
6. Search, pin, copy, delete, and clear operate correctly.
7. Password fields do not capture clipboard content into LongPaste history.
8. Switching back to the previous keyboard works through Android's input-method picker.
