package game.core.animation;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

public class AnimationPreviewer extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private List<BufferedImage> frames;
    private int currentFrame = 0;
    private Timer timer;
    private int baseDelay = 50; // 50ms per unit of delay

    public AnimationPreviewer(int width, int height) {
        Dimension size = new Dimension(width, height);
        setPreferredSize(size);
        setMinimumSize(size);
        
        frames = new ArrayList<>();
        
        timer = new Timer(baseDelay * 4, e -> {
            if (frames != null && !frames.isEmpty()) {
                currentFrame = (currentFrame + 1) % frames.size();
                repaint();
            }
        });
        timer.start();
    }

    public void setFrames(List<BufferedImage> newFrames) {
        if (newFrames == null) {
            this.frames = new ArrayList<>();
        } else {
            this.frames = new ArrayList<>(newFrames);
        }
        
        if (frames.isEmpty()) {
            currentFrame = 0;
        } else if (currentFrame >= frames.size()) {
            currentFrame = 0;
        }
        repaint();
    }

    public void setFrameDelay(int delayMultiplier) {
        int delay = Math.max(1, delayMultiplier) * baseDelay;
        if (timer != null) {
            timer.setDelay(delay);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (frames != null && !frames.isEmpty() && currentFrame < frames.size()) {
            BufferedImage img = frames.get(currentFrame);
            if (img != null) {
                int x = (getWidth() - img.getWidth()) / 2;
                int y = (getHeight() - img.getHeight()) / 2;
                g.drawImage(img, x, y, this);
            }
        }
    }
}
