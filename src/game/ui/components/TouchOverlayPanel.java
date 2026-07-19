package game.ui.components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import game.Main;
import game.core.input.KeyboardController;
import game.ui.Theme;

/**
 * On-screen touch controller overlay for touchscreen and Android devices.
 * Emulates key presses directly on KeyboardController when touch regions are interacted with.
 */
public class TouchOverlayPanel extends JPanel {

    private final Main main;
    
    // Virtual control bounds
    private int dpadX = 120;
    private int dpadY = 120; // distance from bottom/left
    private int dpadRadius = 60;
    
    private int btnAX = 100; // distance from right
    private int btnAY = 130; // distance from bottom
    private int btnARadius = 26;
    
    private int btnBX = 170; // distance from right
    private int btnBY = 80;  // distance from bottom
    private int btnBRadius = 22;

    // Pressed states for rendering
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean aPressed = false;
    private boolean bPressed = false;

    public TouchOverlayPanel(Main main) {
        this.main = main;
        setOpaque(false);
        setLayout(null);
        
        // Handle touch/mouse events
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouse(e.getPoint(), true);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                releaseAll();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouse(e.getPoint(), false);
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    @Override
    public boolean contains(int x, int y) {
        int w = getWidth();
        int h = getHeight();
        
        int dpCenterX = dpadX;
        int dpCenterY = h - dpadY;
        
        int aCenterX = w - btnAX;
        int aCenterY = h - btnAY;
        
        int bCenterX = w - btnBX;
        int bCenterY = h - btnBY;
        
        double distDpad = Math.hypot(x - dpCenterX, y - dpCenterY);
        double distA = Math.hypot(x - aCenterX, y - aCenterY);
        double distB = Math.hypot(x - bCenterX, y - bCenterY);
        
        return (distDpad <= dpadRadius) || (distA <= btnARadius) || (distB <= btnBRadius);
    }

    private void handleMouse(Point p, boolean isNewPress) {
        int w = getWidth();
        int h = getHeight();
        
        // Calculate center coordinates
        int dpCenterX = dpadX;
        int dpCenterY = h - dpadY;
        
        int aCenterX = w - btnAX;
        int aCenterY = h - btnAY;
        
        int bCenterX = w - btnBX;
        int bCenterY = h - btnBY;
        
        // Check D-Pad
        double distDpad = p.distance(dpCenterX, dpCenterY);
        boolean newUp = false, newDown = false, newLeft = false, newRight = false;
        if (distDpad <= dpadRadius) {
            int dx = p.x - dpCenterX;
            int dy = p.y - dpCenterY;
            if (Math.abs(dx) > 15 || Math.abs(dy) > 15) { // deadzone
                if (dy < -15) newUp = true;
                if (dy > 15) newDown = true;
                if (dx < -15) newLeft = true;
                if (dx > 15) newRight = true;
            }
        }
        
        // Update D-pad key presses
        KeyboardController kc = main.getKeyboardController();
        updateKeyState(upPressed, newUp, kc.upKey1);
        updateKeyState(downPressed, newDown, kc.downKey1);
        updateKeyState(leftPressed, newLeft, kc.leftKey1);
        updateKeyState(rightPressed, newRight, kc.rightKey1);
        
        upPressed = newUp;
        downPressed = newDown;
        leftPressed = newLeft;
        rightPressed = newRight;
        
        // Check Action A
        double distA = p.distance(aCenterX, aCenterY);
        boolean newA = (distA <= btnARadius);
        updateKeyState(aPressed, newA, kc.enterKey);
        aPressed = newA;
        
        // Check Action B
        double distB = p.distance(bCenterX, bCenterY);
        boolean newB = (distB <= btnBRadius);
        updateKeyState(bPressed, newB, kc.escKey);
        bPressed = newB;
        
        repaint();
    }
    
    private void updateKeyState(boolean oldState, boolean newState, int keyCode) {
        if (!oldState && newState) {
            triggerPress(keyCode);
        } else if (oldState && !newState) {
            triggerRelease(keyCode);
        }
    }
    
    private void releaseAll() {
        KeyboardController kc = main.getKeyboardController();
        if (upPressed) triggerRelease(kc.upKey1);
        if (downPressed) triggerRelease(kc.downKey1);
        if (leftPressed) triggerRelease(kc.leftKey1);
        if (rightPressed) triggerRelease(kc.rightKey1);
        if (aPressed) triggerRelease(kc.enterKey);
        if (bPressed) triggerRelease(kc.escKey);
        
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
        aPressed = false;
        bPressed = false;
        repaint();
    }
    
    private void triggerPress(int keyCode) {
        KeyEvent ke = new KeyEvent(main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
        main.getKeyboardController().keyPressed(ke);
    }
    
    private void triggerRelease(int keyCode) {
        KeyEvent ke = new KeyEvent(main, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
        main.getKeyboardController().keyReleased(ke);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = getWidth();
        int h = getHeight();
        
        // ── Render D-Pad ──
        int dpCenterX = dpadX;
        int dpCenterY = h - dpadY;
        
        // Outer D-pad circle
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        g2.setColor(new Color(15, 15, 25));
        g2.fillOval(dpCenterX - dpadRadius, dpCenterY - dpadRadius, dpadRadius * 2, dpadRadius * 2);
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        g2.setColor(new Color(255, 215, 0, 100)); // Gold border
        g2.drawOval(dpCenterX - dpadRadius, dpCenterY - dpadRadius, dpadRadius * 2, dpadRadius * 2);
        
        // D-pad Directions
        drawArrow(g2, dpCenterX, dpCenterY - 35, 16, 12, upPressed, 0);      // UP
        drawArrow(g2, dpCenterX, dpCenterY + 35, 16, 12, downPressed, 180);  // DOWN
        drawArrow(g2, dpCenterX - 35, dpCenterY, 16, 12, leftPressed, 270);  // LEFT
        drawArrow(g2, dpCenterX + 35, dpCenterY, 16, 12, rightPressed, 90);  // RIGHT
        
        // ── Render Button A ──
        int aCenterX = w - btnAX;
        int aCenterY = h - btnAY;
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, aPressed ? 0.6f : 0.3f));
        g2.setColor(aPressed ? new Color(255, 200, 50) : new Color(30, 30, 45));
        g2.fillOval(aCenterX - btnARadius, aCenterY - btnARadius, btnARadius * 2, btnARadius * 2);
        g2.setColor(new Color(255, 215, 0, 200));
        g2.drawOval(aCenterX - btnARadius, aCenterY - btnARadius, btnARadius * 2, btnARadius * 2);
        
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("A", aCenterX - fm.stringWidth("A") / 2, aCenterY + fm.getAscent() / 2 - 2);
        
        // ── Render Button B ──
        int bCenterX = w - btnBX;
        int bCenterY = h - btnBY;
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bPressed ? 0.6f : 0.3f));
        g2.setColor(bPressed ? new Color(220, 70, 70) : new Color(30, 30, 45));
        g2.fillOval(bCenterX - btnBRadius, bCenterY - btnBRadius, btnBRadius * 2, btnBRadius * 2);
        g2.setColor(new Color(255, 100, 100, 180));
        g2.drawOval(bCenterX - btnBRadius, bCenterY - btnBRadius, btnBRadius * 2, btnBRadius * 2);
        
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        fm = g2.getFontMetrics();
        g2.drawString("B", bCenterX - fm.stringWidth("B") / 2, bCenterY + fm.getAscent() / 2 - 2);
        
        g2.dispose();
    }
    
    private void drawArrow(Graphics2D g2, int cx, int cy, int sizeW, int sizeH, boolean pressed, double angleDegrees) {
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(cx, cy);
        g.rotate(Math.toRadians(angleDegrees));
        
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pressed ? 0.8f : 0.4f));
        g.setColor(pressed ? new Color(255, 220, 100) : new Color(180, 180, 200));
        
        // Draw arrow polygon pointing up
        int[] xPoints = { 0, -sizeW / 2, sizeW / 2 };
        int[] yPoints = { -sizeH / 2, sizeH / 2, sizeH / 2 };
        g.fillPolygon(xPoints, yPoints, 3);
        
        g.dispose();
    }
}
