package game.core.effects;

import game.core.unit.MapUnit;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Random;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

/**
 * Topmost layer (Layer 5) for visual effects like Fog, Rain, Snow, and Night.
 * Fog logic hides enemy units beyond a certain radius from player units.
 */
public class EffectsLayer {

    public enum EffectType { NONE, RAIN, FOG, SNOW, NIGHT }
    
    private EffectType currentEffect = EffectType.NONE;
    private int fogRadius = 4; // in tiles
    private Random rng = new Random();
    
    // Animation states
    private float rainOffset = 0;
    private List<Point> snowFlakes;

    public EffectsLayer() {
        // Initialize some random snowflakes for snow effect
        snowFlakes = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            snowFlakes.add(new Point(rng.nextInt(1000), rng.nextInt(1000)));
        }
    }

    public void setEffect(EffectType effect) {
        this.currentEffect = effect;
    }

    public EffectType getEffect() {
        return currentEffect;
    }

    /**
     * Renders the visual effects on top of the map.
     */
    public void render(Graphics2D g, int mapW, int mapH, int tileSize, float zoom, 
                       List<MapUnit> units, MapUnit selectedUnit) {
        
        if (currentEffect == EffectType.NONE) return;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (currentEffect) {
            case NONE:
                break;
            case FOG:
                drawFog(g, mapW, mapH, tileSize, units);
                break;
            case RAIN:
                drawRain(g, mapW, mapH, tileSize);
                break;
            case SNOW:
                drawSnow(g, mapW, mapH, tileSize);
                break;
            case NIGHT:
                drawNight(g, mapW, mapH, tileSize);
                break;
        }
    }

    private void drawFog(Graphics2D g, int mapW, int mapH, int tileSize, List<MapUnit> units) {
        // Create a black overlay for the whole map
        Area fogArea = new Area(new Rectangle(0, 0, mapW * tileSize, mapH * tileSize));
        
        // "Cut out" circles around player units
        for (MapUnit u : units) {
            if (u.faction == MapUnit.Faction.PLAYER) {
                int r = fogRadius * tileSize;
                int cx = u.position.x * tileSize + tileSize / 2;
                int cy = u.position.y * tileSize + tileSize / 2;
                
                Ellipse2D circle = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
                fogArea.subtract(new Area(circle));
            }
        }
        
        g.setColor(new Color(0, 0, 0, 180));
        g.fill(fogArea);
    }

    private void drawRain(Graphics2D g, int mapW, int mapH, int tileSize) {
        g.setColor(new Color(150, 150, 255, 80));
        g.setStroke(new BasicStroke(1.0f));
        
        rainOffset += 2.0f;
        if (rainOffset > 20) rainOffset = 0;
        
        for (int x = 0; x < mapW * tileSize; x += 15) {
            for (int y = (int)rainOffset - 20; y < mapH * tileSize; y += 20) {
                g.drawLine(x, y, x - 5, y + 10);
            }
        }
    }

    private void drawSnow(Graphics2D g, int mapW, int mapH, int tileSize) {
        g.setColor(new Color(255, 255, 255, 150));
        
        for (Point p : snowFlakes) {
            p.y += 1;
            p.x += (rng.nextBoolean() ? 1 : -1);
            
            if (p.y > mapH * tileSize) p.y = -5;
            if (p.x > mapW * tileSize) p.x = 0;
            if (p.x < 0) p.x = mapW * tileSize;
            
            g.fillOval(p.x, p.y, 2, 2);
        }
    }

    private void drawNight(Graphics2D g, int mapW, int mapH, int tileSize) {
        g.setColor(new Color(0, 0, 50, 100)); // Semi-transparent dark blue
        g.fillRect(0, 0, mapW * tileSize, mapH * tileSize);
    }

    /**
     * Helper for the MapUnitTestingScreen to check if a unit should be hidden by fog.
     */
    public boolean isVisible(MapUnit target, List<MapUnit> units) {
        if (currentEffect != EffectType.FOG || target.faction == MapUnit.Faction.PLAYER) return true;
        
        for (MapUnit u : units) {
            if (u.faction == MapUnit.Faction.PLAYER) {
                int dist = Math.abs(u.position.x - target.position.x) + Math.abs(u.position.y - target.position.y);
                if (dist <= fogRadius) return true;
            }
        }
        return false;
    }
}
