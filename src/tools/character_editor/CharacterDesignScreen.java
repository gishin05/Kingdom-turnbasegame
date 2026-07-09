package tools.character_editor;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.EventQueue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import game.core.animation.AnimationCommand;
import game.core.animation.AnimationScript;
import game.core.util.AssetManager;
import game.core.unit.UnitStats;

public class CharacterDesignScreen extends JPanel  {

    private static final long serialVersionUID = 1L;
    
    
    // UI Panels
    private JPanel leftPanel;
    private JPanel centerPanel;
    private JPanel rightPanel;
    private JPanel topToolbar;
    
    // Preview component
    private AnimationPreview animationPreview;
    private game.core.battle.BattleManager battleManager = new game.core.battle.BattleManager();
    
    // Selectors
    private JComboBox<String> categorySelector;
    private JComboBox<String> unitSelector;
    private JComboBox<String> weaponSelector;
    private JComboBox<String> modeSelector;
    private JComboBox<String> terrainSelector;
    
    // Background Engine
    private game.core.battle.BattleBackgroundEngine bgEngine = new game.core.battle.BattleBackgroundEngine();

    // Palette management
    private java.util.List<Color> detectedColors = new java.util.ArrayList<>();
    private java.util.Map<Color, Color> colorReplacements = new java.util.HashMap<>();
    private java.util.Stack<java.util.Map<Color, Color>> paletteHistory = new java.util.Stack<>();
    private JPanel paletteContainer;

    // Stats UI
    private JSpinner spinStr, spinMag, spinSkl, spinSpd, spinLck, spinDef, spinRes, spinMov, spinCon, spinAid, spinTrv;
    private JSpinner spinLvl, spinExp, spinMaxHp;
    private JComboBox<String> affSelector;
    private JSpinner spinBattles, spinWins, spinLosses;
    private JComboBox<String> typeSelector;
    private JComboBox<String> subTypeSelector;
    private JTextField txtCond;
    private UnitStats currentStats = new UnitStats();
    private static final String ANIMS_BASE_DIR = game.core.util.GamePaths.BATTLE.getAbsolutePath();

    

    public CharacterDesignScreen() {

        
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        initToolbar();
        initLeftPanel();
        initRightPanel();
        initCenterPanel(); // animationPreview created here
        
        // All selectors default to null — user must choose manually
        categorySelector.setSelectedIndex(-1);
        // animationPreview.loadFrames() skipped until user selects a unit
    }
    
    private void initToolbar() {
        topToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topToolbar.setOpaque(false);
        
        JButton btnBack = new JButton("Back");
        btnBack.addActionListener(e -> {
            
                Window w = SwingUtilities.getWindowAncestor(btnBack);
                if (w != null) {
                    w.dispose();
                } else {
                    System.exit(0);
                }
        });
        
        JButton btnSave = new JButton("Save");
        btnSave.addActionListener(e -> animationPreview.saveRecoloredAnimation());
        JButton btnLoad = new JButton("Load");
        JButton btnExport = new JButton("Export PNG");
        
        JButton btnEditor = new JButton("Editor");
        btnEditor.setBackground(new Color(60, 140, 60));
        btnEditor.setForeground(Color.WHITE);
        btnEditor.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem battleAnim = new JMenuItem("Battle Animation");
            battleAnim.addActionListener(ev -> {
                JFrame f = new JFrame("Animation Editor"); f.setSize(1280, 720); f.setLocationRelativeTo(null); f.setContentPane(new AnimationEditorScreen()); f.setVisible(true);
            });
            JMenuItem mapUnitAnim = new JMenuItem("Map Unit Animation");
            mapUnitAnim.addActionListener(ev -> {
                JFrame f = new JFrame("Map Unit Animation"); f.setSize(1280, 720); f.setLocationRelativeTo(null); f.setContentPane(new MapUnitAnimationScreen()); f.setVisible(true);
            });
            
            menu.show(btnEditor, 0, btnEditor.getHeight());
        });
        
        
        topToolbar.add(btnBack);
        topToolbar.add(btnEditor);
        topToolbar.add(btnSave);
        topToolbar.add(btnLoad);
        topToolbar.add(btnExport);
        
        add(topToolbar, BorderLayout.NORTH);
    }
    
    private void initLeftPanel() {
        leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        leftPanel.setPreferredSize(new Dimension(300, 800));
        
        categorySelector = new JComboBox<>(new String[]{"Champion", "Unit"});
        unitSelector = new JComboBox<>();
        weaponSelector = new JComboBox<>();
        modeSelector = new JComboBox<>();
        terrainSelector = new JComboBox<>(bgEngine.getAvailableTerrains());
        terrainSelector.setSelectedIndex(-1);
        
        addSection(leftPanel, "Category", categorySelector);
        addSection(leftPanel, "Unit", unitSelector);
        addSection(leftPanel, "Weapon", weaponSelector);
        addSection(leftPanel, "Action type", modeSelector);
        addSection(leftPanel, "Terrain", terrainSelector);

        categorySelector.addActionListener(e -> updateUnitList());
        unitSelector.addActionListener(e -> updateWeaponList());
        weaponSelector.addActionListener(e -> {
            loadStatsForUnit();
            if (animationPreview != null) animationPreview.loadFrames();
        });
        modeSelector.addActionListener(e -> { if (animationPreview != null) animationPreview.refresh(); });
        terrainSelector.addActionListener(e -> { if (animationPreview != null) animationPreview.repaint(); });
        
        // Initial population done AFTER initStatsPanel so spinners are not null
        
        JButton btnPlay = new JButton("Play / Pause");
        
        JButton btnReset = new JButton("Reset Animation");
        btnReset.addActionListener(e -> {
            animationPreview.resetBattle();
            animationPreview.currentFrameIndex = 0;
            animationPreview.repaint();
        });
 
        addSection(leftPanel, "Controls", btnPlay);
        leftPanel.add(btnReset);
        
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        initStatsPanel();
        
        add(leftPanel, BorderLayout.WEST);
    }

    private void updateUnitList() {
        String cat = (String) categorySelector.getSelectedItem();
        unitSelector.removeAllItems();
        File catDir = new File(ANIMS_BASE_DIR + "/" + cat);
        if (catDir.exists() && catDir.isDirectory()) {
            File[] folders = catDir.listFiles(File::isDirectory);
            if (folders != null) {
                for (File f : folders) unitSelector.addItem(f.getName());
            }
        }
        if (unitSelector.getItemCount() > 0) unitSelector.setSelectedIndex(0);
    }

    private void updateWeaponList() {
        String cat = (String) categorySelector.getSelectedItem();
        String unit = (String) unitSelector.getSelectedItem();
        weaponSelector.removeAllItems();
        if (unit == null) return;
        
        File unitDir = new File(ANIMS_BASE_DIR + "/" + cat + "/" + unit);
        if (unitDir.exists() && unitDir.isDirectory()) {
            File[] folders = unitDir.listFiles(File::isDirectory);
            if (folders != null) {
                for (File f : folders) weaponSelector.addItem(f.getName());
            }
        }
        if (weaponSelector.getItemCount() > 0) weaponSelector.setSelectedIndex(0);
    }

    private void initStatsPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 9));
        tabs.setBackground(Color.BLACK);
        tabs.setForeground(Color.WHITE);
        
        // --- PAGE 1: CHARACTER & FIGHTING ---
        JPanel p1 = new JPanel(new GridLayout(0, 2, 1, 1));
        p1.setBackground(Color.BLACK);
        p1.setOpaque(true);
        
        spinLvl = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        spinExp = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));
        spinMaxHp = new JSpinner(new SpinnerNumberModel(20, 1, 80, 1));
        affSelector = new JComboBox<>(new String[]{"Fire", "Thunder", "Wind", "Water", "Ice", "Light", "Anima", "Dark"});
        styleCompCompact(affSelector);
        
        spinStr = new JSpinner(new SpinnerNumberModel(5, 0, 31, 1));
        spinMag = new JSpinner(new SpinnerNumberModel(0, 0, 31, 1));
        spinSkl = new JSpinner(new SpinnerNumberModel(5, 0, 31, 1));
        spinSpd = new JSpinner(new SpinnerNumberModel(5, 0, 31, 1));
        spinLck = new JSpinner(new SpinnerNumberModel(5, 0, 31, 1));
        spinDef = new JSpinner(new SpinnerNumberModel(5, 0, 31, 1));
        spinRes = new JSpinner(new SpinnerNumberModel(5, 0, 31, 1));
        
        styleSpinnerCompact(spinLvl); styleSpinnerCompact(spinExp); styleSpinnerCompact(spinMaxHp);
        styleSpinnerCompact(spinStr); styleSpinnerCompact(spinMag); styleSpinnerCompact(spinSkl);
        styleSpinnerCompact(spinSpd); styleSpinnerCompact(spinLck); styleSpinnerCompact(spinDef);
        styleSpinnerCompact(spinRes);

        addLabeledComp(p1, "Lvl:", spinLvl);
        addLabeledComp(p1, "Exp:", spinExp);
        addLabeledComp(p1, "Max HP:", spinMaxHp);
        addLabeledComp(p1, "Affinity:", affSelector);
        addLabeledComp(p1, "Str:", spinStr);
        addLabeledComp(p1, "Mag:", spinMag);
        addLabeledComp(p1, "Skl:", spinSkl);
        addLabeledComp(p1, "Spd:", spinSpd);
        addLabeledComp(p1, "Lck:", spinLck);
        addLabeledComp(p1, "Def:", spinDef);
        addLabeledComp(p1, "Res:", spinRes);
        
        // --- PAGE 2: PERSONAL DATA ---
        JPanel p2 = new JPanel(new GridLayout(0, 2, 1, 1));
        p2.setBackground(Color.BLACK);
        p2.setOpaque(true);
        
        spinMov = new JSpinner(new SpinnerNumberModel(5, 0, 15, 1));
        spinCon = new JSpinner(new SpinnerNumberModel(5, 0, 25, 1));
        spinAid = new JSpinner(new SpinnerNumberModel(4, 0, 25, 1));
        spinTrv = new JSpinner(new SpinnerNumberModel(0, 0, 25, 1));
        typeSelector = new JComboBox<>(new String[]{"Land Unit", "Ocean Unit", "Air Unit"});
        styleCompCompact(typeSelector);
        subTypeSelector = new JComboBox<>(new String[]{"Armored", "Mounted", "Mounted Armored", "Infantry", "Siege", "Flier", "Armored Flier"});
        styleCompCompact(subTypeSelector);
        subTypeSelector.addActionListener(e -> {
            String selected = (String) subTypeSelector.getSelectedItem();
            if (selected == null) return;
            if ("Flier".equals(selected)) {
                typeSelector.setSelectedItem("Air Unit");
                spinRes.setValue(Math.max(12, (int) spinRes.getValue() + 5));
            } else if ("Armored Flier".equals(selected)) {
                typeSelector.setSelectedItem("Air Unit");
                spinDef.setValue(Math.max(15, (int) spinDef.getValue() + 8));
            } else if ("Armored".equals(selected)) {
                typeSelector.setSelectedItem("Land Unit");
                spinDef.setValue(Math.max(12, (int) spinDef.getValue() + 4));
            } else if ("Mounted".equals(selected) || "Mounted Armored".equals(selected)) {
                typeSelector.setSelectedItem("Land Unit");
            }
        });
        txtCond = new JTextField("None");
        txtCond.setBackground(Color.BLACK);
        txtCond.setForeground(Color.WHITE);
        txtCond.setFont(new Font("SansSerif", Font.PLAIN, 10));
        
        styleSpinnerCompact(spinMov); styleSpinnerCompact(spinCon); styleSpinnerCompact(spinAid);
        styleSpinnerCompact(spinTrv);
        
        addLabeledComp(p2, "Move:", spinMov);
        addLabeledComp(p2, "Con:", spinCon);
        addLabeledComp(p2, "Aid:", spinAid);
        addLabeledComp(p2, "Trv:", spinTrv);
        addLabeledComp(p2, "Type:", typeSelector);
        addLabeledComp(p2, "Sub Type:", subTypeSelector);
        addLabeledComp(p2, "Cond:", txtCond);

        // Weapon Ranks removed (p3 deleted)

        // --- PAGE 4: B/W/L RECORD ---
        JPanel p4 = new JPanel(new GridLayout(0, 2, 1, 1));
        p4.setBackground(Color.BLACK);
        p4.setOpaque(true);
        
        spinBattles = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
        spinWins = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
        spinLosses = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
        styleSpinnerCompact(spinBattles); styleSpinnerCompact(spinWins); styleSpinnerCompact(spinLosses);
        
        addLabeledComp(p4, "Battles:", spinBattles);
        addLabeledComp(p4, "Wins:", spinWins);
        addLabeledComp(p4, "Losses:", spinLosses);

        tabs.addTab("Basic", p1);
        tabs.addTab("Personal", p2);
        // Weapon Ranks removed
        tabs.addTab("Record", p4);

        JButton btnSaveStats = new JButton("SAVE GLOBAL STATS");
        btnSaveStats.setBackground(new Color(60, 100, 160));
        btnSaveStats.setForeground(Color.WHITE);
        btnSaveStats.addActionListener(e -> saveCurrentStats());
        
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.BLACK);
        container.setOpaque(true);
        container.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60)), "CHARACTER INFO", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 10), Color.LIGHT_GRAY));
        
        container.add(tabs, BorderLayout.CENTER);
        container.add(btnSaveStats, BorderLayout.SOUTH);
        
        leftPanel.add(container);
    }

    private void addLabeledComp(JPanel p, String text, JComponent c) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 9));
        l.setForeground(new Color(200, 200, 200));
        p.add(l);
        p.add(c);
    }

    private void styleSpinnerCompact(JSpinner s) {
        s.setFont(new Font("SansSerif", Font.PLAIN, 10));
        JComponent editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(new Color(20, 20, 20));
            tf.setForeground(Color.WHITE);
            tf.setCaretColor(Color.WHITE);
        }
    }

    private void styleCompCompact(JComponent c) {
        c.setFont(new Font("SansSerif", Font.PLAIN, 10));
        c.setBackground(new Color(20, 20, 20));
        c.setForeground(Color.WHITE);
    }

    private void saveCurrentStats() {
        String cat = (String) categorySelector.getSelectedItem();
        String unitName = (String) unitSelector.getSelectedItem();
        if (unitName == null) return;
        
        currentStats.unitName = unitName;
        currentStats.unitType = (String) typeSelector.getSelectedItem();
        currentStats.subUnitType = (String) subTypeSelector.getSelectedItem();
        currentStats.level = (int) spinLvl.getValue();
        currentStats.exp = (int) spinExp.getValue();
        currentStats.maxHp = (int) spinMaxHp.getValue();
        currentStats.strength = (int) spinStr.getValue();
        currentStats.magic = (int) spinMag.getValue();
        currentStats.skill = (int) spinSkl.getValue();
        currentStats.speed = (int) spinSpd.getValue();
        currentStats.luck = (int) spinLck.getValue();
        currentStats.defense = (int) spinDef.getValue();
        currentStats.resistance = (int) spinRes.getValue();
        // currentStats.affinity = (String) affSelector.getSelectedItem();
        currentStats.move = (int) spinMov.getValue();
        // currentStats.con = (int) spinCon.getValue();
        // currentStats.aid = (int) spinAid.getValue();
        // currentStats.trv = (int) spinTrv.getValue();
        // currentStats.cond = txtCond.getText();
        
        currentStats.battles = (int) spinBattles.getValue();
        currentStats.wins = (int) spinWins.getValue();
        currentStats.losses = (int) spinLosses.getValue();
        
        File dir = new File(ANIMS_BASE_DIR + "/" + cat + "/" + unitName);
        if (!dir.exists()) dir.mkdirs();
        File statsFile = new File(dir, "stats.json");
        try {
            java.nio.file.Files.write(statsFile.toPath(), currentStats.toJson().getBytes());
            JOptionPane.showMessageDialog(this, "Stats saved globally for " + unitName + "!");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadStatsForUnit() {
        String cat = (String) categorySelector.getSelectedItem();
        String unitName = (String) unitSelector.getSelectedItem();
        if (unitName == null) return;
        
        // Update Lvl/Exp visibility based on category
        boolean isChamp = cat.equalsIgnoreCase("Champion");
        spinLvl.setEnabled(isChamp);
        spinExp.setEnabled(isChamp);
        
        File statsFile = new File(ANIMS_BASE_DIR + "/" + cat + "/" + unitName + "/stats.json");
        if (statsFile.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(statsFile.toPath()));
                currentStats = UnitStats.fromJson(content);
            } catch (Exception ex) { 
                ex.printStackTrace(); 
                currentStats = new UnitStats();
            }
        } else {
            currentStats = new UnitStats();
        }
        
        // Update UI
        spinLvl.setValue(currentStats.level);
        spinExp.setValue(currentStats.exp);
        spinMaxHp.setValue(currentStats.maxHp);
        // affSelector.setSelectedItem(currentStats.affinity);

        spinStr.setValue(currentStats.strength);
        spinMag.setValue(currentStats.magic);
        spinSkl.setValue(currentStats.skill);
        spinSpd.setValue(currentStats.speed);
        spinLck.setValue(currentStats.luck);
        spinDef.setValue(currentStats.defense);
        spinRes.setValue(currentStats.resistance);
        
        spinMov.setValue(currentStats.move);
        // spinCon.setValue(currentStats.con);
        // spinAid.setValue(currentStats.aid);
        // spinTrv.setValue(currentStats.trv);
        
        typeSelector.setSelectedItem(currentStats.unitType);
        subTypeSelector.setSelectedItem(currentStats.subUnitType);
        // txtCond.setText(currentStats.cond);

        // Weapon ranks removed from UI/Logic
        
        spinBattles.setValue(currentStats.battles);
        spinWins.setValue(currentStats.wins);
        spinLosses.setValue(currentStats.losses);
    }
    
    private void addSection(JPanel parent, String title, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), title, TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12), Color.WHITE));
        p.add(comp, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(280, 60));
        parent.add(p);
        parent.add(Box.createRigidArea(new Dimension(0, 4)));
    }
    
    private void initRightPanel() {
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        rightPanel.setPreferredSize(new Dimension(300, 800));
        
        paletteContainer = new JPanel();
        paletteContainer.setLayout(new BoxLayout(paletteContainer, BoxLayout.Y_AXIS));
        paletteContainer.setOpaque(false);
        paletteContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Palette", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12), Color.WHITE));

        rightPanel.add(paletteContainer);
        
        // Palette Controls (Undo/Reset)
        JPanel paletteControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        paletteControls.setOpaque(false);
        
        JButton btnUndo = new JButton("Undo");
        btnUndo.addActionListener(e -> undoPalette());
        
        JButton btnResetColors = new JButton("Reset Color");
        btnResetColors.addActionListener(e -> resetPalette());
        
        paletteControls.add(btnUndo);
        paletteControls.add(btnResetColors);
        rightPanel.add(paletteControls);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Frame Gallery Panel
        JPanel galleryContainer = new JPanel(new BorderLayout());
        galleryContainer.setOpaque(false);
        galleryContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Frame Gallery", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12), Color.WHITE));
        
        galleryPanel = new JPanel();
        galleryPanel.setLayout(new BoxLayout(galleryPanel, BoxLayout.Y_AXIS));
        galleryPanel.setBackground(Color.BLACK);
        
        JScrollPane scrollPane = new JScrollPane(galleryPanel);
        scrollPane.setPreferredSize(new Dimension(280, 500)); // Larger height
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        galleryContainer.add(scrollPane, BorderLayout.CENTER);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        rightPanel.add(galleryContainer);
        
        add(rightPanel, BorderLayout.EAST);
    }
    
    private JPanel galleryPanel;
    private List<String> currentModeFrameNames = new ArrayList<>();
    
    private void updatePaletteUI() {
        if (paletteContainer == null) return;
        paletteContainer.removeAll();
        
        for (int i = 0; i < detectedColors.size(); i++) {
            Color original = detectedColors.get(i);
            Color current = colorReplacements.get(original);
            
            JPanel p = new JPanel(new BorderLayout());
            p.setOpaque(false);
            p.setMaximumSize(new Dimension(280, 30));
            
            String category = getCategoryForIndex(i + 1);
            JLabel lbl = new JLabel("Index " + (i + 1) + ": " + category);
            lbl.setForeground(Color.WHITE);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
            p.add(lbl, BorderLayout.WEST);
            
            JButton btn = new JButton();
            btn.setBackground(current);
            btn.setPreferredSize(new Dimension(50, 20));
            btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            
            btn.addActionListener(e -> {
                Color newC = JColorChooser.showDialog(this, "Edit Palette Color", btn.getBackground());
                if (newC != null) {
                    newC = snapToGBA(newC);
                    savePaletteHistory();
                    btn.setBackground(newC);
                    colorReplacements.put(original, newC);
                    animationPreview.repaint();
                }
            });
            
            p.add(btn, BorderLayout.EAST);
            paletteContainer.add(p);
            paletteContainer.add(Box.createRigidArea(new Dimension(0, 2)));
        }
        paletteContainer.revalidate();
        paletteContainer.repaint();
    }

    private String getCategoryForIndex(int idx) {
        if (idx >= 1 && idx <= 4) return "Skin/Face";
        if (idx >= 5 && idx <= 8) return "Hair/Primary";
        if (idx >= 9 && idx <= 12) return "Armor/Secondary";
        if (idx >= 13 && idx <= 14) return "Cloth/Misc";
        if (idx == 15) return "Outline/Border";
        return "Misc";
    }

    private Color snapToGBA(Color c) {
        // GBA colors are 5-bit (0-31), scaled to 0-255 in 8-bit.
        // Formula: (value / 8) * 8
        int r = (c.getRed() / 8) * 8;
        int g = (c.getGreen() / 8) * 8;
        int b = (c.getBlue() / 8) * 8;
        return new Color(r, g, b);
    }

    private void savePaletteHistory() {
        paletteHistory.push(new java.util.HashMap<>(colorReplacements));
        if (paletteHistory.size() > 20) paletteHistory.remove(0); // Limit history
    }

    private void undoPalette() {
        if (!paletteHistory.isEmpty()) {
            colorReplacements = paletteHistory.pop();
            updatePaletteUI();
            animationPreview.repaint();
        }
    }

    private void resetPalette() {
        savePaletteHistory();
        for (Color c : detectedColors) {
            colorReplacements.put(c, c);
        }
        updatePaletteUI();
        animationPreview.repaint();
    }

    private void detectPalette(BufferedImage img) {
        if (img == null) return;
        
        // Use a set to collect unique colors, excluding background
        java.util.Set<Color> uniqueColors = new java.util.LinkedHashSet<>();
        
        // 1. First, check the first 16 pixels of the first row (standard palette strip)
        for (int x = 0; x < Math.min(img.getWidth(), 16); x++) {
            Color c = new Color(img.getRGB(x, 0), true);
            if (!isGBAGreen(c) && c.getAlpha() != 0) {
                uniqueColors.add(c);
            }
        }
        
        // 2. If we haven't found many colors, scan the whole image for actual character colors
        if (uniqueColors.size() < 5) {
            for (int y = 0; y < img.getHeight(); y += 2) { // Step by 2 for speed
                for (int x = 0; x < img.getWidth(); x += 2) {
                    Color c = new Color(img.getRGB(x, y), true);
                    if (!isGBAGreen(c) && c.getAlpha() != 0) {
                        uniqueColors.add(c);
                        if (uniqueColors.size() >= 15) break;
                    }
                }
                if (uniqueColors.size() >= 15) break;
            }
        }
        
        detectedColors.clear();
        detectedColors.addAll(uniqueColors);
        
        // Initialize replacements
        for (Color c : detectedColors) {
            colorReplacements.putIfAbsent(c, c);
        }
        
        // Pad to 15 colors so the UI slots are always full
        while (detectedColors.size() < 15) {
            Color black = new Color(0, 0, 0);
            detectedColors.add(black);
            colorReplacements.putIfAbsent(black, black);
        }
    }

    private boolean isGBAGreen(Color c) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        // Smart chroma-key check for typical animation rips
        if (g >= 220 && r <= 140 && b <= 140) return true; // FEBuilder green
        if (g > 200 && r < 80 && b < 80) return true; // Pure green
        return (Math.abs(r - 168) < 10 && Math.abs(g - 208) < 10 && Math.abs(b - 160) < 10);
    }

    private void initCenterPanel() {
        centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        
        animationPreview = new AnimationPreview();
        centerPanel.add(animationPreview);
        
        add(centerPanel, BorderLayout.CENTER);
    }
    
    public void refresh() {
        if (animationPreview != null) animationPreview.animTimer.start();
    }

    public void pause() {
        if (animationPreview != null) animationPreview.animTimer.stop();
    }

    // --- Animation Preview Component ---

    private class AnimationPreview extends JPanel {
        private static final long serialVersionUID = 1L;
        private BufferedImage[] frames;
        private int currentFrameIndex = 0;
        private Timer animTimer;
        
        // Script-based playback
        private AnimationScript currentScript;
        private int currentCommandIndex = 0;
        private int durationCounter = 0;
        private java.util.Map<String, BufferedImage> frameMap = new java.util.HashMap<>();

        // Battle Simulation Data
        private game.core.battle.BattleManager.BattleResult activeBattle;
        private int currentHitIndex = 0;

        // Enemy/Target
        private BufferedImage[] enemyFrames;
        private int enemyHp, enemyMaxHp = 50;
        private int playerHp, playerMaxHp = 40;
        private int enemyCommandIndex = 0;
        private int enemyDurationCounter = 0;

        // Visual Effects
        private int screenShakeX = 0;
        private int screenShakeY = 0;
        private int flashDuration = 0;
        private int dimDuration = 0;
        private List<DamagePopUp> damagePopUps = new ArrayList<>();
        private List<HitSpark> hitSparks = new ArrayList<>();

        private class DamagePopUp {
            int x, y, damage;
            int life = 60;
            public DamagePopUp(int x, int y, int damage) {
                this.x = x; this.y = y; this.damage = damage;
            }
        }

        private class HitSpark {
            int x, y, life = 10;
            public HitSpark(int x, int y) { this.x = x; this.y = y; }
        }

        public AnimationPreview() {
            setPreferredSize(new Dimension(800, 600));
            setBackground(Color.BLACK);
            
            playerHp = playerMaxHp;
            enemyHp = enemyMaxHp;
            
            loadFrames();
            loadEnemyFrames();
            startNewBattle();
            
            animTimer = new Timer(16, e -> { // Ticking at 60fps
                updateAnimation();
                updateVisualEffects();
                repaint();
            });
            
            // Explicitly stop timer by default (Default by Pause)
            animTimer.stop(); 
        }

        public void refresh() {
            currentFrameIndex = 0;
            currentCommandIndex = 0;
            durationCounter = 0;
            resetBattle();
            loadFrames();
            repaint();
        }

        private void updateVisualEffects() {
            if (screenShakeX != 0) screenShakeX = (screenShakeX > 0) ? -screenShakeX + 1 : -screenShakeX - 1;
            if (Math.abs(screenShakeX) < 1) screenShakeX = 0;
            
            if (screenShakeY != 0) screenShakeY = (screenShakeY > 0) ? -screenShakeY + 1 : -screenShakeY - 1;
            if (Math.abs(screenShakeY) < 1) screenShakeY = 0;

            if (flashDuration > 0) flashDuration--;
            if (dimDuration > 0) dimDuration--;
            
            damagePopUps.removeIf(p -> {
                p.y--; 
                p.life--;
                return p.life <= 0;
            });

            hitSparks.removeIf(s -> {
                s.life--;
                return s.life <= 0;
            });
        }

        public void resetBattle() {
            playerHp = playerMaxHp;
            enemyHp = enemyMaxHp;
            currentCommandIndex = 0;
            durationCounter = 0;
            currentHitIndex = 0;
            startNewBattle();
        }

        private void startNewBattle() {
            // Reset HP if someone is dead before starting
            if (playerHp <= 0 || enemyHp <= 0) {
                playerHp = playerMaxHp;
                enemyHp = enemyMaxHp;
            }
            
            game.core.battle.BattleManager.Combatant player = new game.core.battle.BattleManager.Combatant(null, "Ephraim", playerHp, playerMaxHp);
            player.attack = 18; player.defense = 8; player.speed = 14; player.skill = 12;
            player.weaponType = game.core.battle.BattleManager.WeaponType.LANCE;
            player.weaponAtk = 7; player.weaponHit = 85;
            
            game.core.battle.BattleManager.Combatant enemy = new game.core.battle.BattleManager.Combatant(null, "Knight", enemyHp, enemyMaxHp);
            enemy.attack = 12; enemy.defense = 10; enemy.speed = 4; enemy.skill = 8;
            enemy.weaponType = game.core.battle.BattleManager.WeaponType.LANCE;
            enemy.weaponAtk = 8; enemy.weaponHit = 70;
            
            activeBattle = battleManager.generateBattle(player, enemy, 1);
            currentHitIndex = 0;
            System.out.println("Battle Generated: " + activeBattle.hits.size() + " hits.");
        }

        private void updateAnimation() {
            if (activeBattle == null) startNewBattle();

            // Player Animation Logic
            if (currentScript != null && !currentScript.isEmpty()) {
                durationCounter++;
                AnimationCommand cmd = null;
                String selectedModeStr = (String) modeSelector.getSelectedItem();
                int modeNum = 1;
                if (selectedModeStr != null) {
                    try { modeNum = Integer.parseInt(selectedModeStr.split(" - ")[0]); } catch (Exception e) {}
                }
                List<AnimationCommand> cmds = currentScript.getCommands(modeNum);
                if (currentCommandIndex < cmds.size()) {
                    cmd = cmds.get(currentCommandIndex);
                }

                if (cmd != null && cmd.getType() == AnimationCommand.Type.FRAME) {
                    if (durationCounter >= cmd.getDuration()) {
                        durationCounter = 0;
                        advanceCommand();
                    }
                } else if (cmd != null && (cmd.getType() == AnimationCommand.Type.COMMAND || cmd.getType() == AnimationCommand.Type.END)) {
                    handleScriptCommand(cmd != null ? cmd.getCommandCode() : null);
                    advanceCommand();
                }
            }

            // Enemy Animation Logic
            if (enemyFrames != null && enemyFrames.length > 0) {
                enemyDurationCounter++;
                if (enemyDurationCounter >= 5) {
                    enemyDurationCounter = 0;
                    enemyCommandIndex = (enemyCommandIndex + 1) % enemyFrames.length;
                }
            }
        }

        private void handleScriptCommand(String code) {
            if (code == null) return;
            
            // FE GBA Commands
            if (code.equals("C01") || code.equals("C03") || code.equals("C07")) {
                // Potential Hit Moment
                if (currentHitIndex < activeBattle.hits.size()) {
                    game.core.battle.BattleManager.BattleHit hit = activeBattle.hits.get(currentHitIndex);
                    
                    // Show Damage PopUp on Target
                    int targetX = hit.isAttacker ? 60 : 180; 
                    damagePopUps.add(new DamagePopUp(targetX, 80, hit.damage));
                    hitSparks.add(new HitSpark(targetX, 80));
                    
                    if (hit.isAttacker) {
                        enemyHp = Math.max(0, enemyHp - hit.damage);
                        screenShakeX = 10;
                    } else {
                        playerHp = Math.max(0, playerHp - hit.damage);
                        screenShakeX = 10;
                    }
                    currentHitIndex++;
                }
            } else if (code.equals("C04")) {
                screenShakeY = 12;
            } else if (code.equals("C05") || code.equals("C06")) {
                flashDuration = 30;
                dimDuration = 40; // Dim screen for skill
            }
        }

        private void advanceCommand() {
            if (currentScript == null) return;
            String selectedModeStr = (String) modeSelector.getSelectedItem();
            int modeNum = 1;
            if (selectedModeStr != null) {
                try { modeNum = Integer.parseInt(selectedModeStr.split(" - ")[0]); } catch (Exception e) {}
            }
            List<AnimationCommand> cmds = currentScript.getCommands(modeNum);
            currentCommandIndex++;
            if (currentCommandIndex >= cmds.size()) {
                currentCommandIndex = 0;
            }
            
            // If the next command is also a COMMAND (not FRAME), execute it immediately
            AnimationCommand next = cmds.get(currentCommandIndex);
            if (next.getType() == AnimationCommand.Type.COMMAND) {
                updateAnimation();
            }
        }

        private void loadFrames() {
            String cat = (String) categorySelector.getSelectedItem();
            String unitName = (String) unitSelector.getSelectedItem();
            String weaponName = (String) weaponSelector.getSelectedItem();
            
            if (unitName == null || weaponName == null) return;
            File weaponDir = new File(ANIMS_BASE_DIR + "/" + cat + "/" + unitName + "/" + weaponName);
            if (!weaponDir.exists()) {
                System.out.println("Weapon dir not found: " + weaponDir.getAbsolutePath());
                return;
            }

            List<File> frameFiles = new ArrayList<>();
            findFramesRecursive(weaponDir, "", "", frameFiles); // search entire weapon subtree
            
            // Fallback: get all PNGs directly in weaponDir
            if (frameFiles.isEmpty()) {
                File[] files = weaponDir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
                if (files != null) for (File f : files) frameFiles.add(f);
            }
            
            // Find script.txt: walk up from first frame until we find one within weaponDir
            File scriptFile = null;
            if (!frameFiles.isEmpty()) {
                File dir = frameFiles.get(0).getParentFile();
                while (dir != null && dir.getAbsolutePath().startsWith(weaponDir.getAbsolutePath())) {
                    File candidate = new File(dir, "script.txt");
                    if (candidate.exists()) { scriptFile = candidate; break; }
                    dir = dir.getParentFile();
                }
                // Also check weaponDir itself
                if (scriptFile == null) {
                    File candidate = new File(weaponDir, "script.txt");
                    if (candidate.exists()) scriptFile = candidate;
                }
            }
            
            currentScript = null;
            frameMap.clear();
            
            if (scriptFile != null && scriptFile.exists()) {
                currentScript = new game.core.animation.AnimationScript(scriptFile);
                
                // POPULATE MODES
                updateModeSelector(currentScript);
                
                String selectedModeStr = (String) modeSelector.getSelectedItem();
                int modeNum = 1;
                if (selectedModeStr != null) {
                    try { modeNum = Integer.parseInt(selectedModeStr.split(" - ")[0]); } catch (Exception e) {}
                }
                
                List<AnimationCommand> cmds = currentScript.getCommands(modeNum);
                
                // Load only frames used in this mode
                for (AnimationCommand cmd : cmds) {
                    if (cmd.getType() == AnimationCommand.Type.FRAME) {
                        String fName = cmd.getFrameName();
                        if (!frameMap.containsKey(fName)) {
                            File fFile = new File(scriptFile.getParentFile(), fName);
                            if (fFile.exists()) {
                                try {
                                    BufferedImage img = AssetManager.getImage(fFile.getPath());
                                    frameMap.put(fName, img);
                                } catch (Exception ex) {}
                            }
                        }
                    }
                }
                
                // Populate frames array only with frames used in this mode (in order)
                List<BufferedImage> modeFrames = new ArrayList<>();
                currentModeFrameNames.clear();
                for (AnimationCommand cmd : cmds) {
                    if (cmd.getType() == AnimationCommand.Type.FRAME) {
                        String fName = cmd.getFrameName();
                        BufferedImage img = frameMap.get(fName);
                        if (img != null) {
                            modeFrames.add(img);
                            currentModeFrameNames.add(fName);
                        }
                    }
                }
                frames = modeFrames.toArray(new BufferedImage[0]);
                
                currentFrameIndex = 0;
                currentCommandIndex = 0;
                durationCounter = 0;
                
                // Palette Recognition
                if (frameMap.size() > 0) {
                    BufferedImage firstFrame = frameMap.values().iterator().next();
                    detectPalette(firstFrame);
                    updatePaletteUI();
                }
                
                updateGallery();
            } else {
                frames = null;
                updateGallery();
            }
        }

        private void saveRecoloredAnimation() {
            if (currentScript == null || frameMap.isEmpty()) return;
            
            int confirm = JOptionPane.showConfirmDialog(CharacterDesignScreen.this, 
                "This will overwrite the existing frames for this character with the new colors. Proceed?",
                "Save Recolored Animation", JOptionPane.YES_NO_OPTION);
                
            if (confirm != JOptionPane.YES_OPTION) return;

            File scriptFile = currentScript.getScriptFile();
            File dir = scriptFile.getParentFile();

            for (java.util.Map.Entry<String, BufferedImage> entry : frameMap.entrySet()) {
                String fileName = entry.getKey();
                BufferedImage original = entry.getValue();
                
                int w = original.getWidth();
                int h = original.getHeight();
                BufferedImage recolored = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int argb = original.getRGB(x, y);
                        Color originalColor = new Color(argb, true);
                        
                        Color replacement = colorReplacements.get(originalColor);
                        if (replacement != null) {
                            recolored.setRGB(x, y, replacement.getRGB());
                        } else {
                            recolored.setRGB(x, y, argb);
                        }
                    }
                }
                
                try {
                    javax.imageio.ImageIO.write(recolored, "png", new File(dir, fileName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            JOptionPane.showMessageDialog(CharacterDesignScreen.this, "Animation frames updated successfully!");
        }

        private void updateModeSelector(game.core.animation.AnimationScript script) {
            // Temporarily disable listener to avoid infinite refresh
            java.awt.event.ActionListener[] listeners = modeSelector.getActionListeners();
            for (java.awt.event.ActionListener l : listeners) modeSelector.removeActionListener(l);
            
            Object currentSelection = modeSelector.getSelectedItem();
            modeSelector.removeAllItems();
            List<Integer> availableModes = script.getAvailableModes();
            java.util.Collections.sort(availableModes);
            
            for (int m : availableModes) {
                modeSelector.addItem(getModeName(m));
            }
            
            // Try to restore selection or default to Mode 1
            if (currentSelection != null) modeSelector.setSelectedItem(currentSelection);
            if (modeSelector.getSelectedIndex() == -1 && !availableModes.isEmpty()) modeSelector.setSelectedIndex(0);
            
            for (java.awt.event.ActionListener l : listeners) modeSelector.addActionListener(l);
        }

        private String getModeName(int mode) {
            if (currentScript != null) {
                return mode + " - " + currentScript.getActionTypeForMode(mode);
            }
            return mode + " - Unknown";
        }

        private void updateGallery() {
            if (galleryPanel == null) return;
            galleryPanel.removeAll();
            if (frames != null) {
                for (int i = 0; i < frames.length; i++) {
                    BufferedImage frame = frames[i];
                    if (frame == null) continue;
                    
                    int w = frame.getWidth();
                    int h = frame.getHeight();
                    BufferedImage thumb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int argb = frame.getRGB(x, y);
                            int r = (argb >> 16) & 0xFF;
                            int g = (argb >> 8) & 0xFF;
                            int b = argb & 0xFF;
                            
                            // GBA green (168, 208, 160) with small tolerance
                            if (Math.abs(r - 168) < 5 && Math.abs(g - 208) < 5 && Math.abs(b - 160) < 5) {
                                thumb.setRGB(x, y, 0);
                            } else {
                                thumb.setRGB(x, y, argb);
                            }
                        }
                    }
                    
                    JPanel item = new JPanel();
                    item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
                    item.setOpaque(false);
                    item.setAlignmentX(Component.CENTER_ALIGNMENT);
                    
                    // Fit to width (max 240px)
                    int maxW = 240;
                    double scale = (double) maxW / w;
                    if (scale > 2.0) scale = 2.0; // Don't over-scale small sprites
                    int drawW = (int) (w * scale);
                    int drawH = (int) (h * scale);
                    
                    ImageIcon icon = new ImageIcon(thumb.getScaledInstance(drawW, drawH, Image.SCALE_SMOOTH));
                    JLabel imgLabel = new JLabel(icon);
                    imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    imgLabel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 70)));
                    
                    String fName = "Frame " + i;
                    if (i < currentModeFrameNames.size()) fName = currentModeFrameNames.get(i);
                    
                    JLabel numLabel = new JLabel(fName);
                    numLabel.setForeground(Color.GRAY);
                    numLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
                    numLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    
                    item.add(imgLabel);
                    item.add(numLabel);
                    item.add(Box.createRigidArea(new Dimension(0, 15)));
                    
                    galleryPanel.add(item);
                }
            }
            galleryPanel.revalidate();
            galleryPanel.repaint();
        }

        private void loadEnemyFrames() {
            // Load default enemy from the new Unit/Knight/lance path
            File enemyDir = new File(ANIMS_BASE_DIR + "/Unit/Knight/lance");
            if (!enemyDir.exists()) enemyDir = new File(ANIMS_BASE_DIR + "/Unit/K.Soldier/lance");
            if (!enemyDir.exists()) enemyDir = new File(ANIMS_BASE_DIR + "/Knight/lance"); // legacy fallback
            
            if (enemyDir.exists()) {
                File[] files = enemyDir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
                if (files != null) {
                    java.util.Arrays.sort(files);
                    enemyFrames = new BufferedImage[files.length];
                    for (int i = 0; i < files.length; i++) {
                        try {
                            enemyFrames[i] = AssetManager.getImage(files[i].getPath());
                        } catch (Exception e) {}
                    }
                }
            }
        }

        private void findFramesRecursive(File dir, String action, String type, List<File> results) {
            File[] files = dir.listFiles();
            if (files == null) return;

            String actionLower = action.toLowerCase();
            String typeLower = type.toLowerCase();
            
            boolean inActionDir = dir.getName().toLowerCase().contains(actionLower);
            boolean inTypeDir = type.toLowerCase().isEmpty() || dir.getName().toLowerCase().contains(typeLower);
            
            // Common abbreviations in GBA repo
            String altAction = actionLower;
            if (actionLower.equals("sword")) altAction = "swd";
            else if (actionLower.equals("lance")) altAction = "lnc";
            else if (actionLower.equals("magic")) altAction = "magi";

            boolean inAltActionDir = dir.getName().toLowerCase().contains(altAction);

            for (File f : files) {
                if (f.isDirectory()) {
                    findFramesRecursive(f, actionLower, typeLower, results);
                } else if (f.getName().toLowerCase().endsWith(".png")) {
                    String name = f.getName().toLowerCase();
                    
                    // Basic weapon match
                    boolean weaponMatch = inActionDir || inAltActionDir || name.contains(actionLower + "_") || name.contains(altAction + "_");
                    
                    if (weaponMatch) {
                        if (name.contains("sheet") || name.contains("palette") || name.contains("preview")) continue;
                        
                        boolean isCrit = name.contains("crit");
                        boolean isDodge = name.contains("dodge") || name.contains("avoid");
                        
                        if (typeLower.equals("critical")) {
                            if (isCrit || inTypeDir) results.add(f);
                        } else if (typeLower.equals("dodge")) {
                            if (isDodge || inTypeDir) results.add(f);
                        } else {
                            // Normal type - exclude crit and dodge if they are clearly marked
                            if (!isCrit && !isDodge) results.add(f);
                        }
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            
            // Create Internal GBA Buffer (248x160)
            BufferedImage buffer = new BufferedImage(248, 160, BufferedImage.TYPE_INT_ARGB);
            Graphics2D bg = buffer.createGraphics();
            
            // Background color for buffer (Fallback if bgEngine fails)
            bg.setColor(new Color(20, 20, 25));
            bg.fillRect(0, 0, 248, 160);

            // Render Tiled Background & Platform
            String selectedTerrain = (String) terrainSelector.getSelectedItem();
            bgEngine.render(bg, 0, 0, selectedTerrain);
            
            // Render Green Guidelines Platform (Layer 1.5)
            // Guidelines: Feet at 102px from top, centered for Player (150px) and Enemy (90px)
            bg.setColor(new Color(30, 120, 30)); // Darker green base
            bg.fillRect(0, 92, 248, 68);
            bg.setColor(new Color(60, 160, 60)); // Lighter green surface
            bg.fillRect(0, 94, 248, 60);
            bg.setColor(new Color(150, 255, 150, 80)); // Edge highlight
            bg.setStroke(new BasicStroke(1));
            bg.drawRect(0, 92, 248, 68);
            
            // Apply Screen Dimming
            if (dimDuration > 0) {
                bg.setColor(new Color(0, 0, 0, 160));
                bg.fillRect(0, 0, 248, 160);
            }
            
            // Draw Enemy to Buffer (Left side, facing Right)
            if (enemyFrames != null) {
                drawToBuffer(bg, enemyFrames, null, 0, enemyCommandIndex, null, 0, 0, false);
            }

            // Draw Player to Buffer (Right side, facing Left/Mirrored)
            drawToBuffer(bg, frames, currentScript, currentCommandIndex, currentFrameIndex, frameMap, 0, 0, true);
            
            // Draw Hit Sparks
            for (HitSpark s : hitSparks) {
                bg.setColor(Color.WHITE);
                int r = 10 - s.life;
                bg.drawOval(s.x - r, s.y - r, r*2, r*2);
                bg.drawLine(s.x - r*2, s.y, s.x + r*2, s.y);
                bg.drawLine(s.x, s.y - r*2, s.x, s.y + r*2);
            }

            // Draw Damage Numbers
            bg.setFont(new Font("SansSerif", Font.BOLD, 14));
            for (DamagePopUp p : damagePopUps) {
                bg.setColor(Color.WHITE);
                bg.drawString(String.valueOf(p.damage), p.x - 10, p.y);
            }
            
            bg.dispose();

            // Scale and draw buffer to component
            int scale = Math.min(getWidth() / 248, getHeight() / 160);
            if (scale < 1) scale = 1;
            
            int drawW = 248 * scale;
            int drawH = 160 * scale;
            int offX = (getWidth() - drawW) / 2;
            int offY = (getHeight() - drawH) / 2;
            
            g2.drawImage(buffer, offX, offY, drawW, drawH, null);
        }

        private void drawToBuffer(Graphics2D g, BufferedImage[] animFrames, AnimationScript script, 
                                 int cmdIdx, int frameIdx, java.util.Map<String, BufferedImage> map, 
                                 int x, int y, boolean mirror) {
            
            BufferedImage original = null;
            if (script != null && !script.isEmpty()) {
                String selectedModeStr = (String) modeSelector.getSelectedItem();
                int modeNum = 1;
                if (selectedModeStr != null) {
                    try { modeNum = Integer.parseInt(selectedModeStr.split(" - ")[0]); } catch (Exception e) {}
                }
                List<AnimationCommand> cmds = script.getCommands(modeNum);
                if (cmdIdx < cmds.size()) {
                    AnimationCommand cmd = cmds.get(cmdIdx);
                    if (cmd.getType() == AnimationCommand.Type.FRAME) {
                        original = map.get(cmd.getFrameName());
                    }
                }
            } else if (animFrames != null && animFrames.length > 0) {
                original = animFrames[frameIdx % animFrames.length];
            }

            if (original == null) return;
            
            int w = original.getWidth();
            int h = original.getHeight();
            
            // Create visible version (handle transparency)
            BufferedImage visibleImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int py = 0; py < h; py++) {
                for (int px = 0; px < w; px++) {
                    if (py < 5) { // Palette filter
                        visibleImg.setRGB(px, py, 0);
                        continue;
                    }
                    int argb = original.getRGB(px, py);
                    Color originalColor = new Color(argb, true);
                    
                    if (originalColor.getAlpha() == 0 || isGBAGreen(originalColor)) {
                        visibleImg.setRGB(px, py, 0);
                        continue;
                    }
                    
                    Color replacement = colorReplacements.get(originalColor);
                    if (replacement != null) {
                        visibleImg.setRGB(px, py, replacement.getRGB());
                    } else {
                        visibleImg.setRGB(px, py, argb);
                    }
                }
            }

            // Slice the first 248x160 from the left if the frame is oversized
            // This must happen BEFORE mirroring so both sides get the correct region
            if (w > 250) {
                int sliceW = Math.min(248, w);
                int sliceH = Math.min(160, h);
                visibleImg = visibleImg.getSubimage(0, 0, sliceW, sliceH);
                w = sliceW;
                h = sliceH;
            }

            // Anchor Bottom-Center relative to standard 248x160 canvas
            int drawX = (248 - w) / 2;
            int drawY = 160 - h;
            
            if (mirror) {
                g.drawImage(visibleImg, drawX + w, drawY, -w, h, null);
            } else {
                g.drawImage(visibleImg, drawX, drawY, w, h, null);
            }
        }
    }

    public static void main(String[] args) {
        // Bootstrap JavaFX runtime for BGM/audio if needed
        try {
            new javafx.embed.swing.JFXPanel();
            javafx.application.Platform.setImplicitExit(false);
        } catch (Throwable t) {
            System.err.println("Failed to initialize JavaFX: " + t.getMessage());
        }

        EventQueue.invokeLater(() -> {
            try {
                // Set native look and feel
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {}

                JFrame frame = new JFrame("Character Design Editor");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1920, 1080);
                frame.setLocationRelativeTo(null);

                CharacterDesignScreen screen = new CharacterDesignScreen();
                frame.setContentPane(screen);
                frame.setVisible(true);

                // Start animation/refresh if needed
                screen.refresh();

                // Play BGM and load audio
                try {
                    game.core.util.SoundManager.playBgm();
                    game.core.util.SoundManager.setBgmVolume(0.3);
                    game.core.util.SoundManager.attachToContainer(frame);
                } catch (Throwable t) {
                    System.err.println("Failed to attach SoundManager: " + t.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
