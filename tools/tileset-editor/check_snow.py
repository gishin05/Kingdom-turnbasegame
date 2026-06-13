import re

path = r"c:\Users\BASANTA\eclipse-workspace\TurnBasedGame\tools\tileset-editor\snow-editor.html"
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Is there any button with importData?
match = re.search(r'<button[^>]*>.*?Import JSON.*?</button>', content)
if match:
    print("Found button:", repr(match.group(0)))
else:
    print("No import button found!")

match_js = re.search(r'function importData', content)
if match_js:
    print("Found function importData")
else:
    print("No function importData found!")
