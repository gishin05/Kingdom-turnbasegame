package game.core.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime palette-swap utility for team coloring.
 * Detects blue-hued pixels (the default "team color" in GBA-style sprites)
 * and shifts them to match the target player color while preserving
 * saturation and brightness.
 *
 * Results are cached to avoid recomputing every frame.
 */
public class SpriteColorer {

    // Cache: "srcHashCode_targetColorHex" -> recolored image
    private static final Map<String, BufferedImage> cache = new HashMap<>();

    // Blue hue range in degrees (HSB) — typical GBA team-color pixels (expanded to catch cyan highlights and indigo shadows)
    private static final float BLUE_HUE_MIN = 170f / 360f;  // ~170°
    private static final float BLUE_HUE_MAX = 265f / 360f;  // ~265°

    /**
     * Recolor a sprite frame: shift all blue-hued pixels to the target color's hue.
     * Non-blue pixels are left untouched.
     *
     * @param src         original sprite image
     * @param targetColor the player's team color
     * @return a new BufferedImage with team colors swapped
     */
    public static BufferedImage recolor(BufferedImage src, Color targetColor) {
        if (src == null) return null;

        float[] targetHSB = Color.RGBtoHSB(targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue(), null);
        boolean isTargetAlreadyBlue = (targetHSB[0] >= BLUE_HUE_MIN && targetHSB[0] <= BLUE_HUE_MAX);
        
        String key = System.identityHashCode(src) + "_" + Integer.toHexString(targetColor.getRGB());
        BufferedImage cached = cache.get(key);
        if (cached != null) return cached;

        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        float targetHue = targetHSB[0];
        
        int topLeft = src.getRGB(0, 0);
        int tAlpha = (topLeft >> 24) & 0xFF;
        int tR = (topLeft >> 16) & 0xFF;
        int tG = (topLeft >> 8) & 0xFF;
        int tB = topLeft & 0xFF;
        // Consider top-left pixel as background if it is significantly green or magenta
        boolean topLeftIsBackground = (tAlpha > 0 && ((tG > tR + 20 && tG > tB + 20) || (tR > 200 && tB > 200 && tG < 50)));

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;

                if (alpha == 0) {
                    result.setRGB(x, y, 0); // fully transparent
                    continue;
                }

                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                boolean isBackground = false;
                
                if (topLeftIsBackground && argb == topLeft) {
                    isBackground = true;
                } else if (r == 112 && g == 248 && b == 112) {
                    // Exact match for common FEBuilder green (#70F870)
                    isBackground = true;
                } else if (Math.abs(r - 168) < 10 && Math.abs(g - 208) < 10 && Math.abs(b - 160) < 10) {
                    // Match for older/duller green exports, allowing slight variance but not broad sweeping greens
                    isBackground = true;
                }

                if (isBackground) {
                    result.setRGB(x, y, 0);
                    continue;
                }

                float[] hsb = Color.RGBtoHSB(r, g, b, null);

                // Check if this pixel is in the "blue team color" range
                if (hsb[0] >= BLUE_HUE_MIN && hsb[0] <= BLUE_HUE_MAX && hsb[1] >= 0.08f) {
                    if (isTargetAlreadyBlue) {
                        // Original color is already blue; keep original artist's hand-drawn blue details!
                        result.setRGB(x, y, argb);
                    } else {
                        // Scale original saturation and brightness by target's levels
                        float newSat = hsb[1] * targetHSB[1];
                        float newBri = hsb[2] * targetHSB[2];
                        int newRGB = Color.HSBtoRGB(targetHue, newSat, newBri);
                        // Preserve original alpha
                        result.setRGB(x, y, (alpha << 24) | (newRGB & 0x00FFFFFF));
                    }
                } else {
                    result.setRGB(x, y, argb); // keep original
                }
            }
        }

        cache.put(key, result);
        return result;
    }

    /**
     * Clears the entire recolor cache. Call when switching screens or maps.
     */
    public static void clearCache() {
        cache.clear();
    }
}
