package game.core.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

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
        
        // Bulk read all pixels at once — much faster than per-pixel getRGB(x,y)
        int[] srcPixels = src.getRGB(0, 0, w, h, null, 0, w);
        int[] dstPixels = new int[srcPixels.length];
        
        int topLeft = srcPixels[0];
        int tAlpha = (topLeft >> 24) & 0xFF;
        int tR = (topLeft >> 16) & 0xFF;
        int tG = (topLeft >> 8) & 0xFF;
        int tB = topLeft & 0xFF;
        // Consider top-left pixel as background if it is significantly green or magenta
        boolean topLeftIsBackground = (tAlpha > 0 && ((tG > tR + 20 && tG > tB + 20) || (tR > 200 && tB > 200 && tG < 50)));

        float[] hsb = new float[3]; // Reuse array to avoid per-pixel allocation
        
        for (int i = 0; i < srcPixels.length; i++) {
            int argb = srcPixels[i];
            int alpha = (argb >> 24) & 0xFF;

            if (alpha == 0) {
                dstPixels[i] = 0; // fully transparent
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
                dstPixels[i] = 0;
                continue;
            }

            Color.RGBtoHSB(r, g, b, hsb);

            // Check if this pixel is in the "blue team color" range
            if (hsb[0] >= BLUE_HUE_MIN && hsb[0] <= BLUE_HUE_MAX && hsb[1] >= 0.08f) {
                if (isTargetAlreadyBlue) {
                    // Original color is already blue; keep original artist's hand-drawn blue details!
                    dstPixels[i] = argb;
                } else {
                    // Scale original saturation and brightness by target's levels
                    float newSat = hsb[1] * targetHSB[1];
                    float newBri = hsb[2] * targetHSB[2];
                    int newRGB = Color.HSBtoRGB(targetHue, newSat, newBri);
                    // Preserve original alpha
                    dstPixels[i] = (alpha << 24) | (newRGB & 0x00FFFFFF);
                }
            } else {
                dstPixels[i] = argb; // keep original
            }
        }
        
        // Bulk write all pixels at once
        result.setRGB(0, 0, w, h, dstPixels, 0, w);

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
