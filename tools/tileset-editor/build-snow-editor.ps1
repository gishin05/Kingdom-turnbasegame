# Build Snow Tileset Editor HTML
# Reads the Snow.png base64 and injects it into the HTML template

$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("$PSScriptRoot\..\..\assets\data\tilesets\Snow.png"))

$html = @"
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Snow Tileset Editor</title>
    <meta name="description" content="Interactive terrain property editor for the Snow tileset (16x16 tiles)">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-dark: #0a101a;
            --bg-card: #0e1824;
            --bg-cell: #10202e;
            --bg-cell-hover: #183040;
            --bg-cell-filled: #102a2a;
            --border: #204050;
            --border-ice: #60b8e8;
            --border-ice-dim: rgba(96, 184, 232, 0.25);
            --text: #d0e8f8;
            --text-dim: #708898;
            --text-ice: #60b8e8;
            --accent-green: #60d898;
            --accent-blue: #60a5fa;
            --accent-red: #e87050;
            --accent-purple: #c084fc;
            --accent-snow: #b0d8f0;
            --accent-frost: #80c0e0;
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Inter', sans-serif;
            background: var(--bg-dark);
            color: var(--text);
            min-height: 100vh;
        }

        /* ── HEADER ── */
        .header {
            background: linear-gradient(135deg, #060c18 0%, #0e1e30 50%, #0a101a 100%);
            border-bottom: 1px solid var(--border-ice-dim);
            padding: 20px 32px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: sticky;
            top: 0;
            z-index: 100;
            backdrop-filter: blur(12px);
        }

        .header h1 {
            font-size: 22px;
            font-weight: 700;
            color: var(--text-ice);
            letter-spacing: 1.5px;
            text-transform: uppercase;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .header h1 span {
            font-size: 14px;
            color: var(--text-dim);
            font-weight: 400;
            letter-spacing: 0;
            text-transform: none;
        }

        .header-actions {
            display: flex;
            gap: 10px;
            align-items: center;
        }

        .btn {
            font-family: 'Inter', sans-serif;
            font-size: 13px;
            font-weight: 600;
            padding: 10px 20px;
            border: 1px solid var(--border-ice-dim);
            border-radius: 8px;
            background: var(--bg-cell);
            color: var(--text);
            cursor: pointer;
            transition: all 0.2s ease;
            letter-spacing: 0.5px;
        }

        .btn:hover {
            background: #1a3848;
            border-color: var(--border-ice);
            box-shadow: 0 0 12px rgba(96, 184, 232, 0.2);
        }

        .btn-primary {
            background: linear-gradient(135deg, #2068a0, #40a0d8);
            color: #fff;
            border-color: var(--border-ice);
        }

        .btn-primary:hover {
            background: linear-gradient(135deg, #3080c0, #60b8e8);
            box-shadow: 0 0 20px rgba(96, 184, 232, 0.35);
        }

        .btn-danger {
            border-color: rgba(232, 112, 80, 0.4);
        }

        .btn-danger:hover {
            background: rgba(232, 112, 80, 0.15);
            border-color: var(--accent-red);
        }

        .btn-success {
            background: linear-gradient(135deg, #185848, #308068);
            color: #fff;
            border-color: #308068;
        }

        .btn-success:hover {
            background: linear-gradient(135deg, #287058, #60d898);
            box-shadow: 0 0 20px rgba(96, 216, 152, 0.3);
        }

        /* ── STATS BAR ── */
        .stats-bar {
            display: flex;
            gap: 24px;
            padding: 12px 32px;
            background: var(--bg-card);
            border-bottom: 1px solid var(--border);
            font-size: 13px;
        }

        .stat {
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .stat-label {
            color: var(--text-dim);
        }

        .stat-value {
            font-family: 'JetBrains Mono', monospace;
            font-weight: 600;
            color: var(--accent-blue);
        }

        .stat-value.filled {
            color: var(--accent-green);
        }

        /* ── CONTROLS BAR ── */
        .controls-bar {
            display: flex;
            gap: 16px;
            padding: 14px 32px;
            background: rgba(14, 24, 36, 0.8);
            border-bottom: 1px solid var(--border);
            align-items: center;
            flex-wrap: wrap;
        }

        .control-group {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .control-group label {
            font-size: 12px;
            color: var(--text-dim);
            font-weight: 500;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .control-input {
            font-family: 'JetBrains Mono', monospace;
            font-size: 13px;
            padding: 6px 12px;
            background: var(--bg-cell);
            border: 1px solid var(--border);
            border-radius: 6px;
            color: var(--text);
            outline: none;
            transition: border-color 0.2s;
        }

        .control-input:focus {
            border-color: var(--border-ice);
        }

        select.control-input {
            cursor: pointer;
        }

        .toggle-btn {
            font-family: 'Inter', sans-serif;
            font-size: 12px;
            padding: 6px 14px;
            border: 1px solid var(--border);
            border-radius: 6px;
            background: var(--bg-cell);
            color: var(--text-dim);
            cursor: pointer;
            transition: all 0.2s;
        }

        .toggle-btn.active {
            border-color: var(--border-ice);
            color: var(--text-ice);
            background: rgba(96, 184, 232, 0.08);
        }

        /* ── GRID CONTAINER ── */
        .grid-container {
            padding: 24px;
            overflow: auto;
            max-height: calc(100vh - 200px);
        }

        .tile-grid {
            display: grid;
            gap: 1px;
            background: var(--border);
            border: 2px solid var(--border-ice-dim);
            border-radius: 4px;
            width: fit-content;
        }

        /* ── TILE CELL ── */
        .tile-cell {
            background: var(--bg-cell);
            position: relative;
            cursor: pointer;
            transition: all 0.15s ease;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            overflow: hidden;
        }

        .tile-cell:hover {
            background: var(--bg-cell-hover);
            z-index: 10;
            box-shadow: 0 0 0 2px var(--border-ice);
        }

        .tile-cell.has-data {
            background: var(--bg-cell-filled);
            border-color: rgba(96, 216, 152, 0.3);
        }

        .tile-cell.has-data:hover {
            background: #183838;
        }

        .tile-cell .tile-id {
            font-family: 'JetBrains Mono', monospace;
            font-size: 9px;
            color: var(--text-dim);
            position: absolute;
            top: 2px;
            left: 3px;
            z-index: 2;
            pointer-events: none;
            line-height: 1;
        }

        .tile-cell.has-data .tile-id {
            color: var(--accent-green);
        }

        .tile-cell .tile-preview {
            width: 100%;
            height: 100%;
            image-rendering: pixelated;
            position: absolute;
            top: 0;
            left: 0;
            opacity: 0.3;
        }

        .tile-cell.show-images .tile-preview {
            opacity: 1;
        }

        .tile-cell .tile-name-preview {
            font-size: 7px;
            color: var(--accent-green);
            position: absolute;
            bottom: 1px;
            left: 2px;
            right: 2px;
            text-align: center;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
            z-index: 2;
            pointer-events: none;
            line-height: 1;
            font-weight: 600;
        }

        .tile-cell .tile-cost-preview {
            font-family: 'JetBrains Mono', monospace;
            font-size: 9px;
            color: var(--accent-snow);
            position: absolute;
            top: 2px;
            right: 3px;
            z-index: 2;
            pointer-events: none;
            line-height: 1;
            font-weight: 600;
        }

        /* ── EDIT MODAL ── */
        .modal-overlay {
            display: none;
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.7);
            backdrop-filter: blur(6px);
            z-index: 1000;
            align-items: center;
            justify-content: center;
        }

        .modal-overlay.show {
            display: flex;
        }

        .modal {
            background: var(--bg-card);
            border: 1px solid var(--border-ice-dim);
            border-radius: 16px;
            padding: 0;
            min-width: 420px;
            max-width: 520px;
            box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6), 0 0 40px rgba(96, 184, 232, 0.06);
            animation: modalIn 0.2s ease;
        }

        @keyframes modalIn {
            from { opacity: 0; transform: scale(0.95) translateY(10px); }
            to { opacity: 1; transform: scale(1) translateY(0); }
        }

        .modal-header {
            display: flex;
            align-items: center;
            gap: 16px;
            padding: 20px 24px;
            border-bottom: 1px solid var(--border);
        }

        .modal-tile-preview {
            width: 64px;
            height: 64px;
            image-rendering: pixelated;
            border: 2px solid var(--border-ice);
            border-radius: 8px;
            background: var(--bg-dark);
        }

        .modal-tile-info h2 {
            font-size: 18px;
            font-weight: 700;
            color: var(--text-ice);
        }

        .modal-tile-info p {
            font-size: 13px;
            color: var(--text-dim);
            margin-top: 2px;
        }

        .modal-body {
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 18px;
        }

        .form-group {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .form-group label {
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.8px;
            color: var(--text-dim);
        }

        .form-group input, .form-group select {
            font-family: 'Inter', sans-serif;
            font-size: 14px;
            padding: 10px 14px;
            background: var(--bg-dark);
            border: 1px solid var(--border);
            border-radius: 8px;
            color: var(--text);
            outline: none;
            transition: border-color 0.2s;
        }

        .form-group input:focus, .form-group select:focus {
            border-color: var(--border-ice);
            box-shadow: 0 0 0 3px rgba(96, 184, 232, 0.08);
        }

        .form-row {
            display: grid;
            grid-template-columns: 1fr 1fr 1fr;
            gap: 12px;
        }

        .modal-footer {
            display: flex;
            justify-content: space-between;
            padding: 16px 24px;
            border-top: 1px solid var(--border);
        }

        .modal-footer .btn-group {
            display: flex;
            gap: 8px;
        }

        /* ── TERRAIN PRESETS ── */
        .preset-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
            gap: 6px;
        }

        .preset-btn {
            font-family: 'Inter', sans-serif;
            font-size: 11px;
            font-weight: 500;
            padding: 6px 8px;
            border: 1px solid var(--border);
            border-radius: 6px;
            background: var(--bg-cell);
            color: var(--text-dim);
            cursor: pointer;
            transition: all 0.15s;
            text-align: center;
        }

        .preset-btn:hover {
            border-color: var(--border-ice);
            color: var(--text-ice);
            background: rgba(96, 184, 232, 0.08);
        }

        /* ── EXPORT PANEL ── */
        .export-panel {
            display: none;
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.7);
            backdrop-filter: blur(6px);
            z-index: 1000;
            align-items: center;
            justify-content: center;
        }

        .export-panel.show {
            display: flex;
        }

        .export-content {
            background: var(--bg-card);
            border: 1px solid var(--border-ice-dim);
            border-radius: 16px;
            padding: 24px;
            min-width: 600px;
            max-width: 800px;
            max-height: 80vh;
            overflow: auto;
            box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6);
        }

        .export-content h2 {
            color: var(--text-ice);
            margin-bottom: 16px;
        }

        .export-textarea {
            width: 100%;
            min-height: 300px;
            background: var(--bg-dark);
            border: 1px solid var(--border);
            border-radius: 8px;
            color: var(--accent-green);
            font-family: 'JetBrains Mono', monospace;
            font-size: 13px;
            padding: 16px;
            resize: vertical;
            outline: none;
        }

        .export-actions {
            display: flex;
            gap: 10px;
            margin-top: 16px;
            justify-content: flex-end;
        }

        /* ── IMPORT PANEL ── */
        .import-panel {
            display: none;
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.7);
            backdrop-filter: blur(6px);
            z-index: 1000;
            align-items: center;
            justify-content: center;
        }

        .import-panel.show {
            display: flex;
        }

        /* ── SCROLLBAR ── */
        ::-webkit-scrollbar { width: 8px; height: 8px; }
        ::-webkit-scrollbar-track { background: var(--bg-dark); }
        ::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
        ::-webkit-scrollbar-thumb:hover { background: #305868; }

        /* ── LOADING ── */
        .loading {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 60vh;
            flex-direction: column;
            gap: 16px;
        }

        .loading-spinner {
            width: 40px;
            height: 40px;
            border: 3px solid var(--border);
            border-top-color: var(--border-ice);
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
        }

        @keyframes spin { to { transform: rotate(360deg); } }

        .nav-hint {
            font-size: 12px;
            color: var(--text-dim);
            padding: 2px 10px;
            background: rgba(255,255,255,0.03);
            border-radius: 4px;
            border: 1px solid var(--border);
        }
    </style>
</head>
<body>

    <!-- HEADER -->
    <div class="header">
        <h1>
            ❄ SNOW TILESET EDITOR
            <span>— Snow.png (16×16 tiles)</span>
        </h1>
        <div class="header-actions">
            <span class="nav-hint">Click a tile to edit · Enter = Save & Next · Esc = Close</span>
            <button class="btn btn-danger" onclick="clearAll()">Clear All</button>
            <button class="btn" onclick="importData()">📥 Import JSON</button>
            <button class="btn btn-primary" onclick="exportData()">📦 Export JSON</button>
        </div>
    </div>

    <!-- STATS BAR -->
    <div class="stats-bar">
        <div class="stat">
            <span class="stat-label">Total Tiles:</span>
            <span class="stat-value" id="totalTiles">—</span>
        </div>
        <div class="stat">
            <span class="stat-label">Configured:</span>
            <span class="stat-value filled" id="filledTiles">0</span>
        </div>
        <div class="stat">
            <span class="stat-label">Grid:</span>
            <span class="stat-value" id="gridSize">—</span>
        </div>
        <div class="stat">
            <span class="stat-label">Hovered:</span>
            <span class="stat-value" id="hoveredId">—</span>
        </div>
    </div>

    <!-- CONTROLS BAR -->
    <div class="controls-bar">
        <div class="control-group">
            <label>Cell Size:</label>
            <select class="control-input" id="cellSizeSelect" onchange="changeCellSize()">
                <option value="24">24px (Tiny)</option>
                <option value="32">32px (Small)</option>
                <option value="48" selected>48px (Medium)</option>
                <option value="64">64px (Large)</option>
                <option value="96">96px (XL)</option>
            </select>
        </div>
        <button class="toggle-btn active" id="toggleImages" onclick="toggleImages()">🖼 Show Tile Images</button>
        <button class="toggle-btn" id="toggleIds" onclick="toggleIds()">🔢 Show IDs</button>
        <button class="toggle-btn active" id="toggleNames" onclick="toggleNames()">🏷 Show Names</button>
        <div class="control-group" style="margin-left: auto;">
            <label>Filter:</label>
            <select class="control-input" id="filterSelect" onchange="applyFilter()">
                <option value="all">All Tiles</option>
                <option value="configured">Configured Only</option>
                <option value="unconfigured">Unconfigured Only</option>
            </select>
        </div>
    </div>

    <!-- GRID -->
    <div class="grid-container" id="gridContainer">
        <div class="loading">
            <div class="loading-spinner"></div>
            <span style="color: var(--text-dim);">Loading snow tileset...</span>
        </div>
    </div>

    <!-- EDIT MODAL -->
    <div class="modal-overlay" id="editModal">
        <div class="modal">
            <div class="modal-header">
                <canvas class="modal-tile-preview" id="modalPreview" width="64" height="64"></canvas>
                <div class="modal-tile-info">
                    <h2>Tile #<span id="modalTileId">0</span></h2>
                    <p>Row <span id="modalRow">0</span>, Col <span id="modalCol">0</span></p>
                </div>
            </div>
            <div class="modal-body">
                <div class="form-group">
                    <label>Terrain Type (Quick Pick)</label>
                    <div class="preset-grid" id="presetGrid"></div>
                </div>
                <div class="form-group">
                    <label>Name / Type</label>
                    <input type="text" id="inputName" placeholder="e.g. SNOW, ICE, TUNDRA..." autocomplete="off">
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Land Cost</label>
                        <input type="number" id="inputLandCost" value="1" min="-1" step="1">
                    </div>
                    <div class="form-group">
                        <label>Ocean Cost</label>
                        <input type="number" id="inputOceanCost" value="-1" min="-1" step="1">
                    </div>
                    <div class="form-group">
                        <label>Air Cost</label>
                        <input type="number" id="inputAirCost" value="1" min="-1" step="1">
                    </div>
                </div>
                <div class="form-row" style="grid-template-columns: 1fr 1fr;">
                    <div class="form-group">
                        <label>Def Bonus</label>
                        <input type="number" id="inputDef" value="0" min="0" step="1">
                    </div>
                    <div class="form-group">
                        <label>Avo Bonus</label>
                        <input type="number" id="inputAvo" value="0" min="0" step="1">
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-danger" onclick="clearTile()">Clear</button>
                <div class="btn-group">
                    <button class="btn" onclick="closeModal()">Cancel</button>
                    <button class="btn" onclick="saveTileAndPrev()">◀ Save & Prev</button>
                    <button class="btn btn-primary" onclick="saveTileAndNext()">Save & Next ▶</button>
                </div>
            </div>
        </div>
    </div>

    <!-- EXPORT PANEL -->
    <div class="export-panel" id="exportPanel">
        <div class="export-content">
            <h2>📦 Exported JSON — Snow.json</h2>
            <p style="color: var(--text-dim); margin-bottom: 12px;">Copy this JSON and save it as <code>Snow.json</code> in your tilesets data folder.</p>
            <textarea class="export-textarea" id="exportTextarea" readonly></textarea>
            <div class="export-actions">
                <button class="btn" onclick="closeExport()">Close</button>
                <button class="btn btn-success" onclick="downloadExport()">💾 Download Snow.json</button>
                <button class="btn btn-primary" onclick="copyExport()">📋 Copy to Clipboard</button>
            </div>
        </div>
    </div>

    <!-- IMPORT PANEL -->
    <div class="import-panel" id="importPanel">
        <div class="export-content">
            <h2>📥 Import JSON — Snow.json</h2>
            <p style="color: var(--text-dim); margin-bottom: 12px;">Paste your existing Snow.json data below to load it into the editor.</p>
            <textarea class="export-textarea" id="importTextarea" placeholder='{ "tiles": { "0": { "type": "SNOW", ... } } }'></textarea>
            <div class="export-actions">
                <button class="btn" onclick="closeImport()">Cancel</button>
                <button class="btn btn-primary" onclick="applyImport()">✅ Apply Import</button>
            </div>
        </div>
    </div>

    <script>
        // ── CONFIG ──
        const TILESET_PATH = 'data:image/png;base64,$b64';
        const TILE_W = 16;
        const TILE_H = 16;
        const STORAGE_KEY = 'tileset_editor_snow';

        // ── Snow-specific terrain presets ──
        const PRESETS = [
            { name: 'SNOW',      land: 1,  ocean: -1, air: 1, def: 0, avo: 0 },
            { name: 'ICE',       land: 2,  ocean: -1, air: 1, def: 0, avo: 5 },
            { name: 'GLACIER',   land: 2,  ocean: -1, air: 1, def: 1, avo: 5 },
            { name: 'TUNDRA',    land: 1,  ocean: -1, air: 1, def: 0, avo: 5 },
            { name: 'PINE',      land: 2,  ocean: -1, air: 1, def: 1, avo: 10 },
            { name: 'MOUNTAIN',  land: -1, ocean: -1, air: 1, def: 3, avo: 0 },
            { name: 'FROZEN_LAKE', land: -1, ocean: 1, air: 1, def: 0, avo: 0 },
            { name: 'RIVER',     land: -1, ocean: 1,  air: 1, def: 0, avo: 0 },
            { name: 'BRIDGE',    land: 1,  ocean: -1, air: 1, def: 0, avo: 0 },
            { name: 'ROAD',      land: 1,  ocean: -1, air: 1, def: 0, avo: 0 },
            { name: 'ICE_WALL',  land: -1, ocean: -1, air: -1, def: 0, avo: 0 },
            { name: 'CLIFF',     land: -1, ocean: -1, air: 1, def: 0, avo: 0 },
            { name: 'VILLAGE',   land: 1,  ocean: -1, air: 1, def: 1, avo: 10 },
            { name: 'FORT',      land: 1,  ocean: -1, air: 1, def: 2, avo: 15 },
            { name: 'WALL',      land: -1, ocean: -1, air: -1, def: 0, avo: 0 },
            { name: 'GATE',      land: 1,  ocean: -1, air: 1, def: 2, avo: 10 },
            { name: 'CHEST',     land: 1,  ocean: -1, air: 1, def: 0, avo: 0 },
        ];

        // ── STATE ──
        let tileData = {};
        let tilesetImage = null;
        let gridCols = 0;
        let gridRows = 0;
        let totalTiles = 0;
        let cellSize = 48;
        let currentEditId = null;
        let showImages = true;
        let showIds = false;
        let showNames = true;

        // ── LOAD TILESET ──
        function loadTileset() {
            const img = new Image();
            img.onload = () => {
                tilesetImage = img;
                gridCols = Math.floor(img.width / TILE_W);
                gridRows = Math.floor(img.height / TILE_H);
                totalTiles = gridCols * gridRows;

                document.getElementById('totalTiles').textContent = totalTiles;
                document.getElementById('gridSize').textContent = gridCols + '×' + gridRows;

                loadSavedData();
                buildGrid();
                buildPresets();
            };
            img.src = TILESET_PATH;
        }

        // ── BUILD GRID ──
        function buildGrid() {
            const container = document.getElementById('gridContainer');
            const grid = document.createElement('div');
            grid.className = 'tile-grid';
            grid.style.gridTemplateColumns = 'repeat(' + gridCols + ', ' + cellSize + 'px)';
            grid.style.gridTemplateRows = 'repeat(' + gridRows + ', ' + cellSize + 'px)';

            for (let id = 0; id < totalTiles; id++) {
                const row = Math.floor(id / gridCols);
                const col = id % gridCols;

                const cell = document.createElement('div');
                cell.className = 'tile-cell' + (showImages ? ' show-images' : '');
                cell.dataset.id = id;
                cell.style.width = cellSize + 'px';
                cell.style.height = cellSize + 'px';

                // Tile preview
                const canvas = document.createElement('canvas');
                canvas.className = 'tile-preview';
                canvas.width = TILE_W;
                canvas.height = TILE_H;
                const ctx = canvas.getContext('2d');
                ctx.drawImage(tilesetImage, col * TILE_W, row * TILE_H, TILE_W, TILE_H, 0, 0, TILE_W, TILE_H);
                cell.appendChild(canvas);

                // ID label
                const idLabel = document.createElement('span');
                idLabel.className = 'tile-id';
                idLabel.textContent = id;
                idLabel.style.display = showIds ? 'block' : 'none';
                cell.appendChild(idLabel);

                // Name label
                const nameLabel = document.createElement('span');
                nameLabel.className = 'tile-name-preview';
                nameLabel.style.display = showNames ? 'block' : 'none';
                cell.appendChild(nameLabel);

                // Cost label
                const costLabel = document.createElement('span');
                costLabel.className = 'tile-cost-preview';
                cell.appendChild(costLabel);

                // Apply saved data
                const data = tileData[id];
                if (data) {
                    cell.classList.add('has-data');
                    nameLabel.textContent = data.name;
                    costLabel.textContent = data.landCost;
                }

                cell.addEventListener('click', () => openModal(id));
                cell.addEventListener('mouseenter', () => {
                    document.getElementById('hoveredId').textContent = '#' + id + ' (R' + row + ' C' + col + ')';
                });

                grid.appendChild(cell);
            }

            container.innerHTML = '';
            container.appendChild(grid);
            updateStats();
        }

        // ── PRESETS ──
        function buildPresets() {
            const grid = document.getElementById('presetGrid');
            grid.innerHTML = '';
            for (const p of PRESETS) {
                const btn = document.createElement('button');
                btn.className = 'preset-btn';
                btn.textContent = p.name;
                btn.addEventListener('click', () => {
                    document.getElementById('inputName').value = p.name;
                    document.getElementById('inputLandCost').value = p.land;
                    document.getElementById('inputOceanCost').value = p.ocean;
                    document.getElementById('inputAirCost').value = p.air;
                    document.getElementById('inputDef').value = p.def;
                    document.getElementById('inputAvo').value = p.avo;
                });
                grid.appendChild(btn);
            }
        }

        // ── MODAL ──
        function openModal(id) {
            currentEditId = id;
            const row = Math.floor(id / gridCols);
            const col = id % gridCols;

            document.getElementById('modalTileId').textContent = id;
            document.getElementById('modalRow').textContent = row;
            document.getElementById('modalCol').textContent = col;

            // Draw preview
            const canvas = document.getElementById('modalPreview');
            const ctx = canvas.getContext('2d');
            ctx.clearRect(0, 0, 64, 64);
            ctx.imageSmoothingEnabled = false;
            ctx.drawImage(tilesetImage, col * TILE_W, row * TILE_H, TILE_W, TILE_H, 0, 0, 64, 64);

            // Fill form
            const data = tileData[id];
            document.getElementById('inputName').value = data ? data.name : '';
            document.getElementById('inputLandCost').value = data ? data.landCost : 1;
            document.getElementById('inputOceanCost').value = data ? data.oceanCost : -1;
            document.getElementById('inputAirCost').value = data ? data.airCost : 1;
            document.getElementById('inputDef').value = data ? data.def : 0;
            document.getElementById('inputAvo').value = data ? data.avo : 0;

            document.getElementById('editModal').classList.add('show');
            setTimeout(() => document.getElementById('inputName').focus(), 100);
        }

        function saveTile() {
            const name = document.getElementById('inputName').value.trim();
            if (!name) {
                delete tileData[currentEditId];
            } else {
                tileData[currentEditId] = {
                    name: name,
                    landCost: parseInt(document.getElementById('inputLandCost').value) || 0,
                    oceanCost: parseInt(document.getElementById('inputOceanCost').value) || 0,
                    airCost: parseInt(document.getElementById('inputAirCost').value) || 0,
                    def: parseInt(document.getElementById('inputDef').value) || 0,
                    avo: parseInt(document.getElementById('inputAvo').value) || 0,
                };
            }
            updateCell(currentEditId);
            saveToLocalStorage();
            updateStats();
        }

        function saveTileAndNext() {
            saveTile();
            const nextId = currentEditId + 1;
            if (nextId < totalTiles) {
                openModal(nextId);
            } else {
                closeModal();
            }
        }

        function saveTileAndPrev() {
            saveTile();
            const prevId = currentEditId - 1;
            if (prevId >= 0) {
                openModal(prevId);
            } else {
                closeModal();
            }
        }

        function clearTile() {
            delete tileData[currentEditId];
            updateCell(currentEditId);
            saveToLocalStorage();
            updateStats();
            closeModal();
        }

        function closeModal() {
            document.getElementById('editModal').classList.remove('show');
            currentEditId = null;
        }

        function updateCell(id) {
            const cell = document.querySelector('.tile-cell[data-id="' + id + '"]');
            if (!cell) return;
            const data = tileData[id];
            const nameLabel = cell.querySelector('.tile-name-preview');
            const costLabel = cell.querySelector('.tile-cost-preview');

            if (data) {
                cell.classList.add('has-data');
                nameLabel.textContent = data.name;
                costLabel.textContent = data.landCost;
            } else {
                cell.classList.remove('has-data');
                nameLabel.textContent = '';
                costLabel.textContent = '';
            }
        }

        // ── TOGGLES ──
        function toggleImages() {
            showImages = !showImages;
            document.getElementById('toggleImages').classList.toggle('active', showImages);
            document.querySelectorAll('.tile-cell').forEach(c => c.classList.toggle('show-images', showImages));
        }

        function toggleIds() {
            showIds = !showIds;
            document.getElementById('toggleIds').classList.toggle('active', showIds);
            document.querySelectorAll('.tile-id').forEach(el => el.style.display = showIds ? 'block' : 'none');
        }

        function toggleNames() {
            showNames = !showNames;
            document.getElementById('toggleNames').classList.toggle('active', showNames);
            document.querySelectorAll('.tile-name-preview').forEach(el => el.style.display = showNames ? 'block' : 'none');
        }

        function changeCellSize() {
            cellSize = parseInt(document.getElementById('cellSizeSelect').value);
            buildGrid();
        }

        function applyFilter() {
            const filter = document.getElementById('filterSelect').value;
            document.querySelectorAll('.tile-cell').forEach(cell => {
                const id = parseInt(cell.dataset.id);
                const hasData = !!tileData[id];
                if (filter === 'all') cell.style.display = '';
                else if (filter === 'configured') cell.style.display = hasData ? '' : 'none';
                else if (filter === 'unconfigured') cell.style.display = hasData ? 'none' : '';
            });
        }

        // ── STATS ──
        function updateStats() {
            document.getElementById('filledTiles').textContent = Object.keys(tileData).length;
        }

        // ── EXPORT ──
        function exportData() {
            const result = { tiles: {} };
            const sortedIds = Object.keys(tileData).map(Number).sort((a, b) => a - b);

            for (const id of sortedIds) {
                const d = tileData[id];
                result.tiles[String(id)] = {
                    type: d.name,
                    def: d.def,
                    avo: d.avo,
                    costs: {
                        "Land Unit": d.landCost,
                        "Ocean Unit": d.oceanCost,
                        "Air Unit": d.airCost
                    }
                };
            }

            const json = JSON.stringify(result, null, 2);
            document.getElementById('exportTextarea').value = json;
            document.getElementById('exportPanel').classList.add('show');
        }

        function copyExport() {
            const textarea = document.getElementById('exportTextarea');
            textarea.select();
            navigator.clipboard.writeText(textarea.value).then(() => {
                const btn = event.target;
                btn.textContent = '✅ Copied!';
                setTimeout(() => btn.textContent = '📋 Copy to Clipboard', 1500);
            });
        }

        function downloadExport() {
            const json = document.getElementById('exportTextarea').value;
            const blob = new Blob([json], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'Snow.json';
            a.click();
            URL.revokeObjectURL(url);
        }

        function closeExport() {
            document.getElementById('exportPanel').classList.remove('show');
        }

        // ── IMPORT ──
        function importData() {
            document.getElementById('importTextarea').value = '';
            document.getElementById('importPanel').classList.add('show');
        }

        function applyImport() {
            try {
                const json = JSON.parse(document.getElementById('importTextarea').value);
                if (json.tiles) {
                    for (const [id, t] of Object.entries(json.tiles)) {
                        tileData[parseInt(id)] = {
                            name: t.type || '',
                            landCost: t.costs ? t.costs['Land Unit'] : 1,
                            oceanCost: t.costs ? t.costs['Ocean Unit'] : -1,
                            airCost: t.costs ? t.costs['Air Unit'] : 1,
                            def: t.def || 0,
                            avo: t.avo || 0,
                        };
                    }
                    saveToLocalStorage();
                    buildGrid();
                    closeImport();
                } else {
                    alert('Invalid format: expected { "tiles": { ... } }');
                }
            } catch (e) {
                alert('Invalid JSON: ' + e.message);
            }
        }

        function closeImport() {
            document.getElementById('importPanel').classList.remove('show');
        }

        // ── CLEAR ──
        function clearAll() {
            if (!confirm('Clear all tile data? This cannot be undone.')) return;
            tileData = {};
            saveToLocalStorage();
            buildGrid();
        }

        // ── LOCAL STORAGE ──
        function saveToLocalStorage() {
            try {
                localStorage.setItem(STORAGE_KEY, JSON.stringify(tileData));
            } catch (e) {}
        }

        function loadSavedData() {
            try {
                const saved = localStorage.getItem(STORAGE_KEY);
                if (saved) {
                    tileData = JSON.parse(saved);
                }
            } catch (e) {}
        }

        // ── KEYBOARD ──
        document.addEventListener('keydown', (e) => {
            if (currentEditId === null) return;

            if (e.key === 'Escape') {
                closeModal();
                return;
            }

            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                saveTileAndNext();
                return;
            }
        });

        // Close modals on overlay click
        document.getElementById('editModal').addEventListener('click', (e) => {
            if (e.target === document.getElementById('editModal')) closeModal();
        });
        document.getElementById('exportPanel').addEventListener('click', (e) => {
            if (e.target === document.getElementById('exportPanel')) closeExport();
        });
        document.getElementById('importPanel').addEventListener('click', (e) => {
            if (e.target === document.getElementById('importPanel')) closeImport();
        });

        // ── INIT ──
        loadTileset();
    </script>
</body>
</html>
"@

Set-Content -Path "$PSScriptRoot\snow-editor.html" -Value $html -Encoding UTF8
Write-Host "Snow Tileset Editor built: $PSScriptRoot\snow-editor.html"
