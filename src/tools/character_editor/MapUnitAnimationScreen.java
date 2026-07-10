package tools.character_editor;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.EventQueue;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import game.core.animation.AnimationPreviewer;
import game.ui.util.ImportProgressDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.io.File;

import game.core.util.GamePaths;

public class MapUnitAnimationScreen extends JPanel  {

    private static final long serialVersionUID = 1L;
    
    private JPanel leftPanel;
    private JPanel centerPanel;
    private JPanel rightPanel;
    private JPanel galleryPanel;
    private PixelEditor pixelEditor;
    private AnimationPreviewer previewer;
    private Color selectedColor = Color.WHITE;
    private JPanel paletteSection;
    private JTextField txtName;
    private JComboBox<String> comboCategory;
    private JComboBox<String> comboAction;
    private static final String[] ACTIONS = {"Standing", "Walk_Down", "Walk_Up", "Walk_Side", "Selected"};
    private enum Tool { PENCIL, BUCKET, ERASER, ERASER_ALL }
    private Tool currentTool = Tool.PENCIL;
    private int selectedFrameIndex = -1;
    private boolean isLoading = false;


    private static class ScriptItem {
        boolean isCommand;
        String text;
        int duration = 5;
        
        ScriptItem(boolean isCommand, String text) {
            this.isCommand = isCommand;
            this.text = text;
        }
    }
    private List<ScriptItem> scriptItems = new ArrayList<>();
    private java.util.Map<String, BufferedImage> imageCache = new java.util.HashMap<>();

    // Palette colors
    private Color colorHair = new Color(224, 216, 64);
    private Color colorSkin = new Color(248, 208, 152);
    private Color colorPrimary = new Color(100, 100, 100);
    private Color colorSecondary = new Color(247, 173, 82);
    private Color colorCloth = new Color(82, 82, 115);
    private Color colorLeather = new Color(148, 100, 66);
    private Color colorBorder = new Color(40, 30, 40);

    public MapUnitAnimationScreen() {

        
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        initToolbar();
        initLeftPanel();
        initRightPanel();
        initCenterPanel();
    }

    private void initToolbar() {
        JPanel topToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topToolbar.setOpaque(false);

        JButton btnBack = new JButton("Back");
        btnBack.addActionListener(e -> { Window w = SwingUtilities.getWindowAncestor(btnBack); if (w != null) w.dispose(); });

        JButton btnSave = new JButton("Save Animation");
        btnSave.addActionListener(e -> saveAnimation());
        JButton btnLoad = new JButton("Load Animation");
        btnLoad.addActionListener(e -> selectAndLoadAnimation());

        JButton btnClear = new JButton("Clear Canvas");
        btnClear.addActionListener(e -> pixelEditor.clear());

        JButton btnImport = new JButton("Import Media");
        btnImport.addActionListener(e -> importImage());

        // Size Selector
        JLabel lblSize = new JLabel(" Canvas Size: ");
        lblSize.setForeground(Color.WHITE);
        String[] sizes = {"32x32", "32x64", "64x64"};
        JComboBox<String> sizeBox = new JComboBox<>(sizes);
        sizeBox.addActionListener(e -> {
            String s = (String) sizeBox.getSelectedItem();
            if (s.equals("32x32")) pixelEditor.setCanvasSize(32, 32);
            else if (s.equals("32x64")) pixelEditor.setCanvasSize(32, 64);
            else if (s.equals("64x64")) pixelEditor.setCanvasSize(64, 64);
        });

        // Zoom Slider
        JLabel lblZoom = new JLabel("  Zoom: ");
        lblZoom.setForeground(Color.WHITE);
        JSlider zoomSlider = new JSlider(1, 40, 15);
        zoomSlider.setOpaque(false);
        zoomSlider.setPreferredSize(new Dimension(150, 20));
        zoomSlider.addChangeListener(e -> {
            if (pixelEditor != null) {
                pixelEditor.setZoom(zoomSlider.getValue());
            }
        });

        topToolbar.add(btnBack);
        topToolbar.add(new JSeparator(JSeparator.VERTICAL));
        topToolbar.add(btnLoad);
        topToolbar.add(btnImport);
        topToolbar.add(btnSave);
        topToolbar.add(btnClear);
        topToolbar.add(Box.createRigidArea(new Dimension(10, 0)));
        topToolbar.add(lblSize);
        topToolbar.add(sizeBox);
        topToolbar.add(lblZoom);
        topToolbar.add(zoomSlider);

        // Speed Slider
        JLabel lblSpeed = new JLabel("  Speed: ");
        lblSpeed.setForeground(Color.WHITE);
        JSlider speedSlider = new JSlider(1, 20, 4);
        speedSlider.setOpaque(false);
        speedSlider.setInverted(true); // Lower delay = faster speed
        speedSlider.setPreferredSize(new Dimension(100, 20));
        speedSlider.addChangeListener(e -> {
            if (previewer != null) {
                previewer.setFrameDelay(speedSlider.getValue());
            }
        });
        topToolbar.add(lblSpeed);
        topToolbar.add(speedSlider);

        JButton btnTest = new JButton("Test");
        btnTest.setBackground(new Color(60, 120, 255));
        btnTest.setForeground(Color.WHITE);
        btnTest.addActionListener(e -> { javax.swing.JOptionPane.showMessageDialog(this, "Unit Testing is not available in standalone mode."); });
        topToolbar.add(Box.createRigidArea(new Dimension(10, 0)));
        topToolbar.add(btnTest);

        add(topToolbar, BorderLayout.NORTH);
    }

    private void importImage() {
        String category = (String) comboCategory.getSelectedItem();
        String charName = txtName.getText().trim();
        if (category == null || charName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Name and select a Category before importing.", "Missing Info", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser chooser = new JFileChooser(GamePaths.UNITS);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedImage img = javax.imageio.ImageIO.read(file);
                if (img != null) {
                    JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
                    JSpinner wSpin = new JSpinner(new SpinnerNumberModel(pixelEditor.canvasWidth, 8, 256, 1));
                    JSpinner hSpin = new JSpinner(new SpinnerNumberModel(pixelEditor.canvasHeight, 8, 256, 1));
                    JSpinner startSpin = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
                    JSpinner endSpin = new JSpinner(new SpinnerNumberModel(99, 0, 1000, 1));
                    JCheckBox resizeCheck = new JCheckBox("Resize Canvas?", true);
                    
                    panel.add(new JLabel("Frame Width:")); panel.add(wSpin);
                    panel.add(new JLabel("Frame Height:")); panel.add(hSpin);
                    panel.add(new JLabel("Start Frame Index:")); panel.add(startSpin);
                    panel.add(new JLabel("End Frame Index:")); panel.add(endSpin);
                    panel.add(new JLabel("")); panel.add(resizeCheck);
                    
                    int result = JOptionPane.showConfirmDialog(this, panel, "Import Settings", JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        int fw = (int)wSpin.getValue();
                        int fh = (int)hSpin.getValue();
                        int start = (int)startSpin.getValue();
                        int end = (int)endSpin.getValue();
                        
                        if (resizeCheck.isSelected()) {
                            pixelEditor.setCanvasSize(fw, fh);
                        }
                        processImportedImage(img, "Imported", fw, fh, start, end);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error importing image: " + e.getMessage());
            }
        }
    }

    private void processImportedImage(BufferedImage fullImg, String namePrefix, int fw, int fh, int start, int end) {
        if (fullImg == null) return;
        
        int cols = fullImg.getWidth() / fw;
        int rows = fullImg.getHeight() / fh;
        
        int cw = pixelEditor.canvasWidth;
        int ch = pixelEditor.canvasHeight;

        int totalFrames = cols * rows;
        if (totalFrames == 0) {
            // If image is smaller than slice, just use it as one frame
            BufferedImage frame = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frame.createGraphics();
            g.drawImage(fullImg, (cw - fullImg.getWidth()) / 2, ch - fullImg.getHeight(), null);
            g.dispose();
            frame = processGreenAndShadow(frame);
            addFrameToCache(frame, namePrefix);
            updateGallery();
            updatePreviewer();
        } else {
            int validCount = Math.min(end, totalFrames - 1) - start + 1;
            if (validCount <= 0) validCount = 1;
            final int totalValid = validCount;
            ImportProgressDialog.run(this, "Importing Frames", updater -> {
                int frameIdx = 0;
                int processed = 0;
                for (int y = 0; y < rows; y++) {
                    for (int x = 0; x < cols; x++) {
                        if (frameIdx >= start && frameIdx <= end) {
                            processed++;
                            updater.update(processed, totalValid, "Frame " + processed + " / " + totalValid);
                            BufferedImage sub = fullImg.getSubimage(x * fw, y * fh, fw, fh);
                            
                            if (fw != cw || fh != ch) {
                                BufferedImage centered = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
                                Graphics2D g = centered.createGraphics();
                                g.drawImage(sub, (cw - fw) / 2, ch - fh, null);
                                g.dispose();
                                sub = centered;
                            }
                            
                            sub = processGreenAndShadow(sub);
                            final BufferedImage fSub = sub;
                            try {
                                SwingUtilities.invokeAndWait(() -> addFrameToCache(fSub, namePrefix));
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                        frameIdx++;
                    }
                }
            }, () -> {
                updateGallery();
                updatePreviewer();
            });
        }
    }

    private BufferedImage processGreenAndShadow(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                if (a == 0) continue;
                
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                
                // Classic FE bright green -> Transparent
                if (g > 200 && r < 150 && b < 150 && Math.abs(r - b) < 30) {
                    dst.setRGB(x, y, 0x00000000);
                } 
                // Classic FE dark green -> Semi-transparent black shadow
                else if (g > 100 && g < 180 && r < 120 && b < 120 && Math.abs(r - b) < 30) {
                    dst.setRGB(x, y, new Color(0, 0, 0, 100).getRGB());
                } else {
                    dst.setRGB(x, y, argb);
                }
            }
        }
        return dst;
    }

    private void addFrameToCache(BufferedImage img, String namePrefix) {
        String name = namePrefix + "_" + System.currentTimeMillis() + "_" + (int)(Math.random()*1000) + ".png";
        imageCache.put(name, img);
        ScriptItem si = new ScriptItem(false, name);
        si.duration = 10; // default
        scriptItems.add(si);
        
        // Also add to all movement types on disk
        String category = (String) comboCategory.getSelectedItem();
        String charName = txtName.getText().trim();
        if (category != null && !charName.isEmpty()) {
            for (String action : ACTIONS) {
                String actionPath = GamePaths.unitActionDir(category, charName, action).getPath();
                File dir = new File(actionPath);
                if (!dir.exists()) dir.mkdirs();
                try {
                    javax.imageio.ImageIO.write(img, "png", new File(dir, name));
                    // Append to script.txt
                    java.nio.file.Files.write(new File(dir, "script.txt").toPath(),
                        (si.duration + " " + name + "\n").getBytes(), 
                        java.nio.file.StandardOpenOption.CREATE, 
                        java.nio.file.StandardOpenOption.APPEND);
                } catch (Exception e) {}
            }
        }
        selectFrame(scriptItems.size() - 1);
    }

    private void initLeftPanel() {
        leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(Color.BLACK);
        leftPanel.setOpaque(true);
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        leftPanel.setPreferredSize(new Dimension(250, 800));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        JPanel charInfo = new JPanel(new GridLayout(3, 2, 5, 5));
        charInfo.setOpaque(false);
        charInfo.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Unit Info", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12), Color.WHITE));
        
        JLabel lblCategory = new JLabel("Category:");
        lblCategory.setForeground(Color.WHITE);
        comboCategory = new JComboBox<>(new String[]{"Champion", "Unit"});
        comboCategory.setSelectedIndex(0);
        comboCategory.setBackground(new Color(30, 30, 40));
        comboCategory.setForeground(Color.CYAN);

        JLabel lblName = new JLabel("Name:");
        lblName.setForeground(Color.WHITE);
        txtName = new JTextField("");
        txtName.setBackground(new Color(30, 30, 40));
        txtName.setForeground(Color.CYAN);
        
        JLabel lblAction = new JLabel("Action:");
        lblAction.setForeground(Color.WHITE);
        comboAction = new JComboBox<>(ACTIONS);
        comboAction.setSelectedIndex(-1);
        comboAction.setBackground(new Color(30, 30, 40));
        comboAction.setForeground(Color.CYAN);
        
        ActionListener changeListener = e -> {
            if (isLoading) return;
            String category = (String) comboCategory.getSelectedItem();
            String name = txtName.getText().trim();
            String action = (String) comboAction.getSelectedItem();
            if (category != null && !name.isEmpty() && action != null) {
                String path = GamePaths.unitActionDir(category, name, action).getPath();
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    loadAnimation(path);
                }
            }
        };
        comboCategory.addActionListener(changeListener);
        comboAction.addActionListener(changeListener);
        
        charInfo.add(lblCategory);
        charInfo.add(comboCategory);
        charInfo.add(lblName);
        charInfo.add(txtName);
        charInfo.add(lblAction);
        charInfo.add(comboAction);
        
        gbc.gridy++;
        leftPanel.add(charInfo, gbc);
        gbc.gridy++;
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);

        previewer = new AnimationPreviewer(230, 200);
        previewer.setMinimumSize(new Dimension(230, 200));
        gbc.weighty = 0.3;
        gbc.fill = GridBagConstraints.BOTH;
        leftPanel.add(previewer, gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);

        paletteSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        paletteSection.setOpaque(false);
        paletteSection.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Palette Pixels", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12), Color.WHITE));
        
        updatePaletteButtons();
        
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        leftPanel.add(paletteSection, gbc);
        
        gbc.gridy++;
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);

        JPanel toolPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        toolPanel.setOpaque(false);
        toolPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Editor Tools", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12), Color.WHITE));

        JButton btnPencil = new JButton("Pencil");
        JButton btnBucket = new JButton("Fill");
        JButton btnEraser = new JButton("Eraser");
        JButton btnEraserAll = new JButton("Global Eraser");

        JButton[] editorTools = {btnPencil, btnBucket, btnEraser, btnEraserAll};
        for (JButton b : editorTools) {
            b.addActionListener(e -> {
                if (b == btnPencil) currentTool = Tool.PENCIL;
                else if (b == btnBucket) currentTool = Tool.BUCKET;
                else if (b == btnEraser) currentTool = Tool.ERASER;
                else if (b == btnEraserAll) currentTool = Tool.ERASER_ALL;
                
                for (JButton other : editorTools) other.setBackground(null);
                b.setBackground(new Color(60, 120, 255));
            });
            toolPanel.add(b);
        }
        btnPencil.setBackground(new Color(60, 120, 255));

        gbc.gridy++;
        leftPanel.add(toolPanel, gbc);
        gbc.gridy++;
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);

        JPanel frameTools = new JPanel();
        frameTools.setLayout(new BoxLayout(frameTools, BoxLayout.Y_AXIS));
        frameTools.setOpaque(false);
        frameTools.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Frame Tools", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12), Color.WHITE));

        JButton btnAdd = new JButton("Add Frame");
        btnAdd.addActionListener(e -> addFrame());
        JButton btnDelete = new JButton("Delete Frame");
        btnDelete.addActionListener(e -> deleteSelectedFrame());
        JButton btnDuplicate = new JButton("Duplicate");
        btnDuplicate.addActionListener(e -> duplicateFrame());
        JButton btnMoveUp = new JButton("Move Up");
        btnMoveUp.addActionListener(e -> moveFrame(-1));
        JButton btnMoveDown = new JButton("Move Down");
        btnMoveDown.addActionListener(e -> moveFrame(1));

        JButton[] tools = {btnAdd, btnDelete, btnDuplicate, btnMoveUp, btnMoveDown};
        for (JButton tBtn : tools) {
            tBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            tBtn.setMaximumSize(new Dimension(230, 30));
            frameTools.add(tBtn);
            frameTools.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        
        gbc.gridy++;
        leftPanel.add(frameTools, gbc);
        
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        leftPanel.add(Box.createVerticalGlue(), gbc);
    }

    private void initRightPanel() {
        rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.BLACK);
        rightPanel.setOpaque(true);
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        rightPanel.setPreferredSize(new Dimension(250, 800));

        JLabel lblGallery = new JLabel("Frame Gallery");
        lblGallery.setForeground(Color.WHITE);
        lblGallery.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblGallery.setBorder(new EmptyBorder(0, 0, 10, 0));
        rightPanel.add(lblGallery, BorderLayout.NORTH);

        galleryPanel = new JPanel();
        galleryPanel.setLayout(new BoxLayout(galleryPanel, BoxLayout.Y_AXIS));
        galleryPanel.setBackground(Color.BLACK);
        galleryPanel.setOpaque(true);

        JScrollPane scrollPane = new JScrollPane(galleryPanel);
        scrollPane.setBackground(Color.BLACK);
        scrollPane.getViewport().setBackground(Color.BLACK);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        
        rightPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void updatePaletteButtons() {
        if (paletteSection == null) return;
        paletteSection.removeAll();
        
        Color[] colors = {colorHair, colorSkin, colorPrimary, colorSecondary, colorCloth, colorLeather, colorBorder, Color.BLACK, Color.WHITE, Color.GRAY, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA};
        for (int i = 0; i < colors.length; i++) {
            JButton pBtn = new JButton();
            pBtn.setBackground(colors[i]);
            pBtn.setPreferredSize(new Dimension(10, 10));
            pBtn.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            
            pBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        Color newColor = JColorChooser.showDialog(pBtn, "Edit Palette Color", pBtn.getBackground());
                        if (newColor != null) {
                            pBtn.setBackground(newColor);
                            selectedColor = newColor;
                        }
                    } else {
                        selectedColor = pBtn.getBackground();
                    }
                }
            });
            paletteSection.add(pBtn);
        }
        paletteSection.revalidate();
        paletteSection.repaint();
    }

    private void initCenterPanel() {
        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        pixelEditor = new PixelEditor();
        pixelEditor.setCanvasSize(32, 32);
        JScrollPane scrollPane = new JScrollPane(pixelEditor);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerPanel, rightPanel);
        rightSplit.setDividerLocation(800);
        rightSplit.setResizeWeight(1.0);
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightSplit);
        leftSplit.setDividerLocation(250);
        leftSplit.setResizeWeight(0.0);
        add(leftSplit, BorderLayout.CENTER);
    }

    private void selectAndLoadAnimation() {
        JFileChooser chooser = new JFileChooser(GamePaths.UNITS);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadAnimation(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public void saveAnimation() {
        if (scriptItems.isEmpty()) return;
        String category = (String) comboCategory.getSelectedItem();
        String name = txtName.getText().trim();
        String action = (String) comboAction.getSelectedItem();
        if (category == null || name.isEmpty() || action == null) return;
        String path = GamePaths.unitActionDir(category, name, action).getPath();
        try {
            File dir = new File(path);
            if (!dir.exists()) dir.mkdirs();
            if (selectedFrameIndex >= 0 && selectedFrameIndex < scriptItems.size()) {
                ScriptItem item = scriptItems.get(selectedFrameIndex);
                if (!item.isCommand) imageCache.put(item.text, pixelEditor.getCanvasCopy());
            }
            for (ScriptItem item : scriptItems) {
                if (!item.isCommand) {
                    BufferedImage img = imageCache.get(item.text);
                    if (img != null) javax.imageio.ImageIO.write(img, "png", new File(dir, item.text));
                }
            }
            
            // Save JSON metadata
            StringBuilder json = new StringBuilder("{\n  \"frames\": [\n");
            for (int i = 0; i < scriptItems.size(); i++) {
                ScriptItem item = scriptItems.get(i);
                if (!item.isCommand) {
                    json.append("    {\"file\": \"").append(item.text).append("\", \"duration\": ").append(item.duration).append("}");
                    if (i < scriptItems.size() - 1) json.append(",");
                    json.append("\n");
                }
            }
            json.append("  ]\n}");
            java.nio.file.Files.write(new File(dir, "metadata.json").toPath(), json.toString().getBytes());
            
            // Compatibility script.txt
            StringBuilder sb = new StringBuilder();
            for (ScriptItem item : scriptItems) {
                if (item.isCommand) sb.append(item.text).append("\n");
                else sb.append(item.duration).append(" ").append(item.text).append("\n");
            }
            java.nio.file.Files.write(new File(dir, "script.txt").toPath(), sb.toString().getBytes());
            
            // Delete unused PNGs
            java.util.Set<String> usedFiles = new java.util.HashSet<>();
            for (ScriptItem item : scriptItems) {
                if (!item.isCommand) usedFiles.add(item.text);
            }
            File[] allFiles = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
            if (allFiles != null) {
                for (File f : allFiles) {
                    if (!usedFiles.contains(f.getName())) {
                        f.delete();
                    }
                }
            }
            
            JOptionPane.showMessageDialog(this, "Successfully saved " + scriptItems.size() + " frames to:\n" + path);
        } catch (Exception e) { 
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Save failed: " + e.getMessage());
        }
    }

    public void loadAnimation(String folderPath) {
        if (isLoading) return;
        isLoading = true;
        try {
            imageCache.clear(); scriptItems.clear();
            File dir = new File(folderPath);
            
            // Auto-detect Category, Name, Action from path
            try {
                String action = dir.getName();
                String parentName = dir.getParentFile().getName();
                if ("MovingAnimation".equalsIgnoreCase(parentName)) {
                    String name = dir.getParentFile().getParentFile().getName();
                    String category = dir.getParentFile().getParentFile().getParentFile().getName();
                    comboCategory.setSelectedItem(category);
                    txtName.setText(name);
                } else {
                    // Compatibility with legacy: [name] -> [weapon] -> [action]
                    String name = dir.getParentFile().getParentFile().getName();
                    txtName.setText(name);
                    comboCategory.setSelectedItem("Unit"); // default fallback
                }
                comboAction.setSelectedItem(action);
            } catch (Exception e) {
                // Path might not follow standard structure, ignore
            }

            File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
            if (files != null) {
                for (File f : files) {
                    try { imageCache.put(f.getName(), javax.imageio.ImageIO.read(f)); } catch (Exception e) {}
                }
            }
            File scriptFile = new File(dir, "script.txt");
            if (scriptFile.exists()) {
                try {
                    List<String> lines = java.nio.file.Files.readAllLines(scriptFile.toPath());
                    for (String line : lines) {
                        String[] p = line.split("\\s+");
                        if (p.length >= 2) {
                            ScriptItem si = new ScriptItem(false, p[1]);
                            si.duration = Integer.parseInt(p[0]);
                            scriptItems.add(si);
                        }
                    }
                } catch (Exception e) {}
            }
            updateGallery();
            if (!scriptItems.isEmpty()) selectFrame(0);
        } finally {
            isLoading = false;
        }
    }

    private void updateGallery() {
        galleryPanel.removeAll();
        for (int i = 0; i < scriptItems.size(); i++) {
            final int index = i;
            ScriptItem item = scriptItems.get(i);
            if (item.isCommand) continue;
            BufferedImage frame = imageCache.get(item.text);
            if (frame == null) frame = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setBackground(index == selectedFrameIndex ? new Color(50, 70, 100) : new Color(30, 30, 40));
            JLabel lblThumb = new JLabel(new ImageIcon(frame.getScaledInstance(64, 64, Image.SCALE_SMOOTH)));
            lblThumb.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { selectFrame(index); } });
            itemPanel.add(lblThumb, BorderLayout.CENTER);
            galleryPanel.add(itemPanel);
        }
        galleryPanel.revalidate(); galleryPanel.repaint();
    }

    private void saveCurrentCanvas() {
        if (this.selectedFrameIndex >= 0 && this.selectedFrameIndex < scriptItems.size()) {
            ScriptItem prev = scriptItems.get(this.selectedFrameIndex);
            if (!prev.isCommand && pixelEditor != null) {
                imageCache.put(prev.text, pixelEditor.getCanvasCopy());
            }
        }
    }

    private void selectFrame(int index) {
        saveCurrentCanvas();
        
        this.selectedFrameIndex = index;
        if (index >= 0 && index < scriptItems.size()) {
            ScriptItem item = scriptItems.get(index);
            if (!item.isCommand) {
                BufferedImage img = imageCache.get(item.text);
                if (img != null) pixelEditor.setImage(img);
            }
        }
        updateGallery(); updatePreviewer();
    }

    private void updatePreviewer() {
        List<BufferedImage> previewFrames = new ArrayList<>();
        for (ScriptItem item : scriptItems) {
            if (!item.isCommand) {
                BufferedImage img = imageCache.get(item.text);
                if (img != null) previewFrames.add(img);
            }
        }
        previewer.setFrames(previewFrames);
    }

    private void addFrame() {
        String newName = String.format("Unit_%03d.png", imageCache.size());
        BufferedImage newFrame = new BufferedImage(pixelEditor.canvasWidth, pixelEditor.canvasHeight, BufferedImage.TYPE_INT_ARGB);
        imageCache.put(newName, newFrame);
        ScriptItem si = new ScriptItem(false, newName);
        scriptItems.add(si);
        selectFrame(scriptItems.size() - 1);
    }

    private void deleteSelectedFrame() {
        if (selectedFrameIndex >= 0 && selectedFrameIndex < scriptItems.size()) {
            scriptItems.remove(selectedFrameIndex);
            int nextIndex = scriptItems.isEmpty() ? -1 : Math.min(selectedFrameIndex, scriptItems.size() - 1);
            this.selectedFrameIndex = -1; // Bypass auto-save in selectFrame since we already deleted it
            selectFrame(nextIndex);
        }
    }

    private void duplicateFrame() {
        if (selectedFrameIndex >= 0 && selectedFrameIndex < scriptItems.size()) {
            saveCurrentCanvas();
            int currentIndex = selectedFrameIndex;
            ScriptItem original = scriptItems.get(currentIndex);
            ScriptItem copy = new ScriptItem(original.isCommand, original.text);
            scriptItems.add(currentIndex + 1, copy);
            this.selectedFrameIndex = -1;
            selectFrame(currentIndex + 1);
        }
    }

    private void moveFrame(int direction) {
        if (selectedFrameIndex >= 0 && selectedFrameIndex < scriptItems.size()) {
            int newIndex = selectedFrameIndex + direction;
            if (newIndex >= 0 && newIndex < scriptItems.size()) {
                saveCurrentCanvas();
                ScriptItem temp = scriptItems.get(selectedFrameIndex);
                scriptItems.set(selectedFrameIndex, scriptItems.get(newIndex));
                scriptItems.set(newIndex, temp);
                this.selectedFrameIndex = -1;
                selectFrame(newIndex);
            }
        }
    }

    public void refresh() {
        revalidate();
        repaint();
    }
    
    public void pause() {
        // Stop any background preview logic if needed
    }

    private class PixelEditor extends JPanel {
        private static final long serialVersionUID = 1L;
        private int canvasWidth = 32, canvasHeight = 32;
        private int zoom = 15;
        private BufferedImage canvas;
        
        public PixelEditor() {
            setBackground(new Color(40, 40, 50));
            canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { draw(e); }
                @Override public void mouseDragged(MouseEvent e) { draw(e); }
                private void draw(MouseEvent e) {
                    int x = e.getX() / zoom, y = e.getY() / zoom;
                    if (x >= 0 && x < canvasWidth && y >= 0 && y < canvasHeight) {
                        switch (currentTool) {
                            case PENCIL:
                                canvas.setRGB(x, y, selectedColor.getRGB());
                                break;
                            case BUCKET:
                                floodFill(x, y, selectedColor.getRGB());
                                break;
                            case ERASER:
                                canvas.setRGB(x, y, 0);
                                break;
                            case ERASER_ALL:
                                deleteColor(canvas.getRGB(x, y));
                                break;
                        }
                        repaint();
                    }
                }
            };
            addMouseListener(ma); addMouseMotionListener(ma);
        }

        public void setCanvasSize(int w, int h) {
            this.canvasWidth = w; this.canvasHeight = h;
            this.canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            setPreferredSize(new Dimension(w * zoom, h * zoom));
            revalidate(); repaint();
        }

        public void setImage(BufferedImage img) {
            this.canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics g = canvas.getGraphics();
            g.drawImage(img, 0, 0, canvasWidth, canvasHeight, null);
            g.dispose();
            repaint();
        }

        public void setZoom(int z) { this.zoom = z; setPreferredSize(new Dimension(canvasWidth * zoom, canvasHeight * zoom)); revalidate(); repaint(); }
        public BufferedImage getCanvasCopy() {
            BufferedImage copy = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics g = copy.getGraphics(); g.drawImage(canvas, 0, 0, null); g.dispose();
            return copy;
        }
        public void clear() { canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB); repaint(); }

        private void floodFill(int x, int y, int newColor) {
            int oldColor = canvas.getRGB(x, y);
            if (oldColor == newColor) return;
            
            Queue<Point> q = new LinkedList<>();
            q.add(new Point(x, y));
            while (!q.isEmpty()) {
                Point p = q.poll();
                if (p.x < 0 || p.x >= canvasWidth || p.y < 0 || p.y >= canvasHeight) continue;
                if (canvas.getRGB(p.x, p.y) != oldColor) continue;
                
                canvas.setRGB(p.x, p.y, newColor);
                q.add(new Point(p.x + 1, p.y));
                q.add(new Point(p.x - 1, p.y));
                q.add(new Point(p.x, p.y + 1));
                q.add(new Point(p.x, p.y - 1));
            }
        }
        
        private void deleteColor(int targetColor) {
            for (int y = 0; y < canvasHeight; y++) {
                for (int x = 0; x < canvasWidth; x++) {
                    if (canvas.getRGB(x, y) == targetColor) {
                        canvas.setRGB(x, y, 0);
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            for (int y = 0; y < canvasHeight; y++) {
                for (int x = 0; x < canvasWidth; x++) {
                    g2.setColor((x + y) % 2 == 0 ? new Color(60, 60, 60) : new Color(80, 80, 80));
                    g2.fillRect(x * zoom, y * zoom, zoom, zoom);
                }
            }
            g2.drawImage(canvas, 0, 0, canvasWidth * zoom, canvasHeight * zoom, null);
            g2.setColor(new Color(100, 100, 100, 50));
            for (int x = 0; x <= canvasWidth; x++) g2.drawLine(x * zoom, 0, x * zoom, canvasHeight * zoom);
            for (int y = 0; y <= canvasHeight; y++) g2.drawLine(0, y * zoom, canvasWidth * zoom, y * zoom);
            
            Point m = getMousePosition();
            if (m != null) {
                int mx = m.x / zoom;
                int my = m.y / zoom;
                g2.setColor(new Color(255, 255, 255, 100));
                g2.drawRect(mx * zoom, my * zoom, zoom, zoom);
            }
        }
    }
}
