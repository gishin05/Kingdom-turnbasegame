# Kingdom — Turn-Based Strategy Game

A pixelated turn-based strategy game built with **Java Swing** and **JavaFX**, featuring tactical grid-based combat, unit management, and a built-in map editor.

## Features

- **Turn-Based Combat** — Grid-based tactical battles with AI opponents, terrain effects, and battle animations
- **Unit System** — Champions and Units with individual stats, weapons, and movement engines
- **Map & Level Editor** — Built-in design tools for creating custom battle maps and unit placements
- **Versus Mode** — Local versus gameplay with deployment phase and full battle mechanics
- **Save System** — Save and load game progress across sessions
- **Audio & SFX** — Sound effects for movement, combat, and UI interactions
- **Keyboard Controls** — Full keyboard navigation and input support
- **AI Logic** — Computer-controlled opponent with strategic decision making

## Tech Stack

| Technology | Usage |
|---|---|
| Java (Swing) | Core UI framework, rendering, and game screens |
| JavaFX 21.0.2 | Media playback and enhanced graphics |
| Custom pixel art | Sprites, backgrounds, and UI elements |

## Project Structure

```
src/game/
├── Main.java                  # Application entry point (JFrame + CardLayout)
├── core/
│   ├── animation/             # Sprite and battle animations
│   ├── battle/                # Battle manager, terrain, and backgrounds
│   ├── effects/               # Visual effects system
│   ├── engine/                # AI logic, movement, and deployment engines
│   ├── input/                 # Keyboard controller
│   ├── map/                   # Tileset and tile entry system
│   ├── save/                  # Save/load functionality
│   ├── unit/                  # Unit stats, registry, weapons, and map units
│   └── util/                  # Game paths and utilities
└── ui/
    ├── BaseScreen.java        # Base class for all screens
    ├── Theme.java             # Visual theme and styling
    ├── components/            # Reusable UI components (styled buttons, etc.)
    ├── editors/               # Map designer and design room screens
    └── screens/               # Title, menu, settings, versus, and gameplay screens
```

## Prerequisites

- **Java JDK 17+**
- **JavaFX SDK 21.0.2** (included in `lib/`)

## Running the Game

### Eclipse
1. Import the project into Eclipse
2. Ensure JavaFX SDK is on the module path
3. Run `game.Main` with VM arguments:
   ```
   --module-path lib/javafx-sdk-21.0.2/lib --add-modules javafx.controls,javafx.media,javafx.swing,javafx.graphics
   ```

### Command Line
```bash
javac --module-path lib/javafx-sdk-21.0.2/lib --add-modules javafx.controls,javafx.media,javafx.swing,javafx.graphics -d bin src/game/**/*.java

java --module-path lib/javafx-sdk-21.0.2/lib --add-modules javafx.controls,javafx.media,javafx.swing,javafx.graphics -cp bin game.Main
```

## License

All rights reserved.
