# Update Snow tileset metadata

This project stores terrain metadata for the `Snow` tileset in:

- `assets/data/tilesets/Snow.json`

The source of truth is the Snow Tileset Editor (it persists the edited metadata in the browser's local storage). Use this workflow to export from the editor and rewrite `Snow.json` in a format that the game's `Tileset.java` loader can parse reliably.

## 1) Build the Editor (one-time or after Snow.png changes)

```powershell
powershell -ExecutionPolicy Bypass -File tools/tileset-editor/build-snow-editor.ps1
```

This reads `assets/data/tilesets/Snow.png`, base64-encodes it, and produces `tools/tileset-editor/snow-editor.html`.

## 2) Export from the Tileset Editor

1. Open `tools/tileset-editor/snow-editor.html` in a browser.
2. If you have an existing `Snow.json` file, click **Import JSON**, paste the contents of `assets/data/tilesets/Snow.json` or `tools/tileset-editor/snow-export.json`, and click **Apply Import**.
3. Configure the tilesets as needed using the editor interface.
4. Click **Export JSON**.
5. Click **Download Snow.json** or **Copy to Clipboard**.
6. Save the exported JSON into a file, e.g., `tools/tileset-editor/snow-export.json`.

## 3) Rewrite `Snow.json`

From the repository root, run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/tileset-editor/update-snow-tileset.ps1 -InputJson tools/tileset-editor/snow-export.json
```

### Optional: pad missing ids

If you want every id up to `1024` to have an explicit entry (even if it's default SNOW), run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/tileset-editor/update-snow-tileset.ps1 -InputJson tools/tileset-editor/snow-export.json -PadTo 1024
```

## What the script guarantees

- Output shape: `{ "tiles": { ... } }`
- Each tile entry is written on a single line like:
  - `"ID": {"type":"TYPE","def":DEF,"avo":AVO,"costs":{"Land Unit":X,"Ocean Unit":Y,"Air Unit":Z}}`
- Output is JSON-parseable (validated before writing).
