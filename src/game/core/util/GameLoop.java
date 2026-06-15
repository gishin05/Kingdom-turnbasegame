package game.core.util;

import javax.swing.SwingUtilities;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

/**
 * A dedicated game loop thread that separates game logic (updates) from rendering.
 * Uses a fixed time-step for updates and a variable time-step for rendering.
 */
public class GameLoop implements Runnable {

    private final Updateable target;
    private boolean running = false;
    private final int TICKS_PER_SECOND = 60;
    private final long SKIP_TICKS = 1000000000 / TICKS_PER_SECOND;
    private final int MAX_FRAMESKIP = 5;

    public interface Updateable {
        void update(); // Logic update
        void render(); // Trigger repaint
    }

    public GameLoop(Updateable target) {
        this.target = target;
    }

    public void start() {
        if (running) return;
        running = true;
        new Thread(this, "GameLoopThread").start();
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        long nextTick = System.nanoTime();
        int loops;

        while (running) {
            loops = 0;
            while (System.nanoTime() > nextTick && loops < MAX_FRAMESKIP) {
                // Perform logic update
                target.update();
                
                nextTick += SKIP_TICKS;
                loops++;
            }

            // Perform rendering (on EDT)
            SwingUtilities.invokeLater(() -> {
                target.render();
            });

            // Brief sleep to yield CPU
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
