# LongPaste Keyboard v1.1

LongPaste Keyboard is a privacy-first Android IME built around one idea: **use a real keyboard layout, but make long-text clipboard history a first-class part of the keyboard**.

## What changed in v1.1

- Rebuilt the in-keyboard UI as a real QWERTY layout instead of an app-style screen.
- Added a clipboard shelf above the keys for one-tap long-text insertion.
- Added a dedicated clipboard mode with search, pin/unpin, copy, paste, delete, and clear.
- Replaced platform `Button` widgets with custom keyboard keys so text cannot disappear because of theme/button styling.
- Added proper shift behavior and symbol/number mode.
- Added explicit copy-current-field support.
- Added a QWERTY backspace key and editor-action Enter key.
- Added a keyboard status/onboarding screen with clear Android setup actions.
- Added an IME settings activity reference and next-IME support metadata.
- Kept local SQLite storage, 16 KiB chunked insertion, sensitive-field capture suppression, and no Internet permission.

## Core flow

```text
Android text field
        ↓
LongPaste Keyboard (IME)
        ↓
┌──────────────────────────────────┐
│ clipboard shelf / recent clips   │
├──────────────────────────────────┤
│ Q W E R T Y U I O P              │
│  A S D F G H J K L               │
│ ⇧ Z X C V B N M            ⌫     │
│ 123  ,     SPACE      .      ↵    │
└──────────────────────────────────┘
        ↓
Tap a clip → InputConnection.commitText()
```

## Main app

The launcher activity is only the setup/control surface. It does not pretend to be the keyboard. It shows:

- whether LongPaste is enabled as an Android IME
- direct links to keyboard settings and the keyboard picker
- feature and privacy information
- a simple test procedure

## Large text

Stored text remains in the local SQLite database. When a clipboard item is pasted into the current editor, LongPaste retrieves the full content and inserts it through `InputConnection.commitText()` in 16 KiB chunks.

The project targets reliable plain-text handling around 1 MiB. The destination application can still impose its own editor/input limits.

## Build on GitHub

The included workflow remains manual-only.

1. Push the repository to GitHub.
2. Open **Actions**.
3. Select **Build LongPaste Keyboard APK**.
4. Choose **Run workflow**.
5. Download the `LongPaste-debug-apk` artifact.

Build contract:

- JDK 17
- Gradle 9.6.0
- Android Gradle Plugin 9.4.0
- compileSdk 36
- targetSdk 36
- minSdk 26

## Install and test on Android

1. Install the APK.
2. Open **LongPaste Keyboard**.
3. Tap **Enable / manage keyboards**.
4. Enable **LongPaste Keyboard**.
5. Open a text field in another app.
6. Open the Android keyboard picker and select **LongPaste Keyboard**.
7. Verify the QWERTY keys are visible.
8. Copy long text, return to the keyboard, and verify the clipboard shelf shows the item.
9. Tap the clipboard item and verify the complete text is inserted.
10. Open **ALL** to test search, pin, copy, paste, delete, and clear.

## Privacy

- No Internet permission.
- No cloud synchronization.
- No analytics.
- No ads.
- Clipboard history is local to the device.
- Capture is suppressed for common password fields.

See `TEST-REPORT.md` for validation details.


## Space-bar paste

A short Space press inserts a normal space. A long press on Space pastes the latest saved clipboard item. This uses the same chunked `InputConnection` insertion path as the clipboard cards.
