package game.ui.editors; import game.Main; import game.Main.Refreshable;

import game.ui.Theme;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import game.core.map.TileEntry;
import game.core.map.Tileset;
import game.core.util.JsonUtil;

/**
 * A comprehensive level editor interface allowing users to create, modify,
 * and save custom battle maps. Features tile painting, event placement (HQs, forts),
 * and dynamic flood-fill capabilities.
 */
public class MapDesignScreen extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    public static final int TILE_SRC = 16;
    public static final int TILE_RENDER = 32;

    private enum Tool { PENCIL, ERASER, FILL, PICKER, EVENT }
    private enum EventType { NONE, HQ, ARMORY, HOUSE, FORT, AERIE }
    
    private static class EventInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        EventType type;
        int owner;
        EventInfo(EventType t, int o) { type = t; owner = o; }
    }

    private int mapW = 15, mapH = 10;
    private TileEntry[][] mapData;
    private Map<String, Tileset> tilesets = new LinkedHashMap<>();
    private String selectedTilesetName = null;
    private int selectedTileId = 0;
    private Tool currentTool = Tool.PENCIL;
    private boolean showGrid = true;
    private String mapName = "";
    private JPanel canvasPanel;
    private JScrollPane varScroll;
    private TilesetViewer tilesetViewer;
    private JLabel statusLabel;
    
    private JTextField nameInput;
    private JTextArea descInput;
    private JSpinner playerInput;
    private int maxPlayers = 2;
    private String mapDescription = "";
    private JScrollPane canvasScroll;
    private Font pixelFont, pixelFontSm;
    private Point hoveredCell = new Point(-1, -1);

    private List<String> paletteGroups = new ArrayList<>();
    private int currentGroupIdx = 0;

    private boolean showMovable = false;
    private boolean showGridHighlighter = false;
    private double zoomScale = 1.0;

    private String terrainUnitType = "Land Unit";

    private static class MapState {
        TileEntry[][] data;
        Map<Point, EventInfo> events;
        MapState(TileEntry[][] d, Map<Point, EventInfo> e) {
            this.data = new TileEntry[d.length][d[0].length];
            for (int i = 0; i < d.length; i++) {
                for (int j = 0; j < d[i].length; j++) {
                    if (d[i][j] != null) {
                        this.data[i][j] = new TileEntry(d[i][j].tileId, d[i][j].flipX, d[i][j].flipY, d[i][j].paletteId);
                        this.data[i][j].tilesetName = d[i][j].tilesetName;
                    }
                }
            }
            this.events = new HashMap<>();
            for (Map.Entry<Point, EventInfo> entry : e.entrySet()) {
                this.events.put(new Point(entry.getKey()), new EventInfo(entry.getValue().type, entry.getValue().owner));
            }
        }
    }
    private Stack<MapState> undoStack = new Stack<>();
    private void pushUndo() {
        if (undoStack.size() > 50) undoStack.remove(0);
        undoStack.push(new MapState(mapData, eventMap));
    }

    private Map<Point, EventInfo> eventMap = new HashMap<>();
    private EventType selectedEventType = EventType.HQ;
    private int selectedEventOwner = 0;
    private float animPhase = 0;
    private javax.swing.Timer animTimer;
    private Color[] playerColors = {
        new Color(60, 120, 255),  // Blue
        new Color(255, 60, 60),   // Red
        new Color(60, 220, 60),   // Green
        new Color(255, 220, 0),   // Yellow
        new Color(200, 60, 255),  // Purple
        new Color(0, 230, 230)    // Cyan
    };

    public MapDesignScreen(Main main) {
        setLayout(new BorderLayout());
        setBackground(new Color(20, 20, 30));
        
        // Initialize status label early to avoid NPE in updateStatus()
        statusLabel = new JLabel("  Initializing...");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        loadFonts();
        loadAllTiles();
        selectedTileId = 0;
        initMap(mapW, mapH);

        // TOP TOOLBAR
        JPanel toolbar = createToolbar(main);
        add(toolbar, BorderLayout.NORTH);

        // CENTER - MAP CANVAS
        canvasPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawCanvas((Graphics2D) g);
            }
            @Override public Dimension getPreferredSize() {
                return new Dimension((int)(mapW * TILE_RENDER * zoomScale), (int)(mapH * TILE_RENDER * zoomScale));
            }
        };
        canvasPanel.setBackground(new Color(30, 30, 45));
        setupCanvasListeners();
        
        animTimer = new javax.swing.Timer(50, e -> {
            animPhase += 0.2f;
            canvasPanel.repaint();
        });
        animTimer.start();

        canvasScroll = new JScrollPane(canvasPanel);
        canvasScroll.setBackground(new Color(20, 20, 30));
        canvasScroll.getViewport().setBackground(new Color(30, 30, 45));
        canvasScroll.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0, 60)));
        
        // RIGHT - TOOLS & PROPS
        JComponent rightPanel = createRightPanel();

        // SPLIT PANES
        JSplitPane innerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvasScroll, rightPanel);
        innerSplit.setResizeWeight(1.0);
        innerSplit.setDividerSize(6);
        innerSplit.setBorder(null);
        innerSplit.setBackground(new Color(15, 15, 25));

        // LEFT - TILE PALETTE (Stored in variable for JSplitPane)
        JPanel leftPanel = createTilePalette();

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, innerSplit);
        mainSplit.setDividerLocation(250);
        mainSplit.setDividerSize(6);
        mainSplit.setBorder(null);
        mainSplit.setBackground(new Color(15, 15, 25));

        add(mainSplit, BorderLayout.CENTER);

        // BOTTOM - STATUS (Final styling)
        statusLabel.setFont(pixelFontSm);
        statusLabel.setForeground(new Color(180, 180, 200));
        statusLabel.setBackground(new Color(10, 10, 18));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(new EmptyBorder(6, 12, 6, 12));
        add(statusLabel, BorderLayout.SOUTH);
        
        updateStatus();
    }

    private void loadFonts() {
        pixelFont = Theme.getPixelFont(18f);
        pixelFontSm = Theme.getPixelFont(12f);
    }

    private void loadAllTiles() {
        tilesets.clear();
        paletteGroups.clear();
        tilesetImageCache.clear();

        File tsDir = game.core.util.GamePaths.TILESETS;
        if (tsDir.exists()) {
            scanTilesetsRecursive(tsDir);
        }

        if (!paletteGroups.isEmpty()) {
            currentGroupIdx = 0;
            selectedTilesetName = paletteGroups.get(0);
            loadTilesetAsGroup(selectedTilesetName);
        } else {
            selectedTilesetName = "DEFAULT";
        }
    }

    private Map<String, File> tilesetFiles = new HashMap<>();

    private void scanTilesetsRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File f : files) {
            if (f.isDirectory()) {
                scanTilesetsRecursive(f);
            } else if (f.getName().toLowerCase().endsWith(".png")) {
                String gName = f.getName().replace(".png", "");
                paletteGroups.add(gName);
                tilesetFiles.put(gName, f);
            }
        }
        Collections.sort(paletteGroups);
    }

    private Map<String, BufferedImage> tilesetImageCache = new HashMap<>();

    private void loadTilesetAsGroup(String gName) {
        if (tilesets.containsKey(gName)) return; 

        File file = tilesetFiles.get(gName);
        if (file == null || !file.exists()) return;

        try {
            Tileset ts = new Tileset(gName, file, 16, 16);
            tilesets.put(gName, ts);
            tilesetImageCache.put(gName, ts.getFullImage());
        } catch (Exception ignored) {}
    }

    private void initMap(int w, int h) {
        mapW = w; mapH = h;
        mapData = new TileEntry[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                mapData[i][j] = new TileEntry(0);
                mapData[i][j].tilesetName = "DEFAULT";
            }
        }
    }

    // ── TOOLBAR ──
    private JPanel createToolbar(Main main) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(10, 10, 18));
        bar.setBorder(new EmptyBorder(8, 16, 8, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JButton backBtn = styledBtn("← BACK");
        backBtn.addActionListener(e -> main.showScreen(Main.DESIGN_ROOM));
        left.add(backBtn);

        JLabel title = new JLabel("MAP DESIGNER");
        title.setFont(pixelFont.deriveFont(24f));
        title.setForeground(new Color(255, 215, 0));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton newBtn = styledBtn("NEW");
        newBtn.addActionListener(e -> showNewMapDialog());
        JButton undoBtn = styledBtn("↶ UNDO");
        undoBtn.addActionListener(e -> undo());
        right.add(undoBtn);
        JButton saveBtn = styledBtn("SAVE MAP");
        saveBtn.addActionListener(e -> saveMap());
        JButton loadBtn = styledBtn("LOAD");
        loadBtn.addActionListener(e -> loadMap());
        JButton tilesetBtn = styledBtn("TILESET");
        tilesetBtn.addActionListener(e -> loadTileset());
        JButton gridBtn = styledBtn("GRID");
        gridBtn.addActionListener(e -> { showGrid = !showGrid; canvasPanel.repaint(); });
        JButton importBtn = styledBtn("IMPORT");
        importBtn.addActionListener(e -> importMapImage());
        JButton exportBinBtn = styledBtn("EXPORT BIN");
        exportBinBtn.addActionListener(e -> exportTSA());
        right.add(newBtn); right.add(saveBtn); right.add(loadBtn); right.add(tilesetBtn); right.add(gridBtn); right.add(importBtn); right.add(exportBinBtn);

        bar.add(left, BorderLayout.WEST);
        bar.add(title, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JButton styledBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(pixelFontSm);
        b.setForeground(new Color(220, 220, 240));
        b.setBackground(new Color(40, 40, 60));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 215, 0, 80)),
            new EmptyBorder(6, 14, 6, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(60, 50, 20)); }
            public void mouseExited(MouseEvent e) { b.setBackground(new Color(40, 40, 60)); }
        });
        return b;
    }

    // ── TILE PALETTE ──
    private JPanel createTilePalette() {
        JPanel palette = new JPanel(new BorderLayout());
        palette.setPreferredSize(new Dimension(250, 0));
        palette.setBackground(new Color(15, 15, 25));
        palette.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(255, 215, 0, 60)),
            new EmptyBorder(0, 0, 0, 0)));

        // TOP: CATEGORY SELECTION
        JPanel selectorPanel = new JPanel(new BorderLayout(4, 0));
        selectorPanel.setBackground(new Color(10, 10, 18));
        selectorPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JComboBox<String> categoryCombo = new JComboBox<>(paletteGroups.toArray(new String[0]));
        categoryCombo.setSelectedItem(selectedTilesetName);
        categoryCombo.setFont(pixelFontSm);
        categoryCombo.setBackground(new Color(40, 40, 60));
        categoryCombo.setForeground(new Color(255, 230, 100));
        categoryCombo.addActionListener(e -> {
            selectedTilesetName = (String) categoryCombo.getSelectedItem();
            currentGroupIdx = paletteGroups.indexOf(selectedTilesetName);
            updateVariants();
        });

        // Up/Down Buttons
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 2, 0));
        btnPanel.setOpaque(false);
        
        JButton upBtn = styledBtn("▲");
        upBtn.setMargin(new Insets(2, 4, 2, 4));
        upBtn.addActionListener(e -> {
            cycleGroup(-1);
            categoryCombo.setSelectedItem(selectedTilesetName);
        });
        
        JButton downBtn = styledBtn("▼");
        downBtn.setMargin(new Insets(2, 4, 2, 4));
        downBtn.addActionListener(e -> {
            cycleGroup(1);
            categoryCombo.setSelectedItem(selectedTilesetName);
        });
        
        btnPanel.add(upBtn);
        btnPanel.add(downBtn);

        selectorPanel.add(categoryCombo, BorderLayout.CENTER);
        selectorPanel.add(btnPanel, BorderLayout.EAST);

        palette.add(selectorPanel, BorderLayout.NORTH);

        // CENTER: TILESET VIEWER
        tilesetViewer = new TilesetViewer();
        varScroll = new JScrollPane(tilesetViewer);
        varScroll.setBorder(null);
        varScroll.setBackground(new Color(20, 20, 32));
        varScroll.getVerticalScrollBar().setUnitIncrement(16);
        varScroll.getHorizontalScrollBar().setUnitIncrement(16);
        palette.add(varScroll, BorderLayout.CENTER);

        updateVariants();
        return palette;
    }

    private class TilesetViewer extends JPanel {
        private static final long serialVersionUID = 1L;
        public TilesetViewer() {
            setBackground(new Color(20, 20, 32));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Tileset tsObj = tilesets.get(selectedTilesetName);
                    BufferedImage ts = (tsObj != null) ? tsObj.getFullImage() : null;
                    if (ts == null) return;
                    
                    int tw = tsObj.getTileWidth();
                    int th = tsObj.getTileHeight();
                    int tx = e.getX() / tw;
                    int ty = e.getY() / th;
                    int cols = ts.getWidth() / tw;
                    int rows = ts.getHeight() / th;
                    
                    if (tx >= 0 && tx < cols && ty >= 0 && ty < rows) {
                        selectedTileId = ty * cols + tx;
                        currentTool = Tool.PENCIL;
                        repaint();
                        updateStatus();
                    } else {
                        selectedTileId = 0;
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Tileset tsObj = tilesets.get(selectedTilesetName);
            BufferedImage ts = (tsObj != null) ? tsObj.getFullImage() : null;
            if (ts == null) return;
            
            int tw = tsObj.getTileWidth();
            int th = tsObj.getTileHeight();
            
            g.drawImage(ts, 0, 0, null);
            
            // Draw Grid overlay
            g.setColor(new Color(255, 255, 255, 20));
            for (int x = 0; x <= ts.getWidth(); x += tw) g.drawLine(x, 0, x, ts.getHeight());
            for (int y = 0; y <= ts.getHeight(); y += th) g.drawLine(0, y, ts.getWidth(), y);
            
            // Draw Selection
            if (selectedTilesetName != null) {
                int cols = ts.getWidth() / tw;
                int tx = selectedTileId % cols;
                int ty = selectedTileId / cols;
                
                g.setColor(new Color(255, 215, 0));
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(tx * tw, ty * th, tw, th);
                
                // Highlight effect
                g2.setColor(new Color(255, 215, 0, 40));
                g2.fillRect(tx * tw, ty * th, tw, th);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            BufferedImage ts = tilesetImageCache.get(selectedTilesetName);
            if (ts != null) return new Dimension(ts.getWidth(), ts.getHeight());
            return new Dimension(200, 200);
        }
    }

    private void cycleGroup(int dir) {
        if (paletteGroups.isEmpty()) return;
        currentGroupIdx += dir;
        if (currentGroupIdx < 0) currentGroupIdx = paletteGroups.size() - 1;
        if (currentGroupIdx >= paletteGroups.size()) currentGroupIdx = 0;
        
        selectedTilesetName = paletteGroups.get(currentGroupIdx);
        selectedTileId = 0; // Reset to first tile of new tileset
        updateVariants();
        updateStatus();
    }

    private void updateVariants() {
        if (!tilesets.containsKey(selectedTilesetName)) {
            loadTilesetAsGroup(selectedTilesetName);
        }
        tilesetViewer.revalidate();
        tilesetViewer.repaint();
    }

    // ── RIGHT PANEL ──
    private JComponent createRightPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(15, 15, 25));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(255, 215, 0, 60)),
            new EmptyBorder(12, 12, 12, 12)));

        JPanel eventContainer = new JPanel();
        eventContainer.setLayout(new BoxLayout(eventContainer, BoxLayout.Y_AXIS));
        eventContainer.setOpaque(false);
        eventContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        eventContainer.setVisible(false);

        eventContainer.add(Box.createVerticalStrut(20));
        addSectionLabel(eventContainer, "EVENT PLACER");
        eventContainer.add(Box.createVerticalStrut(8));

        JPanel eventToolPanel = new JPanel(new GridLayout(0, 2, 4, 4));
        eventToolPanel.setOpaque(false);
        eventToolPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        
        java.util.function.Function<String, JButton> eventBtn = text -> {
            JButton b = styledBtn(text);
            b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 215, 0, 80)),
                new EmptyBorder(6, 6, 6, 6)));
            return b;
        };

        JButton hqBtn = eventBtn.apply("HQ");
        hqBtn.addActionListener(e -> { selectedEventType = EventType.HQ; updateStatus(); });
        JButton armoryBtn = eventBtn.apply("ARMORY");
        armoryBtn.addActionListener(e -> { selectedEventType = EventType.ARMORY; updateStatus(); });
        JButton houseBtn = eventBtn.apply("HOUSE");
        houseBtn.addActionListener(e -> { selectedEventType = EventType.HOUSE; updateStatus(); });
        JButton fortBtn = eventBtn.apply("FORT");
        fortBtn.addActionListener(e -> { selectedEventType = EventType.FORT; updateStatus(); });
        JButton aerieBtn = eventBtn.apply("AERIE");
        aerieBtn.addActionListener(e -> { selectedEventType = EventType.AERIE; updateStatus(); });
        eventToolPanel.add(hqBtn);
        eventToolPanel.add(armoryBtn);
        eventToolPanel.add(houseBtn);
        eventToolPanel.add(fortBtn);
        eventToolPanel.add(aerieBtn);
        eventToolPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        eventContainer.add(eventToolPanel);

        eventContainer.add(Box.createVerticalStrut(8));
        addInputLabel(eventContainer, "OWNER:");
        String[] pChoices = {"P1", "P2", "P3", "P4", "P5", "P6"};
        JComboBox<String> ownerBox = new JComboBox<>(pChoices);
        styleInput(ownerBox);
        ownerBox.addActionListener(e -> selectedEventOwner = ownerBox.getSelectedIndex());
        ownerBox.setSelectedIndex(-1);
        eventContainer.add(ownerBox);

        p.add(Box.createVerticalStrut(16));
        addSectionLabel(p, "TOOLS");
        p.add(Box.createVerticalStrut(8));
        for (Tool t : Tool.values()) {
            String icon;
            switch (t) {
                case PENCIL: icon = "✏ PENCIL"; break;
                case ERASER: icon = "⌫ ERASER"; break;
                case FILL:   icon = "◆ FILL"; break;
                case PICKER: icon = "◉ PICKER"; break;
                case EVENT:  icon = "⚐ EVENT"; break;
                default: icon = t.name();
            }
            JButton tb = styledBtn(icon);
            tb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            tb.setAlignmentX(Component.LEFT_ALIGNMENT);
            tb.addActionListener(e -> { 
                currentTool = t; 
                eventContainer.setVisible(t == Tool.EVENT);
                p.revalidate();
                p.repaint();
                updateStatus(); 
            });
            p.add(tb);
            p.add(Box.createVerticalStrut(4));
            if (t == Tool.EVENT) {
                p.add(eventContainer);
            }
        }

        p.add(Box.createVerticalStrut(20));
        addSectionLabel(p, "MAP INFO");
        p.add(Box.createVerticalStrut(8));

        JButton sizeBtn = styledBtn("Resize Map: " + mapW + "x" + mapH);
        sizeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        sizeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        sizeBtn.addActionListener(e -> showResizeDialog());
        p.add(sizeBtn);
        p.putClientProperty("sizeBtn", sizeBtn);

        p.add(Box.createVerticalStrut(12));
        addInputLabel(p, "NAME:");
        nameInput = new JTextField("");
        styleInput(nameInput);
        p.add(nameInput);

        p.add(Box.createVerticalStrut(12));
        addInputLabel(p, "DESCRIPTION:");
        descInput = new JTextArea("", 3, 20);
        descInput.setLineWrap(true);
        descInput.setWrapStyleWord(true);
        styleInput(descInput);
        JScrollPane descScroll = new JScrollPane(descInput);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0, 40)));
        p.add(descScroll);

        p.add(Box.createVerticalStrut(12));
        addInputLabel(p, "MAX PLAYERS:");
        playerInput = new JSpinner(new SpinnerNumberModel(maxPlayers, 1, 6, 1));
        styleInput(playerInput);
        p.add(playerInput);

        p.add(Box.createVerticalStrut(4));
        JToggleButton moveBtn = new JToggleButton("Show Movable Tiles");
        moveBtn.setFont(pixelFontSm);
        moveBtn.setForeground(new Color(220, 220, 240));
        moveBtn.setBackground(new Color(40, 40, 60));
        moveBtn.setFocusPainted(false);
        moveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        moveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        moveBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 215, 0, 80)),
            new EmptyBorder(6, 14, 6, 14)));
        moveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        moveBtn.addActionListener(e -> {
            showMovable = moveBtn.isSelected();
            moveBtn.setBackground(showMovable ? new Color(60, 80, 120) : new Color(40, 40, 60));
            canvasPanel.repaint();
        });
        p.add(moveBtn);

        p.add(Box.createVerticalStrut(4));
        JToggleButton gridHlBtn = new JToggleButton("Grid Highlighter");
        gridHlBtn.setFont(pixelFontSm);
        gridHlBtn.setForeground(new Color(220, 220, 240));
        gridHlBtn.setBackground(new Color(40, 40, 60));
        gridHlBtn.setFocusPainted(false);
        gridHlBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        gridHlBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        gridHlBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 215, 0, 80)),
            new EmptyBorder(6, 14, 6, 14)));
        gridHlBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gridHlBtn.addActionListener(e -> {
            showGridHighlighter = gridHlBtn.isSelected();
            gridHlBtn.setBackground(showGridHighlighter ? new Color(120, 100, 40) : new Color(40, 40, 60));
            canvasPanel.repaint();
        });
        p.add(gridHlBtn);

        p.add(Box.createVerticalStrut(4));
        JButton resetZoomBtn = styledBtn("Reset Zoom (100%)");
        resetZoomBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        resetZoomBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetZoomBtn.addActionListener(e -> {
            zoomScale = 1.0;
            canvasPanel.revalidate();
            canvasPanel.repaint();
        });
        p.add(resetZoomBtn);

        p.add(Box.createVerticalStrut(20));
        addSectionLabel(p, "PATH PREVIEW");
        p.add(Box.createVerticalStrut(8));
        
        addInputLabel(p, "PREVIEW FOR:");
        JComboBox<String> uTypeBox = new JComboBox<>(new String[]{"Land Unit", "Ocean Unit", "Air Unit"});
        styleInput(uTypeBox);
        uTypeBox.addActionListener(e -> { terrainUnitType = (String) uTypeBox.getSelectedItem(); canvasPanel.repaint(); });
        p.add(uTypeBox);

        p.add(Box.createVerticalGlue());
        
        JScrollPane scroll = new JScrollPane(p);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(15, 15, 25));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(260, 800));
        return scroll;
    }

    private void addInputLabel(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setFont(pixelFontSm.deriveFont(Font.BOLD, 10f));
        l.setForeground(new Color(150, 150, 180));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 4, 2, 0));
        p.add(l);
    }

    private void styleInput(JComponent c) {
        c.setFont(pixelFontSm);
        c.setForeground(Color.WHITE);
        c.setBackground(new Color(30, 30, 45));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (c instanceof JTextField || c instanceof JTextArea) {
            ((javax.swing.text.JTextComponent)c).setCaretColor(Color.YELLOW);
        }
    }

    private void addSectionLabel(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setFont(pixelFont.deriveFont(14f));
        l.setForeground(new Color(255, 215, 0, 180));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        l.setBorder(new EmptyBorder(0, 0, 4, 0));
        p.add(l);
    }

    // ── CANVAS ──
    private void setupCanvasListeners() {
        MouseAdapter ma = new MouseAdapter() {
            private void handle(MouseEvent e) {
                int tx = (int) (e.getX() / (TILE_RENDER * zoomScale));
                int ty = (int) (e.getY() / (TILE_RENDER * zoomScale));
                if (tx < 0 || tx >= mapW || ty < 0 || ty >= mapH) return;

                if (currentTool == Tool.EVENT) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        Point p = new Point(tx, ty);
                        EventInfo existing = eventMap.get(p);
                        if (existing == null || existing.type != selectedEventType || existing.owner != selectedEventOwner) {
                            pushUndo();
                            eventMap.put(p, new EventInfo(selectedEventType, selectedEventOwner));
                        }
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        Point p = new Point(tx, ty);
                        if (eventMap.containsKey(p)) {
                            pushUndo();
                            eventMap.remove(p);
                        }
                    }
                    canvasPanel.repaint();
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (currentTool == Tool.PENCIL) {
                        if (mapData[ty][tx].tileId != selectedTileId || !mapData[ty][tx].tilesetName.equals(selectedTilesetName)) {
                            pushUndo();
                            mapData[ty][tx].tileId = selectedTileId;
                            mapData[ty][tx].tilesetName = selectedTilesetName;
                        }
                    } else if (currentTool == Tool.ERASER) {
                        if (mapData[ty][tx].tileId != 0) {
                            pushUndo();
                            mapData[ty][tx].tileId = 0;
                            mapData[ty][tx].tilesetName = "DEFAULT";
                        }
                    } else if (currentTool == Tool.FILL) {
                        pushUndo();
                        floodFill(tx, ty, mapData[ty][tx].tileId, selectedTileId, selectedTilesetName);
                    } else if (currentTool == Tool.PICKER) {
                        selectedTileId = mapData[ty][tx].tileId;
                        selectedTilesetName = mapData[ty][tx].tilesetName;
                        currentTool = Tool.PENCIL;
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    if (mapData[ty][tx].tileId != 0 || eventMap.containsKey(new Point(tx, ty))) {
                        pushUndo();
                        mapData[ty][tx].tileId = 0;
                        mapData[ty][tx].tilesetName = "DEFAULT";
                        eventMap.remove(new Point(tx, ty));
                    }
                }
                canvasPanel.repaint();
                updateStatus();
            }
            public void mousePressed(MouseEvent e) { handle(e); }
            public void mouseDragged(MouseEvent e) { handle(e); updateHover(e); }
            public void mouseMoved(MouseEvent e) { updateHover(e); }
            public void mouseExited(MouseEvent e) { hoveredCell.setLocation(-1, -1); canvasPanel.repaint(); }
        };
        canvasPanel.addMouseListener(ma);
        canvasPanel.addMouseMotionListener(ma);
        canvasPanel.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                double oldScale = zoomScale;
                if (e.getWheelRotation() < 0) zoomScale *= 1.1;
                else zoomScale /= 1.1;
                zoomScale = Math.max(0.2, Math.min(zoomScale, 4.0));
                
                // Adjust scroll to keep mouse centered
                if (oldScale != zoomScale) {
                    canvasPanel.revalidate();
                    canvasPanel.repaint();
                }
            }
        });
    }

    private void updateHover(MouseEvent e) {
        int gx = (int) (e.getX() / (TILE_RENDER * zoomScale));
        int gy = (int) (e.getY() / (TILE_RENDER * zoomScale));
        if (gx >= 0 && gx < mapW && gy >= 0 && gy < mapH) {
            hoveredCell.setLocation(gx, gy);
            updateStatus();
        }
        canvasPanel.repaint();
    }

    private void floodFill(int x, int y, int target, int replacement, String newTilesetName) {
        if (target == replacement && mapData[y][x].tilesetName.equals(newTilesetName)) return;
        if (x < 0 || x >= mapW || y < 0 || y >= mapH) return;
        if (mapData[y][x].tileId != target) return;
        
        mapData[y][x].tileId = replacement;
        mapData[y][x].tilesetName = newTilesetName;
        
        floodFill(x + 1, y, target, replacement, newTilesetName);
        floodFill(x - 1, y, target, replacement, newTilesetName);
        floodFill(x, y + 1, target, replacement, newTilesetName);
        floodFill(x, y - 1, target, replacement, newTilesetName);
    }

    private void drawCanvas(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        java.awt.geom.AffineTransform oldAt = g.getTransform();
        g.scale(zoomScale, zoomScale);

        for (int y = 0; y < mapH; y++) {
            for (int x = 0; x < mapW; x++) {
                TileEntry entry = mapData[y][x];
                String tsName = (entry == null || entry.tilesetName == null) ? "DEFAULT" : entry.tilesetName;
                Tileset ts = tilesets.get(tsName);
                if (ts != null) {
                    int id = (entry == null) ? 0 : entry.tileId;
                    BufferedImage tile = ts.getTile(id);
                    if (tile != null) {
                        g.drawImage(tile, x * TILE_RENDER, y * TILE_RENDER, TILE_RENDER, TILE_RENDER, null);
                    }
                }
                
                // Draw Event
                EventInfo ev = eventMap.get(new Point(x, y));
                if (ev != null) drawEventFlag(g, x * TILE_RENDER, y * TILE_RENDER, ev);

                if (showMovable) {
                    if (entry != null && entry.tileId != 0) {
                        int cost = 1;
                        String label = "PLAIN";
                        if (ts != null) {
                            Tileset.TerrainProperty prop = ts.getTerrain(entry.tileId);
                            if (prop != null) {
                                cost = prop.moveCosts.getOrDefault(terrainUnitType, 1);
                                label = prop.type;
                            }
                        }
                        
                        if (cost == -1) {
                            // Impassable for this unit type: draw a red blocked overlay with a white cross
                            g.setColor(new Color(255, 0, 0, 100)); // Red blocked tint
                            g.fillRect(x * TILE_RENDER, y * TILE_RENDER, TILE_RENDER, TILE_RENDER);
                            
                            g.setColor(new Color(255, 255, 255, 180));
                            g.setStroke(new BasicStroke(1.5f));
                            g.drawLine(x * TILE_RENDER + 8, y * TILE_RENDER + 8, (x+1) * TILE_RENDER - 8, (y+1) * TILE_RENDER - 8);
                            g.drawLine((x+1) * TILE_RENDER - 8, y * TILE_RENDER + 8, x * TILE_RENDER + 8, (y+1) * TILE_RENDER - 8);
                        } else {
                            // Movable for this unit type: draw a blue tint
                            g.setColor(new Color(60, 120, 255, 60)); // Blue movement tint
                            g.fillRect(x * TILE_RENDER, y * TILE_RENDER, TILE_RENDER, TILE_RENDER);
                            
                            // Draw the terrain type label
                            g.setColor(new Color(0, 255, 100, 150));
                            g.setFont(pixelFontSm.deriveFont(8f));
                            String shortLabel = label.length() > 6 ? label.substring(0, 6) : label;
                            g.drawString(shortLabel, x * TILE_RENDER + 2, y * TILE_RENDER + 28);
                        }
                    }
                }
            }
        }
        if (showGrid) {
            if (showGridHighlighter) {
                g.setColor(new Color(255, 215, 0, 100));
                g.setStroke(new BasicStroke((float)(1.5f / zoomScale)));
            } else {
                g.setColor(new Color(255, 255, 255, 20));
                g.setStroke(new BasicStroke((float)(1.0f / zoomScale)));
            }
            for (int x = 0; x <= mapW; x++) g.drawLine(x * TILE_RENDER, 0, x * TILE_RENDER, mapH * TILE_RENDER);
            for (int y = 0; y <= mapH; y++) g.drawLine(0, y * TILE_RENDER, mapW * TILE_RENDER, y * TILE_RENDER);
            
            if (showGridHighlighter && hoveredCell.x >= 0) {
                g.setColor(new Color(255, 215, 0, 150));
                g.setStroke(new BasicStroke((float)(2.0f / zoomScale)));
                g.drawRect(hoveredCell.x * TILE_RENDER, hoveredCell.y * TILE_RENDER, TILE_RENDER, TILE_RENDER);
                g.setColor(new Color(255, 215, 0, 40));
                g.fillRect(hoveredCell.x * TILE_RENDER, hoveredCell.y * TILE_RENDER, TILE_RENDER, TILE_RENDER);
            }
        }
        
        g.setTransform(oldAt);
    }

    private void drawEventFlag(Graphics2D g, int x, int y, EventInfo ev) {
        Color pc = (ev.owner >= 0) ? playerColors[ev.owner % playerColors.length] : new Color(150, 150, 150);
        float wave = (float) Math.sin(animPhase + (x + y) * 0.1) * 3f;
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Pole
        g.setColor(new Color(180, 180, 180));
        g.fillRect(x + 6, y + 4, 2, 24);
        
        if (ev.type == EventType.HQ) {
            // Animated Banner
            int[] px = {x + 8, x + 24, (int)(x + 20 + wave), x + 24, x + 8};
            int[] py = {y + 4, y + 4, y + 12, y + 20, y + 20};
            g.setColor(pc);
            g.fillPolygon(px, py, 5);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1f));
            g.drawPolygon(px, py, 5);
            
            // Crown Design
            g.setColor(new Color(255, 215, 0));
            g.fillOval(x + 12, y + 8, 6, 4);
            g.fillRect(x + 13, y + 6, 4, 2);
        } else if (ev.type == EventType.HOUSE) {
            // House / Village icon
            g.setColor(pc);
            // Roof (triangle)
            int[] rx = {x + 6, x + 16, x + 26};
            int[] ry = {y + 14, y + 4, y + 14};
            g.fillPolygon(rx, ry, 3);
            // Walls
            g.fillRect(x + 8, y + 14, 16, 12);
            // Door
            g.setColor(new Color(60, 40, 20));
            g.fillRect(x + 13, y + 18, 6, 8);
            // Window
            g.setColor(new Color(255, 255, 150));
            g.fillRect(x + 10, y + 16, 3, 3);
            // Outline
            g.setColor(new Color(255, 255, 255, 100));
            g.drawPolygon(rx, ry, 3);
            g.drawRect(x + 8, y + 14, 16, 12);
        } else {
            // Simple Flag base design (ARMORY, FORT, AERIE)
            int[] px = {x + 8, x + 24, (int)(x + 20 + wave), x + 8};
            int[] py = {y + 4, y + 6, y + 12, y + 18};
            g.setColor(pc);
            g.fillPolygon(px, py, 4);
            g.setColor(new Color(255, 255, 255, 100));
            g.drawPolygon(px, py, 4);
            
            if (ev.type == EventType.FORT) {
                // Ship logo on flag
                g.setColor(Color.WHITE);
                g.fillRect(x + 12, y + 11, 8, 3); // Hull
                g.fillRect(x + 15, y + 8, 2, 3);  // Mast
            } else if (ev.type == EventType.AERIE) {
                // Wings logo on flag
                g.setColor(Color.WHITE);
                int[] wx = {x + 11, x + 13, x + 15, x + 17, x + 19};
                int[] wy = {y + 10, y + 8, y + 12, y + 8, y + 10};
                g.setStroke(new BasicStroke(1.5f));
                g.drawPolyline(wx, wy, 5);
                g.setStroke(new BasicStroke(1f));
            }
        }
    }

    // ── DIALOGS ──
    private void showNewMapDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        JTextField nameField = new JTextField(mapName);
        JSpinner wSpin = new JSpinner(new SpinnerNumberModel(15, 5, 100, 1));
        JSpinner hSpin = new JSpinner(new SpinnerNumberModel(10, 5, 100, 1));
        panel.add(new JLabel("Map Name:")); panel.add(nameField);
        panel.add(new JLabel("Width:")); panel.add(wSpin);
        panel.add(new JLabel("Height:")); panel.add(hSpin);
        int r = JOptionPane.showConfirmDialog(this, panel, "New Map", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            mapName = nameField.getText().trim();
            if (mapName.isEmpty()) mapName = "Untitled";
            initMap((int) wSpin.getValue(), (int) hSpin.getValue());
            updateSizeBtn();
            canvasPanel.revalidate();
            canvasPanel.repaint();
            updateStatus();
        }
    }

    private void showResizeDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        JSpinner wSpin = new JSpinner(new SpinnerNumberModel(mapW, 5, 100, 1));
        JSpinner hSpin = new JSpinner(new SpinnerNumberModel(mapH, 5, 100, 1));
        panel.add(new JLabel("New Width:")); panel.add(wSpin);
        panel.add(new JLabel("New Height:")); panel.add(hSpin);
        int r = JOptionPane.showConfirmDialog(this, panel, "Resize Map", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            int nw = (int) wSpin.getValue();
            int nh = (int) hSpin.getValue();
            TileEntry[][] newData = new TileEntry[nh][nw];
            for (int y = 0; y < nh; y++) {
                for (int x = 0; x < nw; x++) {
                    if (y < mapH && x < mapW) newData[y][x] = mapData[y][x];
                    else newData[y][x] = new TileEntry(0);
                }
            }
            mapW = nw; mapH = nh;
            mapData = newData;
            updateSizeBtn();
            canvasPanel.revalidate();
            canvasPanel.repaint();
            updateStatus();
        }
    }
    
    private void updateSizeBtn() {
        Component[] comps = getComponents();
        for (Component c : comps) {
            if (c instanceof JPanel) {
                JPanel p = (JPanel) c;
                JButton sb = (JButton) p.getClientProperty("sizeBtn");
                if (sb != null) sb.setText("Resize Map: " + mapW + "x" + mapH);
            }
        }
    }

    private void saveMap() {
        // Validate: check for null/empty tiles
        int emptyCount = 0;
        for (int y = 0; y < mapH; y++) {
            for (int x = 0; x < mapW; x++) {
                if (mapData[y][x] == null) emptyCount++;
            }
        }
        if (emptyCount > 0) {
            JOptionPane.showMessageDialog(this,
                "Cannot save! " + emptyCount + " tile(s) are still empty.\nFill all tiles before saving.",
                "Incomplete Map", JOptionPane.ERROR_MESSAGE);
            return;
        }

        mapName = nameInput.getText().trim();
        mapDescription = descInput.getText().trim();
        maxPlayers = (int) playerInput.getValue();

        JFileChooser fc = new JFileChooser(game.core.util.GamePaths.MAPS_VERSUS.getPath());
        fc.setFileFilter(new FileNameExtensionFilter("JSON Map", "json"));
        fc.setSelectedFile(new File(mapName + ".json"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
                pw.println("{");
                pw.println("  \"name\": \"" + mapName + "\",");
                pw.println("  \"description\": \"" + mapDescription.replace("\"", "\\\"") + "\",");
                pw.println("  \"players\": " + maxPlayers + ",");
                pw.println("  \"events\": [");
                List<Point> keys = new ArrayList<>(eventMap.keySet());
                for (int i = 0; i < keys.size(); i++) {
                    Point p = keys.get(i);
                    EventInfo ev = eventMap.get(p);
                    pw.print("    {\"x\":" + p.x + ", \"y\":" + p.y + ", \"type\":\"" + ev.type + "\", \"owner\":" + ev.owner + "}");
                    if (i < keys.size() - 1) pw.println(","); else pw.println();
                }
                pw.println("  ],");
                pw.println("  \"tileset\": \"" + selectedTilesetName + "\",");
                pw.println("  \"data\": [");
                for (int y = 0; y < mapH; y++) {
                    pw.print("    [");
                    for (int x = 0; x < mapW; x++) {
                        TileEntry te = mapData[y][x];
                        pw.print("{\"id\":" + te.tileId + ",\"ts\":\"" + te.tilesetName + "\"}");
                        if (x < mapW - 1) pw.print(",");
                    }
                    pw.print("]");
                    if (y < mapH - 1) pw.println(","); else pw.println();
                }
                pw.println("  ]");
                pw.println("}");
                JOptionPane.showMessageDialog(this, "Map saved!", "Save", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportTSA() {
        JFileChooser fc = new JFileChooser(game.core.util.GamePaths.MAPS_VERSUS.getPath());
        fc.setFileFilter(new FileNameExtensionFilter("Binary TSA", "bin"));
        fc.setSelectedFile(new File(mapName + ".bin"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.DataOutputStream dos = new java.io.DataOutputStream(new java.io.FileOutputStream(fc.getSelectedFile()))) {
                // Header: Width (1 byte), Height (1 byte)
                dos.writeByte(mapW);
                dos.writeByte(mapH);
                
                // Data: Grid of shorts (TSA)
                for (int y = 0; y < mapH; y++) {
                    for (int x = 0; x < mapW; x++) {
                        dos.writeShort(mapData[y][x].toShort());
                    }
                }
                JOptionPane.showMessageDialog(this, "Exported binary TSA successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        }
    }

    private void loadTileset() {
        JFileChooser fc = new JFileChooser(game.core.util.GamePaths.TILESETS.getPath());
        fc.setDialogTitle("Select Tileset PNG");
        fc.setFileFilter(new FileNameExtensionFilter("Tileset Image", "png", "jpg", "bmp"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage ts = ImageIO.read(fc.getSelectedFile());
                if (ts == null) return;
                
                int cols = ts.getWidth() / TILE_SRC;
                int rows = ts.getHeight() / TILE_SRC;
                
                if (cols == 0 || rows == 0) {
                    JOptionPane.showMessageDialog(this, "Tileset too small.");
                    return;
                }

                String tsName = "TS_" + System.currentTimeMillis();
                try {
                    Tileset tsObj = new Tileset(tsName, fc.getSelectedFile());
                    tilesets.put(tsName, tsObj);
                    tilesetImageCache.put(tsName, tsObj.getFullImage());
                    if (!paletteGroups.contains(tsName)) paletteGroups.add(0, tsName);
                    selectedTilesetName = tsName;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                updateVariants();
                updateStatus();
                
                JOptionPane.showMessageDialog(this, "Loaded Tileset: " + cols + "x" + rows);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading tileset: " + ex.getMessage());
            }
        }
    }

    private void loadMap() {
        JFileChooser fc = new JFileChooser(game.core.util.GamePaths.MAPS_VERSUS.getPath());
        fc.setFileFilter(new FileNameExtensionFilter("JSON Map", "json"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(fc.getSelectedFile().toPath()));
                content = content.trim();
                
                List<TileEntry[]> parsed = new ArrayList<>();
                
                if (content.startsWith("{")) {
                    mapName = JsonUtil.extractJsonVal(content, "name");
                    mapDescription = JsonUtil.extractJsonVal(content, "description");
                    maxPlayers = JsonUtil.parseInt(JsonUtil.extractJsonVal(content, "players"), maxPlayers);
                    
                    String tsName = JsonUtil.extractJsonVal(content, "tileset");
                    if (!tsName.isEmpty()) {
                        selectedTilesetName = tsName;
                        loadTilesetAsGroup(tsName);
                    }

                    eventMap.clear();
                    String eventPart = JsonUtil.extractJsonVal(content, "events");
                    if (!eventPart.isEmpty()) {
                        String[] items = eventPart.split("\\},\\s*\\{");
                        for (String item : items) {
                            try {
                                int ex = JsonUtil.parseInt(JsonUtil.extractJsonVal(item, "x"), 0);
                                int ey = JsonUtil.parseInt(JsonUtil.extractJsonVal(item, "y"), 0);
                                String et = JsonUtil.extractJsonVal(item, "type");
                                if ("BASE".equals(et)) et = "ARMORY";
                                int eo = JsonUtil.parseInt(JsonUtil.extractJsonVal(item, "owner"), 0);
                                eventMap.put(new Point(ex, ey), new EventInfo(EventType.valueOf(et), eo));
                            } catch (Exception ignored) {}
                        }
                    }

                    String dataKey = content.contains("\"mapData\":") ? "mapData" : "data";
                    int dataStart = content.indexOf("\"" + dataKey + "\":");
                    if (dataStart == -1) throw new Exception("No map data found in JSON.");
                    
                    String dataPart = content.substring(dataStart + dataKey.length() + 4);
                    dataPart = dataPart.substring(dataPart.indexOf("[") + 1, dataPart.lastIndexOf("]")).trim();
                    String[] rows = dataPart.split("\\],\\s*\\[");
                    for (String row : rows) {
                        row = row.replaceAll("[\\[\\]\n\r]", "").trim();
                        if (row.isEmpty()) continue;
                        String[] cells;
                        if (row.contains("{")) {
                            cells = row.split("(?<=\\}),\\s*");
                        } else {
                            cells = row.split(",");
                        }
                        TileEntry[] entries = new TileEntry[cells.length];
                        for (int i = 0; i < cells.length; i++) {
                            String cellStr = cells[i].trim();
                            if (cellStr.startsWith("{")) {
                                int id = JsonUtil.parseInt(JsonUtil.extractJsonVal(cellStr, "id"), 0);
                                String ts = JsonUtil.extractJsonVal(cellStr, "ts");
                                entries[i] = new TileEntry(id);
                                entries[i].tilesetName = ts.isEmpty() ? selectedTilesetName : ts;
                                if (!ts.isEmpty()) loadTilesetAsGroup(ts);
                            } else {
                                // Legacy support
                                entries[i] = new TileEntry(JsonUtil.parseInt(cellStr.replaceAll("\"", ""), 0));
                                entries[i].tilesetName = selectedTilesetName;
                            }
                        }
                        parsed.add(entries);
                    }
                    
                    nameInput.setText(mapName);
                    descInput.setText(mapDescription);
                    playerInput.setValue(maxPlayers);
                }

                if (!parsed.isEmpty()) {
                    mapH = parsed.size();
                    mapW = parsed.get(0).length;
                    mapData = parsed.toArray(new TileEntry[0][]);
                    updateSizeBtn();
                    canvasPanel.revalidate();
                    canvasPanel.repaint();
                    updateStatus();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void importMapImage() {
        JFileChooser fc = new JFileChooser(game.core.util.GamePaths.DATA_ROOT.getPath());
        fc.setFileFilter(new FileNameExtensionFilter("Image Files", "png", "jpg", "jpeg", "bmp"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage img = ImageIO.read(fc.getSelectedFile());
                if (img == null) return;
                
                int iw = img.getWidth();
                int ih = img.getHeight();
                
                // Custom Import Dialog
                JPanel importPanel = new JPanel(new GridLayout(4, 2, 8, 8));
                JSpinner sizeSpin = new JSpinner(new SpinnerNumberModel(16, 8, 64, 8));
                JSpinner xOffSpin = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
                JSpinner yOffSpin = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
                importPanel.add(new JLabel("Tile Size:")); importPanel.add(sizeSpin);
                importPanel.add(new JLabel("X Offset:")); importPanel.add(xOffSpin);
                importPanel.add(new JLabel("Y Offset:")); importPanel.add(yOffSpin);
                importPanel.add(new JLabel("Resize Map?"));
                JCheckBox resizeCheck = new JCheckBox("", true);
                importPanel.add(resizeCheck);

                int result = JOptionPane.showConfirmDialog(this, importPanel, "Import Settings", JOptionPane.OK_CANCEL_OPTION);
                if (result != JOptionPane.OK_OPTION) return;

                int tSize = (int) sizeSpin.getValue();
                int offX = (int) xOffSpin.getValue();
                int offY = (int) yOffSpin.getValue();
                boolean doResize = resizeCheck.isSelected();

                int cols = (iw - offX) / tSize;
                int rows = (ih - offY) / tSize;
                
                if (cols <= 0 || rows <= 0) {
                    JOptionPane.showMessageDialog(this, "Settings result in 0 tiles. Check offsets and size.");
                    return;
                }

                if (doResize) {
                    pushUndo();
                    mapW = cols; mapH = rows;
                    mapData = new TileEntry[mapH][mapW];
                    for (int y = 0; y < mapH; y++) {
                        for (int x = 0; x < mapW; x++) {
                            mapData[y][x] = new TileEntry(0);
                            mapData[y][x].tilesetName = "DEFAULT";
                        }
                    }
                }
                
                String tsName = "IMP_" + System.currentTimeMillis();
                try {
                    // Create imported directory if it doesn't exist
                    File importedDir = game.core.util.GamePaths.TILESETS_IMPORTED;
                    if (!importedDir.exists()) importedDir.mkdirs();
                    
                    File tsFile = new File(importedDir, tsName + ".png");
                    ImageIO.write(img, "png", tsFile);
                    
                    Tileset tsObj = new Tileset(tsName, tsFile, tSize, tSize);
                    tilesets.put(tsName, tsObj);
                    tilesetFiles.put(tsName, tsFile);
                    tilesetImageCache.put(tsName, tsObj.getFullImage());
                    
                    // Add to palette groups so it stays available
                    if (!paletteGroups.contains(tsName)) paletteGroups.add(1, tsName);
                    
                    for (int y = 0; y < Math.min(rows, mapH); y++) {
                        for (int x = 0; x < Math.min(cols, mapW); x++) {
                            TileEntry entry = new TileEntry(y * cols + x);
                            entry.tilesetName = tsName;
                            mapData[y][x] = entry;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                updateVariants();
                updateSizeBtn();
                canvasPanel.revalidate();
                canvasPanel.repaint();
                updateStatus();
                
                JOptionPane.showMessageDialog(this, "Successfully imported tiles.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateStatus() {
        String pos = hoveredCell.x >= 0 ? "(" + hoveredCell.x + "," + hoveredCell.y + ")" : "--";
        statusLabel.setText("  Map: " + mapName + " | Size: " + mapW + "x" + mapH +
            " | Tool: " + currentTool + " | Tileset: " + selectedTilesetName + " | Pos: " + pos);
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        MapState state = undoStack.pop();
        
        // Restore dimensions if they changed (not implemented yet but good practice)
        if (state.data.length != mapH || state.data[0].length != mapW) {
            mapH = state.data.length;
            mapW = state.data[0].length;
            updateSizeBtn();
        }
        
        mapData = state.data;
        eventMap = state.events;
        
        canvasPanel.revalidate();
        canvasPanel.repaint();
        updateStatus();
    }

    @Override public void refresh() { revalidate(); repaint(); }
    @Override public void pause() {}
}
