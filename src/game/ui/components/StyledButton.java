package game.ui.components;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * UI DESIGN OVERVIEW:
 * This component is part of the pixelated game UI approach.
 * It integrates with the layered architecture, utilizing dynamic backgrounds
 * and semi-transparent panels to maintain a visually rich, modern aesthetic.
 */

public class StyledButton extends JButton {
    private static final long serialVersionUID = 1L;
    private float hoverAlpha = 0f;
    private Timer hoverTimer;

    /**
     * Creates a new styled button with custom text and font.
     * Initializes the button to have a transparent base and sets up hover animations.
     * 
     * @param text The text to display on the button
     * @param font The font used for the button's text
     */
    public StyledButton(String text, Font font) {
        super(text);
        setFont(font);
        setForeground(Color.WHITE);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setHorizontalTextPosition(SwingConstants.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                startHoverAnimation(true);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                startHoverAnimation(false);
            }
        });
    }

    /**
     * Manages the hover animation state by gradually adjusting the alpha value.
     * Creates a smooth fade-in/fade-out effect.
     * 
     * @param forward If true, the button is being hovered over (fade in). 
     *                If false, the mouse exited the button (fade out).
     */
    private void startHoverAnimation(boolean forward) {
        if (hoverTimer != null) hoverTimer.stop();
        hoverTimer = new Timer(20, e -> {
            if (forward) {
                hoverAlpha += 0.1f;
                if (hoverAlpha >= 1f) { hoverAlpha = 1f; hoverTimer.stop(); }
            } else {
                hoverAlpha -= 0.1f;
                if (hoverAlpha <= 0f) { hoverAlpha = 0f; hoverTimer.stop(); }
            }
            repaint();
        });
        hoverTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = getWidth();
        int h = getHeight();

        // Render an outer glow effect that becomes visible during mouse hover
        if (hoverAlpha > 0) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hoverAlpha * 0.3f));
            g2.setColor(new Color(255, 200, 50));
            g2.fillRoundRect(2, 2, w - 4, h - 4, 15, 15);
        }

        // Note: Button Base is intentionally left transparent.
        
        // Render the border and corner ornaments.
        // The border is always visible but becomes more opaque upon mouse hover.
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f + hoverAlpha * 0.4f));
        g2.setColor(hoverAlpha > 0 ? new Color(255, 215, 0) : Color.WHITE);
        int thickness = 2;
        int cornerSize = 12;
        
        // Draw the main rectangular border
        g2.setStroke(new java.awt.BasicStroke(thickness));
        g2.drawRoundRect(thickness/2, thickness/2, w - thickness, h - thickness, 5, 5);

        // Draw decorative brackets on all four corners for a stylized look
        g2.fillRect(0, 0, cornerSize, thickness * 2);
        g2.fillRect(0, 0, thickness * 2, cornerSize);
        g2.fillRect(w - cornerSize, 0, cornerSize, thickness * 2);
        g2.fillRect(w - thickness * 2, 0, thickness * 2, cornerSize);
        g2.fillRect(0, h - thickness * 2, cornerSize, thickness * 2);
        g2.fillRect(0, h - cornerSize, thickness * 2, cornerSize);
        g2.fillRect(w - cornerSize, h - thickness * 2, cornerSize, thickness * 2);
        g2.fillRect(w - thickness * 2, h - cornerSize, thickness * 2, cornerSize);

        // Render a subtle, non-antialiased drop shadow for the text to give a retro aesthetic
        g2.setColor(new Color(0, 0, 0, 200));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF); // Ensures crisp, pixelated text
        g2.drawString(getText(), (w - g2.getFontMetrics().stringWidth(getText())) / 2 + 2, (h + g2.getFontMetrics().getAscent()) / 2 - 2 + 2);

        g2.dispose();

        // Update the primary text color: bright cyan when hovered, plain white otherwise
        setForeground(hoverAlpha > 0 ? new Color(200, 255, 255) : Color.WHITE);
        super.paintComponent(g);
    }
}
