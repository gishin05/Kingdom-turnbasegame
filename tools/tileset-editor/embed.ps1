$imgPath = "c:\Users\BASANTA\eclipse-workspace\TurnBasedGame\assets\data\tilesets\Plain.png"
$htmlPath = "c:\Users\BASANTA\eclipse-workspace\TurnBasedGame\tools\tileset-editor\index.html"
$bytes = [IO.File]::ReadAllBytes($imgPath)
$b64 = [Convert]::ToBase64String($bytes)
$content = [IO.File]::ReadAllText($htmlPath)
$content = $content.Replace("'../../assets/data/tilesets/Plain.png'", "'data:image/png;base64,$b64'")
[IO.File]::WriteAllText($htmlPath, $content)
Write-Host "Done - embedded $($b64.Length) chars of base64"
