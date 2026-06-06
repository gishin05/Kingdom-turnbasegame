package game.ui.editors;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import game.Main;
import game.ui.BaseScreen;
import game.ui.Theme;

/**
 * Hub screen for accessing the various creative editors in the game.
 * Currently serves as a portal to the MapDesignScreen, utilizing an animated 
 * card-based layout to showcase available editors.
 */
public class DesignRoomScreen extends BaseScreen {

    private static final long serialVersionUID = 1L;

    private static final String[] TITLES = { "MAP DESIGN" };

    private int selectedIndex = 0;
    private float selectionGlow = 0f;
    private boolean glowUp = true;
    private Timer glowTimer;
    private JPanel[] menuItems;
    
    private java.awt.Image mapDesignImg;

    public DesignRoomScreen(Main main) {
        super(main);
        
        try {
            mapDesignImg = javax.imageio.ImageIO.read(getClass().getResource(game.core.util.GamePaths.bundledResource("graphics/ui/map_design_card.png")));
        } catch (Exception e) {
            System.err.println("Failed to load design card images.");
        }

        initVideoBackground(game.core.util.GamePaths.bundledResource("graphics/backgrounds/Menu_bg.mp4"));

        JLayeredPane layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Layer 0 – video background (BaseScreen)
        layeredPane.add(jfxPanel, JLayeredPane.DEFAULT_LAYER);

        // Layer 1 – dark overlay
        JPanel darkOverlay = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 140));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        darkOverlay.setOpaque(false);
        layeredPane.add(darkOverlay, JLayeredPane.PALETTE_LAYER);

        // Layer 2 – main UI
        JPanel mainUI = new JPanel(new GridBagLayout());
        mainUI.setOpaque(false);
        layeredPane.add(mainUI, JLayeredPane.MODAL_LAYER);

        JPanel centerWrapper = new JPanel();
        centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.Y_AXIS));
        centerWrapper.setOpaque(false);

        JLabel screenTitle = new JLabel("DESIGN ROOM");
        screenTitle.setFont(Theme.getTitleFont());
        screenTitle.setForeground(Theme.GOLD);
        screenTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Create & Customize Your World");
        subtitle.setFont(Theme.getSmallFont());
        subtitle.setForeground(new Color(180, 180, 200));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerWrapper.add(screenTitle);
        centerWrapper.add(Box.createVerticalStrut(6));
        centerWrapper.add(subtitle);
        centerWrapper.add(Box.createVerticalStrut(28));
        centerWrapper.add(makeSeparator(500));
        centerWrapper.add(Box.createVerticalStrut(32));

        JPanel cardsRow = new JPanel(new GridLayout(1, TITLES.length, 40, 0));
        cardsRow.setOpaque(false);

        menuItems = new JPanel[TITLES.length];
        for (int i = 0; i < TITLES.length; i++) {
            menuItems[i] = makeCard(TITLES[i], i);
            cardsRow.add(menuItems[i]);
        }
        centerWrapper.add(cardsRow);

        mainUI.add(centerWrapper, new GridBagConstraints());

        // Back icon button
        JPanel backBtn = createBackButton();
        JPanel backLayer = new JPanel(null);
        backLayer.setOpaque(false);
        backLayer.add(backBtn);
        layeredPane.add(backLayer, JLayeredPane.POPUP_LAYER);

        glowTimer = new Timer(30, e -> {
            if (glowUp) { selectionGlow += 0.04f; if (selectionGlow >= 1f) { selectionGlow = 1f; glowUp = false; } }
            else        { selectionGlow -= 0.04f; if (selectionGlow <= 0f) { selectionGlow = 0f; glowUp = true; } }
            if (menuItems != null) for (JPanel p : menuItems) p.repaint();
        });
        glowTimer.start();

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int w = getWidth(), h = getHeight();
                jfxPanel.setBounds(0, 0, w, h);
                darkOverlay.setBounds(0, 0, w, h);
                mainUI.setBounds(0, 0, w, h);
                backLayer.setBounds(0, 0, w, h);
                backBtn.setBounds(20, 20, 44, 44);
            }
        });

        selectIndex(0);
    }

    private JPanel createBackButton() {
        return new JPanel() {
            private float hover = 0f;
            private Timer t;
            {
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { main.showScreen(Main.MENU); }
                    @Override public void mouseEntered(MouseEvent e) { animHover(true); }
                    @Override public void mouseExited(MouseEvent e)  { animHover(false); }
                });
            }
            private void animHover(boolean in) {
                if (t != null) t.stop();
                t = new Timer(16, e -> {
                    hover += in ? 0.12f : -0.12f;
                    hover = Math.max(0f, Math.min(1f, hover));
                    repaint();
                    if (hover == 0f || hover == 1f) ((Timer)e.getSource()).stop();
                });
                t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(0, 0, 0, (int)(120 + hover * 80)));
                g2.fillOval(0, 0, w, h);
                g2.setColor(new Color(255, 215, 0, (int)(120 + hover * 135)));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(1, 1, w - 2, h - 2);
                int cx = w / 2, cy = h / 2;
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx + 6, cy - 8, cx - 4, cy);
                g2.drawLine(cx - 4, cy, cx + 6, cy + 8);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(44, 44); }
        };
    }

    private JPanel makeCard(String title, int index) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean sel = (index == selectedIndex);
                java.awt.geom.RoundRectangle2D clipArea = new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setClip(clipArea);
                java.awt.Image img = mapDesignImg;
                if (img != null) g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                else { g2.setColor(new Color(10, 10, 20)); g2.fill(clipArea); }
                if (sel) {
                    g2.setPaint(new GradientPaint(0, 0, new Color(80, 50, 0, 120), 0, getHeight(), new Color(20, 15, 0, 100)));
                    g2.fill(clipArea);
                } else {
                    g2.setColor(new Color(0, 0, 0, 140));
                    g2.fill(clipArea);
                }
                g2.setClip(null);
                int alpha = sel ? (int)(140 + selectionGlow * 115) : 60;
                g2.setColor(new Color(255, 215, 0, alpha));
                g2.setStroke(new BasicStroke(sel ? 2.5f : 1.5f));
                g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(340, 420));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(Theme.getMenuFont());
        lbl.setForeground(new Color(190, 190, 210));
        lbl.setBorder(new EmptyBorder(0, 10, 16, 10));

        JPanel textArea = new JPanel(new BorderLayout());
        textArea.setBackground(new Color(0, 0, 0, 180));
        textArea.add(lbl, BorderLayout.SOUTH);

        card.add(textArea, BorderLayout.SOUTH);
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                selectIndex(index);
                handleOpen();
            }
            @Override public void mouseEntered(MouseEvent e) {
                lbl.setForeground(Theme.HIGHLIGHT);
                card.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                lbl.setForeground(index == selectedIndex ? Theme.HIGHLIGHT : new Color(190, 190, 210));
                card.repaint();
            }
        });
        return card;
    }

    private JPanel makeSeparator(int width) {
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
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
        sep.setMaximumSize(new Dimension(width, 12));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        return sep;
    }

    private void selectIndex(int idx) {
        selectedIndex = idx;
        for (int i = 0; i < menuItems.length; i++) {
            JLabel lbl = (JLabel) ((JPanel)menuItems[i].getComponent(0)).getComponent(0);
            lbl.setForeground(i == idx ? Theme.HIGHLIGHT : new Color(190, 190, 210));
            menuItems[i].repaint();
        }
    }

    private void handleOpen() {
        if (selectedIndex == 0) main.showScreen(Main.MAP_DESIGN);
    }
}
