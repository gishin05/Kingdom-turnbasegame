package game.ui;

import java.awt.Color;
import java.awt.Font;

import game.core.util.AssetManager;
import game.core.util.GamePaths;

/**
 * UI DESIGN OVERVIEW:
 * This component is part of the pixelated game UI approach.
 * It integrates with the layered architecture, utilizing dynamic backgrounds
 * and semi-transparent panels to maintain a visually rich, modern aesthetic.
 */
/**
 * Centralized design system and theme configuration for the game UI.
 * Defines all standard colors, fonts, and typographic scales to ensure 
 * visual consistency across different screens.
 */
public class Theme {
    
    // --- Brand & UI Colors ---
    public static final Color GOLD = new Color(255, 215, 0);
    public static final Color GOLD_TRANS = new Color(255, 215, 0, 180);
    public static final Color DARK_BG = new Color(10, 10, 18);
    public static final Color DARKER_BG = new Color(5, 5, 10);
    public static final Color PANEL_BG = new Color(30, 30, 45);
    public static final Color TEXT_PRIMARY = new Color(220, 220, 240);
    public static final Color TEXT_SECONDARY = new Color(160, 160, 180);
    public static final Color HIGHLIGHT = new Color(255, 230, 100);
    
    // --- Fonts ---
    private static final String MAIN_FONT_PATH = GamePaths.bundledFile("fonts/PressStart2P.ttf").getPath();
    private static final String ACCENT_FONT_PATH = GamePaths.bundledFile("fonts/PressStart2P.ttf").getPath();
    
    // --- UI Sprites ---
    public static java.awt.image.BufferedImage MENU_BACKGROUND;
    public static java.awt.image.BufferedImage MENU_HAND;
    public static java.awt.image.BufferedImage BATTLE_INFO_BG;
    public static java.awt.image.BufferedImage HEALTH_BAR;
    public static java.awt.image.BufferedImage HEALTH_BAR_BG;
    public static java.awt.image.BufferedImage MAP_CURSOR;
    public static java.awt.image.BufferedImage MENU_BG_BASE;
    public static java.awt.image.BufferedImage MENU_GEM_SMALL;
    
    static {
        try {
            MENU_BACKGROUND = javax.imageio.ImageIO.read(GamePaths.uiFile("BaseMenuBackground.png"));
            MENU_BG_BASE = javax.imageio.ImageIO.read(GamePaths.bundledFile("graphics/ui/menu_bg_base.png"));
            MENU_GEM_SMALL = javax.imageio.ImageIO.read(GamePaths.bundledFile("graphics/ui/menu_gem_small.png"));
            MENU_HAND = javax.imageio.ImageIO.read(GamePaths.uiFile("menuHand.png"));
            BATTLE_INFO_BG = javax.imageio.ImageIO.read(GamePaths.uiFile("BattleInfo.png"));
            HEALTH_BAR = javax.imageio.ImageIO.read(GamePaths.uiFile("HealthBar.png"));
            HEALTH_BAR_BG = javax.imageio.ImageIO.read(GamePaths.uiFile("HealthBarBG.png"));
            MAP_CURSOR = javax.imageio.ImageIO.read(GamePaths.uiFile("Cursor.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Retrieves the primary pixel font dynamically sized.
     * @param size The font size in points
     */
    public static Font getPixelFont(float size) {
        return AssetManager.getFont(MAIN_FONT_PATH, size);
    }
    
    public static Font getAccentFont(float size) {
        return AssetManager.getFont(ACCENT_FONT_PATH, size);
    }
    
    // Standardized typography scale
    public static Font getTitleFont() { return getAccentFont(48f); }
    public static Font getMenuFont()  { return getPixelFont(26f); }
    public static Font getSmallFont() { return getPixelFont(13f); }
    
    /**
     * Draws a 9-sliced image.
     * Assuming the image is a 3x3 grid (e.g. 24x24 where corners are 8x8).
     */
    public static void draw9Slice(java.awt.Graphics g, java.awt.image.BufferedImage img, int x, int y, int width, int height) {
        if (img == null) return;
        int w = img.getWidth();
        int h = img.getHeight();
        int cw = w / 3;
        int ch = h / 3;
        
        // Draw corners
        g.drawImage(img, x, y, x+cw, y+ch, 0, 0, cw, ch, null); // top-left
        g.drawImage(img, x+width-cw, y, x+width, y+ch, w-cw, 0, w, ch, null); // top-right
        g.drawImage(img, x, y+height-ch, x+cw, y+height, 0, h-ch, cw, h, null); // bottom-left
        g.drawImage(img, x+width-cw, y+height-ch, x+width, y+height, w-cw, h-ch, w, h, null); // bottom-right
        
        // Draw edges
        g.drawImage(img, x+cw, y, x+width-cw, y+ch, cw, 0, w-cw, ch, null); // top
        g.drawImage(img, x+cw, y+height-ch, x+width-cw, y+height, cw, h-ch, w-cw, h, null); // bottom
        g.drawImage(img, x, y+ch, x+cw, y+height-ch, 0, ch, cw, h-ch, null); // left
        g.drawImage(img, x+width-cw, y+ch, x+width, y+height-ch, w-cw, ch, w, h-ch, null); // right
        
        // Draw center
        g.drawImage(img, x+cw, y+ch, x+width-cw, y+height-ch, cw, ch, w-cw, h-ch, null);
    }

    public static void draw9SliceScaled(java.awt.Graphics g, java.awt.image.BufferedImage img, int x, int y, int width, int height, float scale) {
        if (img == null) return;
        int w = img.getWidth();
        int h = img.getHeight();
        int cw = (int)((w / 3) * scale);
        int ch = (int)((h / 3) * scale);
        
        // Draw corners
        g.drawImage(img, x, y, x+cw, y+ch, 0, 0, w/3, h/3, null); // top-left
        g.drawImage(img, x+width-cw, y, x+width, y+ch, w - w/3, 0, w, h/3, null); // top-right
        g.drawImage(img, x, y+height-ch, x+cw, y+height, 0, h - h/3, w/3, h, null); // bottom-left
        g.drawImage(img, x+width-cw, y+height-ch, x+width, y+height, w - w/3, h - h/3, w, h, null); // bottom-right
        
        // Draw edges
        g.drawImage(img, x+cw, y, x+width-cw, y+ch, w/3, 0, w - w/3, h/3, null); // top
        g.drawImage(img, x+cw, y+height-ch, x+width-cw, y+height, w/3, h - h/3, w - w/3, h, null); // bottom
        g.drawImage(img, x, y+ch, x+cw, y+height-ch, 0, h/3, w/3, h - h/3, null); // left
        g.drawImage(img, x+width-cw, y+ch, x+width, y+height-ch, w - w/3, h/3, w, h - h/3, null); // right
        
        // Draw center
        g.drawImage(img, x+cw, y+ch, x+width-cw, y+height-ch, w/3, h/3, w - w/3, h - h/3, null);
    }
}
