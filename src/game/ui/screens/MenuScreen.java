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

public class MenuScreen extends BaseScreen {

    private static final long serialVersionUID = 1L;

    private static final String[] MENU_TITLES = {
        "CAMPAIGN", "WAR ROOM", "VERSUS", "DESIGN ROOM", "EXIT"
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

    public MenuScreen(Main main) {
        super(main);
        initVideoBackground(GamePaths.bundledResource("graphics/backgrounds/Menu_bg.mp4"));

        JLayeredPane layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Layer 0: Video background (BaseScreen)
        layeredPane.add(jfxPanel, JLayeredPane.DEFAULT_LAYER);

        // Layer 1: Full-screen dark overlay
        JPanel darkOverlay = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 100));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        darkOverlay.setOpaque(false);
        layeredPane.add(darkOverlay, JLayeredPane.PALETTE_LAYER);

        // Layer 2: Main UI
        JPanel mainUI = new JPanel(new BorderLayout());
        mainUI.setOpaque(false);
        layeredPane.add(mainUI, JLayeredPane.MODAL_LAYER);

        JPanel menuColumn = new JPanel();
        menuColumn.setLayout(new BoxLayout(menuColumn, BoxLayout.Y_AXIS));
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

        JPanel leftWrapper = new JPanel(new GridBagLayout());
        leftWrapper.setOpaque(false);
        leftWrapper.setPreferredSize(new Dimension(380, Integer.MAX_VALUE));
        leftWrapper.setBorder(new EmptyBorder(0, 40, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        leftWrapper.add(menuColumn, gbc);
        mainUI.add(leftWrapper, BorderLayout.WEST);

        glowTimer = new Timer(30, e -> {
            if (glowUp) { selectionGlow += 0.04f; if (selectionGlow >= 1f) { selectionGlow = 1f; glowUp = false; } }
            else        { selectionGlow -= 0.04f; if (selectionGlow <= 0f) { selectionGlow = 0f; glowUp = true; } }
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

    private JPanel createSeparator() {
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int mid = getHeight() / 2;
                g2.setPaint(new GradientPaint(0, mid, new Color(255, 215, 0, 0), getWidth() / 2, mid, Theme.GOLD_TRANS));
                g2.fillRect(0, mid - 1, getWidth() / 2, 2);
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
                if (index == 2 && versusPopup != null && versusPopup.isVisible()) {
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
            case 0: main.showScreen(Main.SAVE_SELECTION); break;
            case 1: break;
            case 2: showVersusPopup(); break;
            case 3: main.showScreen(Main.DESIGN_ROOM); break;
            case 4: System.exit(0); break;
        }
    }

    private void buildVersusPopup() {
        versusPopup = new JPopupMenu();
        versusPopup.setOpaque(false);
        versusPopup.setBackground(new Color(0, 0, 0, 0));
        versusPopup.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        versusPopup.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { clearVersusForcedHover(); }
            @Override public void popupMenuCanceled(PopupMenuEvent e) { clearVersusForcedHover(); }
        });

        JPanel continueItem = createPopupMenuItemPanel("CONTINUE", () -> {
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
        });

        JPanel newItem = createPopupMenuItemPanel("NEW", () -> {
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
        });

        versusPopupItems = new JPanel[] { continueItem, newItem };

        versusPopup.add(continueItem);
        versusPopup.add(Box.createVerticalStrut(6));
        versusPopup.add(newItem);
    }

    private void showVersusPopup() {
        if (versusPopup == null) buildVersusPopup();
        if (menuItems == null || menuItems.length <= 2 || menuItems[2] == null) return;
        JPanel versusItem = menuItems[2];
        forceVersusHover();
        versusPopup.show(versusItem, versusItem.getWidth(), 0);
    }

    private void forceVersusHover() {
        hoveredIndex = 2;
        if (menuLabels != null && menuLabels.length > 2 && menuLabels[2] != null) menuLabels[2].setForeground(Theme.HIGHLIGHT);
        if (menuArrows != null && menuArrows.length > 2 && menuArrows[2] != null) menuArrows[2].setVisible(true);
        if (menuItems != null && menuItems.length > 2 && menuItems[2] != null) menuItems[2].repaint();
    }

    private void clearVersusForcedHover() {
        if (hoveredIndex != 2) return;
        hoveredIndex = -1;
        if (menuLabels != null && menuLabels.length > 2 && menuLabels[2] != null) menuLabels[2].setForeground(new Color(190, 190, 210));
        if (menuArrows != null && menuArrows.length > 2 && menuArrows[2] != null) menuArrows[2].setVisible(false);
        if (menuItems != null && menuItems.length > 2 && menuItems[2] != null) menuItems[2].repaint();
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
}
