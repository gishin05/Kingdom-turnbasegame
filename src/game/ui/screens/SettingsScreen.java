package game.ui.screens;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import game.Main;
import game.core.input.KeyboardController;
import game.core.util.SoundManager;
import game.ui.BaseScreen;
import game.ui.Theme;

/**
 * UI DESIGN OVERVIEW:
 * This component is part of the pixelated game UI approach.
 * It integrates with the layered architecture, utilizing dynamic backgrounds
 * and semi-transparent panels to maintain a visually rich, modern aesthetic.
 */

/**
 * Settings screen for adjusting BGM volume, SFX volume, and window resolution.
 */
public class SettingsScreen extends BaseScreen {

    private static final long serialVersionUID = 1L;

    private JSlider bgmSlider;
    private JSlider sfxSlider;
    private JLabel bgmValueLabel;
    private JLabel sfxValueLabel;
    private JComboBox<String> resolutionBox;
    private JComboBox<String> mapSizeBox;
    private JCheckBox touchControlsCheck;
    private String backScreen = Main.MENU;

    // Controls UI
    private JButton btnUp1, btnUp2;
    private JButton btnDown1, btnDown2;
    private JButton btnLeft1, btnLeft2;
    private JButton btnRight1, btnRight2;
    private JButton btnEnter;
    private JButton btnEsc;

    // Temporary variables for key bindings
    private int tempUpKey1, tempUpKey2;
    private int tempDownKey1, tempDownKey2;
    private int tempLeftKey1, tempLeftKey2;
    private int tempRightKey1, tempRightKey2;
    private int tempEnterKey;
    private int tempEscKey;

    private float selectionGlow = 0f;
    private boolean glowUp = true;
    private Timer glowTimer;

    public static final String[][] RESOLUTIONS = {
        {"1280x720", "1280", "720"},
        {"1366x768", "1366", "768"},
        {"1600x900", "1600", "900"},
        {"1920x1080", "1920", "1080"},
        {"2560x1440", "2560", "1440"},
        {"Fullscreen", "0", "0"},
    };

    public SettingsScreen(Main main) {
        super(main);
        setBackground(new Color(10, 10, 18));
        initUI();
    }

    public void setBackScreen(String backScreen) {
        this.backScreen = backScreen;
    }

    private void initUI() {
        JLayeredPane layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Background panel with gradient
        JPanel bgPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(10, 10, 25), getWidth(), getHeight(), new Color(20, 15, 35));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle decorative lines
                g2.setColor(new Color(255, 215, 0, 8));
                for (int i = 0; i < getHeight(); i += 4) {
                    g2.drawLine(0, i, getWidth(), i);
                }
                g2.dispose();
            }
        };
        bgPanel.setOpaque(false);
        layeredPane.add(bgPanel, JLayeredPane.DEFAULT_LAYER);

        // Main UI
        JPanel mainUI = new JPanel(new BorderLayout());
        mainUI.setOpaque(false);
        layeredPane.add(mainUI, JLayeredPane.MODAL_LAYER);

        // ── HEADER ──
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(32, 48, 16, 48));

        JButton backBtn = createStyledButton("← BACK");
        backBtn.addActionListener(e -> {
            SoundManager.playButtonSound();
            main.showScreen(backScreen);
        });
        header.add(backBtn, BorderLayout.WEST);

        JLabel title = new JLabel("SETTINGS", SwingConstants.CENTER);
        title.setFont(Theme.getPixelFont(32f));
        title.setForeground(Theme.GOLD);
        header.add(title, BorderLayout.CENTER);

        // Invisible spacer on right to center the title
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(backBtn.getPreferredSize());
        header.add(spacer, BorderLayout.EAST);

        mainUI.add(header, BorderLayout.NORTH);

        // ── CENTER CONTENT ──
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setOpaque(false);
        settingsPanel.setBorder(new EmptyBorder(0, 0, 0, 16));

        JScrollPane scrollPane = new JScrollPane(settingsPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setPreferredSize(new Dimension(550, 420));

        // Custom Styled ScrollBar to match gold theme
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 215, 0, 150));
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(20, 20, 35, 100));
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton jb = new JButton();
                jb.setPreferredSize(new Dimension(0, 0));
                jb.setMinimumSize(new Dimension(0, 0));
                jb.setMaximumSize(new Dimension(0, 0));
                return jb;
            }
        });

        // ── AUDIO SECTION ──
        settingsPanel.add(createSectionHeader("AUDIO"));
        settingsPanel.add(Box.createVerticalStrut(20));

        // BGM Volume
        settingsPanel.add(createSliderLabel("BGM VOLUME"));
        settingsPanel.add(Box.createVerticalStrut(6));
        JPanel bgmRow = createSliderRow();
        bgmSlider = createVolumeSlider();
        bgmValueLabel = createValueLabel("100%");
        bgmSlider.setValue(100);
        bgmSlider.addChangeListener(e -> {
            int val = bgmSlider.getValue();
            bgmValueLabel.setText(val + "%");
            SoundManager.setMasterBgmVolume(val / 100.0);
            SoundManager.setBgmVolume(val / 100.0);
        });
        bgmRow.add(bgmSlider, BorderLayout.CENTER);
        bgmRow.add(bgmValueLabel, BorderLayout.EAST);
        settingsPanel.add(bgmRow);

        settingsPanel.add(Box.createVerticalStrut(20));

        // SFX Volume
        settingsPanel.add(createSliderLabel("SFX VOLUME"));
        settingsPanel.add(Box.createVerticalStrut(6));
        JPanel sfxRow = createSliderRow();
        sfxSlider = createVolumeSlider();
        sfxValueLabel = createValueLabel("100%");
        sfxSlider.setValue(100);
        sfxSlider.addChangeListener(e -> {
            int val = sfxSlider.getValue();
            sfxValueLabel.setText(val + "%");
            SoundManager.setSfxVolume(val / 100.0f);
        });
        // Play a test SFX when the user releases the slider
        sfxSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                SoundManager.playButtonSound();
            }
        });
        sfxRow.add(sfxSlider, BorderLayout.CENTER);
        sfxRow.add(sfxValueLabel, BorderLayout.EAST);
        settingsPanel.add(sfxRow);

        settingsPanel.add(Box.createVerticalStrut(36));

        // ── DISPLAY SECTION ──
        settingsPanel.add(createSectionHeader("DISPLAY"));
        settingsPanel.add(Box.createVerticalStrut(20));

        settingsPanel.add(createSliderLabel("RESOLUTION"));
        settingsPanel.add(Box.createVerticalStrut(6));

        String[] resLabels = new String[RESOLUTIONS.length];
        for (int i = 0; i < RESOLUTIONS.length; i++) resLabels[i] = RESOLUTIONS[i][0];
        resolutionBox = new JComboBox<>(resLabels);
        resolutionBox.setFont(Theme.getPixelFont(16f));
        resolutionBox.setForeground(Color.WHITE);
        resolutionBox.setBackground(new Color(30, 30, 50));
        resolutionBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        resolutionBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        resolutionBox.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0, 60)));
        // Pre-select current resolution
        resolutionBox.setSelectedIndex(findCurrentResolutionIndex());
        resolutionBox.addActionListener(e -> applyResolution());
        settingsPanel.add(resolutionBox);

        settingsPanel.add(Box.createVerticalStrut(36));

        // ── GAMEPLAY SECTION ──
        settingsPanel.add(createSectionHeader("GAMEPLAY"));
        settingsPanel.add(Box.createVerticalStrut(20));

        settingsPanel.add(createSliderLabel("MAP SIZE (ZOOM TO FIT)"));
        settingsPanel.add(Box.createVerticalStrut(6));

        String[] mapSizeOptions = {"15x10", "20x15", "25x20", "30x25", "40x30", "50x50"};
        mapSizeBox = new JComboBox<>(mapSizeOptions);
        mapSizeBox.setFont(Theme.getPixelFont(16f));
        mapSizeBox.setForeground(Color.WHITE);
        mapSizeBox.setBackground(new Color(30, 30, 50));
        mapSizeBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        mapSizeBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapSizeBox.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0, 60)));
        settingsPanel.add(mapSizeBox);

        settingsPanel.add(Box.createVerticalStrut(20));

        touchControlsCheck = new JCheckBox("ENABLE TOUCH OVERLAY (ANDROID)");
        touchControlsCheck.setFont(Theme.getPixelFont(14f));
        touchControlsCheck.setForeground(new Color(180, 180, 210));
        touchControlsCheck.setOpaque(false);
        touchControlsCheck.setFocusPainted(false);
        touchControlsCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        touchControlsCheck.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(30, 30, 50));
                g2.fillRect(x, y, 16, 16);
                g2.setColor(new Color(255, 215, 0, 80));
                g2.drawRect(x, y, 16, 16);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
        });
        touchControlsCheck.setSelectedIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(30, 30, 50));
                g2.fillRect(x, y, 16, 16);
                g2.setColor(Theme.GOLD);
                g2.drawRect(x, y, 16, 16);
                g2.fillRect(x + 4, y + 4, 8, 8);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
        });
        settingsPanel.add(touchControlsCheck);

        settingsPanel.add(Box.createVerticalStrut(36));

        // ── CONTROLS SECTION ──
        settingsPanel.add(createSectionHeader("CONTROLS"));
        settingsPanel.add(Box.createVerticalStrut(20));

        settingsPanel.add(createControlRow("MOVE UP", 
            btnUp1 = createKeyRebindButton(() -> tempUpKey1, val -> tempUpKey1 = val),
            btnUp2 = createKeyRebindButton(() -> tempUpKey2, val -> tempUpKey2 = val)
        ));
        settingsPanel.add(Box.createVerticalStrut(12));
        
        settingsPanel.add(createControlRow("MOVE DOWN", 
            btnDown1 = createKeyRebindButton(() -> tempDownKey1, val -> tempDownKey1 = val),
            btnDown2 = createKeyRebindButton(() -> tempDownKey2, val -> tempDownKey2 = val)
        ));
        settingsPanel.add(Box.createVerticalStrut(12));

        settingsPanel.add(createControlRow("MOVE LEFT", 
            btnLeft1 = createKeyRebindButton(() -> tempLeftKey1, val -> tempLeftKey1 = val),
            btnLeft2 = createKeyRebindButton(() -> tempLeftKey2, val -> tempLeftKey2 = val)
        ));
        settingsPanel.add(Box.createVerticalStrut(12));

        settingsPanel.add(createControlRow("MOVE RIGHT", 
            btnRight1 = createKeyRebindButton(() -> tempRightKey1, val -> tempRightKey1 = val),
            btnRight2 = createKeyRebindButton(() -> tempRightKey2, val -> tempRightKey2 = val)
        ));
        settingsPanel.add(Box.createVerticalStrut(12));

        settingsPanel.add(createControlRow("CONFIRM / SELECT", 
            btnEnter = createKeyRebindButton(() -> tempEnterKey, val -> tempEnterKey = val),
            null
        ));
        settingsPanel.add(Box.createVerticalStrut(12));

        settingsPanel.add(createControlRow("CANCEL / MENU", 
            btnEsc = createKeyRebindButton(() -> tempEscKey, val -> tempEscKey = val),
            null
        ));

        settingsPanel.add(Box.createVerticalStrut(36));

        // ── APPLY / RESET ──
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton resetBtn = createStyledButton("RESET DEFAULTS");
        resetBtn.addActionListener(e -> {
            SoundManager.playButtonSound();
            bgmSlider.setValue(100);
            sfxSlider.setValue(100);
            resolutionBox.setSelectedIndex(3); // 1920x1080
            mapSizeBox.setSelectedIndex(0);
            touchControlsCheck.setSelected(false);
            
            // Reset control bindings
            tempUpKey1 = KeyEvent.VK_UP;
            tempUpKey2 = KeyEvent.VK_W;
            tempDownKey1 = KeyEvent.VK_DOWN;
            tempDownKey2 = KeyEvent.VK_S;
            tempLeftKey1 = KeyEvent.VK_LEFT;
            tempLeftKey2 = KeyEvent.VK_A;
            tempRightKey1 = KeyEvent.VK_RIGHT;
            tempRightKey2 = KeyEvent.VK_D;
            tempEnterKey = KeyEvent.VK_ENTER;
            tempEscKey = KeyEvent.VK_ESCAPE;
            
            // Update button texts
            btnUp1.setText(KeyEvent.getKeyText(tempUpKey1).toUpperCase());
            btnUp2.setText(KeyEvent.getKeyText(tempUpKey2).toUpperCase());
            btnDown1.setText(KeyEvent.getKeyText(tempDownKey1).toUpperCase());
            btnDown2.setText(KeyEvent.getKeyText(tempDownKey2).toUpperCase());
            btnLeft1.setText(KeyEvent.getKeyText(tempLeftKey1).toUpperCase());
            btnLeft2.setText(KeyEvent.getKeyText(tempLeftKey2).toUpperCase());
            btnRight1.setText(KeyEvent.getKeyText(tempRightKey1).toUpperCase());
            btnRight2.setText(KeyEvent.getKeyText(tempRightKey2).toUpperCase());
            btnEnter.setText(KeyEvent.getKeyText(tempEnterKey).toUpperCase());
            btnEsc.setText(KeyEvent.getKeyText(tempEscKey).toUpperCase());
        });
        btnRow.add(resetBtn);

        JButton saveBtn = createStyledButton("SAVE SETTINGS");
        saveBtn.addActionListener(e -> {
            SoundManager.playButtonSound();
            game.core.save.SettingsSaveData data = new game.core.save.SettingsSaveData();
            data.bgmVolume = bgmSlider.getValue() / 100.0;
            data.sfxVolume = sfxSlider.getValue() / 100.0f;
            data.resolutionIndex = resolutionBox.getSelectedIndex();
            data.mapSizeIndex = mapSizeBox.getSelectedIndex();
            data.touchOverlayEnabled = touchControlsCheck.isSelected();
            
            // Save control bindings
            data.upKey1 = tempUpKey1;
            data.upKey2 = tempUpKey2;
            data.downKey1 = tempDownKey1;
            data.downKey2 = tempDownKey2;
            data.leftKey1 = tempLeftKey1;
            data.leftKey2 = tempLeftKey2;
            data.rightKey1 = tempRightKey1;
            data.rightKey2 = tempRightKey2;
            data.enterKey = tempEnterKey;
            data.escKey = tempEscKey;
            
            game.core.save.SaveManager.saveSettings(data);
            
            // Apply control bindings immediately
            main.getKeyboardController().loadFromSettings(data);
            main.updateTouchOverlayVisibility(data.touchOverlayEnabled);
            
            main.showScreen(backScreen);
        });
        btnRow.add(saveBtn);

        settingsPanel.add(btnRow);

        centerWrapper.add(scrollPane);
        mainUI.add(centerWrapper, BorderLayout.CENTER);

        // ── FOOTER ──
        JPanel footer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int mid = getHeight() / 2;
                g2.setPaint(new GradientPaint(0, mid, new Color(255, 215, 0, 0), getWidth() / 2, mid, Theme.GOLD_TRANS));
                g2.fillRect(0, mid - 1, getWidth() / 2, 2);
                g2.setPaint(new GradientPaint(getWidth() / 2, mid, Theme.GOLD_TRANS, getWidth(), mid, new Color(255, 215, 0, 0)));
                g2.fillRect(getWidth() / 2, mid - 1, getWidth() / 2, 2);
                g2.dispose();
            }
        };
        footer.setOpaque(false);
        footer.setPreferredSize(new Dimension(0, 32));
        mainUI.add(footer, BorderLayout.SOUTH);

        // Glow animation timer
        glowTimer = new Timer(30, e -> {
            if (glowUp) { selectionGlow += 0.04f; if (selectionGlow >= 1f) { selectionGlow = 1f; glowUp = false; } }
            else        { selectionGlow -= 0.04f; if (selectionGlow <= 0f) { selectionGlow = 0f; glowUp = true; } }
        });
        glowTimer.start();

        // Resize handler for layered pane
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = getWidth(), h = getHeight();
                bgPanel.setBounds(0, 0, w, h);
                mainUI.setBounds(0, 0, w, h);
            }
        });
    }

    // ── UI FACTORY METHODS ──
    // These methods generate consistent UI components used throughout the settings panel,
    // ensuring the design language remains cohesive without duplicating code.

    /**
     * Creates a standardized section header with a title and a fading gold underline.
     */
    private JPanel createSectionHeader(String text) {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.getPixelFont(20f));
        lbl.setForeground(Theme.GOLD);
        section.add(lbl, BorderLayout.WEST);

        // Decorative separator line
        JPanel line = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int y = getHeight() / 2;
                g2.setPaint(new GradientPaint(0, y, new Color(255, 215, 0, 100), getWidth(), y, new Color(255, 215, 0, 0)));
                g2.fillRect(0, y, getWidth(), 1);
                g2.dispose();
            }
        };
        line.setOpaque(false);
        line.setBorder(new EmptyBorder(0, 16, 0, 0));
        section.add(line, BorderLayout.CENTER);

        return section;
    }

    private JLabel createSliderLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.getPixelFont(13f).deriveFont(Font.BOLD));
        lbl.setForeground(new Color(160, 160, 190));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 4, 0, 0));
        return lbl;
    }

    private JPanel createSliderRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return row;
    }

    private JSlider createVolumeSlider() {
        JSlider slider = new JSlider(0, 100, 100);
        slider.setOpaque(false);
        slider.setForeground(Theme.GOLD);
        slider.setFont(Theme.getSmallFont());
        slider.setFocusable(false);
        return slider;
    }

    private JLabel createValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.getPixelFont(14f));
        lbl.setForeground(new Color(255, 230, 100));
        lbl.setPreferredSize(new Dimension(60, 30));
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        return lbl;
    }

    /**
     * Generates a secondary styled button specifically tuned for the settings screen.
     * Features a dark background with a gold border and hover color transitions.
     */
    private JButton createStyledButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.getPixelFont(14f));
        b.setForeground(new Color(220, 220, 240));
        b.setBackground(new Color(40, 40, 60));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 215, 0, 80)),
            new EmptyBorder(8, 20, 8, 20)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(60, 50, 20)); }
            public void mouseExited(MouseEvent e) { b.setBackground(new Color(40, 40, 60)); }
        });
        return b;
    }

    // ── RESOLUTION ──

    private int findCurrentResolutionIndex() {
        int w = main.getWidth();
        int h = main.getHeight();
        for (int i = 0; i < RESOLUTIONS.length - 1; i++) {
            if (Integer.parseInt(RESOLUTIONS[i][1]) == w && Integer.parseInt(RESOLUTIONS[i][2]) == h) {
                return i;
            }
        }
        // Check fullscreen
        if (main.getExtendedState() == JFrame.MAXIMIZED_BOTH && main.isUndecorated()) {
            return RESOLUTIONS.length - 1;
        }
        return 3; // Default to 1920x1080
    }

    private void applyResolution() {
        int idx = resolutionBox.getSelectedIndex();
        if (idx < 0) return;
        main.applyResolution(idx, RESOLUTIONS[idx][0], RESOLUTIONS[idx][1], RESOLUTIONS[idx][2]);
    }

    private JPanel createControlRow(String actionName, JButton btn1, JButton btn2) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel label = new JLabel(actionName);
        label.setFont(Theme.getPixelFont(14f));
        label.setForeground(new Color(180, 180, 210));
        row.add(label, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        if (btn1 != null) {
            btnPanel.add(btn1);
        }
        if (btn2 != null) {
            btnPanel.add(btn2);
        }
        row.add(btnPanel, BorderLayout.EAST);

        return row;
    }

    private JButton createKeyRebindButton(java.util.function.Supplier<Integer> getter, java.util.function.Consumer<Integer> setter) {
        JButton b = new JButton("");
        b.setFont(Theme.getPixelFont(12f));
        b.setForeground(new Color(255, 230, 100));
        b.setBackground(new Color(30, 30, 50));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0, 60)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(140, 32));
        b.setMaximumSize(new Dimension(140, 32));
        
        b.addActionListener(e -> {
            SoundManager.playButtonSound();
            b.setText("PRESS KEY...");
            b.setBackground(new Color(60, 20, 20));
            b.requestFocusInWindow();
            
            KeyListener kl = new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent ke) {
                    int code = ke.getKeyCode();
                    if (code != KeyEvent.VK_UNDEFINED) {
                        setter.accept(code);
                        b.setText(KeyEvent.getKeyText(code).toUpperCase());
                        b.setBackground(new Color(30, 30, 50));
                        b.removeKeyListener(this);
                    }
                }
            };
            b.addKeyListener(kl);
        });
        
        b.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                b.setBackground(new Color(30, 30, 50));
                b.setText(KeyEvent.getKeyText(getter.get()).toUpperCase());
                for (KeyListener kl : b.getKeyListeners()) {
                    b.removeKeyListener(kl);
                }
            }
        });
        return b;
    }

    @Override
    public void refresh() {
        // Sync slider positions with current SoundManager state
        bgmSlider.setValue((int) (SoundManager.getMasterBgmVolume() * 100));
        sfxSlider.setValue((int) (SoundManager.getSfxVolume() * 100));
        bgmValueLabel.setText(bgmSlider.getValue() + "%");
        sfxValueLabel.setText(sfxSlider.getValue() + "%");

        // Temporarily remove action listener to prevent double-applying
        ActionListener[] listeners = resolutionBox.getActionListeners();
        for (ActionListener l : listeners) resolutionBox.removeActionListener(l);
        
        resolutionBox.setSelectedIndex(findCurrentResolutionIndex());
        
        for (ActionListener l : listeners) resolutionBox.addActionListener(l);

        game.core.save.SettingsSaveData data = game.core.save.SaveManager.loadSettings();
        if (data != null && data.mapSizeIndex >= 0 && data.mapSizeIndex < mapSizeBox.getItemCount()) {
            mapSizeBox.setSelectedIndex(data.mapSizeIndex);
        }
        if (data != null) {
            touchControlsCheck.setSelected(data.touchOverlayEnabled);
        }

        // Sync temp control fields from KeyboardController
        KeyboardController kc = main.getKeyboardController();
        tempUpKey1 = kc.upKey1;
        tempUpKey2 = kc.upKey2;
        tempDownKey1 = kc.downKey1;
        tempDownKey2 = kc.downKey2;
        tempLeftKey1 = kc.leftKey1;
        tempLeftKey2 = kc.leftKey2;
        tempRightKey1 = kc.rightKey1;
        tempRightKey2 = kc.rightKey2;
        tempEnterKey = kc.enterKey;
        tempEscKey = kc.escKey;

        // Set button texts
        if (btnUp1 != null) {
            btnUp1.setText(KeyEvent.getKeyText(tempUpKey1).toUpperCase());
            btnUp2.setText(KeyEvent.getKeyText(tempUpKey2).toUpperCase());
            btnDown1.setText(KeyEvent.getKeyText(tempDownKey1).toUpperCase());
            btnDown2.setText(KeyEvent.getKeyText(tempDownKey2).toUpperCase());
            btnLeft1.setText(KeyEvent.getKeyText(tempLeftKey1).toUpperCase());
            btnLeft2.setText(KeyEvent.getKeyText(tempLeftKey2).toUpperCase());
            btnRight1.setText(KeyEvent.getKeyText(tempRightKey1).toUpperCase());
            btnRight2.setText(KeyEvent.getKeyText(tempRightKey2).toUpperCase());
            btnEnter.setText(KeyEvent.getKeyText(tempEnterKey).toUpperCase());
            btnEsc.setText(KeyEvent.getKeyText(tempEscKey).toUpperCase());
        }

        revalidate();
        repaint();
    }

    @Override
    public void pause() {
        // no-op
    }
}
