# LongPaste Keyboard PRD v1.1

## Product
LongPaste Keyboard is a privacy-first Android IME with persistent local clipboard history and large-text insertion.

## Core problem
The product avoids dependence on Gboard's clipboard-history behavior by storing clipboard entries in its own local database and inserting them through the Android IME `InputConnection`.

## MVP requirements
- Third-party Android IME
- Local clipboard history
- Large text support with a 1 MiB target
- Chunked `commitText()` insertion using 16 KiB chunks
- Clipboard search
- Pin/unpin
- Copy back to system clipboard
- Delete/clear
- Sensitive editor capture suppression
- No Internet permission
- No cloud/analytics/ads

## Non-goals
- Modifying Gboard
- Promising unlimited input to every destination app
- Cloud clipboard sync
- Full Gboard feature parity

## Architecture
```text
Android Clipboard → ClipboardManager → SQLite local history
                                      ↓
                           Keyboard Clipboard UI
                                      ↓
                            LargeTextChunker
                                      ↓
                        InputConnection.commitText()
```

## Build contract
- JDK 17
- Gradle 9.6.0
- Android Gradle Plugin 9.4.0
- Kotlin Gradle Plugin 2.2.10
- compileSdk 36
- targetSdk 36
- minSdk 26

## Acceptance target
The unit-level chunking test must reconstruct a 1 MiB payload byte-for-byte/character-for-character with no loss. Full Android build/device validation must be performed by GitHub Actions and on the target POCO F3 because this environment does not contain an Android SDK.
