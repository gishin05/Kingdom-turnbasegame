package game.ui.screens;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import game.Main;
import game.ui.BaseScreen;
import game.ui.Theme;
import game.core.util.JsonUtil;

/**
 * Configuration screen for setting up a local multiplayer "Versus" match.
 * Allows players to select a map, toggle fog of war, set game modes,
 * and configure faction/color assignments for up to 6 players.
 */
public class VersusScreen extends BaseScreen {
    private static final long serialVersionUID = 1L;
    
    private List<MapInfo> availableMaps = new ArrayList<>();
    private List<MapInfo> filteredMaps = new ArrayList<>();
    private int mapIdx = 0;
    
    private String[] modes = {"Classic", "Timed", "Survival"};
    private int modeIdx = 0;
    
    private int fogIdx = 0;
    
    private int numPlayers = 2;
    private List<PlayerSettings> playerSettings = new ArrayList<>();
    
    private JPanel playersContainer;
    private JLabel mapValLabel;
    
    private static final String[] factions = {"Kingdom", "Empire", "Alliance", "Undead"};
    private static final Color[] DEFAULT_COLORS = {new Color(0, 162, 232), new Color(240, 60, 60), new Color(60, 200, 100), new Color(250, 210, 50), new Color(210, 80, 210), new Color(255, 140, 0)};
    private static final String[] colorNames = {"Blue", "Red", "Green", "Yellow", "Purple", "Orange"};
    
    private JPanel loadingOverlay;
    
    private Map<String, File> tilesetFiles = new HashMap<>();
    private Map<String, game.core.map.Tileset> loadedTilesets = new HashMap<>();
    private Map<String, BufferedImage> mapPreviews = new HashMap<>();
    
    public VersusScreen(Main main) {
        super(main);
        initVideoBackground(game.core.util.GamePaths.bundledResource("graphics/backgrounds/Menu_bg.mp4"));

        JLayeredPane layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Layer 0: Video background
        layeredPane.add(jfxPanel, JLayeredPane.DEFAULT_LAYER);

        // Layer 1: Dark Overlay
        JPanel overlay = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 180));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        overlay.setOpaque(false);
        layeredPane.add(overlay, JLayeredPane.PALETTE_LAYER);

        // Layer 2: UI Content
        JPanel uiPanel = new JPanel(new BorderLayout());
        uiPanel.setOpaque(false);
        layeredPane.add(uiPanel, JLayeredPane.MODAL_LAYER);
        
        // Layer 3: Loading Overlay
        loadingOverlay = new JPanel(new BorderLayout());
        loadingOverlay.setBackground(new Color(0, 0, 0, 200));
        JLabel loadLbl = new JLabel("LOADING ASSETS...", SwingConstants.CENTER);
        loadLbl.setFont(Theme.getTitleFont());
        loadLbl.setForeground(Theme.GOLD);
        loadingOverlay.add(loadLbl, BorderLayout.CENTER);
        loadingOverlay.setVisible(false);
        layeredPane.add(loadingOverlay, JLayeredPane.DRAG_LAYER);
        
        scanTilesets(game.core.util.GamePaths.TILESETS);
        initPlayerSettings();
        scanMaps();
        
        // --- TOP BAR ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(20, 40, 20, 40));
        
        JButton backBtn = styledBtn("← BACK");
        backBtn.addActionListener(e -> main.showScreen(Main.MENU));
        topBar.add(backBtn, BorderLayout.WEST);
        
        JLabel title = new JLabel("VERSUS SETUP");
        title.setFont(Theme.getTitleFont());
        title.setForeground(Theme.GOLD);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        topBar.add(title, BorderLayout.CENTER);
        
        uiPanel.add(topBar, BorderLayout.NORTH);
        
        // --- CONTENT SPLIT ---
        JPanel content = new JPanel(new GridLayout(1, 2, 20, 0));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 40, 20, 40));
        
        // LEFT: Map & Global Settings
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        JPanel mapBox = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Theme.PANEL_BG);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.GOLD_TRANS);
                g.drawRect(5, 5, getWidth()-10, getHeight()-10);
                
                if (!filteredMaps.isEmpty()) {
                    MapInfo m = filteredMaps.get(mapIdx);
                    
                    BufferedImage preview = getMapPreview(m);
                    if (preview != null) {
                        int pw = preview.getWidth();
                        int ph = preview.getHeight();
                        int bw = getWidth() - 10;
                        int bh = getHeight() - 10;
                        
                        double scale = Math.min((double)bw / pw, (double)bh / ph);
                        int sw = (int)(pw * scale);
                        int sh = (int)(ph * scale);
                        
                        int dx = 5 + (bw - sw) / 2;
                        int dy = 5 + (bh - sh) / 2;
                        
                        g.drawImage(preview, dx, dy, sw, sh, null);
                        
                        // Dark overlay on top so text is readable
                        g.setColor(new Color(0, 0, 0, 150));
                        g.fillRect(5, 5, getWidth()-10, getHeight()-10);
                    }
                    
                    g.setColor(Theme.GOLD_TRANS);
                    g.drawRect(5, 5, getWidth()-10, getHeight()-10);
                    
                    g.setColor(Color.WHITE);
                    g.setFont(Theme.getMenuFont());
                    g.drawString(m.name, 20, 40);
                    g.setFont(Theme.getSmallFont());
                    g.setColor(Color.GRAY);
                    g.drawString("Max Players: " + m.players, 20, 65);
                } else {
                    g.setColor(Theme.GOLD_TRANS);
                    g.drawRect(5, 5, getWidth()-10, getHeight()-10);
                    g.setColor(Color.RED);
                    g.drawString("NO MAPS FOUND FOR " + numPlayers + " PLAYERS", 20, 40);
                }
            }
        };
        mapBox.setPreferredSize(new Dimension(500, 280));
        mapBox.setMaximumSize(new Dimension(500, 280));
        mapBox.setBorder(BorderFactory.createLineBorder(Theme.GOLD_TRANS, 2));
        leftPanel.add(mapBox);
        leftPanel.add(Box.createVerticalStrut(20));
        
        leftPanel.add(createMapSelector());
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(createSettingSelector("BATTLE MODE", modes, modeIdx, val -> modeIdx = val));
        leftPanel.add(Box.createVerticalStrut(10));
        
        String[] playerCountOptions = {"2", "3", "4", "5", "6"};
        leftPanel.add(createSettingSelector("NUMBER OF PLAYERS", playerCountOptions, numPlayers - 2, val -> {
            numPlayers = val + 2;
            filterMaps();
            updatePlayerSettingsCount();
        }));
        leftPanel.add(Box.createVerticalStrut(10));
        
        String[] fogOptions = {"OFF", "ON"};
        leftPanel.add(createSettingSelector("FOG OF WAR", fogOptions, fogIdx, val -> fogIdx = val));
        
        content.add(leftPanel);
        
        // RIGHT: Player Settings Scrollable
        playersContainer = new JPanel();
        playersContainer.setLayout(new BoxLayout(playersContainer, BoxLayout.Y_AXIS));
        playersContainer.setOpaque(false);
        
        JScrollPane scroll = new JScrollPane(playersContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Theme.GOLD_TRANS), "PLAYER SETTINGS", 0, 0, Theme.getSmallFont(), Theme.GOLD));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        content.add(scroll);
        uiPanel.add(content, BorderLayout.CENTER);
        
        // --- BOTTOM: START ---
        JPanel bottomBar = new JPanel();
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(10, 0, 40, 0));
        
        JButton startBtn = styledBtn("START BATTLE");
        startBtn.setFont(Theme.getMenuFont());
        startBtn.setPreferredSize(new Dimension(280, 50));
        startBtn.addActionListener(e -> startBattle());
        bottomBar.add(startBtn);
        
        uiPanel.add(bottomBar, BorderLayout.SOUTH);
        
        updatePlayerSettingsUI();
 
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int w = getWidth(), h = getHeight();
                jfxPanel.setBounds(0, 0, w, h);
                overlay.setBounds(0, 0, w, h);
                uiPanel.setBounds(0, 0, w, h);
                if (loadingOverlay != null) loadingOverlay.setBounds(0, 0, w, h);
            }
        });
    }
    
    private void scanMaps() {
        availableMaps.clear();
        File dir = game.core.util.GamePaths.MAPS_VERSUS;
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(f.toPath()));
                    if (content.startsWith("{")) {
                        String name = JsonUtil.extractJsonVal(content, "name");
                        if (name.isEmpty()) name = f.getName().replace(".json", "");
                        int players = 2;
                        try { players = Integer.parseInt(JsonUtil.extractJsonVal(content, "players")); } catch (Exception e) {}
                        availableMaps.add(new MapInfo(name, players));
                    } else {
                        availableMaps.add(new MapInfo(f.getName().replace(".json", ""), 2));
                    }
                } catch (Exception ignored) {}
            }
        }
        filterMaps();
    }
    
    private void scanTilesets(File dir) {
        File[] files = dir.listFiles(); if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanTilesets(f);
            else if (f.getName().endsWith(".png")) tilesetFiles.put(f.getName().replace(".png", ""), f);
        }
    }
    
    private BufferedImage getMapPreview(MapInfo m) {
        if (mapPreviews.containsKey(m.name)) return mapPreviews.get(m.name);
        
        File path = game.core.util.GamePaths.mapVersusFileForMap(m.name);
        if (!path.exists()) return null;
        
        try {
            String json = new String(java.nio.file.Files.readAllBytes(path.toPath()));
            String tsDefault = JsonUtil.extractJsonVal(json, "tileset");
            int dataStart = json.indexOf("\"data\":");
            if (dataStart == -1) dataStart = json.indexOf("data\":");
            if (dataStart != -1) {
                int arrayStart = json.indexOf("[", dataStart);
                int depth = 0, arrayEnd = -1;
                for (int i = arrayStart; i < json.length(); i++) {
                    char ch = json.charAt(i); if (ch == '[') depth++; else if (ch == ']') depth--;
                    if (depth == 0) { arrayEnd = i; break; }
                }
                if (arrayEnd != -1) {
                    String dataPart = json.substring(arrayStart + 1, arrayEnd).trim();
                    String[] rows = dataPart.split("\\],\\s*\\[");
                    int mapH = rows.length;
                    int mapW = 0;
                    int[][] mapData = null;
                    String[][] mapTSData = null;
                    
                    for (int y = 0; y < mapH; y++) {
                        String row = rows[y].trim();
                        if (row.startsWith("[")) row = row.substring(1);
                        if (row.endsWith("]")) row = row.substring(0, row.length() - 1);
                        if (row.endsWith(",")) row = row.substring(0, row.length() - 1);
                        List<String> cells = new ArrayList<>();
                        int cdepth = 0; StringBuilder sb = new StringBuilder();
                        for (char ch : row.toCharArray()) {
                            if (ch == '{') cdepth++; else if (ch == '}') cdepth--;
                            if (ch == ',' && cdepth == 0) { cells.add(sb.toString().trim()); sb.setLength(0); } else sb.append(ch);
                        }
                        cells.add(sb.toString().trim());
                        if (y == 0) { mapW = cells.size(); mapData = new int[mapH][mapW]; mapTSData = new String[mapH][mapW]; }
                        for (int x = 0; x < Math.min(cells.size(), mapW); x++) {
                            String c = cells.get(x);
                            if (c.startsWith("{")) {
                                mapData[y][x] = JsonUtil.parseInt(JsonUtil.extractJsonVal(c, "id"), 0); mapTSData[y][x] = JsonUtil.extractJsonVal(c, "ts");
                            } else {
                                mapTSData[y][x] = tsDefault; mapData[y][x] = JsonUtil.parseInt(c.replaceAll("\"", ""), 0);
                            }
                            String ts = mapTSData[y][x];
                            if (ts != null && !ts.isEmpty() && !loadedTilesets.containsKey(ts) && tilesetFiles.containsKey(ts))
                                loadedTilesets.put(ts, new game.core.map.Tileset(ts, tilesetFiles.get(ts), 16, 16));
                        }
                    }
                    
                    if (mapW > 0 && mapH > 0) {
                        BufferedImage preview = new BufferedImage(mapW * 16, mapH * 16, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = preview.createGraphics();
                        for (int y = 0; y < mapH; y++) {
                            for (int x = 0; x < mapW; x++) {
                                String tsName = mapTSData[y][x];
                                game.core.map.Tileset ts = loadedTilesets.get(tsName);
                                if (ts != null) {
                                    BufferedImage tile = ts.getTile(mapData[y][x]);
                                    if (tile != null) g.drawImage(tile, x*16, y*16, 16, 16, null);
                                }
                            }
                        }
                        
                        int eventsStart = json.indexOf("\"events\":");
                        if (eventsStart != -1) {
                            int eArrayStart = json.indexOf("[", eventsStart);
                            int eDepth = 0, eArrayEnd = -1;
                            for (int i = eArrayStart; i < json.length(); i++) {
                                char ch = json.charAt(i); if (ch == '[') eDepth++; else if (ch == ']') eDepth--;
                                if (eDepth == 0) { eArrayEnd = i; break; }
                            }
                            if (eArrayEnd != -1) {
                                String eventsPart = json.substring(eArrayStart + 1, eArrayEnd);
                                String[] eventEntries = eventsPart.split("\\},");
                                for (String entry : eventEntries) {
                                    entry = entry.trim(); if (entry.isEmpty()) continue;
                                    if (!entry.startsWith("{")) entry = "{" + entry; if (!entry.endsWith("}")) entry = entry + "}";
                                    int ex = JsonUtil.parseInt(JsonUtil.extractJsonVal(entry, "x"), -1);
                                    int ey = JsonUtil.parseInt(JsonUtil.extractJsonVal(entry, "y"), -1);
                                    int eo = JsonUtil.parseInt(JsonUtil.extractJsonVal(entry, "owner"), -1);
                                    if (ex != -1) {
                                        Color c = new Color(150, 150, 150);
                                        if (eo >= 0 && eo < DEFAULT_COLORS.length) c = DEFAULT_COLORS[eo];
                                        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
                                        g.fillRect(ex*16 + 2, ey*16 + 2, 12, 12);
                                        g.setColor(Color.WHITE);
                                        g.drawRect(ex*16 + 2, ey*16 + 2, 12, 12);
                                    }
                                }
                            }
                        }
                        
                        g.dispose();
                        mapPreviews.put(m.name, preview);
                        return preview;
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        
        return null;
    }
    
    private void filterMaps() {
        filteredMaps.clear();
        for (MapInfo m : availableMaps) {
            if (m.players >= numPlayers) filteredMaps.add(m);
        }
        mapIdx = 0;
        if (mapValLabel != null) {
            mapValLabel.setText(!filteredMaps.isEmpty() ? filteredMaps.get(0).name : "NONE");
        }
        repaint();
    }
    
    private void initPlayerSettings() {
        playerSettings.clear();
        for (int i = 0; i < 6; i++) playerSettings.add(new PlayerSettings(i));
    }
    
    private void updatePlayerSettingsCount() { updatePlayerSettingsUI(); }
    
    private void updatePlayerSettingsUI() {
        playersContainer.removeAll();
        for (int i = 0; i < numPlayers; i++) {
            playersContainer.add(createPlayerPanel(playerSettings.get(i)));
            playersContainer.add(Box.createVerticalStrut(10));
        }
        playersContainer.revalidate();
        playersContainer.repaint();
    }
    
    private JPanel createPlayerPanel(PlayerSettings ps) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.PANEL_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ps.color, 1),
            new EmptyBorder(10, 15, 10, 15)
        ));
        p.setMaximumSize(new Dimension(500, 120));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lbl = new JLabel("PLAYER " + (ps.index + 1));
        lbl.setFont(Theme.getSmallFont().deriveFont(Font.BOLD));
        lbl.setForeground(ps.color);
        p.add(lbl, gbc);
        
        gbc.gridx = 1;
        JComboBox<String> colorBox = new JComboBox<>(colorNames);
        colorBox.setFont(Theme.getSmallFont());
        colorBox.setSelectedIndex(findColorIndex(ps.color));
        colorBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                int idx = index >= 0 ? index : colorBox.getSelectedIndex();
                if (idx >= 0 && idx < DEFAULT_COLORS.length) l.setForeground(DEFAULT_COLORS[idx]);
                return l;
            }
        });
        colorBox.addActionListener(e -> {
            int idx = colorBox.getSelectedIndex();
            if (idx >= 0 && idx < DEFAULT_COLORS.length) {
                ps.color = DEFAULT_COLORS[idx];
                updatePlayerSettingsUI();
            }
        });
        p.add(colorBox, gbc);
        
        gbc.gridy = 1; gbc.gridx = 0;
        p.add(new JLabel("FACTION") {{ setFont(Theme.getSmallFont()); setForeground(Color.GRAY); }}, gbc);
        
        gbc.gridx = 1;
        JComboBox<String> factionBox = new JComboBox<>(factions);
        factionBox.setSelectedIndex(ps.factionIdx);
        factionBox.setFont(Theme.getSmallFont());
        factionBox.addActionListener(e -> ps.factionIdx = factionBox.getSelectedIndex());
        p.add(factionBox, gbc);
        
        gbc.gridx = 2;
        p.add(new JLabel("GOLD") {{ setFont(Theme.getSmallFont()); setForeground(Color.GRAY); }}, gbc);
        
        gbc.gridx = 3;
        JSpinner goldSpin = new JSpinner(new SpinnerNumberModel(ps.gold, 0, 10000, 100));
        goldSpin.setFont(Theme.getSmallFont());
        goldSpin.addChangeListener(e -> ps.gold = (int) goldSpin.getValue());
        p.add(goldSpin, gbc);


        
        gbc.gridx = 4;
        JButton colorBtn = styledBtn("🎨");
        colorBtn.setMargin(new Insets(2, 6, 2, 6));
        colorBtn.addActionListener(e -> {
            ps.color = DEFAULT_COLORS[(ps.index + 1) % DEFAULT_COLORS.length];
            updatePlayerSettingsUI();
        });
        p.add(colorBtn, gbc);
        
        return p;
    }
    
    private JPanel createMapSelector() {
        JPanel wrap = new JPanel(new BorderLayout(10, 2));
        wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(400, 70));
        
        JLabel lbl = new JLabel("SELECT MAP");
        lbl.setFont(Theme.getSmallFont().deriveFont(Font.BOLD));
        lbl.setForeground(Theme.GOLD_TRANS);
        wrap.add(lbl, BorderLayout.NORTH);
        
        JPanel ctr = new JPanel(new BorderLayout(10, 0));
        ctr.setOpaque(false);
        
        JButton l = styledBtn("◀");
        JButton r = styledBtn("▶");
        
        mapValLabel = new JLabel(!filteredMaps.isEmpty() ? filteredMaps.get(mapIdx).name : "NONE");
        mapValLabel.setFont(Theme.getMenuFont());
        mapValLabel.setForeground(Color.WHITE);
        mapValLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        l.addActionListener(e -> {
            if (filteredMaps.isEmpty()) return;
            mapIdx = (mapIdx - 1 + filteredMaps.size()) % filteredMaps.size();
            mapValLabel.setText(filteredMaps.get(mapIdx).name);
            repaint();
        });
        
        r.addActionListener(e -> {
            if (filteredMaps.isEmpty()) return;
            mapIdx = (mapIdx + 1) % filteredMaps.size();
            mapValLabel.setText(filteredMaps.get(mapIdx).name);
            repaint();
        });
        
        ctr.add(l, BorderLayout.WEST);
        ctr.add(mapValLabel, BorderLayout.CENTER);
        ctr.add(r, BorderLayout.EAST);
        
        wrap.add(ctr, BorderLayout.CENTER);
        return wrap;
    }
    
    private JPanel createSettingSelector(String label, String[] options, int current, java.util.function.Consumer<Integer> callback) {
        JPanel wrap = new JPanel(new BorderLayout(10, 2));
        wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(400, 70));
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.getSmallFont().deriveFont(Font.BOLD));
        lbl.setForeground(Theme.GOLD_TRANS);
        wrap.add(lbl, BorderLayout.NORTH);
        
        JPanel ctr = new JPanel(new BorderLayout(10, 0));
        ctr.setOpaque(false);
        
        JButton l = styledBtn("◀");
        JButton r = styledBtn("▶");
        
        JLabel val = new JLabel(options[current]);
        val.setFont(Theme.getMenuFont());
        val.setForeground(Color.WHITE);
        val.setHorizontalAlignment(SwingConstants.CENTER);
        
        l.addActionListener(e -> {
            int idx = 0;
            for(int i=0; i<options.length; i++) if(options[i].equals(val.getText())) idx = i;
            int next = (idx - 1 + options.length) % options.length;
            callback.accept(next);
            val.setText(options[next]);
            repaint();
        });
        
        r.addActionListener(e -> {
            int idx = 0;
            for(int i=0; i<options.length; i++) if(options[i].equals(val.getText())) idx = i;
            int next = (idx + 1) % options.length;
            callback.accept(next);
            val.setText(options[next]);
            repaint();
        });
        
        ctr.add(l, BorderLayout.WEST);
        ctr.add(val, BorderLayout.CENTER);
        ctr.add(r, BorderLayout.EAST);
        
        wrap.add(ctr, BorderLayout.CENTER);
        return wrap;
    }
    
    private JButton styledBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.getSmallFont());
        b.setForeground(Theme.TEXT_PRIMARY);
        b.setBackground(new Color(40, 40, 60));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.GOLD_TRANS),
            new EmptyBorder(6, 12, 6, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    
    private void startBattle() {
        if (filteredMaps.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a valid map first!");
            return;
        }
        MapInfo m = filteredMaps.get(mapIdx);
        
        // Setup the gameplay screen
        VersusGameplayScreen gameplay = null;
        for (java.awt.Component comp : main.getContentPane().getComponents()) {
            if (comp instanceof VersusGameplayScreen) {
                gameplay = (VersusGameplayScreen) comp;
                break;
            }
        }
        
        if (gameplay != null) {
            final VersusGameplayScreen gp = gameplay;
            List<PlayerSettings> activePlayers = new ArrayList<>(playerSettings.subList(0, numPlayers));
            loadingOverlay.setVisible(true);
            
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    gp.fogOfWarEnabled = (fogIdx == 1);
                    gp.setupGame(game.core.util.GamePaths.mapVersusFileForMap(m.name).getPath(), activePlayers);
                    return null;
                }
                @Override protected void done() {
                    loadingOverlay.setVisible(false);
                    main.showScreen(Main.VERSUS_GAMEPLAY);
                }
            };
            worker.execute();
        } else {
            JOptionPane.showMessageDialog(this, "Error: VersusGameplayScreen not found!");
        }
    }
    
    @Override public void refresh() { super.refresh(); scanMaps(); }
    
    public static class PlayerSettings {
        public int index;
        public int factionIdx = 0;
        public Color color;
        public int gold = 1000;
        PlayerSettings(int i) { this.index = i; this.color = DEFAULT_COLORS[i % DEFAULT_COLORS.length]; }
        public PlayerSettings(int i, Color c) { this.index = i; this.color = (c != null ? c : DEFAULT_COLORS[i % DEFAULT_COLORS.length]); }
    }

    public class MapInfo {
        public String name;
        public int players;
        MapInfo(String n, int p) { this.name = n; this.players = p; }
    }

    private int findColorIndex(Color c) {
        if (c == null) return 0;
        for (int i = 0; i < DEFAULT_COLORS.length; i++) {
            if (DEFAULT_COLORS[i].getRGB() == c.getRGB()) return i;
        }
        return 0;
    }
}
