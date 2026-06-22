package game.core.util;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

/**
 * Centralized manager for loading and caching game assets.
 * Reduces disk I/O and prevents redundant object creation.
 */
public class AssetManager {

    private static final Map<String, BufferedImage> imageCache = new HashMap<>();
    private static final Map<String, Font> fontCache = new HashMap<>();

    /**
     * Loads or retrieves an image from the cache.
     * @param path Absolute or project-relative file path (e.g. assets/bundled/graphics/ui/logo.png)
     * @return The BufferedImage, or null if loading fails.
     */
    public static BufferedImage getImage(String path) {
        if (imageCache.containsKey(path)) {
            return imageCache.get(path);
        }

        try {
            File file = new File(path);
            if (file.exists()) {
                BufferedImage img = ImageIO.read(file);
                imageCache.put(path, img);
                return img;
            }
        } catch (IOException e) {
            System.err.println("Error loading image: " + path);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Loads or retrieves a font from the cache.
     * @param path The path to the .ttf file.
     * @param size The font size.
     * @return The Font object.
     */
    public static Font getFont(String path, float size) {
        String key = path + "_" + size;
        if (fontCache.containsKey(key)) {
            return fontCache.get(key);
        }

        try {
            File file = new File(path);
            if (file.exists()) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(size);
                fontCache.put(key, font);
                return font;
            }
        } catch (Exception e) {
            System.err.println("Error loading font: " + path);
            e.printStackTrace();
        }
        return new Font("SansSerif", Font.PLAIN, (int) size);
    }

    /**
     * Preloads essential assets into the cache.
     * To be called during the StartupScreen.
     */
    public static void preloadEssentialAssets() {
        System.out.println("Preloading essential assets...");
        
        getImage(GamePaths.bundledFile("graphics/ui/logo.png").getPath());
        getFont(GamePaths.bundledFile("fonts/pixel_font.ttf").getPath(), 24);
        getFont(GamePaths.bundledFile("fonts/pixel_font.ttf").getPath(), 48);
        
        System.out.println("Preloading complete.");
    }

    /**
     * Clears the asset cache to free memory.
     */
    public static void clearCache() {
        imageCache.clear();
        fontCache.clear();
    }
}
