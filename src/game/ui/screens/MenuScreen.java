package game.ui.screens;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import game.Main;
import game.core.util.SoundManager;
import game.core.util.GamePaths;
import game.ui.BaseScreen;
import game.ui.Theme;
import game.core.save.SaveManager;

/**
 * UI DESIGN OVERVIEW:
 * This component is part of the pixelated game UI approach.
 * It integrates with the layered architecture, utilizing dynamic backgrounds
 * and semi-transparent panels to maintain a visually rich, modern aesthetic.
 */

/**
 * The primary navigation menu.
 * Features a dark translucent overlay over a video background,
 * and a left-aligned vertical menu column with animated hover states and sub-menus.
 */
public class MenuScreen extends BaseScreen {

    private static final long serialVersionUID = 1L;

    private static final String[] MENU_TITLES = {
        "VERSUS", "DESIGN ROOM", "SETTINGS", "EXIT"
    };

    private int hoveredIndex = -1;
    private float selectionGlow = 0f;
    private Timer glowTimer;
    private boolean glowUp = true;
    private JPanel[] menuItems;
    private JLabel[] menuLabels;
    private JLabel[] menuArrows;
    private JPopupMenu versusPopup;
    private JPanel[] versusPopupItems;
    private Runnable[] versusPopupActions;
    private int popupSelectedIndex = -1;
    private int keyCooldown = 0;

    public MenuScreen(Main main) {
        super(main);
        initVideoBackground(GamePaths.bundledResource("graphics/backgrounds/Menu_bg.mp4"));

        JLayeredPane layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Layer 0: Video background (BaseScreen)
        layeredPane.add(jfxPanel, JLayeredPane.DEFAULT_LAYER);

        // Layer 1: Full-screen dark translucent overlay.
        // This dims the background video to ensure the menu text remains readable.
        JPanel darkOverlay = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 100)); // Black with ~40% opacity
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        darkOverlay.setOpaque(false);
        layeredPane.add(darkOverlay, JLayeredPane.PALETTE_LAYER);

        // Layer 2: Main Interactive UI
        JPanel mainUI = new JPanel(new BorderLayout());
        mainUI.setOpaque(false);
        layeredPane.add(mainUI, JLayeredPane.MODAL_LAYER);

        // Left column housing the vertical list of menu buttons
        JPanel menuColumn = new JPanel();
        menuColumn.setLayout(new BoxLayout(menuColumn, BoxLayout.Y_AXIS)); // Stack elements vertically
        menuColumn.setOpaque(false);

        menuColumn.add(createSeparator());
        menuColumn.add(Box.createVerticalStrut(24));

        menuItems = new JPanel[MENU_TITLES.length];
        menuLabels = new JLabel[MENU_TITLES.length];
        menuArrows = new JLabel[MENU_TITLES.length];
        for (int i = 0; i < MENU_TITLES.length; i++) {
            menuItems[i] = createMenuItemPanel(MENU_TITLES[i], i);
            menuColumn.add(menuItems[i]);
            menuColumn.add(Box.createVerticalStrut(6));
        }

        buildVersusPopup();

        menuColumn.add(Box.createVerticalStrut(12));
        menuColumn.add(createSeparator());

        // Wrapper panel to vertically center the menu column on the left side of the screen.
        // Uses GridBagLayout because it automatically centers its contents by default.
        JPanel leftWrapper = new JPanel(new GridBagLayout());
        leftWrapper.setOpaque(false);
        leftWrapper.setPreferredSize(new Dimension(380, Integer.MAX_VALUE));
        leftWrapper.setBorder(new EmptyBorder(0, 40, 0, 0)); // 40px left padding from the screen edge

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST; // Align to the left inside the centered block
        leftWrapper.add(menuColumn, gbc);
        mainUI.add(leftWrapper, BorderLayout.WEST);

        // Animation loop for the pulsating glow effect on hovered menu items.
        // Oscillates a float between 0.0 and 1.0 continuously.
        glowTimer = new Timer(30, e -> {
            if (glowUp) { selectionGlow += 0.04f; if (selectionGlow >= 1f) { selectionGlow = 1f; glowUp = false; } }
            else        { selectionGlow -= 0.04f; if (selectionGlow <= 0f) { selectionGlow = 0f; glowUp = true; } }
            
            // Force repaint on all menu items to render the new glow frame
            for (JPanel p : menuItems) p.repaint();
            if (versusPopup != null && versusPopup.isVisible() && versusPopupItems != null) {
                for (JPanel p : versusPopupItems) if (p != null) p.repaint();
            }
        });
        glowTimer.start();

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int w = getWidth(), h = getHeight();
                jfxPanel.setBounds(0, 0, w, h);
                darkOverlay.setBounds(0, 0, w, h);
                mainUI.setBounds(0, 0, w, h);
            }
        });
    }

    /**
     * Creates a decorative horizontal separator line with a fading gradient.
     * Starts transparent, becomes gold in the center, and fades to transparent on the right.
     */
    private JPanel createSeparator() {
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int mid = getHeight() / 2;
                
                // Left half: Transparent -> Solid
                g2.setPaint(new GradientPaint(0, mid, new Color(255, 215, 0, 0), getWidth() / 2, mid, Theme.GOLD_TRANS));
                g2.fillRect(0, mid - 1, getWidth() / 2, 2);
                
                // Right half: Solid -> Transparent
                g2.setPaint(new GradientPaint(getWidth() / 2, mid, Theme.GOLD_TRANS, getWidth(), mid, new Color(255, 215, 0, 0)));
                g2.fillRect(getWidth() / 2, mid - 1, getWidth() / 2, 2);
                g2.dispose();
            }
        };
        sep.setOpaque(false);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(300, 12));
        return sep;
    }

    private JPanel createMenuItemPanel(String title, int index) {
        JPanel item = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (index == hoveredIndex) {
                    float glow = selectionGlow;
                    g2.setColor(new Color(255, 215, 0, (int)(15 + glow * 25)));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(new Color(255, 215, 0, (int)(200 + glow * 55)));
                    g2.fillRoundRect(0, 4, 4, getHeight() - 8, 4, 4);
                    g2.setColor(new Color(255, 215, 0, (int)(80 + glow * 80)));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                }
                g2.dispose();
            }
        };
        item.setOpaque(false);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setMaximumSize(new Dimension(300, 52));
        item.setPreferredSize(new Dimension(300, 52));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel arrow = new JLabel("▶") {{
            setFont(Theme.getSmallFont());
            setForeground(Theme.GOLD);
            setBorder(new EmptyBorder(0, 14, 0, 8));
            setVisible(false);
        }};
        JLabel lbl = new JLabel(title) {{
            setFont(Theme.getMenuFont());
            setForeground(new Color(190, 190, 210));
        }};

        if (index >= 0 && index < MENU_TITLES.length) {
            menuLabels[index] = lbl;
            menuArrows[index] = arrow;
        }

        item.add(arrow, BorderLayout.WEST);
        item.add(lbl, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                SoundManager.playButtonSound();
                handleMenuAction(index);
            }
            @Override public void mouseEntered(MouseEvent e) {
                SoundManager.playCursor();
                hoveredIndex = index;
                lbl.setForeground(Theme.HIGHLIGHT);
                arrow.setVisible(true);
                item.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                if (index == 0 && versusPopup != null && versusPopup.isVisible()) {
                    // Keep Versus highlighted while its extension popup is open.
                    return;
                }
                if (hoveredIndex == index) {
                    hoveredIndex = -1;
                    lbl.setForeground(new Color(190, 190, 210));
                    arrow.setVisible(false);
                    item.repaint();
                }
            }
        });
        return item;
    }

    private void handleMenuAction(int index) {
        switch (index) {
            case 0: showVersusPopup(); break;
            case 1: main.showScreen(Main.DESIGN_ROOM); break;
            case 2: main.showScreen(Main.SETTINGS); break;
            case 3: System.exit(0); break;
        }
    }

    private void buildVersusPopup() {
        versusPopup = new JPopupMenu();
        versusPopup.setOpaque(false);
        versusPopup.setBackground(new Color(0, 0, 0, 0));
        versusPopup.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        versusPopup.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { clearVersusForcedHover(); popupSelectedIndex = -1; }
            @Override public void popupMenuCanceled(PopupMenuEvent e) { clearVersusForcedHover(); popupSelectedIndex = -1; }
        });

        Runnable continueAction = () -> {
            if (!SaveManager.hasVersusSave()) {
                JOptionPane.showMessageDialog(this, "No saved Versus game found.");
                return;
            }
            try {
                game.ui.screens.VersusGameplayScreen gameplay = null;
                for (java.awt.Component comp : main.getContentPane().getComponents()) {
                    if (comp instanceof game.ui.screens.VersusGameplayScreen) {
                        gameplay = (game.ui.screens.VersusGameplayScreen) comp;
                        break;
                    }
                }
                if (gameplay == null) {
                    JOptionPane.showMessageDialog(this, "Error: VersusGameplayScreen not found!");
                    return;
                }
                gameplay.loadSavedGame(SaveManager.loadVersus());
                main.showScreen(Main.VERSUS_GAMEPLAY);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to load Versus save.");
                ex.printStackTrace();
            }
        };
        JPanel continueItem = createPopupMenuItemPanel("CONTINUE", continueAction);

        Runnable newAction = () -> {
            if (SaveManager.hasVersusSave()) {
                int choice = JOptionPane.showConfirmDialog(
                    this,
                    "This will overwrite the last saved Versus game. Continue?",
                    "Confirm New Versus Game",
                    JOptionPane.YES_NO_OPTION
                );
                if (choice != JOptionPane.YES_OPTION) return;
                SaveManager.deleteVersusSave();
            }
            main.showScreen(Main.VERSUS);
        };
        JPanel newItem = createPopupMenuItemPanel("NEW", newAction);

        versusPopupItems = new JPanel[] { continueItem, newItem };
        versusPopupActions = new Runnable[] { continueAction, newAction };

        versusPopup.add(continueItem);
        versusPopup.add(Box.createVerticalStrut(6));
        versusPopup.add(newItem);
    }

    private void showVersusPopup() {
        if (versusPopup == null) buildVersusPopup();
        if (menuItems == null || menuItems.length <= 0 || menuItems[0] == null) return;
        JPanel versusItem = menuItems[0];
        forceVersusHover();
        versusPopup.show(versusItem, versusItem.getWidth(), 0);
    }

    private void forceVersusHover() {
        hoveredIndex = 0;
        if (menuLabels != null && menuLabels.length > 0 && menuLabels[0] != null) menuLabels[0].setForeground(Theme.HIGHLIGHT);
        if (menuArrows != null && menuArrows.length > 0 && menuArrows[0] != null) menuArrows[0].setVisible(true);
        if (menuItems != null && menuItems.length > 0 && menuItems[0] != null) menuItems[0].repaint();
    }

    private void clearVersusForcedHover() {
        if (hoveredIndex != 0) return;
        hoveredIndex = -1;
        if (menuLabels != null && menuLabels.length > 0 && menuLabels[0] != null) menuLabels[0].setForeground(new Color(190, 190, 210));
        if (menuArrows != null && menuArrows.length > 0 && menuArrows[0] != null) menuArrows[0].setVisible(false);
        if (menuItems != null && menuItems.length > 0 && menuItems[0] != null) menuItems[0].repaint();
    }

    private JPanel createPopupMenuItemPanel(String title, Runnable onClick) {
        JPanel item = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getClientProperty("hover") == Boolean.TRUE) {
                    float glow = selectionGlow;
                    g2.setColor(new Color(255, 215, 0, (int)(15 + glow * 25)));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(new Color(255, 215, 0, (int)(200 + glow * 55)));
                    g2.fillRoundRect(0, 4, 4, getHeight() - 8, 4, 4);
                    g2.setColor(new Color(255, 215, 0, (int)(80 + glow * 80)));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                }
                g2.dispose();
            }
        };
        item.setOpaque(false);
        item.setMaximumSize(new Dimension(220, 22));
        item.setPreferredSize(new Dimension(220, 22));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel arrow = new JLabel("▶") {{
            setFont(Theme.getSmallFont());
            setForeground(Theme.GOLD);
            setBorder(new EmptyBorder(0, 14, 0, 8));
            setVisible(false);
        }};
        JLabel lbl = new JLabel(title) {{
            setFont(Theme.getMenuFont().deriveFont(18f));
            setForeground(new Color(190, 190, 210));
        }};

        item.add(arrow, BorderLayout.WEST);
        item.add(lbl, BorderLayout.CENTER);

        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                SoundManager.playButtonSound();
                if (onClick != null) onClick.run();
                if (versusPopup != null) versusPopup.setVisible(false);
            }
            @Override public void mouseEntered(MouseEvent e) {
                SoundManager.playCursor();
                item.putClientProperty("hover", Boolean.TRUE);
                lbl.setForeground(Theme.HIGHLIGHT);
                arrow.setVisible(true);
                item.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                item.putClientProperty("hover", Boolean.FALSE);
                lbl.setForeground(new Color(190, 190, 210));
                arrow.setVisible(false);
                item.repaint();
            }
        };
        item.addMouseListener(hover);
        arrow.addMouseListener(hover);
        lbl.addMouseListener(hover);
        return item;
    }

    private void setMenuHover(int index) {
        for (int i = 0; i < menuItems.length; i++) {
            if (i == index) {
                menuLabels[i].setForeground(Theme.HIGHLIGHT);
                menuArrows[i].setVisible(true);
            } else {
                menuLabels[i].setForeground(new Color(190, 190, 210));
                menuArrows[i].setVisible(false);
            }
            menuItems[i].repaint();
        }
    }

    private void setPopupHover(int index) {
        for (int i = 0; i < versusPopupItems.length; i++) {
            JPanel p = versusPopupItems[i];
            JLabel arrow = (JLabel) p.getComponent(0);
            JLabel lbl = (JLabel) p.getComponent(1);
            if (i == index) {
                p.putClientProperty("hover", Boolean.TRUE);
                lbl.setForeground(Theme.HIGHLIGHT);
                arrow.setVisible(true);
            } else {
                p.putClientProperty("hover", Boolean.FALSE);
                lbl.setForeground(new Color(190, 190, 210));
                arrow.setVisible(false);
            }
            p.repaint();
        }
    }

    @Override protected boolean useGameLoop() { return true; }

    @Override public void update() {
        game.core.input.KeyboardController input = main.getKeyboardController();
        if (input == null) return;

        if (keyCooldown > 0) keyCooldown--;

        if (versusPopup != null && versusPopup.isVisible()) {
            if (popupSelectedIndex == -1) {
                popupSelectedIndex = 0;
                setPopupHover(0);
            }
            if (keyCooldown == 0) {
                if (input.upPressed) {
                    popupSelectedIndex = (popupSelectedIndex - 1 + versusPopupItems.length) % versusPopupItems.length;
                    setPopupHover(popupSelectedIndex);
                    game.core.util.SoundManager.playCursor();
                    keyCooldown = 8;
                } else if (input.downPressed) {
                    popupSelectedIndex = (popupSelectedIndex + 1) % versusPopupItems.length;
                    setPopupHover(popupSelectedIndex);
                    game.core.util.SoundManager.playCursor();
                    keyCooldown = 8;
                }
            }
            if (input.consumeEnter()) {
                game.core.util.SoundManager.playButtonSound();
                versusPopupActions[popupSelectedIndex].run();
                versusPopup.setVisible(false);
            }
            if (input.consumeEsc()) {
                versusPopup.setVisible(false);
                clearVersusForcedHover();
            }
        } else {
            if (hoveredIndex == -1 && menuItems != null && menuItems.length > 0) {
                hoveredIndex = 0;
                setMenuHover(0);
            }
            if (keyCooldown == 0) {
                if (input.upPressed) {
                    hoveredIndex = (hoveredIndex - 1 + menuItems.length) % menuItems.length;
                    setMenuHover(hoveredIndex);
                    game.core.util.SoundManager.playCursor();
                    keyCooldown = 8;
                } else if (input.downPressed) {
                    hoveredIndex = (hoveredIndex + 1) % menuItems.length;
                    setMenuHover(hoveredIndex);
                    game.core.util.SoundManager.playCursor();
                    keyCooldown = 8;
                }
            }
            if (input.consumeEnter()) {
                game.core.util.SoundManager.playButtonSound();
                handleMenuAction(hoveredIndex);
            }
        }
    }
}
