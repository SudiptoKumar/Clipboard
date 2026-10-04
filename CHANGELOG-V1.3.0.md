# LongPaste Keyboard v1.3.0

## Fixes

- Fixed the GitHub build failure caused by `HorizontalScrollView.LayoutParams` in the IME source.
- Reworked input view construction to use compatible `ViewGroup.LayoutParams` / `LinearLayout.LayoutParams` types.
- Made large-text chunking UTF-16-safe so surrogate pairs are not split across paste chunks.

## Keyboard UX

- Rebuilt the keyboard layout toward a compact Gboard-like structure.
- Added a compact toolbar.
- Added a recent-clipboard suggestion strip.
- Kept the standard QWERTY three-letter-row structure.
- Added a dedicated `?123` symbols mode.
- Added a dedicated space-bar action.
- **Long-press Space = paste the latest saved clipboard item.**
- Short press Space still inserts a normal space.
- Improved key spacing, sizing, pressed states, and accessibility descriptions.

## Large Paste

- Continues to use `InputConnection.commitText()` through `LargeTextChunker`.
- Chunks are protected from splitting UTF-16 surrogate pairs.
- Stored clipboard entries remain unchanged if an insertion fails.

## Build

- Version code: 6
- Version name: 1.3.0
- Manual GitHub Actions build remains unchanged in policy: `workflow_dispatch` only.

