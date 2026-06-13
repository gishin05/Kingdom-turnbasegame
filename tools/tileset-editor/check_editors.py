def check_editor(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    import re
    # Print the button to see exactly what characters it has
    match = re.search(r'<div class="header-actions">[\s\S]*?</div>', content)
    if match:
        print(f"--- {path} ---")
        print(repr(match.group(0)))

check_editor(r"c:\Users\BASANTA\eclipse-workspace\TurnBasedGame\tools\tileset-editor\desert-editor.html")
check_editor(r"c:\Users\BASANTA\eclipse-workspace\TurnBasedGame\tools\tileset-editor\snow-editor.html")
