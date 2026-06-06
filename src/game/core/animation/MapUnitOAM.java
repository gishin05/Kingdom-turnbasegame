package game.core.animation;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Emulates GBA OAM rendering for map units.
 * Supports horizontal mirroring for efficient directional animations.
 */
public class MapUnitOAM {

    /**
     * Draws a unit frame with optional mirroring.
     */
    public static void drawUnit(Graphics2D g2, BufferedImage img, int x, int y, int scale, boolean mirror) {
        if (img == null) return;
        
        if (mirror) {
            AffineTransform old = g2.getTransform();
            // Flip around the center of the sprite
            g2.translate(x + img.getWidth() * scale, y);
            g2.scale(-1, 1);
            g2.drawImage(img, 0, 0, img.getWidth() * scale, img.getHeight() * scale, null);
            g2.setTransform(old);
        } else {
            g2.drawImage(img, x, y, img.getWidth() * scale, img.getHeight() * scale, null);
        }
    }
}
