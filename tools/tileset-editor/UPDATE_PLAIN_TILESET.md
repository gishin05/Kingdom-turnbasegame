# Update Plain tileset metadata

This project stores terrain metadata for the `Plain` tileset in:

- `assets/data/tilesets/Plain.json`

The source of truth is the Tileset Editor (it persists the edited metadata in the browser’s local storage). Use this workflow to export from the editor and rewrite `Plain.json` in a format that the game’s `Tileset.java` loader can parse reliably.

## 1) Export from the Tileset Editor

1. Open `tools/tileset-editor/index.html` in a browser.
2. Click **Export JSON**.
3. Click **Copy to Clipboard** (or manually copy the entire JSON from the text area).
4. Save the copied JSON into a file, e.g. `tools/tileset-editor/plain-export.json`.

## 2) Rewrite `Plain.json`

From the repository root, run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/tileset-editor/update-plain-tileset.ps1 -InputJson tools/tileset-editor/plain-export.json
```

### Optional: pad missing ids

If you want every id up to `1024` to have an explicit entry (even if it’s default PLAIN), run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/tileset-editor/update-plain-tileset.ps1 -InputJson tools/tileset-editor/plain-export.json -PadTo 1024
```

## What the script guarantees

- Output shape: `{ "tiles": { ... } }`
- Each tile entry is written on a single line like:
  - `"ID": {"type":"TYPE","def":DEF,"avo":AVO,"costs":{"Land Unit":X,"Ocean Unit":Y,"Air Unit":Z}}`
- Output is JSON-parseable (validated before writing).

