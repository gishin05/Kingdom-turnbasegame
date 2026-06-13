def replace_snow():
    path = r"c:\Users\BASANTA\eclipse-workspace\TurnBasedGame\tools\tileset-editor\snow-editor.html"
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find the button using regex. It's just a button that calls importData()
    import re
    content = re.sub(
        r'<button class="btn" onclick="importData\(\)">.*?</button>',
        '''<button class="btn btn-primary" onclick="document.getElementById('importInput').click()">📥 Import JSON</button>\n            <input type="file" id="importInput" accept=".json" style="display: none" onchange="handleImportFile(event)">''',
        content
    )

    # Now replace the JS block. We'll use regex to match from function importData() to the end of closeImport()
    js_pattern = re.compile(r'function importData\(\) \{[\s\S]*?function closeImport\(\) \{[\s\S]*?\}', re.MULTILINE)
    
    js_replacement = '''function handleImportFile(event) {
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
                        if (typeof updateStats === 'function') updateStats();
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
        }'''
        
    content = js_pattern.sub(js_replacement, content)
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
        
    print("Done")

replace_snow()
