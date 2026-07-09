# This script runs the Character Editor as an independent tool.
# It sets up the classpath to include the main game's compiled classes and JavaFX libraries.

$workspaceRoot = "..\.."
$binDir = "$workspaceRoot\bin"
$libDir = "$workspaceRoot\lib"
$javafxLib = "$libDir\javafx-sdk-21.0.2\lib"

# Check if bin directory exists, otherwise the user needs to build the project in Eclipse
if (-not (Test-Path $binDir)) {
    Write-Host "Error: The 'bin' directory was not found. Please build the project in Eclipse first." -ForegroundColor Red
    Exit 1
}

# Add JavaFX libraries to module path and add modules
$modulePath = "--module-path ""$javafxLib"""
$addModules = "--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web"

# Run the CharacterEditorApp
Write-Host "Launching Character & Animation Editor..." -ForegroundColor Green
$javaVersion = java -version 2>&1
if ($javaVersion -match "1\.8") {
    Write-Host "Warning: Your default java is Java 8. This editor requires Java 21." -ForegroundColor Yellow
    Write-Host "Please run CharacterEditorApp.java directly from Eclipse." -ForegroundColor Yellow
} else {
    $javaArgs = @(
        "--module-path", $javafxLib,
        "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web",
        "-cp", $binDir,
        "tools.character_editor.CharacterEditorApp"
    )
    & java $javaArgs
}
