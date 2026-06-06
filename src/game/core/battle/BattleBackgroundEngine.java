package game.core.battle;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

public class BattleBackgroundEngine {
    
    private Map<String, BattleTerrain> loadedTerrains = new HashMap<>();
    
    /**
     * Map of available battle background environments.
     * Used for rendering the backdrop during combat sequences.
     */
    
    /**
     * Initializes the background engine with a default set of primitive terrains.
     * Each terrain provides a distinct thematic color palette.
     */
    public BattleBackgroundEngine() {
        // Register default backdrop aesthetics based on typical environment biomes
        loadedTerrains.put("Plain", new BattleTerrain("Plain", new Color(135, 206, 235))); // Daylight sky
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
     * Renders the battle background backdrop onto the provided graphics context.
     * Fits typical retro GBA bounds (e.g., 240x160 canvas).
     * 
     * @param g           The Graphics2D context to draw upon
     * @param shakeX      Horizontal offset for screen shake effects
     * @param shakeY      Vertical offset for screen shake effects
     * @param terrainName The name of the terrain to look up and render
     */
    public void render(Graphics2D g, int shakeX, int shakeY, String terrainName) {
        BattleTerrain terrain = getTerrain(terrainName);
        
        // Apply global screen shake translation
        g.translate(shakeX, shakeY);
        
        // Layer 1: Distant Sky/Backdrop
        g.setColor(terrain.skyColor);
        // We draw slightly larger than the screen (overscanning) to ensure black borders
        // don't bleed into view when the screen shake offset is applied.
        g.fillRect(-10, -10, 268, 180); 
        
        // Layer 2: Distant Horizon / Mountains silhouette
        // Rendered as a darker band across the horizon for depth.
        g.setColor(terrain.skyColor.darker());
        g.fillRect(-10, 80, 268, 40);

        // Note: The foreground battle platform (Layer 3) was intentionally removed.
        // It is handled by separate map tile logic or omitted for a cleaner look.
        
        // Revert the shake translation so subsequent rendering operations are unaffected
        g.translate(-shakeX, -shakeY);
    }
}
