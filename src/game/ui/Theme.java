package game.ui;

import java.awt.Color;
import java.awt.Font;

import game.core.util.AssetManager;
import game.core.util.GamePaths;
public class Theme {
    
    // --- Colors ---
    public static final Color GOLD = new Color(255, 215, 0);
    public static final Color GOLD_TRANS = new Color(255, 215, 0, 180);
    public static final Color DARK_BG = new Color(10, 10, 18);
    public static final Color DARKER_BG = new Color(5, 5, 10);
    public static final Color PANEL_BG = new Color(30, 30, 45);
    public static final Color TEXT_PRIMARY = new Color(220, 220, 240);
    public static final Color TEXT_SECONDARY = new Color(160, 160, 180);
    public static final Color HIGHLIGHT = new Color(255, 230, 100);
    
    // --- Fonts ---
    private static final String MAIN_FONT_PATH = GamePaths.bundledFile("fonts/pixel_font.ttf").getPath();
    private static final String ACCENT_FONT_PATH = GamePaths.bundledFile("fonts/pixel_font.ttf").getPath();
    
    public static Font getPixelFont(float size) {
        return AssetManager.getFont(MAIN_FONT_PATH, size);
    }
    
    public static Font getAccentFont(float size) {
        return AssetManager.getFont(ACCENT_FONT_PATH, size);
    }
    
    public static Font getTitleFont() { return getAccentFont(48f); }
    public static Font getMenuFont() { return getPixelFont(26f); }
    public static Font getSmallFont() { return getPixelFont(13f); }
}
