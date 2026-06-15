package game.core.battle;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

public class BattleTerrain {
    public String name;
    public List<BufferedImage> tiles;
    public BufferedImage fullPlatform;
    public Color skyColor;

    public BattleTerrain(String name, Color skyColor) {
        this.name = name;
        this.skyColor = skyColor;
        this.tiles = new ArrayList<>();
        loadTiles();
    }

    private void loadTiles() {
        File dir = game.core.util.GamePaths.battleTerrain(name);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, fName) -> fName.toLowerCase().endsWith(".png"));
            if (files != null) {
                // Ensure alphabetical order: map first (Index 0), then platforms
                java.util.Arrays.sort(files);
                for (File file : files) {
                    try {
                        BufferedImage img = ImageIO.read(file);
                        if (img != null) {
                            if (img.getWidth() >= 120) {
                                fullPlatform = img;
                            } else {
                                tiles.add(img);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        // Fallback mock tile if folder empty
        if (tiles.isEmpty()) {
            BufferedImage mock = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = mock.createGraphics();
            g.setColor(new Color(60, 180, 60)); // Default Green
            g.fillRect(0, 0, 16, 16);
            g.setColor(new Color(40, 140, 40));
            g.drawRect(0, 0, 15, 15);
            g.dispose();
            tiles.add(mock);
        }
    }
}
