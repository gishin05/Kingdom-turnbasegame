package game.core.battle;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

public class BattleBackgroundEngine {
    
    private Map<String, BattleTerrain> loadedTerrains = new HashMap<>();
    
    // Fallback TSAs
    // In GBA, the screen is composed of 8x8 tiles. We're using 16x16 map tiles for the floor.
    // 248 width = ~16 tiles horizontally. 160 height = 10 tiles vertically.
    // We will place a 3-tile deep Battle Platform at the bottom (Y = 112 to 160)
    
    public BattleBackgroundEngine() {
        // Load default terrains based on user's directory
        loadedTerrains.put("Plain", new BattleTerrain("Plain", new Color(135, 206, 235))); // Light Blue Sky
        loadedTerrains.put("Forest", new BattleTerrain("Forest", new Color(20, 80, 40)));   // Darker Sky
        loadedTerrains.put("Mountain", new BattleTerrain("Mountain", new Color(180, 190, 200)));
        loadedTerrains.put("Desert", new BattleTerrain("Desert", new Color(255, 220, 130)));
        loadedTerrains.put("Castle", new BattleTerrain("Castle", new Color(40, 40, 50)));   // Dark interior
        loadedTerrains.put("Water", new BattleTerrain("Water", new Color(100, 150, 200)));
    }

    public BattleTerrain getTerrain(String name) {
        return loadedTerrains.getOrDefault(name, loadedTerrains.get("Plain"));
    }
    
    public String[] getAvailableTerrains() {
        return loadedTerrains.keySet().toArray(new String[0]);
    }

    /**
     * Renders the battle background into a 248x160 buffer
     */
    public void render(Graphics2D g, int shakeX, int shakeY, String terrainName) {
        BattleTerrain terrain = getTerrain(terrainName);
        
        // Apply Quake/Shake Offset
        g.translate(shakeX, shakeY);
        
        // 1. Draw Sky/Backdrop (Layer 3)
        g.setColor(terrain.skyColor);
        g.fillRect(-10, -10, 268, 180); // Overscan area to prevent black borders during shake
        
        // Draw Distant Horizon Line (Layer 2)
        g.setColor(terrain.skyColor.darker());
        g.fillRect(-10, 80, 268, 40);

        // 2. Battle Platform drawing removed at user request
        
        // Revert Quake Translation
        g.translate(-shakeX, -shakeY);
    }
}
