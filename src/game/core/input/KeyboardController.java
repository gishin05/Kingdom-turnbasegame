package game.core.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import game.core.save.SettingsSaveData;

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

    // Custom Key Bindings (defaults to standard layouts)
    public int upKey1 = KeyEvent.VK_UP;
    public int upKey2 = KeyEvent.VK_W;
    public int downKey1 = KeyEvent.VK_DOWN;
    public int downKey2 = KeyEvent.VK_S;
    public int leftKey1 = KeyEvent.VK_LEFT;
    public int leftKey2 = KeyEvent.VK_A;
    public int rightKey1 = KeyEvent.VK_RIGHT;
    public int rightKey2 = KeyEvent.VK_D;
    public int enterKey = KeyEvent.VK_ENTER;
    public int escKey = KeyEvent.VK_ESCAPE;

    public void loadFromSettings(SettingsSaveData settings) {
        this.upKey1 = settings.upKey1;
        this.upKey2 = settings.upKey2;
        this.downKey1 = settings.downKey1;
        this.downKey2 = settings.downKey2;
        this.leftKey1 = settings.leftKey1;
        this.leftKey2 = settings.leftKey2;
        this.rightKey1 = settings.rightKey1;
        this.rightKey2 = settings.rightKey2;
        this.enterKey = settings.enterKey;
        this.escKey = settings.escKey;
    }

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
        if (code == upKey1 || code == upKey2) {
            upPressed = true;
        }
        if (code == downKey1 || code == downKey2) {
            downPressed = true;
        }
        if (code == leftKey1 || code == leftKey2) {
            leftPressed = true;
        }
        if (code == rightKey1 || code == rightKey2) {
            rightPressed = true;
        }
        
        // ── Action Keys ──
        if (code == enterKey) {
            enterPressed = true;
        }
        if (code == escKey) {
            escPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == upKey1 || code == upKey2) {
            upPressed = false;
        }
        if (code == downKey1 || code == downKey2) {
            downPressed = false;
        }
        if (code == leftKey1 || code == leftKey2) {
            leftPressed = false;
        }
        if (code == rightKey1 || code == rightKey2) {
            rightPressed = false;
        }
        
        // When action keys are released, we reset their "consumed" state.
        // This ensures that holding down the key doesn't trigger the action repeatedly,
        // but pressing it a second time (after releasing) will trigger it again.
        if (code == enterKey) {
            enterPressed = false;
            enterConsumed = false;
        }
        if (code == escKey) {
            escPressed = false;
            escConsumed = false;
        }
    }
}
