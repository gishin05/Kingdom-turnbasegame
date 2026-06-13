def update_editor(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Replace the import button
    old_button = '<button class="btn" onclick="importData()">📥 Import JSON</button>'
    new_button = '''<button class="btn btn-primary" onclick="document.getElementById('importInput').click()">📥 Import JSON</button>
            <input type="file" id="importInput" accept=".json" style="display: none" onchange="handleImportFile(event)">'''
    
    if old_button in content:
        content = content.replace(old_button, new_button)
        print(f"Replaced button in {path}")
    else:
        print(f"Button not found in {path}")

    # 2. Replace the JS function block
    import re
    js_regex = r'function importData\(\).*?function closeImport\(\) \{[\s\S]*?\}'
    
    replacement_js = '''function handleImportFile(event) {
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
        }'''
        
    if re.search(js_regex, content):
        content = re.sub(js_regex, replacement_js, content)
        print(f"Replaced JS in {path}")
    else:
        print(f"JS regex not found in {path}")
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
        

update_editor(r"c:\Users\BASANTA\eclipse-workspace\TurnBasedGame\tools\tileset-editor\desert-editor.html")
update_editor(r"c:\Users\BASANTA\eclipse-workspace\TurnBasedGame\tools\tileset-editor\snow-editor.html")
