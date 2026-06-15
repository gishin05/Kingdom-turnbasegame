package game.ui.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import game.Main;
import game.core.util.GamePaths;
import game.ui.BaseScreen;
import game.ui.Theme;

/**
 * UI DESIGN OVERVIEW:
 * This component is part of the pixelated game UI approach.
 * It integrates with the layered architecture, utilizing dynamic backgrounds
 * and semi-transparent panels to maintain a visually rich, modern aesthetic.
 */

/**
 * The initial landing screen of the game.
 * Uses a multi-layered approach:
 * - A background looping video (handled by BaseScreen)
 * - An animated blinking text overlay
 * - A centrally aligned game logo
 */
public class TitleScreen extends BaseScreen {

    private static final long serialVersionUID = 1L;
    private Image logoImage;
    private boolean showPressStart = true; // State for the blinking cursor/text effect

    public TitleScreen(Main main) {
        super(main);
        
        try {
            logoImage = new ImageIcon(getClass().getResource(GamePaths.bundledResource("graphics/ui/logo.png"))).getImage();
        } catch (Exception e) {
            System.err.println("Could not load logo asset");
        }

        initVideoBackground(GamePaths.bundledResource("graphics/backgrounds/Stable_bg.mp4"));

        // Use JLayeredPane to stack UI elements over the video background
        JLayeredPane layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Z-Index 0: Base Video background 
        layeredPane.add(jfxPanel, JLayeredPane.DEFAULT_LAYER);

        // Z-Index 1: Transparent UI Overlay panel holding the Logo and "Press Start" text.
        // Uses null layout to allow precise absolute positioning during window resizes.
        JPanel uiPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (logoImage != null) {
                    int imgWidth = logoImage.getWidth(this);
                    int imgHeight = logoImage.getHeight(this);
                    if (imgWidth > 0 && imgHeight > 0) {
                        int targetWidth = Math.min(400, getWidth() - 40);
                        double aspectRatio = (double) imgHeight / imgWidth;
                        int targetHeight = (int) (targetWidth * aspectRatio);
                        int x = (getWidth() - targetWidth) / 2;
                        int y = 30;
                        g.drawImage(logoImage, x, y, targetWidth, targetHeight, this);
                    }
                }
            }
        };
        uiPanel.setOpaque(false);
        layeredPane.add(uiPanel, JLayeredPane.PALETTE_LAYER);

        // Click to Start Label
        JLabel lblPressStart = new JLabel("Press Enter or Click to Start");
        lblPressStart.setForeground(Color.YELLOW);
        lblPressStart.setFont(Theme.getPixelFont(24f));
        lblPressStart.setHorizontalAlignment(SwingConstants.CENTER);
        uiPanel.add(lblPressStart);

        // Blinking effect
        Timer blinkTimer = new Timer(500, e -> {
            showPressStart = !showPressStart;
            lblPressStart.setVisible(showPressStart);
            repaint();
        });
        blinkTimer.start();

        // Global screen click listener: anywhere the user clicks transitions to the Main Menu
        uiPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                game.core.util.SoundManager.playButtonSound();
                main.showScreen(Main.MENU);
            }
        });

        // Handle resizing
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = getWidth();
                int h = getHeight();
                jfxPanel.setBounds(0, 0, w, h);
                uiPanel.setBounds(0, 0, w, h);
                lblPressStart.setBounds(0, h - 200, w, 100);
            }
        });
    }

    @Override protected boolean useGameLoop() { return true; }

    @Override public void update() {
        game.core.input.KeyboardController input = main.getKeyboardController();
        if (input == null) return;
        
        if (input.consumeEnter()) {
            game.core.util.SoundManager.playButtonSound();
            main.showScreen(Main.MENU);
        }
    }
}
