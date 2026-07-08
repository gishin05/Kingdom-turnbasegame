# Update Desert tileset metadata

This project stores terrain metadata for the `Desert` tileset in:

- `assets/data/tilesets/Desert.json`

The source of truth is the Desert Tileset Editor (it persists the edited metadata in the browser’s local storage). Use this workflow to export from the editor and rewrite `Desert.json` in a format that the game’s `Tileset.java` loader can parse reliably.

## 1) Export from the Tileset Editor

1. Open `tools/tileset-editor/desert-editor.html` in a browser.
2. If you have an existing `Desert.json` file, click **Import JSON**, paste the contents of `assets/data/tilesets/Desert.json` or `tools/tileset-editor/desert-export.json`, and click **Apply Import**.
3. Configure the tilesets as needed using the editor interface.
4. Click **Export JSON**.
5. Click **Download Desert.json** or **Copy to Clipboard**.
6. Save the exported JSON into a file, e.g., `tools/tileset-editor/desert-export.json`.

## 2) Rewrite `Desert.json`

From the repository root, run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/tileset-editor/update-desert-tileset.ps1 -InputJson tools/tileset-editor/desert-export.json
```

### Optional: pad missing ids

If you want every id up to `1024` to have an explicit entry (even if it's default SAND), run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/tileset-editor/update-desert-tileset.ps1 -InputJson tools/tileset-editor/desert-export.json -PadTo 1024
```

## What the script guarantees

- Output shape: `{ "tiles": { ... } }`
- Each tile entry is written on a single line like:
  - `"ID": {"type":"TYPE","def":DEF,"avo":AVO,"costs":{"Land Unit":X,"Ocean Unit":Y,"Air Unit":Z}}`
- Output is JSON-parseable (validated before writing).
