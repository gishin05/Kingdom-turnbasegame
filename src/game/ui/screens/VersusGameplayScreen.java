package game.ui.screens;

import game.Main;
import game.ui.BaseScreen;
import game.ui.Theme;
import game.core.unit.*;
import game.core.map.Tileset;
import game.core.util.GamePaths;
import game.core.util.SpriteColorer;
import game.core.util.SoundManager;
import game.core.util.JsonUtil;
import game.core.battle.BattleManager;
import game.core.animation.AnimationScript;
import game.core.animation.AnimationCommand;
import game.core.engine.*;
import game.core.save.SaveManager;
import game.core.save.VersusSaveData;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * The primary gameplay screen for local multiplayer (Versus) matches.
 * Handles rendering the battle map, processing unit movement and attacks,
 * managing the turn cycle, and displaying combat cinematics.
 */
import game.core.engine.AiLogic;

/**
 * UI DESIGN OVERVIEW:
 * This component is part of the pixelated game UI approach.
 * It integrates with the layered architecture, utilizing dynamic backgrounds
 * and semi-transparent panels to maintain a visually rich, modern aesthetic.
 */

public class VersusGameplayScreen extends BaseScreen {
    private static final long serialVersionUID = 1L;
    private static final int STATS_BOX_WIDTH = 380;
    public final int TILE_SIZE = 16;
    public int mapW, mapH;
    public int[][] mapData;
    public String[][] mapTSData;
    public Map<String, Tileset> loadedTilesets = new HashMap<>();
    private Map<String, File> tilesetFiles = new HashMap<>();
    public List<MapUnit> units = new ArrayList<>();
    private MapUnit selectedUnit = null;
    private Point oldUnitPos = null;
    public Set<Point> moveRange = new HashSet<>();
    public Set<Point> attackRange = new HashSet<>();
    private Map<Point, Point> pathParent = new HashMap<>();
    public int currentDay = 1, currentPlayerIdx = 0;
    public List<VersusScreen.PlayerSettings> players;
    public float zoomScale = 3.0f;
    private JPanel canvasPanel;
    private JScrollPane scrollPane;
    private JLabel dayLabel, goldLabel;
    private JPanel playerPanel;
    private JPanel enemyPanel;
    private JLabel enemyNameLabel;
    private JLabel enemyHpLabel;
    private MapUnit selectedEnemy = null;
    private int phaseBannerTimer = 0;
    private String lastLoadedMapPath;

    private JLayeredPane gameLayer;
    private JPanel menuOverlay;
    private boolean isPaused = false;
    
    private Map<String, List<BufferedImage>> mapAnimCache = new HashMap<>();
    private Map<String, BufferedImage> recolorCache = new HashMap<>();
    private BufferedImage battlePlatform, battleBg;
    
    public boolean fogOfWarEnabled = false;
    public boolean[][] visibleTiles;
    
    public enum Weather { NONE, RAIN, SNOW, THUNDERSTORM, SANDSTORM, SNOWSTORM }
    public Weather currentWeather = Weather.NONE;
    public int weatherDaysRemaining = 0;
    public Weather pendingWeather = null;
    public int weatherTransitionTimer = 0;
    private String primaryTilesetName = "Plain";
    
    private BufferedImage rainOverlay;
    private BufferedImage snowOverlay;
    private BufferedImage sandOverlay;
    
    // ── Optimization Fields ──────────────────────────────────
    public boolean fogDirty = true;                       // Recalculate fog only when true
    boolean needsRepaint = true;                   // Only repaint canvas when true
    private List<MapUnit> sortedRenderUnits = new ArrayList<>(); // Pre-sorted render list
    public boolean unitOrderDirty = true;                 // Re-sort only when true
    private BufferedImage battleBuffer;                     // Reusable battle cinematic buffer

    // ── Smooth Camera & Pan Fields ────────────────────────
    public double cameraTargetX = -1, cameraTargetY = -1; // Target viewport center (in pixels)
    private double cameraCurrentX = -1, cameraCurrentY = -1; // Current smooth camera position
    private double panVelocityX = 0, panVelocityY = 0;     // Drag inertia velocity
    private Point lastDragPoint = null;                     // For drag delta calculation

    // ── Path Arrow Preview Fields ─────────────────────────
    private Point hoveredTile = null;                        // Tile currently under mouse cursor
    private Point lastKnownMouseViewPos = null;              // Used for edge cursor panning
    private List<Point> previewPath = new ArrayList<>();     // Preview path from unit to hovered tile
    private Map<MapUnit, Integer> healthBarTimers = new HashMap<>(); // Timed health bar display (frames remaining)
    private Map<MapUnit, Double> animatedHpMap = new HashMap<>();    // Smooth trailing HP bar animation
    private Map<MapUnit, Float> dyingUnitsMap = new HashMap<>();     // Fading out dead units on the map
    private int keyCooldown = 0;                                     // Cooldown for grid movement

    // ── Deploy Overlay Fields ─────────────────────────────
    private JPanel deployOverlay;
    private JPanel deployListContainer;
    private javax.swing.Timer deployAnimTimer;
    private int deployAnimFrame = 0;
    private int deploySelectedIndex = 0;
    private java.util.List<Runnable> deployActions = new ArrayList<>();
    private java.util.List<Runnable> deployHovers = new ArrayList<>();
    private Map<String, List<BufferedImage>> deployPreviewCache = new HashMap<>();
    
    // ── Settings Overlay Fields ───────────────────────────
    private int settingsSelectedIndex = 0;
    private java.util.List<JButton> settingsButtons = new ArrayList<>();
    
    // ── Deploy Preview Fields ──
    private JLabel previewNameLbl;
    private JPanel previewSpritePanel;
    private JLabel[] previewStatLbls;
    private JPanel previewWeaponsBox;
    private String previewCategory;
    private String previewUnitName;
    
    // ── AI State Fields ───────────────────────────────
    AiLogic aiLogic = new AiLogic(this);
    
    private void initWeatherOverlays() {
        if (rainOverlay != null) return;
        
        rainOverlay = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D rg = rainOverlay.createGraphics();
        rg.setColor(new Color(150, 200, 255, 120));
        java.util.Random rnd = new java.util.Random(12345);
        for(int i=0; i<80; i++) {
            int x = rnd.nextInt(256);
            int y = rnd.nextInt(256);
            rg.drawLine(x, y, x - 4, y + 12);
        }
        rg.dispose();
        
        snowOverlay = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = snowOverlay.createGraphics();
        sg.setColor(new Color(255, 255, 255, 200));
        for(int i=0; i<80; i++) {
            int x = rnd.nextInt(256);
            int y = rnd.nextInt(256);
            sg.fillOval(x, y, 3, 3);
        }
        sg.dispose();
        
        sandOverlay = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sdg = sandOverlay.createGraphics();
        sdg.setColor(new Color(230, 190, 100, 120));
        for(int i=0; i<150; i++) {
            int x = rnd.nextInt(256);
            int y = rnd.nextInt(256);
            sdg.drawLine(x, y, x - 12, y + 6);
            sdg.fillOval(x, y, 2, 2);
        }
        sdg.dispose();
    }


    private class BattleActor {
        MapUnit mapUnit;
        AnimationScript script;
        Map<String, BufferedImage> allFrames = new HashMap<>();
        int currentMode = AnimationScript.MODE_STANDING;
        int currentCommandIdx = 0, durationCounter = 0;
        String currentFrameName = null;
        boolean mirror = false;
        boolean isAttacker = false;
        int offsetX = 0, offsetY = 0;
        boolean isFinished = false;
        boolean isWaitingForDamage = false;
        double displayHp, targetHp;
        float alpha = 1.0f;
        int hitPauseTimer = 0;

        BattleActor(MapUnit u, boolean isAttacker, int sw, int sh) {
            this.mapUnit = u;
            this.isAttacker = isAttacker;
            this.displayHp = u.currentHp;
            this.targetHp = u.currentHp;
            this.mirror = !isAttacker;
            loadBattleAssets();
        }

        private void loadBattleAssets() {
            String[] cats = {"Champion", "Unit", "units", "battle", "anims"};
            String uName = mapUnit.unitName;
            
            WeaponItem equipped = mapUnit.getEquipped();
            String[] weapons;
            if (equipped != null && equipped.animWeaponFolder != null) {
                weapons = new String[]{equipped.animWeaponFolder, equipped.weaponType, "Lance", "Spear", "lance", "spear", "Sword", "sword", "Axe", "axe", "Bow", "bow", "Magic", "magic", "Attack"};
            } else {
                weapons = new String[]{"Lance", "Spear", "lance", "spear", "Sword", "sword", "Axe", "axe", "Bow", "bow", "Magic", "magic", "Attack"};
            }
            
            File baseDir = null;
            String[] rootPaths = GamePaths.battleAssetSearchRoots();
            for (String root : rootPaths) {
                for (String cat : cats) {
                    for (String w : weapons) {
                        File dir = new File(root + cat + "/" + uName + "/" + w);
                        if (dir.exists()) { baseDir = dir; break; }
                        dir = new File(root + uName + "/" + w);
                        if (dir.exists()) { baseDir = dir; break; }
                    }
                    if (baseDir != null) break;
                }
                if (baseDir != null) break;
            }
            
            // Fallback to "Walk_Down" or generic movement folders only if no real weapon folder was found
            if (baseDir == null) {
                for (String root : rootPaths) {
                    for (String cat : cats) {
                        File dir = new File(root + cat + "/" + uName + "/Walk_Down");
                        if (dir.exists()) { baseDir = dir; break; }
                        dir = new File(root + uName + "/Walk_Down");
                        if (dir.exists()) { baseDir = dir; break; }
                    }
                    if (baseDir != null) break;
                }
            }
            
            if (baseDir != null) {
                File scriptFile = findScriptRecursively(baseDir);
                if (scriptFile != null) this.script = new AnimationScript(scriptFile);
                loadFramesRecursively(baseDir);
            }
        }

        private File findScriptRecursively(File dir) {
            File[] files = dir.listFiles();
            if (files == null) return null;
            for (File f : files) {
                if (f.getName().equals("script.txt")) return f;
            }
            for (File f : files) {
                if (f.isDirectory()) {
                    File found = findScriptRecursively(f);
                    if (found != null) return found;
                }
            }
            return null;
        }

        private void loadFramesRecursively(File dir) {
            File[] files = dir.listFiles(); if (files == null) return;
            for (File f : files) {
                if (f.isDirectory()) loadFramesRecursively(f);
                else if (f.getName().endsWith(".png")) {
                    try { 
                        java.awt.image.BufferedImage img = game.core.util.AssetManager.getImage(f.getAbsolutePath());
                        if (img != null) allFrames.put(f.getName(), img);
                    } catch (Exception e) {}
                }
            }
        }

        void setMode(int mode) {
            this.currentMode = mode;
            this.currentCommandIdx = 0;
            this.durationCounter = 0;
            this.isFinished = (mode == AnimationScript.MODE_STANDING || mode == 12);
            this.isWaitingForDamage = false;
            this.hitPauseTimer = 0;
        }

        void update() {
            if (displayHp > targetHp) displayHp = Math.max(targetHp, displayHp - 0.5);
            else if (displayHp < targetHp) displayHp = Math.min(targetHp, displayHp + 0.5);

            if (hitPauseTimer > 0) {
                hitPauseTimer--;
                return;
            }

            if (isWaitingForDamage) return;
            List<AnimationCommand> cmds = (script != null) ? script.getCommands(currentMode) : new ArrayList<>();
            if (cmds.isEmpty()) { 
                // Fallback: If script is missing or empty, mark finished but allow frame looping
                isFinished = true; 
            }
            
            if (currentCommandIdx < cmds.size()) {
                AnimationCommand cmd = cmds.get(currentCommandIdx);
                if (cmd.getType() == AnimationCommand.Type.FRAME) {
                    if (durationCounter == 0) {
                        currentFrameName = cmd.getFrameName();
                        if (cmd.getSecondaryCommandCode() != null) handleFE8Command(cmd.getSecondaryCommandCode());
                    }
                    durationCounter++;
                    if (durationCounter >= cmd.getDuration()) { durationCounter = 0; currentCommandIdx++; }
                } else if (cmd.getType() == AnimationCommand.Type.COMMAND) {
                    handleFE8Command(cmd.getCommandCode());
                    currentCommandIdx++;
                } else if (cmd.getType() == AnimationCommand.Type.END) {
                    if (currentMode == AnimationScript.MODE_STANDING || currentMode == 12) {
                        // Loop standing/idle animations indefinitely
                        currentCommandIdx = 0;
                        durationCounter = 0;
                    } else if (currentMode == AnimationScript.MODE_DODGE || currentMode == 8) {
                        // Dodge complete, return to standing pose and mark finished
                        setMode(AnimationScript.MODE_STANDING);
                        isFinished = true;
                    } else {
                        isFinished = true;
                    }
                }
            } else { 
                // Fallback: If script is finished or empty for this mode, just loop frames
                isFinished = true; 
                if (!allFrames.isEmpty()) {
                    List<String> keys = new ArrayList<>(allFrames.keySet());
                    durationCounter++;
                    if (durationCounter > 5) {
                        durationCounter = 0;
                        int idx = keys.indexOf(currentFrameName);
                        currentFrameName = keys.get((idx + 1) % keys.size());
                    }
                }
            }
        }

        private void handleFE8Command(String code) {
            if (code == null) return;
            if (code.equals("C01")) {
                boolean isAttackingMode = (this.currentMode == 1 || this.currentMode == 3 || this.currentMode == 5 || this.currentMode == 6 || this.currentMode == 12);
                if (isAttackingMode && currentHitIdx < activeBattle.hits.size()) {
                    BattleManager.BattleHit hit = activeBattle.hits.get(currentHitIdx);
                    if ((this == attackerActor && hit.isAttacker) || (this == defenderActor && !hit.isAttacker)) {
                        BattleActor target = (this == attackerActor) ? defenderActor : attackerActor;
                        
                        this.hitPauseTimer = 15; // Delay in damage hit frame before moving to the next frame
                        
                        if (combatDistance > 3) {
                            VersusGameplayScreen.this.battleCameraTargetX = (this == VersusGameplayScreen.this.attackerActor) ? -100 : 100;
                            VersusGameplayScreen.this.isBattlePanning = true;
                            VersusGameplayScreen.this.pendingHit = hit;
                            VersusGameplayScreen.this.pendingHitActor = this;
                            VersusGameplayScreen.this.pendingTargetActor = target;
                        } else {
                            VersusGameplayScreen.this.applyHit(this, target, hit);
                        }
                    }
                }
            } else if (code.equals("C06")) {
                setMode(AnimationScript.MODE_STANDING);
                isFinished = true;
            } else {
                // Dispatch script sound commands (C1B, C22, C23, C38, C25, etc.)
                BattleManager.Combatant atkC = (this == attackerActor) ? activeBattle.attacker : activeBattle.defender;
                String wpnType = (atkC != null && atkC.weaponType != null) ? atkC.weaponType.name() : null;
                SoundManager.playScriptCommandSfx(code, wpnType);
            }
        }

        void takeHit(BattleManager.BattleHit hit) {
            if (hit.isMiss && !"Supplier".equalsIgnoreCase(mapUnit.unitName)) { 
                setMode(AnimationScript.MODE_DODGE); 
            } else {
                takeHitActual(hit);
                this.isWaitingForDamage = true;
            }
        }

        void takeHitActual(BattleManager.BattleHit hit) {
            this.targetHp = Math.max(0, (int)this.targetHp - hit.damage);
            if (hit.isMiss) {
                shakeTimer = 0;
                flashTimer = 0;
            } else {
                shakeTimer = hit.isCrit ? 15 : 6;
                flashTimer = hit.isCrit ? 8 : 0;
            }

            // Play death sound if unit is killed
            boolean isDead = (this.targetHp <= 0);

            new javax.swing.Timer(600, e -> {
                ((javax.swing.Timer)e.getSource()).stop();
                if (attackerActor != null) attackerActor.isWaitingForDamage = false;
                if (defenderActor != null) defenderActor.isWaitingForDamage = false;
                if (isDead) SoundManager.playHumanFall();
            }).start();
        }

        void draw(Graphics2D g) {
            if (allFrames.isEmpty()) return;
            String frameName = currentFrameName != null ? currentFrameName : (allFrames.keySet().iterator().hasNext() ? allFrames.keySet().iterator().next() : "");
            BufferedImage rawImg = allFrames.get(frameName);
            if (rawImg == null && !frameName.endsWith(".png")) rawImg = allFrames.get(frameName + ".png");
            if (rawImg == null && !allFrames.isEmpty()) rawImg = allFrames.values().iterator().next();
            if (rawImg == null) return;

            boolean needsManualFlip = mirror;
            if (rawImg.getWidth() > 248) {
                rawImg = rawImg.getSubimage(0, 0, 248, Math.min(160, rawImg.getHeight()));
            }
            final BufferedImage finalImg = rawImg;
            final boolean finalFlip = needsManualFlip;

            final Color teamColor = players.get(mapUnit.ownerIndex).color;
            // Key must include mirror to distinguish between attacker/defender panels in the cache
            String cacheKey = mapUnit.unitName + "_" + frameName + "_" + mirror + "_" + teamColor.getRGB();
            BufferedImage colored = recolorCache.computeIfAbsent(cacheKey, k -> SpriteColorer.recolor(finalImg, teamColor));
            
            int fw = colored.getWidth();
            int fh = colored.getHeight();
            
            int baseShift = 0;
            if (combatDistance > 3) {
                baseShift = isAttacker ? 100 : -100;
            } else if (combatDistance >= 2) {
                baseShift = isAttacker ? 40 : -40;
            } else {
                baseShift = isAttacker ? 5 : -5;
            }
            int dx = baseShift + offsetX;
            int dy = offsetY;
            
            Composite oldComp = g.getComposite();
            if (alpha < 1.0f) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            }
            
            if (finalFlip) {
                // Manually flip the image if it wasn't pre-mirrored in the spritesheet
                g.drawImage(colored, dx + fw, dy, -fw, fh, null);
            } else {
                g.drawImage(colored, dx, dy, fw, fh, null);
            }
            
            if (alpha < 1.0f) {
                g.setComposite(oldComp);
            }
            
            if (offsetX != 0) offsetX = (offsetX > 0) ? -offsetX + 1 : -offsetX - 1;
        }
    }

    private BattleManager.BattleResult activeBattle = null;
    public boolean isBattleActive = false;
    private BattleActor attackerActor, defenderActor;
    private int currentHitIdx = 0;
    private int flashTimer = 0, shakeTimer = 0, battleEndDelay = 0;
    private int combatDistance = 1;

    // ── Cinematic Panning State ──────────────────────────────
    private double battleCameraX = 0;
    private double battleCameraTargetX = 0;
    private boolean isBattlePanning = false;
    private BattleManager.BattleHit pendingHit = null;
    private BattleActor pendingHitActor = null;
    private BattleActor pendingTargetActor = null;

    // ── Capture Animation State ───────────────────────────────
    public boolean isCaptureAnimActive = false;
    private EventInfo captureAnimEvent = null;
    private MapUnit captureAnimUnit = null;
    private double captureBarDisplay = 40.0;  // Animated display HP (counts down smoothly)
    private double captureBarTarget  = 40.0;  // Target HP after this capture action
    private int captureAnimTimer = 0;          // Delay counter before marking action complete

    public class EventInfo {
        public int x, y, owner; public String type;
        // Capture state
        public int captureHp = 40;              // Event's capture HP (starts at 40, resets to 40)
        public Integer capturingPlayerIdx = null; // Which player index is currently capturing this event
        public EventInfo(int x, int y, String type, int owner) { this.x = x; this.y = y; this.type = type; this.owner = owner; }
    }
    public Map<Point, EventInfo> eventMap = new HashMap<>();
    private Point lastMousePos;

    public VersusGameplayScreen(Main main) {
        super(main);
        setLayout(new BorderLayout());
        initUI();
        scanTilesets(GamePaths.TILESETS);
        loadStaticAssets();
        canvasPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!isBattleActive) {
                    renderGame((Graphics2D) g);
                }
            }
            @Override public Dimension getPreferredSize() {
                return new Dimension((int)(mapW * TILE_SIZE * zoomScale), (int)(mapH * TILE_SIZE * zoomScale));
            }
        };
        canvasPanel.setBackground(new Color(20, 20, 25));
        scrollPane = new JScrollPane(canvasPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        gameLayer = new JLayeredPane();
        add(gameLayer, BorderLayout.CENTER);
        gameLayer.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
        
        gameLayer.add(playerPanel, JLayeredPane.PALETTE_LAYER);
        gameLayer.add(enemyPanel, JLayeredPane.PALETTE_LAYER);

        menuOverlay = buildMenuOverlay();
        menuOverlay.setVisible(false);
        gameLayer.add(menuOverlay, JLayeredPane.MODAL_LAYER);

        deployOverlay = buildDeployOverlay();
        deployOverlay.setVisible(false);
        gameLayer.add(deployOverlay, JLayeredPane.MODAL_LAYER);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                layoutGameLayer();
            }
        });
        layoutGameLayer();
        setupListeners();
        new javax.swing.Timer(16, e -> update()).start();
    }

    @Override
    public void paint(Graphics g) {
        if (isBattleTransitioning && transitionBackground == null && getWidth() > 0 && getHeight() > 0) {
            transitionBackground = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = transitionBackground.createGraphics();
            super.paint(g2);
            g2.dispose();
        }
        
        super.paint(g);
        if (weatherTransitionTimer > 0) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        } else if (isBattleActive) {
            renderBattleCinematic((Graphics2D) g);
        } else if (phaseBannerTimer > 0) {
            drawPhaseBanner((Graphics2D) g);
        }
        
        if (isBattleTransitioning) {
            drawBattleTransitionOverlay((Graphics2D) g);
        }
    }

    private void drawBattleTransitionOverlay(Graphics2D g) {
        if (transitionAttacker == null || transitionDefender == null || transitionBackground == null) return;
        int sw = getWidth();
        int sh = getHeight();
        
        // Progress from 0.0 (start) to 1.0 (end of transition)
        double p = (6 - battleTransitionTimer) / 6.0;
        double ease = p * p * (3 - 2 * p); // Smoothstep
        
        Composite oldComp = g.getComposite();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        
        // Draw black background first so the map underneath is hidden
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, sw, sh);
        
        // Draw 2 translucent scaled layers of the captured screen to create a fast motion blur / zoom blur
        int layers = 2;
        for (int i = 0; i < layers; i++) {
            // Delay the ease for each layer to create a "trailing" motion blur
            double layerEase = Math.max(0, ease - (i * 0.15)); 
            
            // Zoom up to 3.0x magnification at the end of the transition
            double S = 1.0 + (2.0 * layerEase); 
            
            int drawW = (int)(sw * S);
            int drawH = (int)(sh * S);
            
            // Offset drawing so it scales directly around the focal point!
            int drawX = (int)(transitionFocalX * (1 - S));
            int drawY = (int)(transitionFocalY * (1 - S));
            
            // The first layer is opaque, trailing layers are highly translucent
            float alpha = (i == 0) ? 1.0f : 0.2f; 
            
            // Fade the entire effect slightly to black at the very end to seamlessly blend into the battle cinematic
            float fade = (float)(1.0 - (p * p * p));
            alpha *= fade;
            
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
            g.drawImage(transitionBackground, drawX, drawY, drawW, drawH, null);
        }
        
        g.setComposite(oldComp);
    }

    private void drawPhaseBanner(Graphics2D g) {
        VersusScreen.PlayerSettings p = players.get(currentPlayerIdx);
        int sw = getWidth(), sh = getHeight();
        g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 180));
        g.fillRect(0, sh/2 - 40, sw, 80);
        g.setColor(Color.WHITE); g.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 32));
        String text = "DAY " + currentDay + " - " + getPlayerColorName(p.color, p.index) + " PHASE";
        FontMetrics fm = g.getFontMetrics(); g.drawString(text, (sw - fm.stringWidth(text))/2, sh/2 + 12);
    }


    private void loadStaticAssets() {
        try {
            File pf = new File(GamePaths.BATTLE_PLATFORMS, "Grass.png");
            if (pf.exists()) battlePlatform = javax.imageio.ImageIO.read(pf);
            File bg = new File(GamePaths.DATA_ROOT, "battle/BG/BG.png");
            if (!bg.exists()) {
                bg = new File(GamePaths.BATTLE_BACKGROUNDS, "Grass.png");
            }
            if (bg.exists()) battleBg = javax.imageio.ImageIO.read(bg);
        } catch (Exception e) {}
    }

    private void initUI() {
        playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        playerPanel.setBackground(new Color(30, 30, 40, 220));
        playerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 215, 0, 100), 3),
            BorderFactory.createEmptyBorder(20, 35, 20, 35)
        ));
        
        dayLabel = new JLabel("DAY 1"); dayLabel.setFont(Theme.getPixelFont(26f)); dayLabel.setForeground(Color.WHITE);
        goldLabel = new JLabel("🪙 0"); goldLabel.setFont(Theme.getPixelFont(26f)); goldLabel.setForeground(Color.YELLOW);
        
        playerPanel.add(dayLabel);
        playerPanel.add(Box.createHorizontalStrut(25));
        playerPanel.add(goldLabel);

        enemyPanel = new JPanel();
        enemyPanel.setLayout(new BoxLayout(enemyPanel, BoxLayout.Y_AXIS));
        enemyPanel.setBackground(new Color(30, 30, 40, 220));
        enemyPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 100, 100, 150), 3),
            BorderFactory.createEmptyBorder(25, 40, 25, 40)
        ));
        
        enemyNameLabel = new JLabel("Enemy");
        enemyNameLabel.setFont(Theme.getPixelFont(26f));
        enemyNameLabel.setForeground(Color.WHITE);
        enemyNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        enemyHpLabel = new JLabel("HP: 0/0");
        enemyHpLabel.setFont(Theme.getPixelFont(20f));
        enemyHpLabel.setForeground(new Color(255, 100, 100));
        enemyHpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        enemyPanel.add(enemyNameLabel);
        enemyPanel.add(Box.createVerticalStrut(15));
        enemyPanel.add(enemyHpLabel);
        enemyPanel.setVisible(false);
    }

    private void updateEnemyPanel() {
        if (selectedEnemy != null) {
            enemyNameLabel.setText(selectedEnemy.unitName);
            enemyHpLabel.setText("HP: " + selectedEnemy.currentHp + " / " + selectedEnemy.stats.maxHp);
            enemyPanel.setVisible(true);
            enemyPanel.setSize(enemyPanel.getPreferredSize());
            layoutGameLayer();
        } else {
            enemyPanel.setVisible(false);
        }
    }

    private void layoutGameLayer() {
        if (gameLayer == null) return;
        int w = gameLayer.getWidth();
        int h = gameLayer.getHeight();
        if (w <= 0 || h <= 0) return;
        scrollPane.setBounds(0, 0, w, h);
        if (menuOverlay != null) menuOverlay.setBounds(0, 0, w, h);
        
        int my = 0;
        if (lastKnownMouseViewPos != null) {
            my = lastKnownMouseViewPos.y;
        } else if (hoveredTile != null) {
            JViewport viewPort = scrollPane.getViewport();
            my = (int)(hoveredTile.y * 16 * zoomScale) - viewPort.getViewPosition().y;
        }
        boolean shiftDown = (my < 150);
        
        if (playerPanel != null) {
            Dimension pSize = playerPanel.getPreferredSize();
            int py = shiftDown ? (h - pSize.height - 10) : 10;
            playerPanel.setBounds(10, py, pSize.width, pSize.height);
        }
        if (enemyPanel != null && enemyPanel.isVisible()) {
            Dimension eSize = enemyPanel.getPreferredSize();
            int ey = shiftDown ? (h - eSize.height - 10) : 10;
            enemyPanel.setBounds(w - eSize.width - 10, ey, eSize.width, eSize.height);
        }
        if (deployOverlay != null) deployOverlay.setBounds(0, 0, w, h);
    }

    private JPanel buildMenuOverlay() {
        JPanel overlay = new JPanel(new BorderLayout());
        overlay.setOpaque(false);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(15, 15, 25, 200));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(350, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 2, Theme.GOLD_TRANS),
            BorderFactory.createEmptyBorder(100, 40, 100, 40)
        ));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Font largeFont = Theme.getPixelFont(28f);
        Dimension btnSize = new Dimension(270, 60);

        JButton resume = new JButton("RESUME");
        resume.setFont(largeFont);
        resume.setMaximumSize(btnSize);
        resume.setAlignmentX(Component.LEFT_ALIGNMENT);
        resume.addActionListener(e -> setMenuOpen(false));

        JButton settings = new JButton("SETTINGS");
        settings.setFont(largeFont);
        settings.setMaximumSize(btnSize);
        settings.setAlignmentX(Component.LEFT_ALIGNMENT);
        settings.addActionListener(e -> {
            game.core.util.SoundManager.playButtonSound();
            main.getSettingsScreen().setBackScreen(Main.VERSUS_GAMEPLAY);
            main.showScreen(Main.SETTINGS);
        });

        JButton exit = new JButton("EXIT");
        exit.setFont(largeFont);
        exit.setMaximumSize(btnSize);
        exit.setAlignmentX(Component.LEFT_ALIGNMENT);
        exit.addActionListener(e -> onExitRequested());

        panel.add(resume);
        panel.add(Box.createVerticalStrut(25));
        panel.add(settings);
        panel.add(Box.createVerticalStrut(25));
        panel.add(exit);

        settingsButtons.clear();
        settingsButtons.add(resume);
        settingsButtons.add(settings);
        settingsButtons.add(exit);
        
        for (int i = 0; i < settingsButtons.size(); i++) {
            final int idx = i;
            JButton btn = settingsButtons.get(i);
            btn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    settingsSelectedIndex = idx;
                    updateSettingsHover();
                }
            });
        }
        updateSettingsHover();

        overlay.add(panel, BorderLayout.WEST);

        overlay.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { setMenuOpen(false); }
        });
        panel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { e.consume(); }
        });

        return overlay;
    }

    private void updateSettingsHover() {
        for (int i = 0; i < settingsButtons.size(); i++) {
            JButton btn = settingsButtons.get(i);
            if (i == settingsSelectedIndex) {
                btn.setForeground(Theme.HIGHLIGHT);
            } else {
                btn.setForeground(UIManager.getColor("Button.foreground"));
            }
        }
    }

    private void toggleMenu() { setMenuOpen(menuOverlay == null || !menuOverlay.isVisible()); }

    private void setMenuOpen(boolean open) {
        if (menuOverlay == null) return;
        if (open) {
            settingsSelectedIndex = 0;
            updateSettingsHover();
        }
        menuOverlay.setVisible(open);
        isPaused = open;
        if (!open) canvasPanel.requestFocusInWindow();
        repaint();
    }

    private void onExitRequested() {
        Object[] options = {"Save", "Don't Save", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Do you want to save the game before exiting?",
            "Exit Versus",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        if (choice == 0) {
            try {
                SaveManager.saveVersus(buildSaveData());
                JOptionPane.showMessageDialog(this, "Versus game saved.");
                setMenuOpen(false);
                main.showScreen(Main.MENU);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save Versus game.");
                ex.printStackTrace();
            }
        } else if (choice == 1) {
            setMenuOpen(false);
            main.showScreen(Main.VERSUS);
        }
    }

    private VersusSaveData buildSaveData() {
        VersusSaveData d = new VersusSaveData();
        d.mapPath = (this.lastLoadedMapPath != null) ? this.lastLoadedMapPath : "";
        d.currentDay = this.currentDay;
        d.currentPlayerIdx = this.currentPlayerIdx;

        if (players != null) {
            for (int i = 0; i < players.size(); i++) {
                VersusScreen.PlayerSettings p = players.get(i);
                VersusSaveData.PlayerData pd = new VersusSaveData.PlayerData();
                pd.index = p.index;
                pd.isAI = p.isAI;
                pd.gold = p.gold;
                pd.color = p.color;
                d.players.add(pd);
            }
        }

        for (MapUnit u : units) {
            if (u == null) continue;
            VersusSaveData.UnitData ud = new VersusSaveData.UnitData();
            ud.category = u.category;
            ud.unitName = u.unitName;
            ud.ownerIndex = u.ownerIndex;
            Point pos = (u.position != null) ? u.position : new Point(0, 0);
            ud.x = pos.x;
            ud.y = pos.y;
            ud.currentHp = u.currentHp;
            ud.maxHp = (u.stats != null ? u.stats.maxHp : u.currentHp);
            ud.hasActed = u.hasActed;
            ud.hasMoved = u.hasMoved;
            ud.isDead = u.isDead;
            ud.equippedSlot = u.equippedSlot;
            if (u.inventory != null) {
                for (game.core.unit.WeaponItem item : u.inventory) {
                    ud.inventoryNames.add(item.name);
                    ud.inventoryUses.add(item.currentUses);
                }
            }
            if (u.loadedUnits != null) {
                for (MapUnit lu : u.loadedUnits) {
                    VersusSaveData.UnitData lud = new VersusSaveData.UnitData();
                    lud.category = lu.category;
                    lud.unitName = lu.unitName;
                    lud.ownerIndex = lu.ownerIndex;
                    lud.currentHp = lu.currentHp;
                    lud.maxHp = (lu.stats != null ? lu.stats.maxHp : lu.currentHp);
                    lud.equippedSlot = lu.equippedSlot;
                    if (lu.inventory != null) {
                        for (game.core.unit.WeaponItem item : lu.inventory) {
                            lud.inventoryNames.add(item.name);
                            lud.inventoryUses.add(item.currentUses);
                        }
                    }
                    ud.loadedUnits.add(lud);
                }
            }
            d.units.add(ud);
        }
        return d;
    }

    public void loadSavedGame(VersusSaveData save) {
        if (save == null) return;

        // Reset current state
        this.units.clear();
        this.loadedTilesets.clear();
        this.mapAnimCache.clear();
        this.recolorCache.clear();
        this.eventMap.clear();

        this.currentDay = Math.max(1, save.currentDay);
        this.currentPlayerIdx = Math.max(0, save.currentPlayerIdx);

        // Rebuild players list
        List<VersusScreen.PlayerSettings> rebuilt = new ArrayList<>();
        for (int i = 0; i < save.players.size(); i++) {
            VersusSaveData.PlayerData pd = save.players.get(i);
            VersusScreen.PlayerSettings ps = new VersusScreen.PlayerSettings(pd.index, pd.color);
            ps.isAI = pd.isAI;
            ps.gold = pd.gold;
            rebuilt.add(ps);
        }
        this.players = rebuilt;

        // Load map
        this.lastLoadedMapPath = save.mapPath;
        if (save.mapPath != null && !save.mapPath.isBlank()) {
            loadMap(save.mapPath);
        }

        // Rebuild units
        for (VersusSaveData.UnitData ud : save.units) {
            MapUnit u = new MapUnit(ud.category, ud.unitName, MapUnit.Faction.PLAYER, new Point(ud.x, ud.y));
            u.ownerIndex = ud.ownerIndex;
            u.position = new Point(ud.x, ud.y);
            u.renderPos = new java.awt.geom.Point2D.Double(ud.x, ud.y);
            u.currentHp = ud.currentHp;
            u.stats.maxHp = ud.maxHp;
            u.hasActed = ud.hasActed;
            u.hasMoved = ud.hasMoved;
            u.isDead = ud.isDead;
            u.equippedSlot = ud.equippedSlot;
            if (ud.inventoryNames != null) {
                for (int j = 0; j < ud.inventoryNames.size(); j++) {
                    game.core.unit.WeaponItem w = game.core.unit.WeaponItem.byName(ud.inventoryNames.get(j));
                    if (ud.inventoryUses != null && j < ud.inventoryUses.size()) {
                        w.currentUses = ud.inventoryUses.get(j);
                    }
                    u.inventory.add(w);
                }
            }
            if (ud.loadedUnits != null) {
                for (VersusSaveData.UnitData lud : ud.loadedUnits) {
                    MapUnit lu = new MapUnit(lud.category, lud.unitName, MapUnit.Faction.PLAYER, new Point(-1, -1));
                    lu.ownerIndex = lud.ownerIndex;
                    lu.currentHp = lud.currentHp;
                    lu.stats.maxHp = lud.maxHp;
                    lu.equippedSlot = lud.equippedSlot;
                    if (lud.inventoryNames != null) {
                        for (int k = 0; k < lud.inventoryNames.size(); k++) {
                            game.core.unit.WeaponItem w = game.core.unit.WeaponItem.byName(lud.inventoryNames.get(k));
                            if (lud.inventoryUses != null && k < lud.inventoryUses.size()) {
                                w.currentUses = lud.inventoryUses.get(k);
                            }
                            lu.inventory.add(w);
                        }
                    }
                    u.loadedUnits.add(lu);
                }
            }
            units.add(u);
            loadAnims(u);
        }

        VersusScreen.PlayerSettings p = players.get(currentPlayerIdx);
        dayLabel.setText("DAY " + currentDay + " - PLAYER " + (currentPlayerIdx + 1));
        dayLabel.setForeground(p.color);
        goldLabel.setText("🪙 " + p.gold);
        layoutGameLayer();
        recolorCache.clear();
        fogDirty = true;
        unitOrderDirty = true;
        needsRepaint = true;
        canvasPanel.revalidate();
        canvasPanel.repaint();
    }

    public void setupGame(String path, List<VersusScreen.PlayerSettings> players) {
        this.players = players; this.currentDay = 1; this.currentPlayerIdx = 0;
        this.units.clear(); this.loadedTilesets.clear(); this.mapAnimCache.clear(); this.recolorCache.clear(); this.eventMap.clear();
        this.lastLoadedMapPath = path;
        loadMap(path);
        startTurn();
        canvasPanel.revalidate();
    }

    public boolean isPlayerVisionActive(int ownerIndex) {
        if (players == null || players.isEmpty() || ownerIndex < 0 || ownerIndex >= players.size()) {
            return ownerIndex == currentPlayerIdx;
        }
        VersusScreen.PlayerSettings current = players.get(currentPlayerIdx);
        if (current.isAI) {
            boolean hasHuman = false;
            for (VersusScreen.PlayerSettings p : players) {
                if (!p.isAI) { hasHuman = true; break; }
            }
            if (!hasHuman) return true;
            return !players.get(ownerIndex).isAI;
        } else {
            return ownerIndex == currentPlayerIdx;
        }
    }

    public void updateFogOfWar() {
        if (!fogOfWarEnabled) return;
        if (mapW == 0 || mapH == 0) return;
        if (visibleTiles == null || visibleTiles.length != mapH || visibleTiles[0].length != mapW) {
            visibleTiles = new boolean[mapH][mapW];
        }
        for (int y = 0; y < mapH; y++) {
            for (int x = 0; x < mapW; x++) {
                visibleTiles[y][x] = false;
            }
        }
        
        for (MapUnit u : units) {
            if (isPlayerVisionActive(u.ownerIndex) && !u.isDead) {
                int r = 3;
                for (int y = -r; y <= r; y++) {
                    for (int x = -r; x <= r; x++) {
                        if (Math.abs(x) + Math.abs(y) <= r) {
                            int nx = u.position.x + x;
                            int ny = u.position.y + y;
                            if (nx >= 0 && nx < mapW && ny >= 0 && ny < mapH) {
                                visibleTiles[ny][nx] = true;
                            }
                        }
                    }
                }
            }
        }
        
        for (EventInfo ev : eventMap.values()) {
            if (isPlayerVisionActive(ev.owner)) {
                int r = 2;
                for (int y = -r; y <= r; y++) {
                    for (int x = -r; x <= r; x++) {
                        if (Math.abs(x) + Math.abs(y) <= r) {
                            int nx = ev.x + x;
                            int ny = ev.y + y;
                            if (nx >= 0 && nx < mapW && ny >= 0 && ny < mapH) {
                                visibleTiles[ny][nx] = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadAnims(MapUnit u) {
        String[] actions = {"Standing", "Walk_Down", "Walk_Up", "Walk_Side", "Selected"};
        for (String a : actions) {
            List<BufferedImage> frames = tryLoadMapUnitFrames(u.category, u.unitName, a);
            
            // Fallback to legacy overrides if no frames found for exact unit name
            if (frames.isEmpty()) {
                String fallbackName = null;
                if ("Knight".equalsIgnoreCase(u.unitName)) fallbackName = "Cavalier";
                else if ("Sentinel".equalsIgnoreCase(u.unitName)) fallbackName = "Swordmaster";
                else if ("Ephraim".equalsIgnoreCase(u.unitName)) fallbackName = "Ephraim_Lord";
                
                if (fallbackName != null) {
                    frames = tryLoadMapUnitFrames(u.category, fallbackName, a);
                } else if ("Pegasus".equalsIgnoreCase(u.unitName)) {
                    frames = tryLoadMapUnitFrames("Unit", "Pegasus Knight", a);
                }
            }
            
            if (!frames.isEmpty()) {
                mapAnimCache.put(u.unitName + "_" + a, frames);
            }
        }
    }

    private List<BufferedImage> tryLoadMapUnitFrames(String category, String unitName, String action) {
        List<BufferedImage> frames = new ArrayList<>();
        
        // 1. Try new structure: assets/data/units/[category]/[unitName]/MovingAnimation/[action]
        File dir = new File(GamePaths.UNITS, category + "/" + unitName + "/MovingAnimation/" + action);
        
        // 2. Try legacy category-specific weapon structures: lance/Spear/Lance
        if (!dir.exists()) dir = new File(GamePaths.UNITS, category + "/" + unitName + "/lance/" + action);
        if (!dir.exists()) dir = new File(GamePaths.UNITS, category + "/" + unitName + "/Spear/" + action);
        if (!dir.exists()) dir = new File(GamePaths.UNITS, category + "/" + unitName + "/Lance/" + action);
        
        // 3. Fallback to legacy flat structures
        if (!dir.exists()) dir = new File(GamePaths.UNITS, unitName + "/MovingAnimation/" + action);
        if (!dir.exists()) dir = new File(GamePaths.UNITS, unitName + "/lance/" + action);
        if (!dir.exists()) dir = new File(GamePaths.UNITS, unitName + "/Spear/" + action);
        if (!dir.exists()) dir = new File(GamePaths.UNITS, unitName + "/Lance/" + action);
        
        if (dir.exists()) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".png"));
            if (files != null) {
                Arrays.sort(files);
                for (File f : files) {
                    try {
                        java.awt.image.BufferedImage img = game.core.util.AssetManager.getImage(f.getAbsolutePath());
                        if (img != null) frames.add(img);
                    } catch (Exception e) {}
                }
            }
        }
        return frames;
    }

    public void nextTurn() {
        currentPlayerIdx++;
        if (currentPlayerIdx >= players.size()) { currentPlayerIdx = 0; currentDay++; }
        startTurn();
    }

    private void startTurn() {
        VersusScreen.PlayerSettings p = players.get(currentPlayerIdx);
        int income = 0;
        if (currentDay > 1) {
            income = 100;
            for (EventInfo ev : eventMap.values()) if (ev.owner == currentPlayerIdx) {
                if ("HQ".equals(ev.type)) income += 200; else if ("ARMORY".equals(ev.type)) income += 100; else if ("HOUSE".equals(ev.type)) income += 50; else if ("FORT".equals(ev.type)) income += 100; else if ("AERIE".equals(ev.type)) income += 100;
            }
        }
        p.gold += income;
        dayLabel.setText("DAY " + currentDay + " - PLAYER " + (currentPlayerIdx + 1));
        dayLabel.setForeground(p.color);
        goldLabel.setText("🪙 " + p.gold);
        layoutGameLayer();
        
        // ── Weather Logic ──
        if (currentPlayerIdx == 0) {
            weatherDaysRemaining--;
            if (weatherDaysRemaining <= 0) {
                Weather nextWeather = Weather.NONE;
                double roll = Math.random();
                
                String ts = primaryTilesetName.toLowerCase();
                if (ts.contains("desert")) {
                    if (roll < 0.55) nextWeather = Weather.NONE;
                    else if (roll < 0.75) nextWeather = Weather.SANDSTORM;
                    else if (roll < 0.90) nextWeather = Weather.RAIN;
                    else nextWeather = Weather.THUNDERSTORM;
                } else if (ts.contains("snow")) {
                    if (roll < 0.55) nextWeather = Weather.NONE;
                    else if (roll < 0.85) nextWeather = Weather.SNOW;
                    else nextWeather = Weather.SNOWSTORM;
                } else {
                    if (roll < 0.55) nextWeather = Weather.NONE;
                    else if (roll < 0.70) nextWeather = Weather.RAIN;
                    else if (roll < 0.85) nextWeather = Weather.SNOW;
                    else nextWeather = Weather.THUNDERSTORM;
                }
                
                if (nextWeather != currentWeather) {
                    pendingWeather = nextWeather;
                    weatherTransitionTimer = 60; // 1 second blank screen
                } else {
                    weatherDaysRemaining = (currentWeather == Weather.NONE) ? 5 : 3;
                }
            }
        }
        
        if (currentWeather == Weather.THUNDERSTORM) {
            boolean struck = false;
            for (MapUnit u : units) {
                if (!u.isDead && u.ownerIndex == currentPlayerIdx) {
                    if (Math.random() < 0.20) { // 20% chance to be struck by lightning
                        u.takeDamage(10);
                        flashTimer = 15; // Trigger lightning flash effect
                        healthBarTimers.put(u, 180); // Show health bar for ~3 seconds (180 frames at 60fps)
                        struck = true;
                    }
                }
            }
            if (struck) {
                game.core.util.SoundManager.playThunder();
            }
        }
        
        phaseBannerTimer = 120;
        // Reset all units at the start of any turn so they regain their team colors
        for (MapUnit u : units) { u.hasActed = false; u.hasMoved = false; }
        recolorCache.clear(); // Ensure sprites are re-processed with team colors
        fogDirty = true;           // Fog needs recalculation on turn change
        unitOrderDirty = true;     // Re-sort units
        needsRepaint = true;

        // ── Capture reset check ──────────────────────────────────
        // If the unit that was capturing has died or moved off the event tile, reset it
        for (EventInfo ev : eventMap.values()) {
            if (ev.capturingPlayerIdx == null) continue;
            boolean capturerPresent = false;
            for (MapUnit u : units) {
                if (!u.isDead && u.ownerIndex == ev.capturingPlayerIdx
                        && u.position.x == ev.x && u.position.y == ev.y) {
                    capturerPresent = true;
                    break;
                }
            }
            if (!capturerPresent) {
                ev.captureHp = 40;
                ev.capturingPlayerIdx = null;
            }
        }

        canvasPanel.repaint(); repaint();
    }

    private void scanTilesets(File dir) {
        File[] files = dir.listFiles(); if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanTilesets(f);
            else if (f.getName().endsWith(".png")) tilesetFiles.put(f.getName().replace(".png", ""), f);
        }
    }

    private void loadMap(String path) {
        try {
            String json = new String(java.nio.file.Files.readAllBytes(new File(path).toPath()));
            String tsDefault = JsonUtil.extractJsonVal(json, "tileset");
            this.primaryTilesetName = (tsDefault != null && !tsDefault.isEmpty()) ? tsDefault : "Plain";
            int eventsStart = json.indexOf("\"events\":");
            if (eventsStart != -1) {
                int arrayStart = json.indexOf("[", eventsStart);
                int depth = 0, arrayEnd = -1;
                for (int i = arrayStart; i < json.length(); i++) {
                    char ch = json.charAt(i); if (ch == '[') depth++; else if (ch == ']') depth--;
                    if (depth == 0) { arrayEnd = i; break; }
                }
                if (arrayEnd != -1) {
                    String eventsPart = json.substring(arrayStart + 1, arrayEnd);
                    String[] eventEntries = eventsPart.split("\\},");
                    for (String entry : eventEntries) {
                        entry = entry.trim(); if (entry.isEmpty()) continue;
                        if (!entry.startsWith("{")) entry = "{" + entry; if (!entry.endsWith("}")) entry = entry + "}";
                        int ex = JsonUtil.parseInt(JsonUtil.extractJsonVal(entry, "x"), -1);
                        int ey = JsonUtil.parseInt(JsonUtil.extractJsonVal(entry, "y"), -1);
                        String et = JsonUtil.extractJsonVal(entry, "type");
                        if ("BASE".equals(et)) et = "ARMORY";
                        int eo = JsonUtil.parseInt(JsonUtil.extractJsonVal(entry, "owner"), -1);
                        if (ex != -1) eventMap.put(new Point(ex, ey), new EventInfo(ex, ey, et, eo));
                    }
                }
            }
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
                    mapH = rows.length;
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
                                loadedTilesets.put(ts, new Tileset(ts, tilesetFiles.get(ts), 16, 16));
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void drawCursor(Graphics2D g, int tx, int ty) {
        int px = tx * 16;
        int py = ty * 16;
        
        if (Theme.MAP_CURSOR != null) {
            long t = System.currentTimeMillis();
            // Typical Fire Emblem cursors have 3 frames of animation
            int frame = (int) ((t / 150) % 3); 
            
            // Assuming 32x32 frames in the spritesheet
            int cw = 32;
            int ch = 32;
            int sx = frame * cw;
            int sy = 0;
            
            // Center the 32x32 cursor over the 16x16 tile (-8px offset)
            int dx = px - 8;
            int dy = py - 8;
            
            g.drawImage(Theme.MAP_CURSOR, dx, dy, dx + cw, dy + ch, sx, sy, sx + cw, sy + ch, null);
            return;
        }

        long t = System.currentTimeMillis();
        int offset = (int)(Math.sin(t / 150.0) * 2); // Animate in and out by 2 pixels
        
        Color cursorColor = new Color(255, 255, 255, 200);
        if (players != null && currentPlayerIdx >= 0 && currentPlayerIdx < players.size()) {
            Color pColor = players.get(currentPlayerIdx).color;
            cursorColor = new Color(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 200);
        }
        
        g.setStroke(new BasicStroke(2f));
        g.setColor(cursorColor);
        
        int size = 16;
        
        // Draw 4 corners moving in and out
        // Top-left
        g.drawPolyline(new int[]{px - offset, px - offset, px + 3 - offset}, 
                       new int[]{py + 3 - offset, py - offset, py - offset}, 3);
        // Top-right
        g.drawPolyline(new int[]{px + size - 3 + offset, px + size + offset, px + size + offset}, 
                       new int[]{py - offset, py - offset, py + 3 - offset}, 3);
        // Bottom-right
        g.drawPolyline(new int[]{px + size + offset, px + size + offset, px + size - 3 + offset}, 
                       new int[]{py + size - 3 + offset, py + size + offset, py + size + offset}, 3);
        // Bottom-left
        g.drawPolyline(new int[]{px + 3 - offset, px - offset, px - offset}, 
                       new int[]{py + size + offset, py + size + offset, py + size - 3 + offset}, 3);
                       
        // Inner fill
        g.setColor(new Color(cursorColor.getRed(), cursorColor.getGreen(), cursorColor.getBlue(), 40));
        g.fillRect(px, py, size, size);
    }

    private void renderGame(Graphics2D g) {
        if (mapData == null) return;
        
        // Only recalculate fog when state has changed
        if (fogDirty) {
            updateFogOfWar();
            fogDirty = false;
        }
        
        g.scale(zoomScale, zoomScale);
        
        // ── Viewport culling: calculate visible tile range ──
        Rectangle viewRect = scrollPane.getViewport().getViewRect();
        int startX = Math.max(0, (int)(viewRect.x / (TILE_SIZE * zoomScale)) - 1);
        int startY = Math.max(0, (int)(viewRect.y / (TILE_SIZE * zoomScale)) - 1);
        int endX = Math.min(mapW, (int)((viewRect.x + viewRect.width) / (TILE_SIZE * zoomScale)) + 2);
        int endY = Math.min(mapH, (int)((viewRect.y + viewRect.height) / (TILE_SIZE * zoomScale)) + 2);
        
        // ── Draw only visible tiles ──
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                String tsName = mapTSData[y][x]; 
                if (tsName != null && tsName.equalsIgnoreCase("Plain") && 
                    (currentWeather == Weather.SNOW || currentWeather == Weather.SNOWSTORM) &&
                    loadedTilesets.containsKey("Snow")) {
                    tsName = "Snow";
                }
                Tileset ts = loadedTilesets.get(tsName);
                if (ts != null) g.drawImage(ts.getTile(mapData[y][x]), x*16, y*16, 16, 16, null);
                else g.fillRect(x*16, y*16, 16, 16);
            }
        }
        
        // ── Draw flags (only visible ones) ──
        for (EventInfo ev : eventMap.values()) {
            if (ev.x >= startX && ev.x < endX && ev.y >= startY && ev.y < endY) {
                drawFlag(g, ev);
            }
        }
        
        // ── Draw range overlays (only visible tiles) ──
        for (Point p : attackRange) {
            if (p.x >= startX && p.x < endX && p.y >= startY && p.y < endY) {
                g.setColor(new Color(255,50,50,120)); g.fillRect(p.x*16, p.y*16, 16, 16);
            }
        }
        for (Point p : moveRange) {
            if (p.x >= startX && p.x < endX && p.y >= startY && p.y < endY) {
                g.setColor(new Color(0,100,255,120)); g.fillRect(p.x*16, p.y*16, 16, 16);
            }
        }
        
        // ── Draw path arrows (preview path from selected unit to hovered tile) ──
        if (!previewPath.isEmpty()) {
            drawPathArrows(g, previewPath, startX, startY, endX, endY);
        }
        
        // ── Draw Cursor (Animated) ──
        if (hoveredTile != null || selectedUnit != null) {
            Point target = (hoveredTile != null) ? hoveredTile : selectedUnit.position;
            if (target.x >= startX && target.x < endX && target.y >= startY && target.y < endY) {
                drawCursor(g, target.x, target.y);
            }
        }
        
        // ── Draw units (cached sorted order) ──
        if (unitOrderDirty) {
            sortedRenderUnits = new ArrayList<>(units);
            sortedRenderUnits.sort(Comparator.comparingDouble(u -> u.renderPos.y));
            unitOrderDirty = false;
        }
        for (MapUnit u : sortedRenderUnits) {
            if (fogOfWarEnabled && visibleTiles != null) {
                if (u.position.y >= 0 && u.position.y < mapH && u.position.x >= 0 && u.position.x < mapW) {
                    int ry = (int) Math.round(u.renderPos.y);
                    int rx = (int) Math.round(u.renderPos.x);
                    if (ry >= 0 && ry < mapH && rx >= 0 && rx < mapW) {
                        if (!visibleTiles[ry][rx] && !isPlayerVisionActive(u.ownerIndex)) {
                            continue;
                        }
                    } else if (!isPlayerVisionActive(u.ownerIndex)) {
                        continue;
                    }
                }
            }
            // Viewport cull units: skip if entirely outside visible area
            int ux = (int) Math.round(u.renderPos.x);
            int uy = (int) Math.round(u.renderPos.y);
            if (ux < startX - 2 || ux > endX + 1 || uy < startY - 2 || uy > endY + 1) continue;
            
            String action = "Standing"; if (u == selectedUnit) action = "Selected";
            if (!u.movePath.isEmpty()) {
                Point next = u.movePath.get(0);
                if (next.x > u.renderPos.x) { action = "Walk_Side"; u.renderMirrorX = true; }
                else if (next.x < u.renderPos.x) { action = "Walk_Side"; u.renderMirrorX = false; }
                else if (next.y > u.renderPos.y) { action = "Walk_Down"; }
                else if (next.y < u.renderPos.y) { action = "Walk_Up"; }
            }
            List<BufferedImage> frames = mapAnimCache.get(u.unitName + "_" + action);
            if (frames == null) frames = mapAnimCache.get(u.unitName + "_Standing");
            if (frames != null && !frames.isEmpty()) {
                BufferedImage img = frames.get(u.animFrame % frames.size());
                Color baseColor = players.get(u.ownerIndex).color;
                final Color finalColor = u.hasActed ? Color.GRAY : baseColor;
                String cacheKey = u.unitName + "_" + action + "_" + u.animFrame % frames.size() + "_" + finalColor.getRGB();
                BufferedImage colored = recolorCache.computeIfAbsent(cacheKey, k -> SpriteColorer.recolor(img, finalColor));
                // Align 32x32 unit sprite so its "feet" sit on the tile center/bottom.
                int px = (int) Math.round(u.renderPos.x * 16.0);
                int py = (int) Math.round(u.renderPos.y * 16.0);
                int dx = px - 8;
                int dy = py - 16;
                boolean mirror = u.renderMirrorX;
                if (action.equals("Walk_Up") || action.equals("Walk_Down")) {
                    mirror = false;
                } else if (action.equals("Standing") || action.equals("Selected")) {
                    mirror = u.renderMirrorX;
                }
                
                Composite oldComp = g.getComposite();
                float alpha = dyingUnitsMap.getOrDefault(u, 1.0f);
                if (alpha < 1.0f) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                }

                if (mirror) {
                    g.drawImage(colored, dx + 32, dy, dx, dy + 32, 0, 0, 32, 32, null);
                } else {
                    g.drawImage(colored, dx, dy, 32, 32, null);
                }
                
                if (alpha < 1.0f) {
                    g.setComposite(oldComp);
                }
            }

            // ── Draw health bar if unit qualifies ──
            boolean isHpAnimating = animatedHpMap.containsKey(u) && animatedHpMap.get(u) > u.currentHp;
            boolean showHpBar = (u == selectedUnit)
                || (hoveredTile != null && u.position.equals(hoveredTile))
                || healthBarTimers.containsKey(u)
                || isHpAnimating;
            if (showHpBar) {
                int hpBarPx = (int) Math.round(u.renderPos.x * 16.0);
                int hpBarPy = (int) Math.round(u.renderPos.y * 16.0);
                drawUnitHealthBar(g, u, hpBarPx - 8, hpBarPy + 17);
            }
        }
        
        // ── Draw Fog of War Overlay (viewport-culled) ──
        if (fogOfWarEnabled && visibleTiles != null) {
            g.setColor(new Color(0, 0, 0, 160));
            for (int y = startY; y < endY; y++) {
                for (int x = startX; x < endX; x++) {
                    if (!visibleTiles[y][x]) {
                        g.fillRect(x * 16, y * 16, 16, 16);
                    }
                }
            }
        }
        
        // ── Draw Weather Layer (viewport-culled) ──
        if (currentWeather != Weather.NONE) {
            initWeatherOverlays();
            long t = System.currentTimeMillis();
            // Use viewport bounds instead of full map for weather tiling
            int vx0 = startX * 16;
            int vy0 = startY * 16;
            int vx1 = endX * 16;
            int vy1 = endY * 16;
            
            if (currentWeather == Weather.RAIN || currentWeather == Weather.THUNDERSTORM) {
                if (currentWeather == Weather.THUNDERSTORM) {
                    g.setColor(new Color(0, 0, 30, 80));
                    g.fillRect(vx0, vy0, vx1 - vx0, vy1 - vy0);
                }
                
                int speed = 15;
                int offsetX = (int) (-(t / speed) % 256);
                int offsetY = (int) ((t / (speed / 3)) % 256);
                if (offsetX > 0) offsetX -= 256;
                if (offsetY > 0) offsetY -= 256;
                
                // Snap to tile-aligned start for weather overlay
                int wxStart = ((vx0 / 256) * 256) + offsetX - 256;
                int wyStart = ((vy0 / 256) * 256) + offsetY - 256;
                
                for (int y = wyStart; y < vy1; y += 256) {
                    for (int x = wxStart; x < vx1; x += 256) {
                        g.drawImage(rainOverlay, x, y, null);
                        if (currentWeather == Weather.THUNDERSTORM) {
                            g.drawImage(rainOverlay, x + 128, y - 128, null);
                        }
                    }
                }
                
                if (currentWeather == Weather.THUNDERSTORM && Math.random() < 0.02) {
                    g.setColor(new Color(255, 255, 255, 120));
                    g.fillRect(vx0, vy0, vx1 - vx0, vy1 - vy0);
                    game.core.util.SoundManager.playThunder();
                }
            } else if (currentWeather == Weather.SNOW || currentWeather == Weather.SNOWSTORM) {
                int speed = (currentWeather == Weather.SNOWSTORM) ? 10 : 40;
                int offsetY = (int) ((t / speed) % 256);
                if (offsetY > 0) offsetY -= 256;
                int sway = (currentWeather == Weather.SNOWSTORM) ? (int) (-(t / speed) % 256) : (int) (Math.sin(t / 1000.0) * 16);
                int offsetX = (sway % 256);
                if (offsetX > 0) offsetX -= 256;

                int wxStart = ((vx0 / 256) * 256) + offsetX - 256;
                int wyStart = ((vy0 / 256) * 256) + offsetY - 256;
                
                for (int y = wyStart; y < vy1; y += 256) {
                    for (int x = wxStart; x < vx1 + 256; x += 256) {
                        g.drawImage(snowOverlay, x, y, null);
                        if (currentWeather == Weather.SNOWSTORM) {
                            g.drawImage(snowOverlay, x + 128, y - 128, null);
                        }
                    }
                }
            } else if (currentWeather == Weather.SANDSTORM) {
                int speed = 8;
                int offsetX = (int) (-(t / speed) % 256);
                int offsetY = (int) ((t / (speed * 2)) % 256);
                if (offsetX > 0) offsetX -= 256;
                if (offsetY > 0) offsetY -= 256;

                int wxStart = ((vx0 / 256) * 256) + offsetX - 256;
                int wyStart = ((vy0 / 256) * 256) + offsetY - 256;
                
                for (int y = wyStart; y < vy1; y += 256) {
                    for (int x = wxStart; x < vx1 + 256; x += 256) {
                        g.drawImage(sandOverlay, x, y, null);
                        g.drawImage(sandOverlay, x + 128, y - 64, null);
                    }
                }
            }
        }

        // ── Draw capture bar above the capturing unit on the map ──
        if (isCaptureAnimActive && captureAnimUnit != null) {
            drawCaptureBarAboveUnit(g, captureAnimUnit);
        }
    }

    private String getPlayerColorName(Color c, int playerIndex) {
        if (c == null) return "PLAYER " + (playerIndex + 1);
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        if (r == 0 && g == 162 && b == 232) return "BLUE";
        if (r == 240 && g == 60 && b == 60) return "RED";
        if (r == 60 && g == 200 && b == 100) return "GREEN";
        if (r == 250 && g == 210 && b == 50) return "YELLOW";
        if (r == 210 && g == 80 && b == 210) return "PURPLE";
        if (r == 50 && g == 210 && b == 210) return "CYAN";
        return "PLAYER " + (playerIndex + 1);
    }


    private void renderBattleCinematic(Graphics2D g) {
        int screenW = getWidth();
        int screenH = getHeight();

        // Solid dark backdrop to completely focus on the battle
        g.setColor(new Color(5, 5, 10));
        g.fillRect(0, 0, screenW, screenH);

        // Reuse pre-allocated battle buffer instead of allocating every frame
        if (battleBuffer == null) {
            battleBuffer = new BufferedImage(248, 160, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D bg2d = battleBuffer.createGraphics();
        // Clear buffer before reuse
        bg2d.setComposite(java.awt.AlphaComposite.Clear);
        bg2d.fillRect(0, 0, 248, 160);
        bg2d.setComposite(java.awt.AlphaComposite.SrcOver);

        if (battleBg != null) {
            bg2d.drawImage(battleBg, 0, 0, 248, 160, null);
        } else {
            bg2d.setColor(new Color(30, 30, 40));
            bg2d.fillRect(0, 0, 248, 160);
        }

        bg2d.translate(-battleCameraX, 0);

        if (battlePlatform != null) {
            int platShift1 = 5;
            int platShift2 = 5;
            if (combatDistance > 3) {
                platShift1 = 100;
                platShift2 = 100;
            } else if (combatDistance >= 2) {
                platShift1 = 40;
                platShift2 = 40;
            }
            bg2d.drawImage(battlePlatform, 10 + platShift1, 110, 100, 40, null);
            bg2d.drawImage(battlePlatform, 138 - platShift2, 110, 100, 40, null);
        }

        boolean attackerOnTop = true;
        if (defenderActor != null && defenderActor.currentMode != 9 && defenderActor.currentMode != 11 && defenderActor.currentMode != 7) {
            attackerOnTop = false;
        }
        if (attackerActor != null && attackerActor.currentMode != 9 && attackerActor.currentMode != 11 && attackerActor.currentMode != 7) {
            attackerOnTop = true;
        }

        if (attackerOnTop) {
            if (defenderActor != null) defenderActor.draw(bg2d);
            if (attackerActor != null) attackerActor.draw(bg2d);
        } else {
            if (attackerActor != null) attackerActor.draw(bg2d);
            if (defenderActor != null) defenderActor.draw(bg2d);
        }

        bg2d.dispose();

        // Calculate aspect-ratio scaling to cover the full screen
        double scaleX = (double) screenW / 248.0;
        double scaleY = (double) screenH / 160.0;
        double scale = Math.max(scaleX, scaleY);

        int scaledW = (int) Math.ceil(248 * scale);
        int scaledH = (int) Math.ceil(160 * scale);
        int drawX = (screenW - scaledW) / 2;
        int drawY = (screenH - scaledH) / 2;

        // If the screen is wider than the battle buffer aspect ratio, align to the bottom to avoid cropping platforms
        if (scaleX > scaleY) {
            drawY = screenH - scaledH;
        }

        int sx = 0;
        int sy = 0;
        if (shakeTimer > 0) {
            sx = (int)(Math.random() * 8 * scale - 4 * scale);
            sy = (int)(Math.random() * 8 * scale - 4 * scale);
        }

        g.drawImage(battleBuffer, drawX + sx, drawY + sy, scaledW, scaledH, null);

        if (flashTimer > 0) {
            g.setColor(new Color(255, 255, 255, 180));
            g.fillRect(drawX, drawY, scaledW, scaledH);
        }

        // The Fire Emblem BattleInfo panels will be drawn individually in drawAdvancedStats
        // so we don't need the full-width glassmorphic bar anymore.

        // Calculate scaled dimensions for the UI boxes to perfectly overlay the pixel art
        int boxNativeW = (Theme.BATTLE_INFO_BG != null) ? Theme.BATTLE_INFO_BG.getWidth() : 106;
        int boxNativeH = (Theme.BATTLE_INFO_BG != null) ? Theme.BATTLE_INFO_BG.getHeight() : 42;
        int scaledBoxW = (int) Math.ceil(boxNativeW * scale);
        int scaledBoxH = (int) Math.ceil(boxNativeH * scale);
        
        // Align panels with the bottom corners of the scaled cinematic area
        int defX = drawX;
        int atkX = drawX + scaledW - scaledBoxW;
        int statY = drawY + scaledH - scaledBoxH;

        // Defender stats on the Left, Attacker stats on the Right
        if (defenderActor != null) drawAdvancedStats(g, defenderActor, defX, statY, scale, scaledBoxW, scaledBoxH);
        if (attackerActor != null) drawAdvancedStats(g, attackerActor, atkX, statY, scale, scaledBoxW, scaledBoxH);
    }

    private void drawAdvancedStats(Graphics2D g, BattleActor actor, int x, int y, double scale, int boxW, int boxH) {
        if (actor == null || activeBattle == null) return;
        BattleManager.Combatant c = (actor == attackerActor) ? activeBattle.attacker : activeBattle.defender;
        BattleManager.Combatant enemy = (actor == attackerActor) ? activeBattle.defender : activeBattle.attacker;
        
        // Draw the classic Fire Emblem BattleInfo background panel scaled up
        if (Theme.BATTLE_INFO_BG != null) {
            g.drawImage(Theme.BATTLE_INFO_BG, x, y, boxW, boxH, null);
        } else {
            g.setColor(new Color(20, 20, 30, 200));
            g.fillRect(x, y, boxW, boxH);
        }
        
        // Helper to get scaled offset
        java.util.function.Function<Double, Integer> s = (val) -> (int)Math.round(val * scale);
        
        // Top Row: Name and Weapon
        g.setColor(Color.WHITE);
        g.setFont(Theme.getPixelFont((float)(6.5 * scale))); // Roughly 16f scaled down natively
        g.drawString(c.name, x + s.apply(8.0), y + s.apply(12.0));
        
        String wpnName = (c.mapUnit.getEquipped() != null) ? c.mapUnit.getEquipped().name : "Unarmed";
        g.setFont(Theme.getPixelFont((float)(5.0 * scale)));
        g.setColor(Theme.HIGHLIGHT);
        int wpnW = g.getFontMetrics().stringWidth(wpnName);
        g.drawString(wpnName, x + boxW - s.apply(6.0) - wpnW, y + s.apply(11.0));
        
        // Middle Row: HP Bar and Number
        int hpY = y + s.apply(17.0);
        int barX = x + s.apply(28.0);
        int barY = hpY;
        
        // Draw Health Bar Background Track scaled
        if (Theme.HEALTH_BAR_BG != null) {
            int bgW = (int)Math.round(Theme.HEALTH_BAR_BG.getWidth() * scale);
            int bgH = (int)Math.round(Theme.HEALTH_BAR_BG.getHeight() * scale);
            g.drawImage(Theme.HEALTH_BAR_BG, barX, barY, bgW, bgH, null);
        }
        
        // Draw filled Health Bar (cropped based on percentage)
        if (Theme.HEALTH_BAR != null) {
            double hpPct = Math.max(0.0, Math.min(1.0, actor.displayHp / c.maxHp));
            int nativeFillW = (int) Math.round(Theme.HEALTH_BAR.getWidth() * hpPct);
            if (nativeFillW > 0) {
                // Draw a sub-image to represent the current HP fill
                java.awt.image.BufferedImage hpFill = Theme.HEALTH_BAR.getSubimage(0, 0, nativeFillW, Theme.HEALTH_BAR.getHeight());
                int fillW = (int)Math.round(nativeFillW * scale);
                int fillH = (int)Math.round(Theme.HEALTH_BAR.getHeight() * scale);
                g.drawImage(hpFill, barX, barY, fillW, fillH, null);
            }
        }
        
        // Exact HP text positioned near the right side of the HP bar
        g.setFont(Theme.getPixelFont((float)(5.5 * scale)));
        g.setColor(Color.WHITE);
        String hpStr = (int)Math.round(actor.displayHp) + "";
        g.drawString(hpStr, x + boxW - s.apply(18.0), hpY + s.apply(6.0));
        
        // Bottom Row: DMG, HIT, CRT
        boolean canAttack = true;
        if (c == activeBattle.defender) {
            canAttack = (combatDistance >= c.weaponMinRange && combatDistance <= c.weaponMaxRange);
        }
        int effDmg = Math.max(0, c.battleAtk - enemy.defense);
        int effHit = Math.max(0, Math.min(100, c.battleHit - enemy.battleAvoid));
        int effCrit = Math.max(0, Math.min(100, c.battleCrit - enemy.battleDodge));
        
        String dmgVal = canAttack ? String.valueOf(effDmg) : "--";
        String hitVal = canAttack ? String.valueOf(effHit) : "--";
        String critVal = canAttack ? String.valueOf(effCrit) : "--";
        
        // Column positions within the BattleInfo panel
        int col1X = x + s.apply(23.0);
        int col2X = x + s.apply(54.0);
        int col3X = x + s.apply(84.0);
        int labelY = y + s.apply(28.0);
        int bottomY = y + s.apply(36.0);
        
        // Draw Labels
        g.setFont(Theme.getPixelFont((float)(4.5 * scale)));
        g.setColor(Theme.HIGHLIGHT);
        g.drawString("DMG", col1X, labelY);
        g.drawString("HIT", col2X, labelY);
        g.drawString("CRT", col3X, labelY);
        
        // Draw Numbers
        g.setFont(Theme.getPixelFont((float)(6.5 * scale)));
        g.setColor(Color.WHITE);
        g.drawString(dmgVal, col1X, bottomY);
        g.drawString(hitVal, col2X, bottomY);
        g.drawString(critVal, col3X, bottomY);
    }

    private void drawSubtypeIcons(Graphics2D g, String subtype, int x, int y) {
        if (subtype == null) return;
        String st = subtype.toLowerCase().trim();
        if (st.equals("armored")) {
            drawIcon(g, "shield", x, y);
        } else if (st.equals("mounted")) {
            drawIcon(g, "horse", x, y);
        } else if (st.equals("mounted armored")) {
            drawIcon(g, "shield", x, y);
            drawIcon(g, "horse", x + 18, y);
        } else if (st.equals("siege")) {
            drawIcon(g, "catapult", x, y);
        } else if (st.equals("flier")) {
            drawIcon(g, "pegasus", x, y);
        } else if (st.equals("armored flier")) {
            drawIcon(g, "dragon", x, y);
        }
    }

    private void drawIcon(Graphics2D g, String type, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if ("shield".equalsIgnoreCase(type)) {
            // Armored: Silver Shield Icon
            java.awt.geom.Path2D.Double shield = new java.awt.geom.Path2D.Double();
            shield.moveTo(x + 2, y + 2);
            shield.lineTo(x + 14, y + 2);
            shield.lineTo(x + 14, y + 7);
            shield.curveTo(x + 14, y + 12, x + 10, y + 15, x + 8, y + 16);
            shield.curveTo(x + 6, y + 15, x + 2, y + 12, x + 2, y + 7);
            shield.closePath();
            
            g2.setPaint(new GradientPaint(x + 2, y + 2, new Color(220, 225, 235), x + 14, y + 16, new Color(140, 150, 165)));
            g2.fill(shield);
            g2.setColor(new Color(60, 70, 85));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(shield);
            
            // Highlight line
            g2.setColor(Color.WHITE);
            g2.drawLine(x + 4, y + 4, x + 4, y + 7);
            
        } else if ("horse".equalsIgnoreCase(type)) {
            // Mounted: Brown Horse Icon
            java.awt.geom.Path2D.Double horse = new java.awt.geom.Path2D.Double();
            horse.moveTo(x + 2, y + 9);
            horse.lineTo(x + 5, y + 7);
            horse.lineTo(x + 7, y + 2); // Ear tip
            horse.lineTo(x + 9, y + 4);
            horse.lineTo(x + 9, y + 2); // Second ear tip
            horse.lineTo(x + 11, y + 5);
            horse.curveTo(x + 13, y + 8, x + 14, y + 11, x + 15, y + 15); // Back/Mane
            horse.lineTo(x + 8, y + 15); // Bottom neck
            horse.curveTo(x + 7, y + 12, x + 5, y + 11, x + 2, y + 11); // Front neck
            horse.closePath();
            
            g2.setPaint(new GradientPaint(x + 2, y + 2, new Color(175, 115, 60), x + 15, y + 15, new Color(90, 50, 20)));
            g2.fill(horse);
            g2.setColor(new Color(65, 35, 10));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(horse);
            
            // Mane highlight (Cream)
            g2.setColor(new Color(245, 235, 215));
            g2.drawLine(x + 10, y + 6, x + 12, y + 9);
            g2.drawLine(x + 11, y + 8, x + 13, y + 11);
            
            // Eye
            g2.setColor(Color.BLACK);
            g2.fillOval(x + 5, y + 5, 2, 2);
            
        } else if ("catapult".equalsIgnoreCase(type)) {
            // Siege: Wooden Catapult Icon
            g2.setColor(new Color(120, 80, 40)); // Oak brown
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawLine(x + 2, y + 11, x + 14, y + 11); // Base
            g2.drawLine(x + 12, y + 11, x + 7, y + 5);  // Support back
            g2.drawLine(x + 3, y + 11, x + 7, y + 5);   // Support front
            
            // Draw throwing arm
            g2.setColor(new Color(160, 110, 60)); // Lighter wood
            g2.drawLine(x + 8, y + 10, x + 4, y + 4); // Arm
            
            // Draw stone bucket and stone
            g2.setColor(new Color(120, 80, 40));
            g2.drawOval(x + 2, y + 2, 4, 4); // Bucket
            g2.setColor(new Color(160, 160, 160));
            g2.fillOval(x + 3, y + 3, 2, 2); // Stone
            
            // Draw Wheels
            g2.setColor(new Color(60, 60, 60)); // Dark iron wheels
            g2.fillOval(x + 3, y + 10, 4, 4);
            g2.fillOval(x + 10, y + 10, 4, 4);
            
        } else if ("pegasus".equalsIgnoreCase(type)) {
            // Flier: Pegasus Icon (White with Sky Blue Wing)
            java.awt.geom.Path2D.Double peg = new java.awt.geom.Path2D.Double();
            peg.moveTo(x + 2, y + 12);
            peg.lineTo(x + 5, y + 10);
            peg.lineTo(x + 6, y + 7); // Head/ears
            peg.lineTo(x + 8, y + 12); // Neck
            peg.lineTo(x + 12, y + 14); // Body
            peg.lineTo(x + 5, y + 14);
            peg.closePath();
            
            g2.setColor(Color.WHITE);
            g2.fill(peg);
            g2.setColor(new Color(150, 180, 200));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(peg);
            
            // Draw Sky Blue Wing
            java.awt.geom.Path2D.Double wing = new java.awt.geom.Path2D.Double();
            wing.moveTo(x + 7, y + 11);
            wing.curveTo(x + 9, y + 5, x + 11, y + 2, x + 15, y + 2); // Wing top curve
            wing.lineTo(x + 13, y + 6);
            wing.lineTo(x + 14, y + 7);
            wing.lineTo(x + 11, y + 9);
            wing.lineTo(x + 12, y + 10);
            wing.closePath();
            
            g2.setPaint(new GradientPaint(x + 7, y + 2, new Color(135, 206, 250), x + 15, y + 11, new Color(30, 144, 255))); // Sky blue gradient
            g2.fill(wing);
            g2.setColor(new Color(0, 100, 200));
            g2.draw(wing);
            
        } else if ("dragon".equalsIgnoreCase(type)) {
            // Armored Flier: Dragon Icon (Crimson Red with Golden eye)
            java.awt.geom.Path2D.Double drag = new java.awt.geom.Path2D.Double();
            drag.moveTo(x + 2, y + 12); // Snout tip
            drag.lineTo(x + 6, y + 10); // Jaw
            drag.lineTo(x + 8, y + 14); // Neck bottom
            drag.lineTo(x + 12, y + 14);
            drag.lineTo(x + 11, y + 9);  // Back head
            drag.lineTo(x + 15, y + 3);  // Horn tip
            drag.lineTo(x + 10, y + 6);  // Horn back
            drag.lineTo(x + 8, y + 4);   // Second horn tip
            drag.lineTo(x + 7, y + 7);
            drag.closePath();
            
            g2.setPaint(new GradientPaint(x + 2, y + 4, new Color(230, 40, 40), x + 12, y + 14, new Color(110, 10, 10))); // Dragon crimson
            g2.fill(drag);
            g2.setColor(new Color(60, 0, 0));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(drag);
            
            // Glowing orange/yellow eye
            g2.setColor(Color.ORANGE);
            g2.fillOval(x + 5, y + 8, 2, 2);
        }
        
        g2.dispose();
    }

    private void drawFlag(Graphics2D g, EventInfo ev) {
        Color pc = (ev.owner >= 0 && ev.owner < players.size()) ? players.get(ev.owner).color : new Color(150, 150, 150);
        int x = ev.x * 16, y = ev.y * 16; long t = System.currentTimeMillis() / 200; double wave = Math.sin(t) * 2;
        g.setColor(new Color(100, 100, 100)); g.fillRect(x + 3, y + 2, 1, 13);
        if ("HQ".equals(ev.type)) {
            int[] px = {x + 4, x + 12, (int)(x + 10 + wave), x + 12, x + 4}; int[] py = {y + 2, y + 2, y + 6, y + 10, y + 10};
            g.setColor(pc); g.fillPolygon(px, py, 5); g.setColor(new Color(255,255,255,150)); g.drawPolygon(px, py, 5);
            g.setColor(Color.YELLOW); g.fillRect(x+6, y+4, 3, 2);
        } else if ("HOUSE".equals(ev.type)) {
            // Plain flag — no icon
            int[] px = {x + 4, x + 12, (int)(x + 10 + wave), x + 4}; int[] py = {y + 2, y + 3, y + 6, y + 9};
            g.setColor(pc); g.fillPolygon(px, py, 4); g.setColor(new Color(255,255,255,150)); g.drawPolygon(px, py, 4);
        } else {
            // Simple Flag base design (ARMORY, FORT, AERIE)
            int[] px = {x + 4, x + 12, (int)(x + 10 + wave), x + 4}; int[] py = {y + 2, y + 3, y + 6, y + 9};
            g.setColor(pc); g.fillPolygon(px, py, 4); g.setColor(new Color(255,255,255,150)); g.drawPolygon(px, py, 4);

            if ("ARMORY".equals(ev.type)) {
                // Tiny sword icon on flag
                g.setColor(Color.WHITE);
                g.drawLine(x + 8, y + 3, x + 8, y + 8); // blade (vertical)
                g.drawLine(x + 6, y + 5, x + 10, y + 5); // crossguard (horizontal)
                g.fillRect(x + 8, y + 8, 1, 1);           // pommel tip
            } else if ("FORT".equals(ev.type)) {
                // Tiny ship logo on flag
                g.setColor(Color.WHITE);
                g.fillRect(x + 6, y + 5, 4, 2); // Hull
                g.fillRect(x + 7, y + 3, 1, 2); // Mast
            } else if ("AERIE".equals(ev.type)) {
                // Tiny wings logo on flag
                g.setColor(Color.WHITE);
                int[] wx = {x + 5, x + 6, x + 7, x + 8, x + 9};
                int[] wy = {y + 5, y + 4, y + 6, y + 4, y + 5};
                g.drawPolyline(wx, wy, 5);
            }
        }
    }

    private void setupListeners() {
        canvasPanel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
                int tx = (int)(e.getX()/(16*zoomScale));
                int ty = (int)(e.getY()/(16*zoomScale));
                simulateGridClick(tx, ty, e.getX(), e.getY());
            }
        });
        canvasPanel.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                lastKnownMouseViewPos = null;
                if (lastMousePos != null) {
                    int dx = lastMousePos.x - e.getX();
                    int dy = lastMousePos.y - e.getY();
                    JViewport viewPort = scrollPane.getViewport();
                    Point vPos = viewPort.getViewPosition();
                    vPos.translate(dx, dy);
                    // Clamp to valid viewport bounds
                    int maxX = canvasPanel.getWidth() - viewPort.getWidth();
                    int maxY = canvasPanel.getHeight() - viewPort.getHeight();
                    vPos.x = Math.max(0, Math.min(vPos.x, maxX));
                    vPos.y = Math.max(0, Math.min(vPos.y, maxY));
                    viewPort.setViewPosition(vPos);
                    // Track velocity for inertia
                    panVelocityX = dx * 0.6;
                    panVelocityY = dy * 0.6;
                    lastDragPoint = e.getPoint();
                    lastMousePos = e.getPoint();
                    // Reset camera tracking so drag takes priority
                    cameraTargetX = -1;
                    cameraTargetY = -1;
                }
            }
            @Override public void mouseMoved(MouseEvent e) {
                JViewport viewPort = scrollPane.getViewport();
                Point vPos = viewPort.getViewPosition();
                lastKnownMouseViewPos = new Point(e.getX() - vPos.x, e.getY() - vPos.y);
                
                int tx = (int)(e.getX() / (16 * zoomScale));
                int ty = (int)(e.getY() / (16 * zoomScale));
                Point newHover = new Point(tx, ty);
                if (!newHover.equals(hoveredTile)) {
                    hoveredTile = newHover;
                    game.core.util.SoundManager.playCursor();
                    updatePreviewPath();
                    needsRepaint = true;
                }
                layoutGameLayer();
            }
        });
        canvasPanel.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                lastDragPoint = null; // Inertia will carry from panVelocity
            }
            @Override public void mouseExited(MouseEvent e) {
                lastKnownMouseViewPos = null;
            }
        });
    }

    /**
     * Rebuilds the preview path from the selected unit to the hovered tile.
     * Only generates a path if the unit is selected, hasn't moved yet, and
     * the hovered tile is within the valid move range.
     */
    private void updatePreviewPath() {
        previewPath.clear();
        if (selectedUnit != null && !selectedUnit.hasMoved && hoveredTile != null && moveRange.contains(hoveredTile)) {
            List<Point> path = MovementEngine.reconstructPath(hoveredTile, selectedUnit.position, pathParent);
            if (path != null && !path.isEmpty()) {
                // Prepend unit's current position as the path start
                previewPath.add(new Point(selectedUnit.position));
                previewPath.addAll(path);
            }
        }
    }

    /**
     * Draws Fire Emblem-style directional path arrows on the map.
     * Renders a thick colored path body with corners at turns and an
     * arrowhead triangle at the destination tile.
     */
    private void drawPathArrows(Graphics2D g, List<Point> path, int startX, int startY, int endX, int endY) {
        if (path == null || path.size() < 2) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Use player's team color for the path
        Color teamColor = players.get(currentPlayerIdx).color;
        Color pathFill = new Color(teamColor.getRed(), teamColor.getGreen(), teamColor.getBlue(), 180);
        Color pathBorder = new Color(
            Math.max(0, teamColor.getRed() - 60),
            Math.max(0, teamColor.getGreen() - 60),
            Math.max(0, teamColor.getBlue() - 60),
            220
        );
        Color pathHighlight = new Color(
            Math.min(255, teamColor.getRed() + 60),
            Math.min(255, teamColor.getGreen() + 60),
            Math.min(255, teamColor.getBlue() + 60),
            120
        );

        int hw = 3; // Half-width of path body in tile pixels

        // Draw path body segments
        for (int i = 0; i < path.size(); i++) {
            Point curr = path.get(i);
            // Skip tiles outside viewport
            if (curr.x < startX - 1 || curr.x > endX || curr.y < startY - 1 || curr.y > endY) continue;

            int cx = curr.x * 16 + 8; // Tile center X
            int cy = curr.y * 16 + 8; // Tile center Y

            Point prev = (i > 0) ? path.get(i - 1) : null;
            Point next = (i < path.size() - 1) ? path.get(i + 1) : null;

            // Determine incoming and outgoing directions (dx, dy)
            int inDx = 0, inDy = 0, outDx = 0, outDy = 0;
            if (prev != null) { inDx = curr.x - prev.x; inDy = curr.y - prev.y; }
            if (next != null) { outDx = next.x - curr.x; outDy = next.y - curr.y; }

            if (i == 0 && next != null) {
                // ── Start segment: draw from center toward next tile edge ──
                drawPathRect(g2, cx, cy, outDx, outDy, hw, 8, pathFill, pathBorder);
                // Small start circle
                g2.setColor(pathFill);
                g2.fillOval(cx - hw, cy - hw, hw * 2, hw * 2);
                g2.setColor(pathBorder);
                g2.drawOval(cx - hw, cy - hw, hw * 2, hw * 2);
            } else if (next == null && prev != null) {
                // ── End segment with arrowhead ──
                // Draw body from edge to slightly before center
                drawPathRect(g2, cx, cy, -inDx, -inDy, hw, -5, pathFill, pathBorder);
                // Draw arrowhead triangle
                drawArrowHead(g2, cx, cy, inDx, inDy, pathFill, pathBorder);
            } else if (prev != null && next != null) {
                // ── Middle segment ──
                // Draw incoming half (from edge to center)
                drawPathRect(g2, cx, cy, -inDx, -inDy, hw, -8, pathFill, pathBorder);
                // Draw outgoing half (from center to edge)
                drawPathRect(g2, cx, cy, outDx, outDy, hw, 8, pathFill, pathBorder);
                // Fill center junction
                g2.setColor(pathFill);
                g2.fillRect(cx - hw, cy - hw, hw * 2, hw * 2);
            }
        }

        // Draw highlight pass for glossy effect on the path body
        for (int i = 0; i < path.size() - 1; i++) {
            Point curr = path.get(i);
            Point next = path.get(i + 1);
            if (curr.x < startX - 1 || curr.x > endX || curr.y < startY - 1 || curr.y > endY) continue;

            int cx = curr.x * 16 + 8;
            int cy = curr.y * 16 + 8;
            int nx = next.x * 16 + 8;
            int ny = next.y * 16 + 8;

            g2.setColor(pathHighlight);
            if (cy == ny) { // Horizontal
                int minX = Math.min(cx, nx);
                g2.drawLine(minX, Math.min(cy, ny) - hw + 1, Math.max(cx, nx), Math.min(cy, ny) - hw + 1);
            } else { // Vertical
                int minY = Math.min(cy, ny);
                g2.drawLine(cx - hw + 1, minY, cx - hw + 1, Math.max(cy, ny));
            }
        }

        g2.dispose();
    }

    /**
     * Draws a rectangular path segment extending from a center point in the given direction.
     * @param cx, cy  Center of the tile
     * @param dirX, dirY  Direction of the segment (-1, 0, or 1)
     * @param hw  Half-width of the path body
     * @param length  How far to extend (positive = outward, negative = inward toward center)
     */
    private void drawPathRect(Graphics2D g, int cx, int cy, int dirX, int dirY, int hw, int length, Color fill, Color border) {
        int x, y, w, h;
        if (dirX != 0) {
            // Horizontal segment
            if (dirX > 0) {
                x = cx; y = cy - hw; w = Math.abs(length); h = hw * 2;
            } else {
                x = cx - Math.abs(length); y = cy - hw; w = Math.abs(length); h = hw * 2;
            }
        } else {
            // Vertical segment
            if (dirY > 0) {
                x = cx - hw; y = cy; w = hw * 2; h = Math.abs(length);
            } else {
                x = cx - hw; y = cy - Math.abs(length); w = hw * 2; h = Math.abs(length);
            }
        }
        g.setColor(fill);
        g.fillRect(x, y, w, h);
        g.setColor(border);
        // Draw only the outer edges (not internal connections)
        if (dirX != 0) {
            g.drawLine(x, y, x + w, y);           // Top edge
            g.drawLine(x, y + h - 1, x + w, y + h - 1); // Bottom edge
        } else {
            g.drawLine(x, y, x, y + h);           // Left edge
            g.drawLine(x + w - 1, y, x + w - 1, y + h); // Right edge
        }
    }

    /**
     * Draws a pointed arrowhead triangle at the destination tile, facing the direction of movement.
     */
    private void drawArrowHead(Graphics2D g, int cx, int cy, int dirX, int dirY, Color fill, Color border) {
        int size = 7; // Arrow point size
        int[] xPts, yPts;

        if (dirX > 0) { // Pointing RIGHT
            xPts = new int[]{ cx + size, cx - 2, cx - 2 };
            yPts = new int[]{ cy, cy - size, cy + size };
        } else if (dirX < 0) { // Pointing LEFT
            xPts = new int[]{ cx - size, cx + 2, cx + 2 };
            yPts = new int[]{ cy, cy - size, cy + size };
        } else if (dirY > 0) { // Pointing DOWN
            xPts = new int[]{ cx, cx - size, cx + size };
            yPts = new int[]{ cy + size, cy - 2, cy - 2 };
        } else { // Pointing UP
            xPts = new int[]{ cx, cx - size, cx + size };
            yPts = new int[]{ cy - size, cy + 2, cy + 2 };
        }

        g.setColor(fill);
        g.fillPolygon(xPts, yPts, 3);
        g.setColor(border);
        g.drawPolygon(xPts, yPts, 3);

        // Inner highlight on arrowhead
        g.setColor(new Color(255, 255, 255, 80));
        if (dirX > 0) g.drawLine(cx - 1, cy - size + 2, cx + size - 2, cy);
        else if (dirX < 0) g.drawLine(cx + 1, cy - size + 2, cx - size + 2, cy);
        else if (dirY > 0) g.drawLine(cx - size + 2, cy - 1, cx, cy + size - 2);
        else g.drawLine(cx - size + 2, cy + 1, cx, cy - size + 2);
    }

    /**
     * Draws a compact pixel-art health bar below a map unit sprite.
     * Uses a gradient fill that shifts green → yellow → red based on HP percentage.
     * Rendered in tile-space coordinates so it scrolls and zooms with the map.
     *
     * @param g  Graphics context (already scaled to tile-space)
     * @param u  The map unit to draw the bar for
     * @param x  Left edge of the bar in tile pixels
     * @param y  Top edge of the bar in tile pixels
     */
    private void drawUnitHealthBar(Graphics2D g, MapUnit u, int x, int y) {
        if (u == null || u.stats == null || u.isDead) return;

        int barW = 16;  // Width of the health bar
        int barH = 2;   // Height of the health bar
        // Center the bar under the 32px-wide sprite
        int barX = x + 8;
        int barY = y;

        // ── Dark background track ──
        g.setColor(new Color(10, 10, 15, 210));
        g.fillRect(barX - 1, barY - 1, barW + 2, barH + 2);

        // ── Recessed inner track ──
        g.setColor(new Color(30, 30, 40, 200));
        g.fillRect(barX, barY, barW, barH);

        double targetHp = u.currentHp;
        double animHp = animatedHpMap.getOrDefault(u, targetHp);

        double animPct = Math.max(0.0, Math.min(1.0, animHp / u.stats.maxHp));
        double hpPct = Math.max(0.0, Math.min(1.0, targetHp / u.stats.maxHp));

        int animFillW = (int) Math.round(barW * animPct);
        int fillW = (int) Math.round(barW * hpPct);

        // ── Animated Damage Trail ──
        if (animFillW > fillW) {
            g.setColor(new Color(255, 200, 200)); // Light red/pink trail
            g.fillRect(barX, barY, animFillW, barH);
        }

        // ── HP fill ──
        if (fillW > 0) {
            Color hpColor;
            if (hpPct > 0.55) {
                hpColor = new Color(50, 210, 90);    // Green
            } else if (hpPct > 0.25) {
                hpColor = new Color(230, 200, 30);   // Yellow
            } else {
                hpColor = new Color(220, 55, 45);    // Red
            }
            g.setColor(hpColor);
            g.fillRect(barX, barY, fillW, barH);

            // Glossy highlight line (1px bright strip on top)
            g.setColor(new Color(255, 255, 255, 90));
            g.drawLine(barX, barY, barX + fillW - 1, barY);
        }

        // ── Thin border ──
        g.setColor(new Color(60, 60, 70, 180));
        g.drawRect(barX - 1, barY - 1, barW + 1, barH + 1);
    }

    private void cancelMove() {
        if (selectedUnit != null && oldUnitPos != null) {
            selectedUnit.position = new Point(oldUnitPos); selectedUnit.renderPos.x = oldUnitPos.x; selectedUnit.renderPos.y = oldUnitPos.y;
            selectedUnit.hasMoved = false; selectedUnit.movePath.clear(); selectedUnit = null; oldUnitPos = null; 
            previewPath.clear();
            fogDirty = true; unitOrderDirty = true; needsRepaint = true;
            canvasPanel.repaint();
        }
    }

    public void simulateGridClick(int tx, int ty, int mouseX, int mouseY) {
        if (phaseBannerTimer > 0) phaseBannerTimer = 0;
        if (isBattleActive || isBattleTransitioning) return;
        for (MapUnit u : units) if (!u.movePath.isEmpty()) return;
        if (players == null || players.isEmpty() || players.get(currentPlayerIdx).isAI) return;
        
        Point p = new Point(tx, ty);
        if (selectedUnit != null && selectedUnit.hasMoved && !selectedUnit.hasActed) { cancelMove(); return; }
        if (moveRange.contains(p) && selectedUnit != null && !selectedUnit.hasMoved) { 
            oldUnitPos = new Point(selectedUnit.position); reconstructPath(p, selectedUnit);
            selectedUnit.hasMoved = true; moveRange.clear(); attackRange.clear();
            previewPath.clear(); hoveredTile = new Point(tx, ty);
            if (selectedUnit.movePath.isEmpty()) showActionMenu(selectedUnit);
        } else {
            selectedUnit = null; moveRange.clear(); attackRange.clear(); previewPath.clear(); hoveredTile = new Point(tx, ty); MapUnit clickedUnit = null;
            for (MapUnit u : units) if (u.position.equals(p)) { clickedUnit = u; break; }
            
            if (clickedUnit != null && clickedUnit.ownerIndex != currentPlayerIdx) {
                selectedEnemy = clickedUnit;
                updateEnemyPanel();
            } else {
                selectedEnemy = null;
                updateEnemyPanel();
            }

            if (clickedUnit != null && !clickedUnit.hasActed && !clickedUnit.hasMoved && clickedUnit.ownerIndex == currentPlayerIdx) {
                selectedUnit = clickedUnit; calculateMoveRange(clickedUnit);
            } else if (clickedUnit == null) {
                EventInfo ev = eventMap.get(p);
                if (ev != null && ev.owner == currentPlayerIdx) {
                    showDeployMenu(ev);
                } else {
                    showGlobalMenu(mouseX, mouseY);
                }
            } else {
                // Do nothing if clicking a unit that has already acted, or an enemy unit
            }
        }
        canvasPanel.repaint();
    }

    /**
     * Processes keyboard inputs forwarded from the global KeyboardController.
     * Handles navigating menus, moving the map cursor, and triggering actions
     * (like selecting a unit or tile).
     * 
     * // [KEYBOARD_CONTROL_MARKER]
     */
    private void handleKeyboardInput(game.core.input.KeyboardController input) {
        if (deployOverlay != null && deployOverlay.isVisible()) {
            if (deployActions.isEmpty() || deployHovers.isEmpty()) return;
            
            if (keyCooldown > 0) {
                keyCooldown--;
            } else {
                if (input.upPressed) {
                    deploySelectedIndex = (deploySelectedIndex - 1 + deployHovers.size()) % deployHovers.size();
                    game.core.util.SoundManager.playCursor();
                    deployHovers.get(deploySelectedIndex).run();
                    keyCooldown = 8;
                } else if (input.downPressed) {
                    deploySelectedIndex = (deploySelectedIndex + 1) % deployHovers.size();
                    game.core.util.SoundManager.playCursor();
                    deployHovers.get(deploySelectedIndex).run();
                    keyCooldown = 8;
                }
            }
            
            if (input.consumeEnter()) {
                deployActions.get(deploySelectedIndex).run();
            }
            if (input.consumeEsc()) {
                hideDeployOverlay();
            }
            return;
        }
        if (players == null || players.isEmpty()) return;
        if (isBattleTransitioning) return;
        if (isPaused) {
            if (menuOverlay != null && menuOverlay.isVisible()) {
                if (keyCooldown > 0) {
                    keyCooldown--;
                } else {
                    if (input.upPressed) {
                        settingsSelectedIndex = (settingsSelectedIndex - 1 + settingsButtons.size()) % settingsButtons.size();
                        game.core.util.SoundManager.playCursor();
                        updateSettingsHover();
                        keyCooldown = 8;
                    } else if (input.downPressed) {
                        settingsSelectedIndex = (settingsSelectedIndex + 1) % settingsButtons.size();
                        game.core.util.SoundManager.playCursor();
                        updateSettingsHover();
                        keyCooldown = 8;
                    }
                }
                
                if (input.consumeEnter()) {
                    game.core.util.SoundManager.playButtonSound();
                    settingsButtons.get(settingsSelectedIndex).doClick();
                }
                if (input.consumeEsc()) {
                    setMenuOpen(false);
                }
            }
            return;
        }
        // If a popup menu (like the Global Action Menu or Unit Action Menu) is currently open
        if (activePopupMenu != null && activePopupMenu.isVisible()) {
            // Check if the user pressed the ESC key to back out
            if (input.consumeEsc()) {
                activePopupMenu.setVisible(false);
                activePopupMenu = null;
                javax.swing.MenuSelectionManager.defaultManager().clearSelectedPath();
                game.core.util.SoundManager.playCancel();
                
                // If a unit was in the middle of moving when the menu was opened, cancel its move and return it to its start position
                if (selectedUnit != null && selectedUnit.hasMoved && !selectedUnit.hasActed) {
                    cancelMove();
                }
            }
            // Ignore all other game controls (like arrow keys) while the menu is open, so the cursor doesn't move behind the menu
            return;
        }

        if (keyCooldown > 0) {
            keyCooldown--;
        } else {
            if (hoveredTile == null) {
                if (!units.isEmpty()) hoveredTile = new Point(units.get(0).position);
                else hoveredTile = new Point(mapW/2, mapH/2);
            }
            
            int dx = 0, dy = 0;
            if (input.upPressed) dy = -1;
            else if (input.downPressed) dy = 1;
            else if (input.leftPressed) dx = -1;
            else if (input.rightPressed) dx = 1;
            
            if (dx != 0 || dy != 0) {
                lastKnownMouseViewPos = null; // Keyboard takes priority
                int newX = Math.max(0, Math.min(mapW - 1, hoveredTile.x + dx));
                int newY = Math.max(0, Math.min(mapH - 1, hoveredTile.y + dy));
                Point newHover = new Point(newX, newY);
                if (!newHover.equals(hoveredTile)) {
                    hoveredTile = newHover;
                    game.core.util.SoundManager.playCursor();
                    updatePreviewPath();
                    
                    // Find if there's any MapUnit located exactly at the newly hovered tile
                    MapUnit hoverUnit = null;
                    for (MapUnit u : units) {
                        if (u.position.equals(hoveredTile)) {
                            hoverUnit = u;
                            break;
                        }
                    }
                    
                    // If we hovered over an enemy unit, update the selectedEnemy reference so the Enemy Info Panel pops up
                    if (hoverUnit != null && hoverUnit.ownerIndex != currentPlayerIdx) {
                        selectedEnemy = hoverUnit;
                    } else {
                        selectedEnemy = null;
                    }
                    updateEnemyPanel();
                    
                    cameraTargetX = newX * 16 * zoomScale;
                    cameraTargetY = newY * 16 * zoomScale;
                    needsRepaint = true;
                    
                    // Immediately re-layout the UI (like Day/Gold and Enemy Info panels) so they dodge out of the way of the new cursor position
                    layoutGameLayer();
                }
                keyCooldown = 6;
            }
        }
        
        if (input.consumeEnter()) {
            if (hoveredTile == null) {
                if (!units.isEmpty()) hoveredTile = new Point(units.get(0).position);
                else hoveredTile = new Point(mapW/2, mapH/2);
            }
            int mouseX = (int)(hoveredTile.x * 16 * zoomScale) + 8;
            int mouseY = (int)(hoveredTile.y * 16 * zoomScale) + 8;
            simulateGridClick(hoveredTile.x, hoveredTile.y, mouseX, mouseY);
        }
        
        if (input.consumeEsc()) {
            if (selectedUnit != null && selectedUnit.hasMoved && !selectedUnit.hasActed) {
                cancelMove();
            } else if (selectedUnit != null) {
                selectedUnit = null; moveRange.clear(); attackRange.clear(); previewPath.clear();
                needsRepaint = true; canvasPanel.repaint();
            }
        }
    }

    public void calculateMoveRange(MapUnit u) {
        moveRange.clear(); attackRange.clear(); pathParent.clear();
        if (u == null) return;
        
        int effectiveMove = u.stats.move;
        if (currentWeather == Weather.RAIN) {
            String uType = (u.stats.unitType == null) ? "" : u.stats.unitType.toUpperCase();
            if (!uType.contains("OCEAN") && !uType.contains("WATER") && !uType.contains("SEA")) {
                effectiveMove -= 2;
            }
        } else if (currentWeather == Weather.SNOW) {
            effectiveMove -= 3;
        } else if (currentWeather == Weather.THUNDERSTORM) {
            effectiveMove -= 3;
        } else if (currentWeather == Weather.SANDSTORM) {
            effectiveMove -= 4;
        } else if (currentWeather == Weather.SNOWSTORM) {
            effectiveMove -= 4;
        }
        effectiveMove = Math.max(0, effectiveMove);
        
        MovementEngine.MovementResult res = MovementEngine.calculateMovement(
            u, units, mapData, mapTSData, loadedTilesets, mapW, mapH, effectiveMove
        );
        moveRange.addAll(res.moveRange);
        attackRange.addAll(res.attackRange);
        pathParent.putAll(res.pathParent);
    }

    public void reconstructPath(Point dest, MapUnit u) {
        u.movePath = MovementEngine.reconstructPath(dest, u.position, pathParent);
    }

    private void showActionMenu(MapUnit u) {
        int tileX = (int)(u.position.x * 16 * zoomScale);
        int tileY = (int)(u.position.y * 16 * zoomScale);
        
        int menuX = tileX + (int)(24 * zoomScale);
        int menuY = tileY - (int)(8 * zoomScale);
        
        Rectangle viewRect = scrollPane.getViewport().getViewRect();
        if (menuX + 200 > viewRect.x + viewRect.width) {
            menuX = tileX - 200; // Place it on the left if too close to right edge
        }
        
        showMainActionMenu(u, menuX, menuY);
    }

    /**
     * Returns true if the unit is eligible to capture the event at its current position.
     * Only Land Units that are NOT of Siege subtype can capture.
     */
    public boolean canCapture(MapUnit u) {
        if (u == null || u.stats == null) return false;
        // Must be a Land Unit
        if (!"Land Unit".equalsIgnoreCase(u.stats.unitType)) return false;
        // Siege subtype is excluded
        if (u.stats.subUnitType != null && u.stats.subUnitType.trim().equalsIgnoreCase("Siege")) return false;
        // Must be standing on an event tile not owned by this player
        EventInfo ev = eventMap.get(u.position);
        return ev != null && ev.owner != u.ownerIndex;
    }

    private JPopupMenu activePopupMenu = null;

    private JPopupMenu createStyledMenu() {
        JPopupMenu menu = new JPopupMenu() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                // Draw 9-slice Fire Emblem menu background
                if (Theme.MENU_BACKGROUND != null) {
                    Theme.draw9Slice(g, Theme.MENU_BACKGROUND, 0, 0, getWidth(), getHeight());
                } else {
                    super.paintComponent(g);
                }
            }
        };
        menu.setOpaque(false); // crucial for the custom background to show properly
        menu.setBackground(new Color(0, 0, 0, 0)); // transparent background
        menu.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6)); // padding inside the borders
        
        // Use a MenuKeyListener to properly intercept W, A, S, D within the popup menu
        // [KEYBOARD_CONTROL_MARKER] - Action Menu Keyboard Navigation
        menu.addMenuKeyListener(new javax.swing.event.MenuKeyListener() {
            @Override
            public void menuKeyPressed(javax.swing.event.MenuKeyEvent e) {
                int code = e.getKeyCode();
                if (code == java.awt.event.KeyEvent.VK_W || code == java.awt.event.KeyEvent.VK_UP) {
                    navigateMenu(menu, -1);
                    e.consume();
                } else if (code == java.awt.event.KeyEvent.VK_S || code == java.awt.event.KeyEvent.VK_DOWN) {
                    navigateMenu(menu, 1);
                    e.consume();
                } else if (code == java.awt.event.KeyEvent.VK_ENTER || code == java.awt.event.KeyEvent.VK_SPACE) {
                    javax.swing.MenuElement[] path = javax.swing.MenuSelectionManager.defaultManager().getSelectedPath();
                    if (path != null && path.length > 0) {
                        javax.swing.MenuElement item = path[path.length - 1];
                        if (item instanceof JMenuItem) {
                            ((JMenuItem) item).doClick();
                            menu.setVisible(false);
                            if (activePopupMenu == menu) {
                                activePopupMenu = null;
                            }
                            javax.swing.MenuSelectionManager.defaultManager().clearSelectedPath();
                            if (main != null && main.getKeyboardController() != null) {
                                main.getKeyboardController().consumeEnter();
                            }
                        }
                    }
                    e.consume();
                }
            }
            @Override public void menuKeyReleased(javax.swing.event.MenuKeyEvent e) {}
            @Override public void menuKeyTyped(javax.swing.event.MenuKeyEvent e) {}
        });
        
        activePopupMenu = menu;
        return menu;
    }

    private void navigateMenu(JPopupMenu menu, int direction) {
        javax.swing.MenuElement[] path = javax.swing.MenuSelectionManager.defaultManager().getSelectedPath();
        if (path == null || path.length == 0) return;
        
        java.awt.Component[] comps = menu.getComponents();
        java.util.List<JMenuItem> items = new java.util.ArrayList<>();
        for (java.awt.Component c : comps) {
            if (c instanceof JMenuItem && c.isVisible() && c.isEnabled()) {
                items.add((JMenuItem) c);
            }
        }
        if (items.isEmpty()) return;
        
        int currentIndex = -1;
        javax.swing.MenuElement currentElement = path[path.length - 1];
        if (currentElement instanceof JMenuItem) {
            currentIndex = items.indexOf(currentElement);
        }
        
        int nextIndex = currentIndex + direction;
        if (nextIndex < 0) nextIndex = items.size() - 1;
        if (nextIndex >= items.size()) nextIndex = 0;
        
        javax.swing.MenuElement[] newPath = new javax.swing.MenuElement[2];
        newPath[0] = menu;
        newPath[1] = items.get(nextIndex);
        javax.swing.MenuSelectionManager.defaultManager().setSelectedPath(newPath);
        game.core.util.SoundManager.playCursor();
    }

    private JMenuItem createStyledMenuItem(String text) {
        JMenuItem item = new JMenuItem(text) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                // Do not paint the default UI to avoid LookAndFeel transparency glitches
                // super.paintComponent(g);
                
                // Draw hand cursor if selected/armed
                if (getModel().isArmed() && Theme.MENU_HAND != null) {
                    int handY = (getHeight() - Theme.MENU_HAND.getHeight()) / 2;
                    g.drawImage(Theme.MENU_HAND, 4, handY, null);
                }
                
                // Draw text manually
                g.setColor(getForeground());
                g.setFont(getFont());
                java.awt.FontMetrics fm = g.getFontMetrics();
                int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g.drawString(getText(), 28, textY);
            }
        };
        item.setFont(Theme.getPixelFont(16f));
        item.setForeground(Color.WHITE);
        item.setBorder(BorderFactory.createEmptyBorder(6, 28, 6, 12));
        item.setOpaque(false);
        item.setBackground(new Color(0, 0, 0, 0));
        
        // Remove the old flat-color hover effect since we now use the hand cursor
        item.addChangeListener(e -> {
            item.repaint();
        });
        return item;
    }

    private void showMainActionMenu(MapUnit u, int x, int y) {
        JPopupMenu menu = createStyledMenu();
        
        // --- 1. Attack Option ---
        boolean hasAttackOptions = false;
        if (!"Supplier".equalsIgnoreCase(u.unitName)) {
            for (WeaponItem wi : u.inventory) {
            if (wi.isWeapon() && !wi.isBroken()) {
                // Find all target enemies within this weapon's specific range
                for (MapUnit other : units) {
                    if (other.ownerIndex != u.ownerIndex && !other.isDead) {
                        int d = Math.abs(other.position.x - u.position.x) + Math.abs(other.position.y - u.position.y);
                        if (d >= wi.minRange && d <= wi.maxRange) {
                            hasAttackOptions = true;
                            break;
                        }
                    }
                }
            }
            if (hasAttackOptions) break;
        }
        }
        
        if (hasAttackOptions) {
            JMenuItem attackOpt = createStyledMenuItem("Attack");
            attackOpt.addActionListener(e -> {
                menu.setVisible(false);
                showWeaponSelectionMenu(u, x, y);
            });
            menu.add(attackOpt);
        }

        // --- 2. Capture Option ---
        if (canCapture(u)) {
            EventInfo ev = eventMap.get(u.position);
            String captureLabel = "Capture";
            JMenuItem captureOpt = createStyledMenuItem(captureLabel);
            captureOpt.addActionListener(e -> {
                menu.setVisible(false);
                performCapture(u, ev);
            });
            menu.add(captureOpt);
        }
        
        // --- 3. Item Option ---
        boolean hasItemOptions = false;
        for (WeaponItem wi : u.inventory) {
            if (!wi.isBroken()) {
                hasItemOptions = true;
                break;
            }
        }
        
        if (hasItemOptions) {
            JMenuItem itemOpt = createStyledMenuItem("Item");
            itemOpt.addActionListener(e -> {
                menu.setVisible(false);
                showItemSelectionMenu(u, x, y);
            });
            menu.add(itemOpt);
        }
        
        // --- 4. Load Option ---
        if (!"Fleet".equalsIgnoreCase(u.unitName) && !"Air Unit".equalsIgnoreCase(u.stats.unitType) && u.category != null && !u.category.equalsIgnoreCase("Champion")) {
            List<MapUnit> adjacentFleets = new ArrayList<>();
            for (MapUnit other : units) {
                if (other.ownerIndex == u.ownerIndex && "Fleet".equalsIgnoreCase(other.unitName) && !other.isDead) {
                    int d = Math.abs(other.position.x - u.position.x) + Math.abs(other.position.y - u.position.y);
                    if (d <= 1 && (other.loadedUnits == null || other.loadedUnits.size() < 3)) {
                        adjacentFleets.add(other);
                    }
                }
            }
            if (!adjacentFleets.isEmpty()) {
                JMenuItem loadOpt = createStyledMenuItem("Load");
                loadOpt.addActionListener(e -> {
                    menu.setVisible(false);
                    if (adjacentFleets.size() == 1) {
                        MapUnit target = adjacentFleets.get(0);
                        u.hasActed = true;
                        u.hasMoved = true;
                        if (target.loadedUnits == null) target.loadedUnits = new ArrayList<>();
                        target.loadedUnits.add(u);
                        units.remove(u);
                        game.core.util.SoundManager.playDecide();
                        selectedUnit = null;
                        hoveredTile = new Point(target.position);
                        fogDirty = true;
                        unitOrderDirty = true;
                        needsRepaint = true;
                        canvasPanel.repaint();
                    } else {
                        showLoadSelectionMenu(u, adjacentFleets, x, y);
                    }
                });
                menu.add(loadOpt);
            }
        }
        
        // --- 5. Drop Option ---
        if ("Fleet".equalsIgnoreCase(u.unitName) && u.loadedUnits != null && !u.loadedUnits.isEmpty()) {
            List<Point> validDropPoints = new ArrayList<>();
            MapUnit toDrop = u.loadedUnits.get(0);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) == 1) {
                        int nx = u.position.x + dx;
                        int ny = u.position.y + dy;
                        if (nx >= 0 && nx < mapW && ny >= 0 && ny < mapH) {
                            int cost = game.core.engine.MovementEngine.getTerrainCost(nx, ny, toDrop.stats.unitType, mapData, mapTSData, loadedTilesets, mapW, mapH);
                            if (cost != -1) {
                                boolean occ = false;
                                for (MapUnit occU : units) if (!occU.isDead && occU.position.x == nx && occU.position.y == ny) { occ = true; break; }
                                if (!occ) validDropPoints.add(new Point(nx, ny));
                            }
                        }
                    }
                }
            }
            if (!validDropPoints.isEmpty()) {
                JMenuItem dropOpt = createStyledMenuItem("Drop");
                dropOpt.addActionListener(e -> {
                    menu.setVisible(false);
                    showDropSelectionMenu(u, toDrop, validDropPoints, x, y);
                });
                menu.add(dropOpt);
            }
        }
        
        // --- 6. Wait Option ---
        boolean isOccupied = false;
        for (MapUnit other : units) {
            if (other != u && !other.isDead && other.position.equals(u.position)) {
                isOccupied = true;
                break;
            }
        }
        if (!isOccupied) {
            JMenuItem waitOpt = createStyledMenuItem("Wait");
            waitOpt.addActionListener(e -> {
                menu.setVisible(false);
                u.hasActed = true;
                selectedUnit = null;
                canvasPanel.repaint();
            });
            menu.add(waitOpt);
        }
        
        menu.show(canvasPanel, x, y);
    }

    /**
     * Executes the Capture action: reduces the event's HP by the unit's current HP,
     * starts the animated capture progress bar, and handles ownership change if HP <= 0.
     */
    public void performCapture(MapUnit u, EventInfo ev) {
        // Capture damage = unit's HP normalized to a 1–10 scale (Advance Wars style)
        // A full-HP unit deals 10 damage per turn → 4 turns to capture a 40 HP event
        int captureDamage = (int) Math.ceil((double) u.currentHp / u.stats.maxHp * 10.0);
        int newHp = Math.max(0, ev.captureHp - captureDamage);

        // Update capturing player tracking
        ev.capturingPlayerIdx = u.ownerIndex;

        // Start capture bar animation
        isCaptureAnimActive = true;
        captureAnimEvent   = ev;
        captureAnimUnit    = u;
        captureBarDisplay  = ev.captureHp;    // Animate from old HP
        captureBarTarget   = newHp;            // Down to new HP
        captureAnimTimer   = 0;

        // Apply damage immediately so logic is ready
        ev.captureHp = newHp;
        // Mark unit as acted
        u.hasActed = true;
        selectedUnit = null;
    }

    /**
     * Renders the capture progress bar directly above the capturing unit on the map.
     * Drawn in tile-space coordinates so it scrolls and zooms with the map.
     * Designed for pixel-art clarity at typical zoom levels (2x–4x).
     */
    private void drawCaptureBarAboveUnit(Graphics2D g, MapUnit u) {
        if (captureAnimEvent == null || u == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        // Use nearest-neighbor for crisp pixel-art rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        Color playerColor = players.get(u.ownerIndex).color;

        // Unit tile-space pixel position
        int unitPx = (int) Math.round(u.renderPos.x * 16.0);
        int unitPy = (int) Math.round(u.renderPos.y * 16.0);

        // ── Layout constants (all in tile-space pixels) ──────────
        int barW = 32;   // full sprite width
        int barH = 4;    // visible bar thickness
        int barX = unitPx - 8;
        int barY = unitPy - 20;

        // ── Background panel ─────────────────────────────────────
        int pad = 2;
        int panelX = barX - pad;
        int panelY = barY - 9;
        int panelW = barW + pad * 2;
        int panelH = barH + 13;

        // Dark translucent backdrop
        g2.setColor(new Color(8, 8, 16, 210));
        g2.fillRect(panelX, panelY, panelW, panelH);

        // 1px player-color border
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(playerColor.getRed(), playerColor.getGreen(), playerColor.getBlue(), 140));
        g2.drawRect(panelX, panelY, panelW - 1, panelH - 1);

        // Bright top-edge highlight (1px)
        g2.setColor(new Color(playerColor.getRed(), playerColor.getGreen(), playerColor.getBlue(), 80));
        g2.drawLine(panelX + 1, panelY + 1, panelX + panelW - 2, panelY + 1);

        // ── HP counter text (e.g. "32/40") ───────────────────────
        // Use a plain small pixel font – at 5px it renders crisply at 3x zoom (≈15px on screen)
        g2.setFont(new Font("Monospaced", Font.BOLD, 5));
        int hpVal = (int) Math.ceil(captureBarDisplay);
        String hpText = hpVal + "/40";
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(hpText);
        int textX = barX + (barW - textW) / 2;
        int textY = barY - 2;

        // Drop shadow for readability
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(hpText, textX + 1, textY + 1);
        // Main text in player color
        g2.setColor(Color.WHITE);
        g2.drawString(hpText, textX, textY);

        // ── Bar track (recessed groove) ──────────────────────────
        g2.setColor(new Color(12, 12, 18));
        g2.fillRect(barX, barY, barW, barH);
        // Inner shadow (top edge of groove)
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawLine(barX, barY, barX + barW - 1, barY);

        // ── Filled segment ───────────────────────────────────────
        double pct = Math.max(0.0, Math.min(1.0, captureBarDisplay / 40.0));
        int fillW = (int) Math.round(barW * pct);
        if (fillW > 0) {
            // Color shifts: red (high HP = hard to capture) → yellow → green (almost captured)
            Color barTop, barBot;
            if (pct > 0.5) {
                barTop = new Color(220, 60, 50);
                barBot = new Color(170, 40, 35);
            } else if (pct > 0.25) {
                barTop = new Color(230, 190, 20);
                barBot = new Color(190, 140, 15);
            } else {
                barTop = new Color(50, 200, 100);
                barBot = new Color(35, 160, 75);
            }

            // Draw bar fill with vertical gradient
            g2.setPaint(new GradientPaint(barX, barY, barTop, barX, barY + barH, barBot));
            g2.fillRect(barX, barY, fillW, barH);

            // Pixel-art highlight line (1px bright strip on top of fill)
            g2.setColor(new Color(255, 255, 255, 90));
            g2.drawLine(barX, barY + 1, barX + fillW - 1, barY + 1);
        }

        // ── Thin border around bar ───────────────────────────────
        g2.setColor(new Color(50, 50, 60));
        g2.drawRect(barX, barY, barW - 1, barH - 1);

        g2.dispose();
    }

    /**
     * Advances the capture bar animation. Called each frame while isCaptureAnimActive.
     */
    private void updateCaptureAnim() {
        if (!isCaptureAnimActive) return;

        // Smoothly animate the display bar toward target
        if (captureBarDisplay > captureBarTarget) {
            captureBarDisplay = Math.max(captureBarTarget, captureBarDisplay - 0.4);
        }

        // Once bar reaches target, wait briefly then finalise
        if (captureBarDisplay <= captureBarTarget + 0.05) {
            captureAnimTimer++;
            if (captureAnimTimer > 80) {
                // Finalise: check if ownership changes
                if (captureAnimEvent != null && captureAnimEvent.captureHp <= 0) {
                    captureAnimEvent.owner = captureAnimUnit.ownerIndex;
                    captureAnimEvent.captureHp = 40;        // Reset for next potential capture
                    captureAnimEvent.capturingPlayerIdx = null;
                }
                isCaptureAnimActive = false;
                captureAnimEvent    = null;
                captureAnimUnit     = null;
                captureAnimTimer    = 0;
                canvasPanel.repaint();
            }
        }
    }

    private void showWeaponSelectionMenu(MapUnit u, int x, int y) {
        JPopupMenu menu = createStyledMenu();
        
        for (WeaponItem wi : u.inventory) {
            if (wi.isWeapon() && !wi.isBroken()) {
                List<MapUnit> targets = new ArrayList<>();
                for (MapUnit other : units) {
                    if (other.ownerIndex != u.ownerIndex && !other.isDead) {
                        int d = Math.abs(other.position.x - u.position.x) + Math.abs(other.position.y - u.position.y);
                        if (d >= wi.minRange && d <= wi.maxRange) {
                            targets.add(other);
                        }
                    }
                }
                
                if (!targets.isEmpty()) {
                    JMenuItem wpnOpt = createStyledMenuItem(wi.name + " (" + wi.currentUses + "/" + wi.maxUses + ")");
                    wpnOpt.addActionListener(e -> {
                        menu.setVisible(false);
                        showTargetSelectionMenu(u, wi, targets, x, y);
                    });
                    menu.add(wpnOpt);
                }
            }
        }
        
        // Back option
        JMenuItem backOpt = createStyledMenuItem("< Back");
        backOpt.addActionListener(e -> {
            menu.setVisible(false);
            showMainActionMenu(u, x, y);
        });
        menu.add(backOpt);
        
        menu.show(canvasPanel, x, y);
    }

    private void showTargetSelectionMenu(MapUnit u, WeaponItem wi, List<MapUnit> targets, int x, int y) {
        JPopupMenu menu = createStyledMenu();
        
        for (MapUnit target : targets) {
            JMenuItem targetOpt = createStyledMenuItem(target.unitName + " (HP: " + target.currentHp + "/" + target.stats.maxHp + ")");
            targetOpt.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent me) {
                    hoveredTile = new Point(target.position);
                    cameraTargetX = target.renderPos.x * 16 * zoomScale + 8 * zoomScale;
                    cameraTargetY = target.renderPos.y * 16 * zoomScale + 8 * zoomScale;
                    canvasPanel.repaint();
                }
            });
            targetOpt.addActionListener(e -> {
                menu.setVisible(false);
                // Equip this weapon
                int idx = u.inventory.indexOf(wi);
                if (idx != -1) {
                    u.equippedSlot = idx;
                }
                u.weaponFolder = wi.animWeaponFolder;
                
                // Start combat cinematic transition
                beginBattleTransition(u, target);
            });
            menu.add(targetOpt);
        }
        
        // Back option
        JMenuItem backOpt = createStyledMenuItem("< Back");
        backOpt.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent me) {
                hoveredTile = new Point(u.position);
                cameraTargetX = u.renderPos.x * 16 * zoomScale + 8 * zoomScale;
                cameraTargetY = u.renderPos.y * 16 * zoomScale + 8 * zoomScale;
                canvasPanel.repaint();
            }
        });
        backOpt.addActionListener(e -> {
            menu.setVisible(false);
            showWeaponSelectionMenu(u, x, y);
        });
        menu.add(backOpt);
        
        menu.show(canvasPanel, x, y);
    }

    private void showItemSelectionMenu(MapUnit u, int x, int y) {
        JPopupMenu menu = createStyledMenu();
        
        for (WeaponItem wi : u.inventory) {
            if (!wi.isBroken()) {
                JMenuItem itemOpt = createStyledMenuItem(wi.name + " (" + wi.currentUses + "/" + wi.maxUses + ")");
                itemOpt.addActionListener(e -> {
                    menu.setVisible(false);
                    showItemActionMenu(u, wi, x, y);
                });
                menu.add(itemOpt);
            }
        }
        
        // Back option
        JMenuItem backOpt = createStyledMenuItem("< Back");
        backOpt.addActionListener(e -> {
            menu.setVisible(false);
            showMainActionMenu(u, x, y);
        });
        menu.add(backOpt);
        
        menu.show(canvasPanel, x, y);
    }

    private void showLoadSelectionMenu(MapUnit u, List<MapUnit> targets, int x, int y) {
        javax.swing.JPopupMenu menu = createStyledMenu();
        for (MapUnit target : targets) {
            String dir = "";
            if (target.position.y < u.position.y) dir = "North";
            else if (target.position.y > u.position.y) dir = "South";
            else if (target.position.x > u.position.x) dir = "East";
            else if (target.position.x < u.position.x) dir = "West";
            else dir = "Here";
            
            JMenuItem opt = createStyledMenuItem("Fleet (" + dir + ")");
            opt.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent me) {
                    hoveredTile = new Point(target.position);
                    cameraTargetX = target.renderPos.x * 16 * zoomScale + 8 * zoomScale;
                    cameraTargetY = target.renderPos.y * 16 * zoomScale + 8 * zoomScale;
                    canvasPanel.repaint();
                }
            });
            opt.addActionListener(e -> {
                menu.setVisible(false);
                u.hasActed = true;
                u.hasMoved = true;
                if (target.loadedUnits == null) target.loadedUnits = new ArrayList<>();
                target.loadedUnits.add(u);
                units.remove(u);
                game.core.util.SoundManager.playDecide();
                selectedUnit = null;
                hoveredTile = new Point(target.position);
                fogDirty = true;
                unitOrderDirty = true;
                needsRepaint = true;
                canvasPanel.repaint();
            });
            menu.add(opt);
        }
        JMenuItem backOpt = createStyledMenuItem("< Back");
        backOpt.addActionListener(e -> {
            menu.setVisible(false);
            showMainActionMenu(u, x, y);
        });
        menu.add(backOpt);
        menu.show(canvasPanel, x, y);
    }

    private void showDropSelectionMenu(MapUnit u, MapUnit droppedUnit, List<Point> tiles, int x, int y) {
        javax.swing.JPopupMenu menu = createStyledMenu();
        for (Point tile : tiles) {
            String dir = "";
            if (tile.y < u.position.y) dir = "North";
            else if (tile.y > u.position.y) dir = "South";
            else if (tile.x > u.position.x) dir = "East";
            else dir = "West";
            
            JMenuItem opt = createStyledMenuItem("Drop " + dir);
            opt.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent me) {
                    hoveredTile = new Point(tile);
                    cameraTargetX = tile.x * 16 * zoomScale + 8 * zoomScale;
                    cameraTargetY = tile.y * 16 * zoomScale + 8 * zoomScale;
                    canvasPanel.repaint();
                }
            });
            opt.addActionListener(e -> {
                menu.setVisible(false);
                u.loadedUnits.remove(droppedUnit);
                droppedUnit.position = new Point(tile);
                droppedUnit.renderPos = new java.awt.geom.Point2D.Double(tile.x, tile.y);
                droppedUnit.hasActed = true;
                droppedUnit.hasMoved = true;
                units.add(droppedUnit);
                game.core.util.SoundManager.playDecide();
                selectedUnit = null;
                hoveredTile = new Point(tile);
                u.hasActed = true;
                fogDirty = true;
                unitOrderDirty = true;
                needsRepaint = true;
                canvasPanel.repaint();
            });
            menu.add(opt);
        }
        JMenuItem backOpt = createStyledMenuItem("< Back");
        backOpt.addActionListener(e -> {
            menu.setVisible(false);
            showMainActionMenu(u, x, y);
        });
        menu.add(backOpt);
        menu.show(canvasPanel, x, y);
    }

    private void showItemActionMenu(MapUnit u, WeaponItem wi, int x, int y) {
        JPopupMenu menu = createStyledMenu();
        
        if (wi.isWeapon()) {
            JMenuItem equipOpt = createStyledMenuItem("Equip");
            equipOpt.addActionListener(e -> {
                menu.setVisible(false);
                int idx = u.inventory.indexOf(wi);
                if (idx != -1) {
                    u.equippedSlot = idx;
                }
                u.weaponFolder = wi.animWeaponFolder;
                JOptionPane.showMessageDialog(this, wi.name + " equipped!");
                canvasPanel.repaint();
            });
            menu.add(equipOpt);
        } else {
            JMenuItem useOpt = createStyledMenuItem("Use (Heal 20 HP)");
            useOpt.addActionListener(e -> {
                menu.setVisible(false);
                u.heal(20);
                wi.useOnce();
                JOptionPane.showMessageDialog(this, wi.name + " used! Healed 20 HP.");
                u.hasActed = true; // Consumes action
                selectedUnit = null;
                canvasPanel.repaint();
            });
            menu.add(useOpt);
        }
        
        // Back option
        JMenuItem backOpt = createStyledMenuItem("< Back");
        backOpt.addActionListener(e -> {
            menu.setVisible(false);
            showItemSelectionMenu(u, x, y);
        });
        menu.add(backOpt);
        
        menu.show(canvasPanel, x, y);
    }

    private int getBattleMode(boolean isCrit, int distance) {
        if (distance >= 2) {
            return isCrit ? 6 : 5; // 6 = Crit Ranged, 5 = Basic Ranged
        } else {
            return isCrit ? 3 : 1; // 3 = Crit Melee, 1 = Basic Melee
        }
    }

    public void applyHit(BattleActor source, BattleActor target, BattleManager.BattleHit hit) {
        target.takeHit(hit);
        currentHitIdx++;

        BattleManager.Combatant atkC = (source == attackerActor) ? activeBattle.attacker : activeBattle.defender;
        String wpnType = (atkC != null && atkC.weaponType != null) ? atkC.weaponType.name() : null;
        boolean isKill = !hit.isMiss && (target.targetHp <= 0);
        SoundManager.playBattleHitSfx(wpnType, hit.isCrit, hit.isMiss, hit.damage, isKill);

        if (!hit.isMiss) source.isWaitingForDamage = true;
    }

    // ── Pre-Battle Transition State ──
    public boolean isBattleTransitioning = false;
    private int battleTransitionTimer = 0;
    private MapUnit transitionAttacker = null;
    private MapUnit transitionDefender = null;
    private BufferedImage transitionBackground = null;
    private double transitionFocalX = 0;
    private double transitionFocalY = 0;

    public void beginBattleTransition(MapUnit a, MapUnit d) {
        if ("Fleet".equalsIgnoreCase(a.unitName) || "Fleet".equalsIgnoreCase(d.unitName)) {
            startCombat(a, d);
            return;
        }
        
        transitionBackground = null; // We will capture this safely on the EDT during the next paint()!
        
        isBattleTransitioning = true;
        battleTransitionTimer = 6; // 0.10 seconds
        transitionAttacker = a;
        transitionDefender = d;
        
        double cx = (a.renderPos.x + d.renderPos.x) / 2.0;
        double cy = (a.renderPos.y + d.renderPos.y) / 2.0;
        cameraTargetX = cx * 16 * zoomScale + 8 * zoomScale;
        cameraTargetY = cy * 16 * zoomScale + 8 * zoomScale;
        
        Point startVPos = scrollPane.getViewport().getViewPosition();
        transitionFocalX = cameraTargetX - startVPos.x;
        transitionFocalY = cameraTargetY - startVPos.y;
        
        game.core.util.SoundManager.playDecide();
    }

    public void startCombat(MapUnit a, MapUnit d) {
        BattleManager bm = new BattleManager();
        BattleManager.BattleResult br = bm.generateBattle(a, d);
        
        if ("Fleet".equalsIgnoreCase(a.unitName) || "Fleet".equalsIgnoreCase(d.unitName)) {
            // Map-only combat resolution
            BattleManager.Combatant atk = br.attacker;
            BattleManager.Combatant def = br.defender;
            
            a.currentHp = atk.hp;
            a.hasActed = true;
            if (a.currentHp <= 0) {
                a.isDead = true;
                units.remove(a);
            } else {
                healthBarTimers.put(a, 180);
            }
            
            d.currentHp = def.hp;
            if (d.currentHp <= 0) {
                d.isDead = true;
                units.remove(d);
            } else {
                healthBarTimers.put(d, 180);
            }
            
            fogDirty = true;
            unitOrderDirty = true;
            needsRepaint = true;
            
            String wpnType = (atk != null && atk.weaponType != null) ? atk.weaponType.name() : null;
            game.core.util.SoundManager.playBattleHitSfx(wpnType, false, false, 5, d.isDead);
            
            selectedUnit = null;
            if (selectedEnemy != null && selectedEnemy.isDead) selectedEnemy = null;
            updateEnemyPanel();
            return;
        }

        activeBattle = br;
        isBattleActive = true; currentHitIdx = 0; battleEndDelay = 0;
        recolorCache.clear(); // Clear cache to prevent old background/mirror artifacts
        attackerActor = new BattleActor(a, true, getWidth(), getHeight()); defenderActor = new BattleActor(d, false, getWidth(), getHeight());
        BattleManager.BattleHit first = activeBattle.hits.get(0);
        combatDistance = Math.abs(a.position.x - d.position.x) + Math.abs(a.position.y - d.position.y);
        
        if (combatDistance > 3) {
            boolean firstIsAttacker = first.isAttacker;
            battleCameraX = firstIsAttacker ? 100 : -100;
            battleCameraTargetX = battleCameraX;
        } else {
            battleCameraX = 0;
            battleCameraTargetX = 0;
        }
        
        attackerActor.setMode(getBattleMode(first.isCrit, combatDistance));
        defenderActor.setMode(AnimationScript.MODE_STANDING);
    }

    private JPanel buildDeployPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        panel.setPreferredSize(new Dimension(300, 0));

        // Name
        previewNameLbl = new JLabel(" ");
        previewNameLbl.setFont(Theme.getPixelFont(24f));
        previewNameLbl.setForeground(new Color(255, 80, 80));
        previewNameLbl.setHorizontalAlignment(SwingConstants.CENTER);
        previewNameLbl.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(255, 80, 80)));
        panel.add(previewNameLbl, BorderLayout.NORTH);

        // Center: Sprite
        previewSpritePanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(40, 40, 40, 200));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.GOLD);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                
                if (previewCategory != null && previewUnitName != null) {
                    List<BufferedImage> frames = deployPreviewCache.get(previewCategory + "/" + previewUnitName);
                    if (frames != null && !frames.isEmpty()) {
                        int idx = (deployAnimFrame / 8) % frames.size();
                        BufferedImage frame = frames.get(idx);
                        Color teamColor = players.get(currentPlayerIdx).color;
                        BufferedImage colored = SpriteColorer.recolor(frame, teamColor);
                        int scale = 4;
                        int dw = colored.getWidth() * scale;
                        int dh = colored.getHeight() * scale;
                        int dx = (getWidth() - dw) / 2;
                        int dy = (getHeight() - dh) / 2;
                        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        g.drawImage(colored, dx, dy, dw, dh, null);
                    }
                }
            }
        };
        previewSpritePanel.setPreferredSize(new Dimension(200, 200));
        previewSpritePanel.setOpaque(false);
        
        JPanel spriteWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        spriteWrap.setOpaque(false);
        spriteWrap.add(previewSpritePanel);
        
        // Stats in white box
        JPanel statsBox = new JPanel(new GridLayout(0, 2, 10, 5));
        statsBox.setOpaque(false);
        statsBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        previewStatLbls = new JLabel[8];
        String[] statNames = {"HP", "STR", "MAG", "SKL", "SPD", "DEF", "RES", "MOV"};
        for (int i = 0; i < 8; i++) {
            JPanel statRow = new JPanel(new BorderLayout());
            statRow.setOpaque(false);
            JLabel nameLbl = new JLabel(statNames[i]);
            nameLbl.setForeground(Color.LIGHT_GRAY);
            nameLbl.setFont(Theme.getPixelFont(14f));
            
            previewStatLbls[i] = new JLabel("-");
            previewStatLbls[i].setForeground(Color.WHITE);
            previewStatLbls[i].setFont(Theme.getPixelFont(16f));
            
            statRow.add(nameLbl, BorderLayout.WEST);
            statRow.add(previewStatLbls[i], BorderLayout.EAST);
            statsBox.add(statRow);
        }

        previewWeaponsBox = new JPanel();
        previewWeaponsBox.setLayout(new BoxLayout(previewWeaponsBox, BoxLayout.Y_AXIS));
        previewWeaponsBox.setOpaque(false);
        javax.swing.border.TitledBorder tb = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 1),
            "WEAPONS",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            Theme.getPixelFont(12f),
            Color.LIGHT_GRAY
        );
        previewWeaponsBox.setBorder(BorderFactory.createCompoundBorder(
            tb,
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JPanel bottomStats = new JPanel(new BorderLayout(0, 10));
        bottomStats.setOpaque(false);
        bottomStats.add(statsBox, BorderLayout.NORTH);
        bottomStats.add(previewWeaponsBox, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setOpaque(false);
        centerPanel.add(spriteWrap, BorderLayout.NORTH);
        centerPanel.add(bottomStats, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }



    private void updateDeployPreview(String cat, String name, UnitStats stats) {
        previewCategory = cat;
        previewUnitName = name;
        
        previewNameLbl.setText(name.toUpperCase());
        
        if (stats != null) {
            previewStatLbls[0].setText(String.valueOf(stats.maxHp));
            previewStatLbls[1].setText(String.valueOf(stats.strength));
            previewStatLbls[2].setText(String.valueOf(stats.magic));
            previewStatLbls[3].setText(String.valueOf(stats.skill));
            previewStatLbls[4].setText(String.valueOf(stats.speed));
            previewStatLbls[5].setText(String.valueOf(stats.defense));
            previewStatLbls[6].setText(String.valueOf(stats.resistance));
            previewStatLbls[7].setText(String.valueOf(stats.move));
        }

        if (previewWeaponsBox != null) {
            previewWeaponsBox.removeAll();
            List<WeaponItem> weapons = game.core.unit.UnitRegistry.getDefaultWeapons(cat, name);
            for (WeaponItem w : weapons) {
                String rangeStr = w.minRange == w.maxRange ? String.valueOf(w.minRange) : w.minRange + "-" + w.maxRange;
                JPanel wRow = new JPanel(new BorderLayout());
                wRow.setOpaque(false);
                wRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
                JLabel wLbl = new JLabel("• " + w.name);
                wLbl.setForeground(Color.WHITE);
                wLbl.setFont(Theme.getPixelFont(12f));
                JLabel rLbl = new JLabel(rangeStr);
                rLbl.setForeground(Color.LIGHT_GRAY);
                rLbl.setFont(Theme.getPixelFont(12f));
                wRow.add(wLbl, BorderLayout.WEST);
                wRow.add(rLbl, BorderLayout.EAST);
                previewWeaponsBox.add(wRow);
                previewWeaponsBox.add(Box.createVerticalStrut(4));
            }
            previewWeaponsBox.revalidate();
            previewWeaponsBox.repaint();
        }
        
        if (previewSpritePanel != null) previewSpritePanel.repaint();
    }

    private JPanel buildDeployOverlay() {
        JPanel overlay = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 210));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        overlay.setOpaque(false);

        // ── Title Bar ──
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        titleBar.setBorder(BorderFactory.createEmptyBorder(30, 50, 10, 50));

        JLabel titleLbl = new JLabel("DEPLOY UNITS");
        titleLbl.setFont(Theme.getTitleFont());
        titleLbl.setForeground(Theme.GOLD);
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
        titleBar.add(titleLbl, BorderLayout.CENTER);

        JButton closeBtn = new JButton("✕ CLOSE");
        closeBtn.setFont(Theme.getPixelFont(18f));
        closeBtn.setForeground(new Color(255, 120, 120));
        closeBtn.setBackground(new Color(60, 30, 30));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 100, 100, 150)),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> hideDeployOverlay());
        titleBar.add(closeBtn, BorderLayout.EAST);

        overlay.add(titleBar, BorderLayout.NORTH);

        // ── Main Content Split ──
        JPanel splitPanel = new JPanel(new BorderLayout(20, 0));
        splitPanel.setOpaque(false);
        splitPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));

        // Left side preview
        splitPanel.add(buildDeployPreviewPanel(), BorderLayout.WEST);

        // Right side list
        JPanel rightListPanel = new JPanel(new BorderLayout());
        rightListPanel.setOpaque(false);

        // ── Column Headers for Right List ──
        JPanel headerRow = new JPanel(new GridBagLayout());
        headerRow.setOpaque(false);
        headerRow.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        GridBagConstraints hgbc = new GridBagConstraints();
        hgbc.fill = GridBagConstraints.HORIZONTAL;
        hgbc.insets = new Insets(0, 5, 0, 5);
        hgbc.gridy = 0;

        Font headerFont = Theme.getPixelFont(14f);
        Color headerColor = Theme.GOLD_TRANS;

        Dimension dimName = new Dimension(200, 20);
        Dimension dimCost = new Dimension(80, 20);
        Dimension dimBtn  = new Dimension(120, 20);

        hgbc.gridx = 0; hgbc.weightx = 1.0;
        JLabel h1 = new JLabel("NAME", SwingConstants.LEFT); h1.setFont(headerFont); h1.setForeground(headerColor);
        h1.setPreferredSize(dimName);
        headerRow.add(h1, hgbc);

        hgbc.gridx = 1; hgbc.weightx = 0.2;
        JLabel h2 = new JLabel("COST", SwingConstants.CENTER); h2.setFont(headerFont); h2.setForeground(headerColor);
        h2.setPreferredSize(dimCost);
        headerRow.add(h2, hgbc);

        hgbc.gridx = 2; hgbc.weightx = 0;
        JLabel h3 = new JLabel("", SwingConstants.CENTER); h3.setFont(headerFont); h3.setForeground(headerColor);
        h3.setPreferredSize(dimBtn);
        headerRow.add(h3, hgbc);

        rightListPanel.add(headerRow, BorderLayout.NORTH);

        // ── Scrollable Unit List ──
        deployListContainer = new JPanel();
        deployListContainer.setLayout(new BoxLayout(deployListContainer, BoxLayout.Y_AXIS));
        deployListContainer.setOpaque(false);

        JScrollPane listScroll = new JScrollPane(deployListContainer);
        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);
        listScroll.setBorder(null);
        listScroll.getVerticalScrollBar().setUnitIncrement(30);
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        rightListPanel.add(listScroll, BorderLayout.CENTER);

        splitPanel.add(rightListPanel, BorderLayout.CENTER);
        overlay.add(splitPanel, BorderLayout.CENTER);

        // ── Gold Footer ──
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(5, 0, 25, 0));
        overlay.add(footer, BorderLayout.SOUTH);

        // Click outside list to close
        overlay.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getSource() == overlay) hideDeployOverlay();
            }
        });

        return overlay;
    }

    private List<BufferedImage> loadDeployPreviewFrames(String category, String unitName) {
        String key = category + "/" + unitName;
        if (deployPreviewCache.containsKey(key)) return deployPreviewCache.get(key);

        List<BufferedImage> frames = tryLoadMapUnitFrames(category, unitName, "Standing");
        if (frames.isEmpty()) frames = tryLoadMapUnitFrames(category, unitName, "Walk_Down");

        // Fallback names
        if (frames.isEmpty()) {
            String fallback = null;
            if ("Knight".equalsIgnoreCase(unitName)) fallback = "Cavalier";
            else if ("Sentinel".equalsIgnoreCase(unitName)) fallback = "Swordmaster";
            else if ("Ephraim".equalsIgnoreCase(unitName)) fallback = "Ephraim_Lord";
            else if ("Pegasus".equalsIgnoreCase(unitName)) fallback = "Pegasus Knight";
            if (fallback != null) {
                frames = tryLoadMapUnitFrames(category, fallback, "Standing");
                if (frames.isEmpty()) frames = tryLoadMapUnitFrames(category, fallback, "Walk_Down");
            }
        }

        deployPreviewCache.put(key, frames);
        return frames;
    }

    private void showDeployMenu(EventInfo ev) {
        game.core.util.SoundManager.playDecide();
        // Refresh the UnitRegistry to capture any newly created unit directories
        game.core.unit.UnitRegistry.reload();

        final String targetCategory;
        if ("HQ".equals(ev.type)) {
            targetCategory = "Champion";
        } else if ("ARMORY".equals(ev.type) || "FORT".equals(ev.type) || "AERIE".equals(ev.type)) {
            targetCategory = "Unit";
        } else {
            return;
        }
        deployListContainer.removeAll();
        deployPreviewCache.clear();
        deployAnimFrame = 0;
        deploySelectedIndex = 0;
        deployActions.clear();
        deployHovers.clear();

        VersusScreen.PlayerSettings currentPlayer = players.get(currentPlayerIdx);
        int playerGold = currentPlayer.gold;

        // ── Build unit entry data ──
        java.util.List<Object[]> unitEntries = new ArrayList<>();

        File battleDirFile = new File(GamePaths.BATTLE, targetCategory);
        File unitsDirFile = new File(GamePaths.UNITS, targetCategory);

        if (battleDirFile.exists() && battleDirFile.isDirectory() && unitsDirFile.exists() && unitsDirFile.isDirectory()) {
            File[] battleSubs = battleDirFile.listFiles(File::isDirectory);
            if (battleSubs != null) {
                Arrays.sort(battleSubs, Comparator.comparing(File::getName));
                for (File bs : battleSubs) {
                    String name = bs.getName();
                    File us = new File(unitsDirFile, name);
                    if (us.exists() && us.isDirectory()) {
                        UnitStats stats = UnitRegistry.get(name);
                        if ("ARMORY".equals(ev.type) && !"Land Unit".equalsIgnoreCase(stats.unitType)) continue;
                        if ("AERIE".equals(ev.type) && !"Air Unit".equalsIgnoreCase(stats.unitType)) continue;
                        if ("FORT".equals(ev.type) && !"Ocean Unit".equalsIgnoreCase(stats.unitType)) continue;
                        int price = DeploymentEngine.calculatePrice(targetCategory, name);
                        unitEntries.add(new Object[]{targetCategory, name, stats, price});
                    }
                }
            }
        }

        // Fallbacks
        if (unitEntries.isEmpty()) {
            if ("ARMORY".equals(ev.type)) unitEntries.add(new Object[]{"Unit", "Knight", UnitRegistry.get("Knight"), 500});
            else if ("HQ".equals(ev.type)) unitEntries.add(new Object[]{"Champion", "Ephraim", UnitRegistry.get("Ephraim"), 1000});
            else if ("FORT".equals(ev.type)) unitEntries.add(new Object[]{"Unit", "Fleet", UnitRegistry.get("Fleet"), 800});
            else if ("AERIE".equals(ev.type)) unitEntries.add(new Object[]{"Unit", "Pegasus Knight", UnitRegistry.get("Pegasus Knight"), 600});
        }

        // ── Build rows ──
        Font nameFont = Theme.getPixelFont(20f);
        Font costFont = Theme.getPixelFont(18f);
        Font btnFont = Theme.getPixelFont(14f);
        Color rowBg = new Color(25, 25, 40, 220);
        Color rowBgHover = new Color(45, 45, 70, 230);
        Color borderColor = new Color(255, 215, 0, 60);

        boolean first = true;

        for (Object[] entry : unitEntries) {
            String cat = (String) entry[0];
            String uName = (String) entry[1];
            UnitStats uStats = (UnitStats) entry[2];
            int price = (int) entry[3];
            boolean canAfford = playerGold >= price;

            // Pre-load animation frames
            loadDeployPreviewFrames(cat, uName);

            if (first) {
                updateDeployPreview(cat, uName, uStats);
                first = false;
            }

            JPanel row = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(borderColor);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
            };
            row.setOpaque(false);
            row.setBackground(rowBg);
            row.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Hover effect
            row.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { 
                    row.setBackground(rowBgHover); 
                    row.repaint(); 
                    updateDeployPreview(cat, uName, uStats);
                }
                @Override public void mouseExited(MouseEvent e) { 
                    row.setBackground(rowBg); 
                    row.repaint(); 
                }
            });

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new Insets(0, 5, 0, 5);
            gbc.gridy = 0;

            // ── Name ──
            gbc.gridx = 0; gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.WEST;
            JLabel nameLbl = new JLabel(uName);
            nameLbl.setFont(nameFont);
            nameLbl.setForeground(Color.WHITE);
            nameLbl.setHorizontalAlignment(SwingConstants.LEFT);
            nameLbl.setPreferredSize(new Dimension(200, 20));
            row.add(nameLbl, gbc);
            gbc.anchor = GridBagConstraints.CENTER;

            // ── Cost ──
            gbc.gridx = 1; gbc.weightx = 0.2;
            JLabel costLbl = new JLabel("🪙 " + price, SwingConstants.CENTER);
            costLbl.setFont(costFont);
            costLbl.setForeground(canAfford ? new Color(255, 230, 80) : new Color(180, 80, 80));
            costLbl.setHorizontalAlignment(SwingConstants.CENTER);
            costLbl.setPreferredSize(new Dimension(80, 20));
            row.add(costLbl, gbc);

            // ── Buy Button ──
            gbc.gridx = 2; gbc.weightx = 0;
            JButton buyBtn = new JButton(canAfford ? "RECRUIT" : "NO GOLD");
            buyBtn.setFont(btnFont);
            buyBtn.setForeground(canAfford ? new Color(200, 255, 200) : new Color(150, 100, 100));
            buyBtn.setBackground(canAfford ? new Color(30, 80, 40) : new Color(50, 30, 30));
            buyBtn.setFocusPainted(false);
            buyBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(canAfford ? new Color(80, 200, 100, 150) : new Color(100, 50, 50, 150)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            buyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            buyBtn.setEnabled(canAfford);
            buyBtn.addActionListener(e -> {
                game.core.util.SoundManager.playDecide();
                hideDeployOverlay();
                deployUnit(cat, uName, price, ev);
            });
            row.add(buyBtn, gbc);

            int thisIndex = deployActions.size();
            deployActions.add(() -> {
                if (canAfford) {
                    game.core.util.SoundManager.playDecide();
                    hideDeployOverlay();
                    deployUnit(cat, uName, price, ev);
                } else {
                    game.core.util.SoundManager.playCancel();
                }
            });
            deployHovers.add(() -> {
                for (java.awt.Component c : deployListContainer.getComponents()) {
                    if (c instanceof JPanel) c.setBackground(rowBg);
                }
                row.setBackground(rowBgHover);
                updateDeployPreview(cat, uName, uStats);
                deployListContainer.repaint();
            });

            deployListContainer.add(row);
            deployListContainer.add(Box.createVerticalStrut(6));
        }

        // ── Update footer with gold display ──
        JPanel footer = (JPanel) deployOverlay.getComponent(2); // SOUTH component
        footer.removeAll();
        JLabel goldInfo = new JLabel("YOUR GOLD: 🪙 " + playerGold);
        goldInfo.setFont(Theme.getPixelFont(22f));
        goldInfo.setForeground(Theme.GOLD);
        footer.add(goldInfo);

        deployListContainer.revalidate();
        if (!deployHovers.isEmpty()) {
            deployHovers.get(deploySelectedIndex).run();
        }
        deployOverlay.setVisible(true);
        isPaused = true;

        // Start animation timer
        if (deployAnimTimer != null) deployAnimTimer.stop();
        deployAnimTimer = new javax.swing.Timer(16, e -> {
            deployAnimFrame++;
            if (deployListContainer != null) deployListContainer.repaint();
            if (previewSpritePanel != null) previewSpritePanel.repaint();
        });
        deployAnimTimer.start();
    }

    private void hideDeployOverlay() {
        if (deployOverlay != null) deployOverlay.setVisible(false);
        if (deployAnimTimer != null) { deployAnimTimer.stop(); deployAnimTimer = null; }
        isPaused = false;
        canvasPanel.requestFocusInWindow();
    }



    public void deployUnit(String cat, String name, int cost, EventInfo ev) {
        VersusScreen.PlayerSettings p = players.get(currentPlayerIdx);
        if (p.gold < cost) { JOptionPane.showMessageDialog(this, "Not enough gold!"); return; }
        p.gold -= cost; goldLabel.setText("🪙 " + p.gold); layoutGameLayer();
        MapUnit u = new MapUnit(cat, name, MapUnit.Faction.PLAYER, new Point(ev.x, ev.y)); u.ownerIndex = currentPlayerIdx;
        
        List<WeaponItem> defaultWeapons = UnitRegistry.getDefaultWeapons(cat, name);
        for (WeaponItem w : defaultWeapons) {
            u.addItem(w);
        }
        
        units.add(u); loadAnims(u); unitOrderDirty = true; fogDirty = true; needsRepaint = true; canvasPanel.repaint();
    }

    private void showGlobalMenu(int x, int y) {
        JPopupMenu menu = createStyledMenu();
        
        JMenuItem endTurnOpt = createStyledMenuItem("End Turn");
        endTurnOpt.addActionListener(e -> nextTurn());
        menu.add(endTurnOpt);
        
        JMenuItem settingsOpt = createStyledMenuItem("Settings (Menu)");
        settingsOpt.addActionListener(e -> toggleMenu());
        menu.add(settingsOpt);
        
        menu.show(canvasPanel, x, y);
    }

    @Override public void update() {
        // Fetch the global keyboard controller state and pass it to our local handler
        game.core.input.KeyboardController input = main.getKeyboardController();
        if (input != null) {
            handleKeyboardInput(input);
        }

        if (isPaused) return;

        if (weatherTransitionTimer > 0) {
            weatherTransitionTimer--;
            if (weatherTransitionTimer == 0 && pendingWeather != null) {
                currentWeather = pendingWeather;
                pendingWeather = null;
                weatherDaysRemaining = (currentWeather == Weather.NONE) ? 5 : 3;
                game.core.util.SoundManager.setRainLoop(currentWeather == Weather.RAIN || currentWeather == Weather.THUNDERSTORM);
                game.core.util.SoundManager.setWindLoop(currentWeather == Weather.SANDSTORM || currentWeather == Weather.SNOWSTORM);
            }
            canvasPanel.repaint();
            repaint();
            return;
        }
        if (isBattleTransitioning) {
            battleTransitionTimer--;
            // Zoom the map in smoothly, and then trigger combat
            if (battleTransitionTimer <= 0) {
                isBattleTransitioning = false;
                startCombat(transitionAttacker, transitionDefender);
            }
            needsRepaint = true;
            // DO NOT return here, otherwise updateSmoothCamera() is skipped and the camera won't pan!
        }
        
        if (isBattleActive) { updateBattle(); repaint(); return; }
        if (isCaptureAnimActive) { updateCaptureAnim(); canvasPanel.repaint(); repaint(); return; }
        if (phaseBannerTimer > 0) {
            phaseBannerTimer--;
            needsRepaint = true;
            repaint();
        } else if (!isBattleTransitioning && players != null && !players.isEmpty() && players.get(currentPlayerIdx).isAI) {
            aiLogic.updateAI();
        }
        // ── Decrement health bar display timers ──
        if (!healthBarTimers.isEmpty()) {
            healthBarTimers.entrySet().removeIf(entry -> {
                entry.setValue(entry.getValue() - 1);
                return entry.getValue() <= 0;
            });
            needsRepaint = true;
        }

        // ── Animate health bars ──
        boolean hpAnimating = false;
        for (MapUnit u : units) {
            double actualHp = u.currentHp;
            Double storedAnimHp = animatedHpMap.get(u);
            
            if (storedAnimHp == null) {
                animatedHpMap.put(u, actualHp);
            } else {
                if (storedAnimHp > actualHp) {
                    double newAnimHp = storedAnimHp - 0.3; // drain speed
                    if (newAnimHp < actualHp) newAnimHp = actualHp;
                    animatedHpMap.put(u, newAnimHp);
                    hpAnimating = true;
                } else if (storedAnimHp < actualHp) {
                    animatedHpMap.put(u, actualHp);
                }
            }
        }
        if (hpAnimating) needsRepaint = true;

        // ── Map Unit Death Fade Animation ──
        for (int i = 0; i < units.size(); i++) {
            MapUnit u = units.get(i);
            if (u.isDead && !dyingUnitsMap.containsKey(u)) {
                dyingUnitsMap.put(u, 1.0f);
                SoundManager.playFadeDieAway1();
            }
        }
        if (!dyingUnitsMap.isEmpty()) {
            dyingUnitsMap.entrySet().forEach(entry -> {
                entry.setValue(entry.getValue() - 0.04f); // 25 frames fade out
            });
            boolean removedAny = units.removeIf(u -> dyingUnitsMap.containsKey(u) && dyingUnitsMap.get(u) <= 0);
            if (removedAny) {
                dyingUnitsMap.keySet().removeIf(u -> !units.contains(u));
                unitOrderDirty = true;
                fogDirty = true;
            }
            needsRepaint = true;
        }

        boolean anyMoving = false;
        boolean anyAnimated = false;
        MapUnit movingUnit = null; // Track the unit currently moving for camera follow
        for (int i = 0; i < units.size(); i++) {
            MapUnit u = units.get(i); u.animTimer++;
            if (u.animTimer > 10) { u.animFrame++; u.animTimer = 0; anyAnimated = true; }
            if (!u.movePath.isEmpty()) {
                anyMoving = true;
                movingUnit = u;
                Point target = u.movePath.get(0);
                double dx = target.x - u.renderPos.x;
                double dy = target.y - u.renderPos.y;
                // Smooth lerp: move 20% of remaining distance each frame, with a minimum speed floor
                double lerpFactor = 0.20;
                double minSpeed = 0.06; // Prevents crawling at the very end
                double moveX = dx * lerpFactor;
                double moveY = dy * lerpFactor;
                // Enforce minimum speed so we don't get stuck asymptotically approaching the target
                if (Math.abs(moveX) < minSpeed && Math.abs(dx) > 0.01) moveX = Math.signum(dx) * minSpeed;
                if (Math.abs(moveY) < minSpeed && Math.abs(dy) > 0.01) moveY = Math.signum(dy) * minSpeed;
                u.renderPos.x += moveX;
                u.renderPos.y += moveY;
                // Snap to target when very close
                if (Math.abs(target.x - u.renderPos.x) < 0.05) u.renderPos.x = target.x;
                if (Math.abs(target.y - u.renderPos.y) < 0.05) u.renderPos.y = target.y;
                if (u.renderPos.x == target.x && u.renderPos.y == target.y) {
                    u.movePath.remove(0);
                    unitOrderDirty = true; // Unit Y position changed
                    if (isArmoredSubtype(u)) SoundManager.playStepHeavy();
                    else if (isMountedSubtype(u)) SoundManager.playStepHorse();
                    else if (isFlierSubtype(u)) SoundManager.playStepFlier();
                    else if (isSiegeSubtype(u)) SoundManager.playStepSiege();
                    else if (isShipSubtype(u)) SoundManager.playStepShip();
                    else if (isInfantrySubtype(u)) SoundManager.playStepInfantry();
                    else SoundManager.playFootstep();
                    if (u.movePath.isEmpty()) { 
                        if (isShipSubtype(u)) SoundManager.stopStepShip();
                        u.position = new Point(target); 
                        fogDirty = true; 
                        if (players != null && !players.isEmpty() && !players.get(u.ownerIndex).isAI) {
                            showActionMenu(u); 
                        }
                    }
                }
            }
        }

        // ── Smooth camera follow for moving unit ──
        if (movingUnit != null) {
            int ry = (int) Math.round(movingUnit.renderPos.y);
            int rx = (int) Math.round(movingUnit.renderPos.x);
            boolean visible = false;
            if (fogOfWarEnabled && visibleTiles != null && ry >= 0 && ry < mapH && rx >= 0 && rx < mapW) {
                visible = visibleTiles[ry][rx] || isPlayerVisionActive(movingUnit.ownerIndex);
            } else if (!fogOfWarEnabled) {
                visible = true;
            } else if (isPlayerVisionActive(movingUnit.ownerIndex)) {
                visible = true;
            }
            if (visible) {
                double unitPixelX = movingUnit.renderPos.x * TILE_SIZE * zoomScale;
                double unitPixelY = movingUnit.renderPos.y * TILE_SIZE * zoomScale;
                cameraTargetX = unitPixelX;
                cameraTargetY = unitPixelY;
            }
        }
        updateSmoothCamera();

        // ── Cursor Edge Panning ──
        if (lastKnownMouseViewPos != null && !isBattleActive && phaseBannerTimer <= 0) {
            int edgeMargin = 40;
            int panSpeed = 10;
            JViewport viewPort = scrollPane.getViewport();
            Point vPos = viewPort.getViewPosition();
            int vw = viewPort.getWidth();
            int vh = viewPort.getHeight();
            boolean panned = false;
            
            if (lastKnownMouseViewPos.x < edgeMargin) { vPos.x -= panSpeed; panned = true; }
            else if (lastKnownMouseViewPos.x > vw - edgeMargin) { vPos.x += panSpeed; panned = true; }
            
            if (lastKnownMouseViewPos.y < edgeMargin) { vPos.y -= panSpeed; panned = true; }
            else if (lastKnownMouseViewPos.y > vh - edgeMargin) { vPos.y += panSpeed; panned = true; }
            
            if (panned) {
                int maxX = canvasPanel.getWidth() - vw;
                int maxY = canvasPanel.getHeight() - vh;
                vPos.x = Math.max(0, Math.min(vPos.x, maxX));
                vPos.y = Math.max(0, Math.min(vPos.y, maxY));
                viewPort.setViewPosition(vPos);
                
                // Re-evaluate hoveredTile since camera moved but physical mouse didn't
                int tx = (int)((vPos.x + lastKnownMouseViewPos.x) / (16 * zoomScale));
                int ty = (int)((vPos.y + lastKnownMouseViewPos.y) / (16 * zoomScale));
                Point newHover = new Point(tx, ty);
                if (!newHover.equals(hoveredTile)) {
                    hoveredTile = newHover;
                    updatePreviewPath();
                }
                needsRepaint = true;
                layoutGameLayer();
                
                // Clear camera target so edge panning doesn't fight smooth camera
                cameraTargetX = -1;
                cameraTargetY = -1;
            }
        }

        // ── Apply drag inertia ──
        if (lastDragPoint == null && (Math.abs(panVelocityX) > 0.5 || Math.abs(panVelocityY) > 0.5)) {
            JViewport viewPort = scrollPane.getViewport();
            Point vPos = viewPort.getViewPosition();
            vPos.x += (int) panVelocityX;
            vPos.y += (int) panVelocityY;
            int maxX = canvasPanel.getWidth() - viewPort.getWidth();
            int maxY = canvasPanel.getHeight() - viewPort.getHeight();
            vPos.x = Math.max(0, Math.min(vPos.x, maxX));
            vPos.y = Math.max(0, Math.min(vPos.y, maxY));
            viewPort.setViewPosition(vPos);
            panVelocityX *= 0.88; // Friction damping
            panVelocityY *= 0.88;
            needsRepaint = true;
            layoutGameLayer();
        }

        // Only repaint when something actually changed
        if (anyMoving || anyAnimated || currentWeather != Weather.NONE || needsRepaint) {
            canvasPanel.repaint();
            this.repaint(); // Crucial for calling our custom paint() method!
            needsRepaint = false;
        }
    }


    /**
     * Smoothly interpolates the camera viewport toward the target position.
     * Uses ease-out lerp for a natural, decelerating camera feel.
     */
    private void updateSmoothCamera() {
        if (cameraTargetX < 0 || cameraTargetY < 0) return;
        JViewport viewPort = scrollPane.getViewport();
        if (viewPort == null) return;
        Point vPos = viewPort.getViewPosition();
        int vpW = viewPort.getWidth();
        int vpH = viewPort.getHeight();

        // Desired viewport top-left to center the target
        double desiredX = cameraTargetX - vpW / 2.0;
        double desiredY = cameraTargetY - vpH / 2.0;

        // Initialize current camera position on first frame
        if (cameraCurrentX < 0) { cameraCurrentX = vPos.x; cameraCurrentY = vPos.y; }

        // Lerp toward desired position (ease-out)
        double camLerp = 0.10;
        cameraCurrentX += (desiredX - cameraCurrentX) * camLerp;
        cameraCurrentY += (desiredY - cameraCurrentY) * camLerp;

        // Clamp to valid bounds
        int maxX = Math.max(0, canvasPanel.getWidth() - vpW);
        int maxY = Math.max(0, canvasPanel.getHeight() - vpH);
        int newX = Math.max(0, Math.min((int) Math.round(cameraCurrentX), maxX));
        int newY = Math.max(0, Math.min((int) Math.round(cameraCurrentY), maxY));

        if (newX != vPos.x || newY != vPos.y) {
            viewPort.setViewPosition(new Point(newX, newY));
            needsRepaint = true;
            // Update UI element positions (dodging) dynamically as the camera smoothly scrolls over the map
            layoutGameLayer();
        }

        // Stop tracking once we've settled close enough and unit is done moving
        if (Math.abs(desiredX - cameraCurrentX) < 1 && Math.abs(desiredY - cameraCurrentY) < 1) {
            boolean anyStillMoving = false;
            for (MapUnit u : units) { if (!u.movePath.isEmpty()) { anyStillMoving = true; break; } }
            if (!anyStillMoving) { cameraTargetX = -1; cameraTargetY = -1; }
        }
    }

    private boolean isArmoredSubtype(MapUnit u) {
        if (u == null || u.stats == null || u.stats.subUnitType == null) return false;
        String st = u.stats.subUnitType.trim().toLowerCase();
        return st.contains("armored");
    }

    private boolean isInfantrySubtype(MapUnit u) {
        if (u == null || u.stats == null || u.stats.subUnitType == null) return false;
        String st = u.stats.subUnitType.trim().toLowerCase();
        return st.equals("infantry");
    }

    private boolean isSiegeSubtype(MapUnit u) {
        if (u == null || u.stats == null || u.stats.subUnitType == null) return false;
        String st = u.stats.subUnitType.trim().toLowerCase();
        return st.equals("siege");
    }

    private boolean isShipSubtype(MapUnit u) {
        if (u == null || u.stats == null || u.stats.subUnitType == null) return false;
        String st = u.stats.subUnitType.trim().toLowerCase();
        return st.equals("ship");
    }

    private boolean isMountedSubtype(MapUnit u) {
        if (u == null || u.stats == null || u.stats.subUnitType == null) return false;
        String st = u.stats.subUnitType.trim().toLowerCase();
        return st.contains("mounted") && !st.contains("armored");
    }

    private boolean isFlierSubtype(MapUnit u) {
        if (u == null) return false;
        if ("Air Unit".equalsIgnoreCase(u.category)) return true;
        if (u.stats == null || u.stats.subUnitType == null) return false;
        String st = u.stats.subUnitType.trim().toLowerCase();
        return st.contains("pegasus") || st.contains("flier") || st.contains("dragon");
    }

    private void updateBattle() {
        if (!isBattleActive || attackerActor == null || defenderActor == null) return;
        
        if (isBattlePanning) {
            double panSpeed = 6.0;
            if (Math.abs(battleCameraX - battleCameraTargetX) <= panSpeed) {
                battleCameraX = battleCameraTargetX;
                isBattlePanning = false;
                if (pendingHit != null) {
                    applyHit(pendingHitActor, pendingTargetActor, pendingHit);
                    pendingHit = null;
                }
            } else if (battleCameraX < battleCameraTargetX) {
                battleCameraX += panSpeed;
            } else {
                battleCameraX -= panSpeed;
            }
            return;
        }
        
        if (flashTimer > 0) flashTimer--; if (shakeTimer > 0) shakeTimer--;
        attackerActor.update(); defenderActor.update();
        if (attackerActor.isFinished && defenderActor.isFinished) {
            if (currentHitIdx < activeBattle.hits.size()) {
                BattleManager.BattleHit next = activeBattle.hits.get(currentHitIdx);
                
                if (combatDistance > 3) {
                    double requiredCamX = next.isAttacker ? 100 : -100;
                    if (Math.abs(battleCameraX - requiredCamX) > 1) {
                        battleCameraTargetX = requiredCamX;
                        isBattlePanning = true;
                        pendingHit = null;
                        return;
                    }
                }

                if (next.isAttacker) attackerActor.setMode(getBattleMode(next.isCrit, combatDistance));
                else defenderActor.setMode(getBattleMode(next.isCrit, combatDistance));
            } else {
                battleEndDelay++;
                
                // ── Battle Cinematic Death Fade ──
                if (battleEndDelay == 1) {
                    if (activeBattle.attacker.hp <= 0) SoundManager.playFadeDieAway1();
                    if (activeBattle.defender.hp <= 0) SoundManager.playFadeDieAway1();
                }
                if (activeBattle.attacker.hp <= 0 && attackerActor != null) {
                    attackerActor.alpha = Math.max(0f, 1.0f - (battleEndDelay / 40f)); // Fade out over 40 frames
                }
                if (activeBattle.defender.hp <= 0 && defenderActor != null) {
                    defenderActor.alpha = Math.max(0f, 1.0f - (battleEndDelay / 40f));
                }

                if (battleEndDelay > 60) {
                    isBattleActive = false;
                    BattleManager.Combatant a = activeBattle.attacker; BattleManager.Combatant d = activeBattle.defender;
                    if (a.mapUnit != null) {
                        a.mapUnit.currentHp = a.hp;
                        a.mapUnit.hasActed = true;
                        if (a.hp <= 0) {
                            a.mapUnit.isDead = true;
                            units.remove(a.mapUnit);
                        }
                    }
                    if (d.mapUnit != null) {
                        d.mapUnit.currentHp = d.hp;
                        if (d.hp <= 0) {
                            d.mapUnit.isDead = true;
                            units.remove(d.mapUnit);
                        }
                    }
                    fogDirty = true;         // Fog may have changed if units died
                    unitOrderDirty = true;   // Unit list changed
                    needsRepaint = true;
                    selectedUnit = null; activeBattle = null; attackerActor = null; defenderActor = null;
                    if (selectedEnemy != null && selectedEnemy.isDead) selectedEnemy = null;
                    updateEnemyPanel();
                }
            }
        }
    }
}
