def fix_snow():
    path = r"c:\Users\BASANTA\eclipse-workspace\TurnBasedGame\tools\tileset-editor\snow-editor.html"
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    # Replace line 593 (index 592)
    lines[592] = '''            <button class="btn btn-primary" onclick="document.getElementById('importInput').click()">📥 Import JSON</button>
            <input type="file" id="importInput" accept=".json" style="display: none" onchange="handleImportFile(event)">\n'''
            
    # Replace lines 1068-1100 (index 1067-1099)
    js_code = '''        function handleImportFile(event) {
            const file = event.target.files[0];
            if (!file) return;

            const reader = new FileReader();
            reader.onload = function(e) {
                try {
                    const json = JSON.parse(e.target.result);
                    if (json.tiles) {
                        for (const [id, t] of Object.entries(json.tiles)) {
                            tileData[parseInt(id)] = {
                                name: t.type || '',
                                landCost: t.costs && t.costs['Land Unit'] !== undefined ? t.costs['Land Unit'] : 1,
                                oceanCost: t.costs && t.costs['Ocean Unit'] !== undefined ? t.costs['Ocean Unit'] : -1,
                                airCost: t.costs && t.costs['Air Unit'] !== undefined ? t.costs['Air Unit'] : 1,
                                def: t.def || 0,
                                avo: t.avo || 0,
                            };
                        }
                        saveToLocalStorage();
                        buildGrid();
                        updateStats();
                        alert('Tileset imported successfully!');
                    } else {
                        alert('Invalid format: expected { "tiles": { ... } }');
                    }
                } catch (err) {
                    alert('Invalid JSON data: ' + err.message);
                }
            };
            reader.readAsText(file);
            event.target.value = '';
        }\n'''
        
    new_lines = lines[:1067] + [js_code] + lines[1100:]
    
    with open(path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
        
fix_snow()
