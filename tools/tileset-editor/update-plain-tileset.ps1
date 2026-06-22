param(
  # Path to an exported JSON file from the tileset editor (or any equivalent JSON containing tiles metadata).
  [Parameter(Mandatory=$true)][string]$InputJson,

  # Output target; defaults to the game's Plain.json.
  [string]$OutputJson = "",

  # Optional padding: when set, ensure every missing tile id in [0..PadTo] exists (default entries).
  [int]$PadTo = -1
)

$ErrorActionPreference = "Stop"

function Get-MaxIdFromMaps {
  param(
    [Parameter(Mandatory=$true)][string]$MapsRoot
  )

  if (!(Test-Path -LiteralPath $MapsRoot)) { return $null }

  $maxId = $null
  Get-ChildItem -LiteralPath $MapsRoot -Recurse -Filter *.json | ForEach-Object {
    $path = $_.FullName
    $text = Get-Content -LiteralPath $path -Raw

    # Typical map format observed: {"id":873,"ts":"..."}
    [regex]::Matches($text, '"id"\s*:\s*(\d+)') | ForEach-Object {
      $id = [int]$_.Groups[1].Value
      if ($maxId -eq $null -or $id -gt $maxId) { $maxId = $id }
    }
  }
  return $maxId
}

function Get-MaxTileIdFromPng {
  param(
    [Parameter(Mandatory=$true)][string]$PngPath,
    [int]$TileWidth = 8,
    [int]$TileHeight = 8
  )

  if (!(Test-Path -LiteralPath $PngPath)) { return $null }
  Add-Type -AssemblyName System.Drawing
  $img = [System.Drawing.Image]::FromFile($PngPath)
  try {
    $cols = [math]::Floor($img.Width / $TileWidth)
    $rows = [math]::Floor($img.Height / $TileHeight)
    return ($cols * $rows)
  } finally {
    $img.Dispose()
  }
}

function Normalize-TilesObject {
  param(
    [Parameter(Mandatory=$true)]$JsonObj
  )

  if ($JsonObj.PSObject.Properties.Name -contains "tiles") {
    return $JsonObj.tiles
  }

  # Some exporters might output just the tiles map at the top level.
  # Heuristic: if keys look numeric and values look like tile props, accept it.
  $props = $JsonObj.PSObject.Properties
  $looksLikeTiles = $true
  foreach ($p in $props) {
    if ($p.Name -notmatch '^\d+$') { $looksLikeTiles = $false; break }
    if ($p.Value -eq $null) { $looksLikeTiles = $false; break }
    if (!($p.Value.PSObject.Properties.Name -contains "type")) { $looksLikeTiles = $false; break }
  }
  if ($looksLikeTiles) { return $JsonObj }

  throw "Input JSON must contain a 'tiles' object, or be a tiles map itself."
}

function Format-TileLine {
  param(
    [Parameter(Mandatory=$true)][string]$Id,
    [Parameter(Mandatory=$true)]$Tile
  )

  $type = [string]$Tile.type
  if ([string]::IsNullOrWhiteSpace($type)) { $type = "PLAIN" }

  $def = 0
  if ($Tile.PSObject.Properties.Name -contains "def") { $def = [int]$Tile.def }

  $avo = 0
  if ($Tile.PSObject.Properties.Name -contains "avo") { $avo = [int]$Tile.avo }

  $costs = $Tile.costs
  $land = 1; $ocean = -1; $air = 1
  if ($costs -ne $null) {
    if ($costs.PSObject.Properties.Name -contains "Land Unit") { $land = [int]$costs."Land Unit" }
    if ($costs.PSObject.Properties.Name -contains "Ocean Unit") { $ocean = [int]$costs."Ocean Unit" }
    if ($costs.PSObject.Properties.Name -contains "Air Unit") { $air = [int]$costs."Air Unit" }
  }

  return "    `"$Id`": {`"type`":`"$type`",`"def`":$def,`"avo`":$avo,`"costs`":{`"Land Unit`":$land,`"Ocean Unit`":$ocean,`"Air Unit`":$air}}"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrWhiteSpace($OutputJson)) {
  $OutputJson = Join-Path $scriptDir "..\..\assets\data\tilesets\Plain.json"
}

$repoRoot = (Resolve-Path (Join-Path $scriptDir "..\..")).Path
$mapsRoot = (Join-Path $repoRoot "assets\data\maps")
$plainPng = (Join-Path $repoRoot "assets\data\tilesets\Plain.png")

if (!(Test-Path -LiteralPath $InputJson)) { throw "Input JSON file not found: $InputJson" }

$raw = Get-Content -LiteralPath $InputJson -Raw
$jsonObj = $raw | ConvertFrom-Json
$tilesObj = Normalize-TilesObject -JsonObj $jsonObj

$maxExportId = $null
$tileMap = @{}
foreach ($p in $tilesObj.PSObject.Properties) {
  if ($p.Name -notmatch '^\d+$') { continue }
  $id = [int]$p.Name
  if ($maxExportId -eq $null -or $id -gt $maxExportId) { $maxExportId = $id }
  $tileMap[$id] = $p.Value
}

$maxMapId = Get-MaxIdFromMaps -MapsRoot $mapsRoot
$maxPngTiles = Get-MaxTileIdFromPng -PngPath $plainPng

if ($PadTo -ge 0) {
  for ($i = 0; $i -le $PadTo; $i++) {
    if (!($tileMap.ContainsKey($i))) {
      $tileMap[$i] = [pscustomobject]@{
        type = "PLAIN"
        def = 0
        avo = 0
        costs = [pscustomobject]@{
          "Land Unit" = 1
          "Ocean Unit" = -1
          "Air Unit" = 1
        }
      }
    }
  }
}

$ids = $tileMap.Keys | Sort-Object

$outLines = New-Object System.Collections.Generic.List[string]
$outLines.Add("{")
$outLines.Add("  `"tiles`": {")
for ($idx = 0; $idx -lt $ids.Count; $idx++) {
  $id = $ids[$idx]
  $line = Format-TileLine -Id "$id" -Tile $tileMap[$id]
  if ($idx -lt ($ids.Count - 1)) { $line = $line + "," }
  $outLines.Add($line)
}
$outLines.Add("  }")
$outLines.Add("}")

$outText = ($outLines -join "`n")

# Validate JSON (round-trip parse) before writing.
$null = $outText | ConvertFrom-Json

$outputDir = Split-Path -Parent $OutputJson
if (!(Test-Path -LiteralPath $outputDir)) { New-Item -ItemType Directory -Path $outputDir | Out-Null }

Set-Content -LiteralPath $OutputJson -Value $outText -Encoding UTF8

Write-Host "Wrote: $OutputJson"
Write-Host "Max export id: $maxExportId"
Write-Host "Max map id: $maxMapId"
Write-Host "Plain.png tile capacity: $maxPngTiles"
if ($PadTo -ge 0) { Write-Host "Padded to: $PadTo" }
