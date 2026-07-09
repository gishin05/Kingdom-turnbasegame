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
import java.io.File;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import javafx.stage.Stage;
import javafx.scene.SnapshotParameters;

public class AnimationEditorScreen extends JPanel  {

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
    private JTextField txtWeapon;
    private JComboBox<String> comboCategory;
    private static final String[] CATEGORIES = {"Champion", "Unit"};
    private int selectedFrameIndex = -1;
    private String activeMode = "Mode 1";
    
    private JDialog progressDialog;
    private JProgressBar progressBar;

    private static class ScriptItem {
        boolean isCommand;
        String text; // Command line or frame name
        int duration = 5;
        String suffix = ""; // For frames, e.g. "p-" or extra commands
        String mode = ""; // Which mode this item belongs to
        
        ScriptItem(boolean isCommand, String text) {
            this.isCommand = isCommand;
            this.text = text;
        }
    }
    private List<ScriptItem> scriptItems = new ArrayList<>();
    private java.util.Map<String, BufferedImage> imageCache = new java.util.HashMap<>();

    // Palette colors (shared with CharacterDesignScreen for consistency)
    private Color colorHair = new Color(224, 216, 64);
    
    private String getModePhaseName(int mode) {
        switch(mode) {
            case 1: return "Basic Attack Phase";
            case 2: return "Melee (Close) Phase";
            case 3: return "Critical Attack Phase";
            case 4: return "Critical (Close) Phase";
            case 5: return "Ranged Attack Phase";
            case 6: return "Critical Ranged Phase";
            case 7: return "Dodge Phase";
            case 8: return "Dodge (Ranged) Phase";
            case 9: return "Standing/Idle Phase";
            case 11: return "Miss Phase";
            case 12: return "Finisher Phase";
            default: return "Mode " + mode + " Phase";
        }
    }

    private String getModePhaseNameFromText(String text) {
        if (text == null || !text.contains("Mode")) return "Custom Phase";
        try {
            String modeStr = text.substring(text.indexOf("Mode") + 4).trim();
            int m = Integer.parseInt(modeStr);
            return getModePhaseName(m);
        } catch (Exception e) {
            return "General Phase";
        }
    }
    private Color colorSkin = new Color(248, 208, 152);
    private Color colorPrimary = new Color(100, 100, 100);
    private Color colorSecondary = new Color(247, 173, 82);
    private Color colorCloth = new Color(82, 82, 115);
    private Color colorLeather = new Color(148, 100, 66);
    private Color colorBorder = new Color(40, 30, 40);

    public AnimationEditorScreen() {

        
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        initToolbar();
        initLeftPanel();
        initRightPanel();
        initCenterPanel();
        
        addDefaultHeaders();
        updateGallery();
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

        // Zoom Slider
        JLabel lblZoom = new JLabel("Zoom: ");
        lblZoom.setForeground(Color.WHITE);
        JSlider zoomSlider = new JSlider(1, 32, 10);
        zoomSlider.setOpaque(false);
        zoomSlider.setPreferredSize(new Dimension(200, 20));
        zoomSlider.addChangeListener(e -> {
            if (pixelEditor != null) {
                pixelEditor.setZoom(zoomSlider.getValue());
            }
        });
        
        JCheckBox chkGuidelines = new JCheckBox("Guidelines", true);
        chkGuidelines.setOpaque(false);
        chkGuidelines.setForeground(Color.WHITE);
        chkGuidelines.addActionListener(e -> pixelEditor.setShowGuidelines(chkGuidelines.isSelected()));

        JButton btnNew = new JButton("New Animation");
        btnNew.setBackground(new Color(60, 60, 100));
        btnNew.setForeground(Color.WHITE);
        btnNew.addActionListener(e -> newAnimation());

        topToolbar.add(btnBack);
        topToolbar.add(new JSeparator(JSeparator.VERTICAL));
        topToolbar.add(btnNew);
        topToolbar.add(btnLoad);
        topToolbar.add(btnImport);
        topToolbar.add(btnSave);
        topToolbar.add(btnClear);
        topToolbar.add(Box.createRigidArea(new Dimension(20, 0)));
        topToolbar.add(lblZoom);
        topToolbar.add(zoomSlider);
        topToolbar.add(Box.createRigidArea(new Dimension(10, 0)));
        topToolbar.add(chkGuidelines);

        // Speed Slider
        JLabel lblSpeed = new JLabel("  Speed: ");
        lblSpeed.setForeground(Color.WHITE);
        JSlider speedSlider = new JSlider(1, 20, 4);
        speedSlider.setOpaque(false);
        speedSlider.setInverted(true);
        speedSlider.setPreferredSize(new Dimension(100, 20));
        speedSlider.addChangeListener(e -> {
            if (previewer != null) {
                previewer.setFrameDelay(speedSlider.getValue());
            }
        });
        topToolbar.add(lblSpeed);
        topToolbar.add(speedSlider);

        add(topToolbar, BorderLayout.NORTH);
    }

    private void importImage() {
        JFileChooser chooser = new JFileChooser(game.core.util.GamePaths.BATTLE.getPath());
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setDialogTitle("Select a folder of images or a single file");
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            
            if (selected.isDirectory()) {
                // Auto-fill Name from folder name
                txtName.setText(selected.getName());
                
                // Detect [Weapon]_without_comment.txt and auto-fill weapon + load as script
                File[] txtFiles = selected.listFiles((d, name) -> name.toLowerCase().endsWith("_without_comment.txt"));
                if (txtFiles != null && txtFiles.length > 0) {
                    File scriptSource = txtFiles[0];
                    String fileName = scriptSource.getName(); // e.g. "Axe_without_comment.txt"
                    String weapon = fileName.substring(0, fileName.toLowerCase().indexOf("_without_comment.txt"));
                    txtWeapon.setText(weapon);
                    
                    // Copy content as script.txt so loadAnimation can parse it
                    try {
                        byte[] content = java.nio.file.Files.readAllBytes(scriptSource.toPath());
                        
                        // Parse the script into scriptItems directly
                        String text = new String(content);
                        String[] lines = text.split("\\r?\\n");
                        String currentMode = "Mode 1";
                        for (String line : lines) {
                            line = line.trim();
                            if (line.isEmpty()) continue;
                            
                            if (line.contains("Mode")) {
                                currentMode = line.substring(line.indexOf("Mode")).trim();
                            }
                            
                            if (line.startsWith("/") || line.startsWith("~") || line.equals("S")) {
                                ScriptItem si = new ScriptItem(true, line);
                                si.mode = currentMode;
                                scriptItems.add(si);
                                continue;
                            }
                            
                            // Skip C-commands (e.g. C01, C03)
                            if (line.matches("^C[0-9A-Fa-f]+$")) {
                                ScriptItem si = new ScriptItem(true, line);
                                si.mode = currentMode;
                                scriptItems.add(si);
                                continue;
                            }
                            
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 2) {
                                try {
                                    int dur = Integer.parseInt(parts[0]);
                                    ScriptItem si = new ScriptItem(false, "");
                                    si.duration = dur;
                                    si.mode = currentMode;
                                    if (parts[1].equals("p-") && parts.length > 2) {
                                        si.text = parts[2];
                                        si.suffix = "p-";
                                    } else {
                                        si.text = parts[1];
                                    }
                                    scriptItems.add(si);
                                } catch (NumberFormatException e) {
                                    ScriptItem si = new ScriptItem(true, line);
                                    si.mode = currentMode;
                                    scriptItems.add(si);
                                }
                            } else {
                                ScriptItem si = new ScriptItem(true, line);
                                si.mode = currentMode;
                                scriptItems.add(si);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                // Import all images from the folder
                File[] files = selected.listFiles((d, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".bmp");
                });
                
                if (files == null || files.length == 0) {
                    JOptionPane.showMessageDialog(this, "No image files found in the selected folder.");
                    return;
                }
                
                java.util.Arrays.sort(files); // Sort alphabetically
                final File[] sortedFiles = files;
                final String folderName = selected.getName();
                
                // Check if script was loaded from _without_comment.txt
                // If so, we already have ScriptItem references to original filenames
                final boolean hasScript = (txtFiles != null && txtFiles.length > 0);
                
                ImportProgressDialog.run(this, "Importing Frames", updater -> {
                    int total = sortedFiles.length;
                    int count = 0;
                    for (int i = 0; i < total; i++) {
                        File file = sortedFiles[i];
                        updater.update(i + 1, total, "Loading: " + file.getName());
                        try {
                            BufferedImage img = javax.imageio.ImageIO.read(file);
                            if (img != null) {
                                // Convert to ARGB and remove green background
                                BufferedImage argbImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                Graphics2D gc = argbImg.createGraphics();
                                gc.drawImage(img, 0, 0, null);
                                gc.dispose();
                                removeGreenBackground(argbImg);
                                
                                // Slice the first 248x160 from the left if oversized, otherwise draw as-is
                                int srcW = argbImg.getWidth();
                                int srcH = argbImg.getHeight();
                                
                                BufferedImage result;
                                if (srcW > 250 || srcH > 160) {
                                    // Crop: take the first 248x160 from top-left
                                    int cropW = Math.min(248, srcW);
                                    int cropH = Math.min(160, srcH);
                                    result = new BufferedImage(248, 160, BufferedImage.TYPE_INT_ARGB);
                                    Graphics2D g2 = result.createGraphics();
                                    g2.drawImage(argbImg.getSubimage(0, 0, cropW, cropH), 0, 0, null);
                                    g2.dispose();
                                } else if (srcW == 248 && srcH == 160) {
                                    result = argbImg;
                                } else {
                                    // Smaller image: draw onto 248x160 canvas (no scaling)
                                    result = new BufferedImage(248, 160, BufferedImage.TYPE_INT_ARGB);
                                    Graphics2D g2 = result.createGraphics();
                                    g2.drawImage(argbImg, 0, 0, null);
                                    g2.dispose();
                                }
                                
                                // Use original filename as cache key (matches script references)
                                final String originalName = file.getName();
                                final BufferedImage fScaled = result;
                                SwingUtilities.invokeAndWait(() -> {
                                    imageCache.put(originalName, fScaled);
                                    
                                    // Only add ScriptItem if no script was pre-loaded
                                    if (!hasScript) {
                                        ScriptItem si = new ScriptItem(false, originalName);
                                        si.mode = activeMode;
                                        
                                        int insertPos = -1;
                                        for (int j = 0; j < scriptItems.size(); j++) {
                                            if (scriptItems.get(j).mode.equals(activeMode)) {
                                                insertPos = j;
                                            }
                                        }
                                        
                                        if (insertPos != -1) {
                                            scriptItems.add(insertPos + 1, si);
                                        } else {
                                            scriptItems.add(si);
                                        }
                                    }
                                });
                                count++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    final int imported = count;
                    SwingUtilities.invokeLater(() -> {
                        updateGallery();
                        updatePreviewer();
                        if (!scriptItems.isEmpty()) {
                            for (int i = 0; i < scriptItems.size(); i++) {
                                if (!scriptItems.get(i).isCommand) {
                                    selectFrame(i);
                                    break;
                                }
                            }
                        }
                        JOptionPane.showMessageDialog(this, "Imported " + imported + " frames from:\n" + folderName);
                    });
                }, null);
            } else {
                // Single file import (image or video)
                String name = selected.getName().toLowerCase();
                if (name.endsWith(".mp4") || name.endsWith(".mov") || name.endsWith(".avi") || name.endsWith(".m4v")) {
                    importVideo(selected);
                    return;
                }
                
                try {
                    BufferedImage img = javax.imageio.ImageIO.read(selected);
                    if (img != null) {
                        processImportedImage(img, "Imported");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error importing image: " + e.getMessage());
                }
            }
        }
    }

    private void importVideo(File file) {
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "File not found: " + file.getAbsolutePath());
            return;
        }

        String input = JOptionPane.showInputDialog(this, "How many frames to extract?", "10");
        if (input == null) return;
        int numFrames;
        try { numFrames = Integer.parseInt(input); } catch (Exception e) { numFrames = 10; }

        // Setup Progress Dialog
        progressDialog = new JDialog((Window)SwingUtilities.getWindowAncestor(this), "Importing Video", Dialog.ModalityType.MODELESS);
        progressBar = new JProgressBar(0, numFrames);
        progressBar.setStringPainted(true);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.add(new JLabel(" Initializing Video Engine..."), BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setSize(300, 80);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setVisible(true);

        final int framesToExtract = numFrames;
        Platform.runLater(() -> {
            try {
                Media media = new Media(file.toURI().toURL().toExternalForm());
                MediaPlayer player = new MediaPlayer(media);
                MediaView view = new MediaView(player);
                view.setFitWidth(1280);
                view.setFitHeight(720);
                
                // IMPORTANT: JavaFX needs a visible window for MediaView to render and snapshots to work
                Stage hiddenStage = new Stage();
                hiddenStage.setOpacity(0.01); // Almost invisible
                hiddenStage.setWidth(1);
                hiddenStage.setHeight(1);
                hiddenStage.setScene(new Scene(new javafx.scene.layout.StackPane(view)));
                hiddenStage.show();

                player.setOnReady(() -> {
                    player.setMute(true);
                    player.play();
                    player.pause(); // Start the engine
                    
                    SwingUtilities.invokeLater(() -> {
                        ((JLabel)progressDialog.getContentPane().getComponent(0)).setText(" Extracting frames...");
                    });
                    double duration = player.getTotalDuration().toSeconds();
                    double interval = duration / (framesToExtract > 1 ? framesToExtract - 1 : 1);
                    extractNextVideoFrame(player, view, hiddenStage, framesToExtract, interval, 0);
                });
                
                player.setOnError(() -> {
                    SwingUtilities.invokeLater(() -> {
                        if (progressDialog != null) progressDialog.dispose();
                        hiddenStage.close();
                        JOptionPane.showMessageDialog(this, "Video Error: " + player.getError().getMessage());
                    });
                });

                // Fail-safe: if status doesn't change from UNKNOWN in 5 seconds
                new Thread(() -> {
                    try { Thread.sleep(5000); } catch (Exception e) {}
                    if (player.getStatus() == MediaPlayer.Status.UNKNOWN) {
                        SwingUtilities.invokeLater(() -> {
                            if (progressDialog != null) progressDialog.dispose();
                            hiddenStage.close();
                            JOptionPane.showMessageDialog(this, "Video Engine Timeout: Is the file format supported?");
                        });
                    }
                }).start();

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    if (progressDialog != null) progressDialog.dispose();
                    JOptionPane.showMessageDialog(this, "Initialization Error: " + e.getMessage());
                });
            }
        });
    }

    private void extractNextVideoFrame(MediaPlayer player, MediaView view, Stage stage, int total, double interval, int current) {
        if (current >= total) {
            player.dispose();
            stage.close();
            SwingUtilities.invokeLater(() -> {
                if (progressDialog != null) progressDialog.dispose();
                JOptionPane.showMessageDialog(this, "Import Complete: " + total + " frames added.");
            });
            return;
        }
        
        final int frameIndex = current;
        player.seek(Duration.seconds(frameIndex * interval));
        
        // Increased delay to 800ms to ensure the video buffer updates correctly between seeks
        PauseTransition pause = new PauseTransition(Duration.millis(800)); 
        pause.setOnFinished(ev -> {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(javafx.scene.paint.Color.BLACK);
            WritableImage wi = view.snapshot(params, null);
            BufferedImage bi = SwingFXUtils.fromFXImage(wi, null);
            
            SwingUtilities.invokeLater(() -> {
                processImportedImage(bi, "VidFrame");
                if (progressBar != null) progressBar.setValue(frameIndex + 1);
                Platform.runLater(() -> extractNextVideoFrame(player, view, stage, total, interval, frameIndex + 1));
            });
        });
        pause.play();
    }

    private void newAnimation() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure? All unsaved progress will be lost.", "New Animation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        
        scriptItems.clear();
        imageCache.clear();
        txtName.setText("");
        txtWeapon.setText("");
        comboCategory.setSelectedIndex(0);
        selectedFrameIndex = -1;
        activeMode = "Mode 1";
        if (pixelEditor != null) pixelEditor.clear();
        
        addDefaultHeaders();
        updateGallery();
        updatePreviewer();
    }

    private void addDefaultHeaders() {
        int[] defaultModes = {1, 3, 5, 6, 7, 8, 9, 11, 12};
        for (int m : defaultModes) {
            ScriptItem header = new ScriptItem(true, "/// - Mode " + m);
            header.mode = "Mode " + m;
            scriptItems.add(header);
        }
    }

    private void processImportedImage(BufferedImage img, String namePrefix) {
        if (img == null) return;
        
        // Convert to ARGB first so we can manipulate alpha
        BufferedImage argbImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gc = argbImg.createGraphics();
        gc.drawImage(img, 0, 0, null);
        gc.dispose();
        
        // Auto-remove green chroma key background BEFORE scaling
        // (scaling can anti-alias green pixels, making them harder to detect)
        removeGreenBackground(argbImg);
        
        // Slice the first 248x160 from the left if oversized, otherwise draw as-is
        int srcW = argbImg.getWidth();
        int srcH = argbImg.getHeight();
        
        BufferedImage scaled;
        if (srcW > 250 || srcH > 160) {
            // Crop: take the first 248x160 from top-left
            int cropW = Math.min(248, srcW);
            int cropH = Math.min(160, srcH);
            scaled = new BufferedImage(248, 160, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.drawImage(argbImg.getSubimage(0, 0, cropW, cropH), 0, 0, null);
            g.dispose();
        } else if (srcW == 248 && srcH == 160) {
            scaled = argbImg;
        } else {
            // Smaller image: draw onto 248x160 canvas (no scaling)
            scaled = new BufferedImage(248, 160, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.drawImage(argbImg, 0, 0, null);
            g.dispose();
        }
        
        // Add to cache with a unique name
        String newName = namePrefix + "_" + imageCache.size() + "_" + (System.currentTimeMillis() % 1000) + ".png";
        imageCache.put(newName, scaled);
        
        ScriptItem si = new ScriptItem(false, newName);
        si.mode = activeMode;
        
        int insertPos = -1;
        for (int i = 0; i < scriptItems.size(); i++) {
            if (scriptItems.get(i).mode.equals(activeMode)) {
                insertPos = i;
            }
        }
        
        this.selectedFrameIndex = -1;
        if (insertPos != -1) {
            scriptItems.add(insertPos + 1, si);
            selectFrame(insertPos + 1);
        } else {
            scriptItems.add(si);
            selectFrame(scriptItems.size() - 1);
        }
        
        updateGallery();
        updatePreviewer();
    }

    private void removeGreenBackground(BufferedImage img) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                if (a == 0) continue; // Already transparent
                
                int r = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                
                boolean isGreen = false;
                
                // 1. Pure/near-pure green (#00FF00, #00F800, FEBuilder green)
                if (r <= 30 && green >= 200 && b <= 30) isGreen = true;
                
                // 2. GBA palette green (168, 208, 160) with tolerance
                if (Math.abs(r - 168) < 15 && Math.abs(green - 208) < 15 && Math.abs(b - 160) < 15) isGreen = true;
                
                // 3. FEBuilder bright green variants
                if (r <= 80 && green >= 220 && b <= 80) isGreen = true;
                
                // 4. General chroma key: green channel clearly dominates
                if (green > 150 && green > r * 1.4 && green > b * 1.4) isGreen = true;
                
                if (isGreen) {
                    img.setRGB(x, y, 0x00000000); // Fully transparent
                }
            }
        }
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
        // Character Info Section
        JPanel charInfo = new JPanel(new GridLayout(3, 2, 5, 5));
        charInfo.setOpaque(false);
        charInfo.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Character Info", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12), Color.WHITE));
        
        JLabel lblCategory = new JLabel("Category:");
        lblCategory.setForeground(Color.WHITE);
        comboCategory = new JComboBox<>(CATEGORIES);
        comboCategory.setSelectedIndex(0);
        comboCategory.setBackground(new Color(30, 30, 40));
        comboCategory.setForeground(Color.CYAN);
        
        JLabel lblName = new JLabel("Name:");
        lblName.setForeground(Color.WHITE);
        txtName = new JTextField("");
        txtName.setBackground(new Color(30, 30, 40));
        txtName.setForeground(Color.CYAN);
        txtName.setCaretColor(Color.WHITE);
        
        JLabel lblWeapon = new JLabel("Weapon:");
        lblWeapon.setForeground(Color.WHITE);
        txtWeapon = new JTextField("");
        txtWeapon.setBackground(new Color(30, 30, 40));
        txtWeapon.setForeground(Color.CYAN);
        txtWeapon.setCaretColor(Color.WHITE);
        
        charInfo.add(lblCategory);
        charInfo.add(comboCategory);
        charInfo.add(lblName);
        charInfo.add(txtName);
        charInfo.add(lblWeapon);
        charInfo.add(txtWeapon);
        
        gbc.gridy++;
        leftPanel.add(charInfo, gbc);
        gbc.gridy++;
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);

        // Animation Previewer
        previewer = new AnimationPreviewer(230, 200);
        previewer.setMinimumSize(new Dimension(230, 200));
        gbc.weighty = 0.3; // Give it some weight
        gbc.fill = GridBagConstraints.BOTH;
        leftPanel.add(previewer, gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);

        // Palette Pixels Section
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

        // Frame Tools Section
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
        
        // Add vertical glue at the bottom to push everything up
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
        JScrollPane scrollPane = new JScrollPane(pixelEditor);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerPanel, rightPanel);
        rightSplit.setDividerLocation(800);
        rightSplit.setDividerSize(5);
        rightSplit.setOpaque(false);
        rightSplit.setBorder(null);
        rightSplit.setResizeWeight(1.0);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightSplit);
        leftSplit.setDividerLocation(250);
        leftSplit.setDividerSize(5);
        leftSplit.setOpaque(false);
        leftSplit.setBorder(null);
        leftSplit.setResizeWeight(0.0);
        
        add(leftSplit, BorderLayout.CENTER);
    }

    private void selectAndLoadAnimation() {
        JFileChooser chooser = new JFileChooser(game.core.util.GamePaths.BATTLE.getPath());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadAnimation(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public void saveAnimation() {
        if (scriptItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No animation to save!");
            return;
        }

        String category = (String) comboCategory.getSelectedItem();
        String name = txtName.getText().trim();
        String weapon = txtWeapon.getText().trim();
        
        if (category == null || category.isEmpty() || name.isEmpty() || weapon.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select Category and enter Name and Weapon!");
            return;
        }

        String path = game.core.util.GamePaths.battleAnimDir(category, name, weapon).getPath();
        try {
            File dir = new File(path);
            if (!dir.exists()) dir.mkdirs();

            // First, update the current selected frame in the cache from the editor canvas
            if (selectedFrameIndex >= 0 && selectedFrameIndex < scriptItems.size()) {
                ScriptItem item = scriptItems.get(selectedFrameIndex);
                if (!item.isCommand) {
                    imageCache.put(item.text, pixelEditor.getCanvasCopy());
                }
            }

            // Save only images referenced in the script
            java.util.Set<String> referenced = new java.util.HashSet<>();
            for (ScriptItem item : scriptItems) {
                if (!item.isCommand) referenced.add(item.text);
            }

            for (String imgName : referenced) {
                BufferedImage img = imageCache.get(imgName);
                if (img != null) {
                    File outputFile = new File(dir, imgName);
                    javax.imageio.ImageIO.write(img, "png", outputFile);
                }
            }

            // SAVE SCRIPT.TXT
            StringBuilder sb = new StringBuilder();
            for (ScriptItem item : scriptItems) {
                if (item.isCommand) {
                    sb.append(item.text).append("\n");
                } else {
                    sb.append(item.duration).append(" ");
                    if (item.suffix != null && !item.suffix.isEmpty()) {
                        sb.append(item.suffix).append(" ");
                    }
                    sb.append(item.text).append("\n");
                }
            }
            
            File scriptFile = new File(dir, "script.txt");
            java.nio.file.Files.write(scriptFile.toPath(), sb.toString().getBytes());

            // Delete unused PNGs
            File[] allFiles = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
            if (allFiles != null) {
                for (File f : allFiles) {
                    if (!referenced.contains(f.getName())) {
                        f.delete();
                    }
                }
            }

            JOptionPane.showMessageDialog(this, "Successfully saved to:\n" + path);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving animation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadAnimation(String folderPath) {
        imageCache.clear();
        scriptItems.clear();
        
        File dir = new File(folderPath);
        
        // Try to auto-fill Category, Name and Weapon from path
        // Expected structure: .../battle/[Category]/[Name]/[Weapon]
        try {
            String absPath = dir.getAbsolutePath().replace("\\", "/");
            String battleMarker = "battle";
            int battleIdx = absPath.lastIndexOf(battleMarker + "/");
            if (battleIdx >= 0) {
                String sub = absPath.substring(battleIdx + battleMarker.length() + 1);
                String[] p = sub.split("/");
                if (p.length >= 3) {
                    // Category / Name / Weapon
                    for (int ci = 0; ci < comboCategory.getItemCount(); ci++) {
                        if (comboCategory.getItemAt(ci).equalsIgnoreCase(p[0])) {
                            comboCategory.setSelectedIndex(ci);
                            break;
                        }
                    }
                    txtName.setText(p[1]);
                    txtWeapon.setText(p[2]);
                } else if (p.length >= 2) {
                    txtName.setText(p[0]);
                    txtWeapon.setText(p[1]);
                } else if (p.length >= 1) {
                    txtName.setText(p[0]);
                }
            }
        } catch (Exception e) {}

        // 1. Load all PNGs into cache with progress
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
        if (files != null && files.length > 0) {
            final File[] pngFiles = files;
            ImportProgressDialog.run(this, "Loading Animation", updater -> {
                int total = pngFiles.length;
                for (int i = 0; i < total; i++) {
                    File file = pngFiles[i];
                    updater.update(i + 1, total, "Loading: " + file.getName());
                    try {
                        BufferedImage img = javax.imageio.ImageIO.read(file);
                        if (img != null) {
                            imageCache.put(file.getName(), img);
                        }
                    } catch (Exception ex) {}
                }
            }, () -> {
                finishLoadAnimation(dir);
            });
        } else {
            finishLoadAnimation(dir);
        }
    }

    private void finishLoadAnimation(File dir) {
        // 2. Parse script.txt (or fallback to [Weapon]_without_comment.txt)
        File scriptFile = new File(dir, "script.txt");
        if (!scriptFile.exists()) {
            // Fallback: look for [Weapon]_without_comment.txt
            File[] txtFiles = dir.listFiles((d, n) -> n.toLowerCase().endsWith("_without_comment.txt"));
            if (txtFiles != null && txtFiles.length > 0) {
                scriptFile = txtFiles[0];
                // Auto-fill weapon from filename
                String fileName = scriptFile.getName();
                String weapon = fileName.substring(0, fileName.toLowerCase().indexOf("_without_comment.txt"));
                txtWeapon.setText(weapon);
            }
        }
        String currentMode = "None";
        if (scriptFile.exists()) {
            try {
                List<String> lines = java.nio.file.Files.readAllLines(scriptFile.toPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    
                    if (line.contains("Mode")) {
                        currentMode = line.substring(line.indexOf("Mode")).trim();
                    }
                    
                    if (line.startsWith("/") || line.startsWith("~") || line.equals("S")) {
                        ScriptItem si = new ScriptItem(true, line);
                        si.mode = currentMode;
                        scriptItems.add(si);
                        continue;
                    }
                    
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int dur = Integer.parseInt(parts[0]);
                            ScriptItem si = new ScriptItem(false, "");
                            si.duration = dur;
                            si.mode = currentMode;
                            if (parts[1].equals("p-") && parts.length > 2) {
                                si.text = parts[2];
                                si.suffix = "p-";
                            } else {
                                si.text = parts[1];
                            }
                            scriptItems.add(si);
                        } catch (NumberFormatException e) {
                            ScriptItem si = new ScriptItem(true, line);
                            si.mode = currentMode;
                            scriptItems.add(si);
                        }
                    } else {
                        ScriptItem si = new ScriptItem(true, line);
                        si.mode = currentMode;
                        scriptItems.add(si);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Default Structure for New Unit
            addDefaultHeaders();
            
            // Put all existing frames into Mode 1 as a starting point
            java.util.List<String> sortedNames = new ArrayList<>(imageCache.keySet());
            java.util.Collections.sort(sortedNames);
            for (String name : sortedNames) {
                ScriptItem si = new ScriptItem(false, name);
                si.mode = "Mode 1";
                
                // Find insert position after Mode 1 header
                int pos = -1;
                for(int i=0; i<scriptItems.size(); i++) {
                    if(scriptItems.get(i).text.contains("Mode 1")) { pos = i; break; }
                }
                if(pos != -1) scriptItems.add(pos + 1, si);
                else scriptItems.add(si);
            }
        }
        
        updateGallery();
        if (!scriptItems.isEmpty()) {
            // Find first frame
            for (int i = 0; i < scriptItems.size(); i++) {
                if (!scriptItems.get(i).isCommand) {
                    selectFrame(i);
                    break;
                }
            }
        }
    }

    private void updateGallery() {
        galleryPanel.removeAll();
        
        // Map to track frame usage across modes
        java.util.Map<String, java.util.Set<String>> usageMap = new java.util.HashMap<>();
        for (ScriptItem item : scriptItems) {
            if (!item.isCommand) {
                usageMap.computeIfAbsent(item.text, k -> new java.util.TreeSet<>()).add(item.mode);
            }
        }

        for (int i = 0; i < scriptItems.size(); i++) {
            final int index = i;
            ScriptItem item = scriptItems.get(i);
            
            if (item.isCommand) {
                // HIDE raw commands like C01, S, ~, but SHOW Mode Headers
                if (item.text.contains("Mode")) {
                    final String itemMode = "Mode " + item.text.substring(item.text.indexOf("Mode") + 4).trim();
                    boolean isActive = activeMode.equals(itemMode);
                    
                    JPanel headerPanel = new JPanel(new BorderLayout());
                    headerPanel.setBackground(isActive ? new Color(60, 70, 100) : new Color(40, 45, 60));
                    headerPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY),
                        isActive ? BorderFactory.createLineBorder(Color.CYAN, 1) : BorderFactory.createEmptyBorder()
                    ));
                    headerPanel.setMaximumSize(new Dimension(250, 40));
                    headerPanel.setPreferredSize(new Dimension(250, 40));
                    headerPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    
                    headerPanel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            activeMode = itemMode;
                            updateGallery();
                        }
                    });
                    
                    JLabel lblPhase = new JLabel("  " + getModePhaseNameFromText(item.text));
                    lblPhase.setForeground(isActive ? Color.WHITE : new Color(200, 220, 255));
                    lblPhase.setFont(new Font("SansSerif", Font.BOLD, 12));
                    
                    JLabel lblMode = new JLabel(item.text + "  ");
                    lblMode.setForeground(isActive ? Color.CYAN : new Color(100, 120, 150));
                    lblMode.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    
                    headerPanel.add(lblPhase, BorderLayout.WEST);
                    headerPanel.add(lblMode, BorderLayout.EAST);
                    
                    galleryPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                    galleryPanel.add(headerPanel);
                    galleryPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                }
                // Skip other commands in the gallery view
                continue;
            }
            
            BufferedImage frame = imageCache.get(item.text);
            if (frame == null) {
                // Placeholder for missing frame
                frame = new BufferedImage(62, 65, BufferedImage.TYPE_INT_ARGB);
            }
            
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setOpaque(true);
            itemPanel.setBackground(index == selectedFrameIndex ? new Color(50, 70, 100) : new Color(25, 25, 30));
            itemPanel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(5, 5, 5, 5),
                BorderFactory.createLineBorder(index == selectedFrameIndex ? Color.CYAN : new Color(60, 60, 70))
            ));
            
            JLabel lblThumb = new JLabel(new ImageIcon(frame.getScaledInstance(62, 65, Image.SCALE_SMOOTH)));
            lblThumb.setCursor(new Cursor(Cursor.HAND_CURSOR));
            lblThumb.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectFrame(index);
                }
            });
            
            JLabel lblName = new JLabel(item.text);
            lblName.setForeground(new Color(180, 180, 190));
            lblName.setFont(new Font("SansSerif", Font.PLAIN, 10));
            lblName.setHorizontalAlignment(SwingConstants.CENTER);
            
            // Script Inputs
            JPanel scriptBox = new JPanel();
            scriptBox.setLayout(new BoxLayout(scriptBox, BoxLayout.Y_AXIS));
            scriptBox.setOpaque(false);
            
            JPanel durCmdRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            durCmdRow.setOpaque(false);
            
            JTextField txtDur = new JTextField(String.valueOf(item.duration), 2);
            txtDur.setBackground(new Color(15, 15, 20));
            txtDur.setForeground(new Color(100, 255, 100));
            txtDur.setCaretColor(Color.WHITE);
            txtDur.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            txtDur.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) {
                    try { item.duration = Integer.parseInt(txtDur.getText()); } catch (Exception ex) {}
                }
            });
            
            JLabel lblDur = new JLabel("Dur:");
            lblDur.setForeground(Color.GRAY);
            lblDur.setFont(new Font("SansSerif", Font.PLAIN, 9));
            durCmdRow.add(lblDur);
            durCmdRow.add(txtDur);
            
            // Usage/Links info
            java.util.Set<String> otherModes = usageMap.get(item.text);
            String linksText = "";
            if (otherModes != null && otherModes.size() > 1) {
                List<String> others = new ArrayList<>(otherModes);
                others.remove(item.mode);
                linksText = "<html><center>Also in: " + String.join(", ", others) + "</center></html>";
            }
            
            JLabel lblLinks = new JLabel(linksText);
            lblLinks.setForeground(new Color(120, 120, 200));
            lblLinks.setFont(new Font("SansSerif", Font.ITALIC, 9));
            lblLinks.setAlignmentX(Component.CENTER_ALIGNMENT);

            scriptBox.add(Box.createRigidArea(new Dimension(0, 4)));
            scriptBox.add(durCmdRow);
            if (!linksText.isEmpty()) {
                scriptBox.add(Box.createRigidArea(new Dimension(0, 2)));
                scriptBox.add(lblLinks);
            }
            
            itemPanel.add(lblThumb, BorderLayout.CENTER);
            itemPanel.add(lblName, BorderLayout.NORTH);
            itemPanel.add(scriptBox, BorderLayout.SOUTH);
            
            galleryPanel.add(itemPanel);
        }
        galleryPanel.revalidate();
        galleryPanel.repaint();
    }

    private void saveCurrentCanvas() {
        if (selectedFrameIndex >= 0 && selectedFrameIndex < scriptItems.size()) {
            ScriptItem prev = scriptItems.get(selectedFrameIndex);
            if (!prev.isCommand && pixelEditor != null) {
                imageCache.put(prev.text, pixelEditor.getCanvasCopy());
            }
        }
    }

    private void selectFrame(int index) {
        // Save current frame's canvas to cache before switching
        saveCurrentCanvas();
        
        this.selectedFrameIndex = index;
        if (index >= 0 && index < scriptItems.size()) {
            ScriptItem item = scriptItems.get(index);
            if (item.mode != null && !item.mode.isEmpty()) {
                activeMode = item.mode;
            }
            if (!item.isCommand) {
                BufferedImage img = imageCache.get(item.text);
                if (img != null) {
                    pixelEditor.setImage(img);
                } else {
                    pixelEditor.clear();
                }
            }
        }
        updateGallery();
        updatePreviewer();
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
        String newName = String.format("Frame_%03d.png", imageCache.size());
        BufferedImage newFrame = new BufferedImage(248, 160, BufferedImage.TYPE_INT_ARGB);
        imageCache.put(newName, newFrame);
        
        ScriptItem si = new ScriptItem(false, newName);
        si.mode = activeMode;
        
        // Find insert position (after last item of current mode)
        int insertPos = -1;
        for (int i = 0; i < scriptItems.size(); i++) {
            if (scriptItems.get(i).mode.equals(activeMode)) {
                insertPos = i;
            }
        }
        
        if (insertPos != -1) {
            scriptItems.add(insertPos + 1, si);
            selectFrame(insertPos + 1);
        } else {
            scriptItems.add(si);
            selectFrame(scriptItems.size() - 1);
        }
    }

    private void deleteSelectedFrame() {
        if (selectedFrameIndex >= 0 && selectedFrameIndex < scriptItems.size()) {
            scriptItems.remove(selectedFrameIndex);
            int nextIndex;
            if (scriptItems.isEmpty()) {
                nextIndex = -1;
                pixelEditor.clear();
            } else {
                nextIndex = Math.min(selectedFrameIndex, scriptItems.size() - 1);
            }
            this.selectedFrameIndex = -1;
            selectFrame(nextIndex);
            updateGallery();
            updatePreviewer();
        }
    }

    private void duplicateFrame() {
        if (selectedFrameIndex >= 0 && selectedFrameIndex < scriptItems.size()) {
            saveCurrentCanvas();
            int currentIndex = selectedFrameIndex;
            ScriptItem original = scriptItems.get(currentIndex);
            ScriptItem copy = new ScriptItem(original.isCommand, original.text);
            copy.duration = original.duration;
            copy.suffix = original.suffix;
            copy.mode = original.mode;
            
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
        // Start in clean state or keep current work
    }

    public void pause() {}

    private class PixelEditor extends JPanel {
        private static final long serialVersionUID = 1L;
        private int canvasWidth = 248;
        private int canvasHeight = 160;
        private int zoom = 10;
        private BufferedImage canvas;
        private boolean showGuidelines = true;
        
        public PixelEditor() {
            updatePreferredSize();
            setBackground(new Color(40, 40, 50));
            canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { draw(e); }
                @Override public void mouseDragged(MouseEvent e) { draw(e); }
                @Override public void mouseReleased(MouseEvent e) { updateGallery(); } // Refresh thumbnails when done drawing
                
                private void draw(MouseEvent e) {
                    int x = e.getX() / zoom;
                    int y = e.getY() / zoom;
                    if (x >= 0 && x < canvasWidth && y >= 0 && y < canvasHeight) {
                        canvas.setRGB(x, y, selectedColor.getRGB());
                        repaint();
                        syncCurrentFrame();
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void syncCurrentFrame() {
            if (selectedFrameIndex >= 0 && selectedFrameIndex < scriptItems.size()) {
                ScriptItem item = scriptItems.get(selectedFrameIndex);
                if (!item.isCommand) {
                    // Update cache with the current canvas state
                    // We use getCanvasCopy to ensure it's a stable image for the cache/previewer
                    imageCache.put(item.text, getCanvasCopy());
                    updatePreviewer();
                }
            }
        }

        public void setImage(BufferedImage img) {
            this.canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics g = canvas.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            repaint();
        }

        public void setZoom(int newZoom) {
            this.zoom = newZoom;
            updatePreferredSize();
            revalidate();
            repaint();
        }
        
        public void setShowGuidelines(boolean show) {
            this.showGuidelines = show;
            repaint();
        }

        private void updatePreferredSize() {
            setPreferredSize(new Dimension(canvasWidth * zoom, canvasHeight * zoom));
        }

        public BufferedImage getCanvasCopy() {
            BufferedImage copy = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics g = copy.getGraphics();
            g.drawImage(canvas, 0, 0, null);
            g.dispose();
            return copy;
        }

        public void clear() {
            canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            
            for (int y = 0; y < canvasHeight; y++) {
                for (int x = 0; x < canvasWidth; x++) {
                    if ((x + y) % 2 == 0) g2.setColor(new Color(60, 60, 60));
                    else g2.setColor(new Color(80, 80, 80));
                    g2.fillRect(x * zoom, y * zoom, zoom, zoom);
                }
            }
            
            g2.drawImage(canvas, 0, 0, canvasWidth * zoom, canvasHeight * zoom, null);
            
            g2.setColor(new Color(100, 100, 100, 50));
            for (int x = 0; x <= canvasWidth; x++) g2.drawLine(x * zoom, 0, x * zoom, canvasHeight * zoom);
            for (int y = 0; y <= canvasHeight; y++) g2.drawLine(0, y * zoom, canvasWidth * zoom, y * zoom);
            
            if (showGuidelines) {
                // Battle Animation Floor Line (Y=100)
                g2.setColor(new Color(100, 255, 100, 150));
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(0, 100 * zoom, canvasWidth * zoom, 100 * zoom);
                
                // Vertical Center Line (X=124)
                g2.setColor(new Color(100, 200, 255, 150));
                g2.drawLine(124 * zoom, 0, 124 * zoom, canvasHeight * zoom);
                
                // Visible Edge Boundary (X=240)
                g2.setColor(new Color(255, 100, 100, 150));
                g2.drawLine(240 * zoom, 0, 240 * zoom, canvasHeight * zoom);
                
                // Anchor Crosshair (124, 100)
                g2.setColor(Color.WHITE);
                int cs = 5 * zoom;
                g2.drawOval(124 * zoom - cs/2, 100 * zoom - cs/2, cs, cs);
                
                g2.setStroke(new BasicStroke(1));
                
                // Text Labels
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.drawString("FLOOR (Y=100)", 5, 100 * zoom - 5);
                g2.drawString("CENTER", 124 * zoom + 5, 15);
                g2.drawString("VISIBLE EDGE", 240 * zoom - 80, 15);
            }

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
