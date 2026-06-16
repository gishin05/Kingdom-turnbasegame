package game.core.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * KeyboardController manages global keyboard input for the game.
 * It tracks the state of movement keys (Arrows and WASD), selection (Enter),
 * and cancellation/back (Escape).
 * 
 * // [KEYBOARD_CONTROL_MARKER] - Core Input State Manager
 */
public class KeyboardController implements KeyListener {

    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean enterPressed, escPressed;

    private boolean enterConsumed = false;
    private boolean escConsumed = false;

    /**
     * Resets the state of all tracked keys to unpressed.
     * Useful when switching screens to prevent sticky keys.
     */
    public void reset() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
        enterPressed = false;
        escPressed = false;
        enterConsumed = false;
        escConsumed = false;
    }

    /**
     * Checks if Enter is pressed and hasn't been consumed yet.
     * If true, consumes the press and returns true.
     */
    public boolean consumeEnter() {
        if (enterPressed && !enterConsumed) {
            enterConsumed = true;
            return true;
        }
        return false;
    }

    /**
     * Checks if Esc is pressed and hasn't been consumed yet.
     * If true, consumes the press and returns true.
     */
    public boolean consumeEsc() {
        if (escPressed && !escConsumed) {
            escConsumed = true;
            return true;
        }
        return false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // ── Movement Keys ──
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
            upPressed = true;
        }
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
            downPressed = true;
        }
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
            leftPressed = true;
        }
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
            rightPressed = true;
        }
        
        // ── Action Keys ──
        if (code == KeyEvent.VK_ENTER) {
            enterPressed = true;
        }
        if (code == KeyEvent.VK_ESCAPE) {
            escPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
            upPressed = false;
        }
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
            downPressed = false;
        }
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
            leftPressed = false;
        }
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
            rightPressed = false;
        }
        
        // When action keys are released, we reset their "consumed" state.
        // This ensures that holding down the key doesn't trigger the action repeatedly,
        // but pressing it a second time (after releasing) will trigger it again.
        if (code == KeyEvent.VK_ENTER) {
            enterPressed = false;
            enterConsumed = false;
        }
        if (code == KeyEvent.VK_ESCAPE) {
            escPressed = false;
            escConsumed = false;
        }
    }
}
