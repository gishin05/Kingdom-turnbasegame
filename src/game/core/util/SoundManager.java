package game.core.util;

import javax.sound.sampled.*;
import java.io.File;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JComboBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.application.Platform;

public class SoundManager {
    private static Clip sfxCursor;
    private static Clip sfxDecide;
    private static Clip sfxCancel;
    private static Clip sfxWindow;
    private static MediaPlayer bgmPlayer;

    // Battle SFX — Weapon Hits
    private static Clip sfxHitMelee;
    private static Clip sfxHitLance;
    private static Clip sfxHitCritical;
    private static Clip sfxHitKill;
    private static Clip sfxNoDamage;
    private static Clip sfxArrowHit;
    private static Clip sfxMiss;

    // Battle SFX — Weapon Swings & Actions
    private static Clip sfxSwordSwing;
    private static Clip sfxAxeSwing;
    private static Clip sfxSpearSpin;
    private static Clip sfxBowRelease;
    private static Clip sfxStepHeavy;

    // Battle SFX — Unit Feedback
    private static Clip sfxHumanFall;
    private static Clip sfxFadeDieAway1;
    private static Clip sfxHpTick;
    private static Clip sfxLevelUp;

    // Map SFX — Footsteps
    private static Clip sfxStepGrass;
    private static Clip sfxStepStone;
    private static Clip sfxStepInfantry;
    private static Clip sfxStepHorse;

    static {
        initSounds();
        initBattleSfx();
        initBgm();
    }

    private static void initSounds() {
        sfxCursor = loadOrSynthesize("HoverAudio.wav", generateCursorWave());
        sfxDecide = loadOrSynthesize("decide.wav", generateDecideWave());
        sfxCancel = loadOrSynthesize("cancel.wav", generateCancelWave());
        sfxWindow = loadOrSynthesize("window.wav", generateWindowWave());
    }

    private static void initBgm() {
        try {
            File bgmFile = new File(GamePaths.BUNDLED_ROOT, "audio/BGsounds.mp3");
            if (bgmFile.exists()) {
                Platform.runLater(() -> {
                    try {
                        Media media = new Media(bgmFile.toURI().toString());
                        bgmPlayer = new MediaPlayer(media);
                        bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                        bgmPlayer.setVolume(1.0);
                        bgmPlayer.play();
                    } catch (Exception e) {
                        System.err.println("Error initializing JavaFX MediaPlayer: " + e.getMessage());
                    }
                });
            } else {
                System.err.println("BGM File not found: " + bgmFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initBattleSfx() {
        // Weapon hits
        sfxHitMelee    = loadAudioFile("hit_melee.aif");
        sfxHitLance    = loadAudioFile("hit_lance.aif");
        sfxHitCritical = loadAudioFile("hit_critical.aif");
        sfxHitKill     = loadAudioFile("hit_kill.aif");
        sfxNoDamage    = loadAudioFile("no_damage.aif");
        sfxArrowHit    = loadAudioFile("arrow_hit.aif");
        sfxMiss        = loadAudioFile("miss.aif");

        // Weapon swings & actions
        sfxSwordSwing  = loadAudioFile("sword_swing.aif");
        sfxAxeSwing    = loadAudioFile("axe_swing.aif");
        sfxSpearSpin   = loadAudioFile("spear_spin.aif");
        sfxBowRelease  = loadAudioFile("bow_release.aif");
        sfxStepHeavy   = loadAudioFile("step_heavy.aif");

        // Unit feedback
        sfxHumanFall   = loadAudioFile("human_fall.aif");
        sfxFadeDieAway1 = loadAudioFile("fade_die_away1.aif");
        sfxHpTick      = loadAudioFile("hp_tick.aif");
        sfxLevelUp     = loadAudioFile("level_up.aif");

        // Map footsteps
        sfxStepGrass   = loadAudioFile("step_grass.aif");
        sfxStepStone   = loadAudioFile("step_stone.aif");
        sfxStepInfantry = loadAudioFile("Infantry_Move.aif");
        sfxStepHorse   = loadAudioFile("horse1_Move.aif");
    }

    private static Clip loadAudioFile(String filename) {
        File file = new File(GamePaths.BUNDLED_ROOT, "audio/" + filename);
        if (file.exists()) {
            try {
                AudioInputStream stream = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                return clip;
            } catch (Exception e) {
                System.err.println("Failed to load audio: " + filename + " - " + e.getMessage());
            }
        }
        return null;
    }

    private static Clip loadOrSynthesize(String filename, byte[] synthesizedFallback) {
        File file = new File(GamePaths.BUNDLED_ROOT, "audio/" + filename);
        if (file.exists()) {
            try {
                AudioInputStream stream = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                return clip;
            } catch (Exception e) {
                System.err.println("Failed to load WAV " + filename + ", falling back to synthesis.");
            }
        }
        return createClip(synthesizedFallback);
    }

    private static Clip createClip(byte[] data) {
        try {
            AudioFormat format = new AudioFormat(8000, 8, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(format, data, 0, data.length);
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- PROCEDURAL RETRO SYNTHESIS ENGINE ---

    private static byte[] generateCursorWave() {
        int rate = 8000;
        int len = (int)(rate * 0.05);
        byte[] buf = new byte[len];
        for (int i = 0; i < len; i++) {
            double t = (double) i / rate;
            double freq = (t < 0.02) ? 880 : 784;
            double amp = Math.exp(-t * 120);
            buf[i] = (byte)(Math.sin(2 * Math.PI * freq * t) * 127 * amp);
        }
        return buf;
    }

    private static byte[] generateDecideWave() {
        int rate = 8000;
        int len = (int)(rate * 0.15);
        byte[] buf = new byte[len];
        for (int i = 0; i < len; i++) {
            double t = (double) i / rate;
            double freq = (t < 0.04) ? 523.25 : 783.99; // C5 to G5
            double amp = Math.exp(-t * 25);
            buf[i] = (byte)(Math.sin(2 * Math.PI * freq * t) * 127 * amp);
        }
        return buf;
    }

    private static byte[] generateCancelWave() {
        int rate = 8000;
        int len = (int)(rate * 0.10);
        byte[] buf = new byte[len];
        for (int i = 0; i < len; i++) {
            double t = (double) i / rate;
            double freq = 330.0 - (t * 1500); // Descending frequency sweep
            double amp = Math.exp(-t * 20);
            buf[i] = (byte)(Math.sin(2 * Math.PI * freq * t) * 127 * amp);
        }
        return buf;
    }

    private static byte[] generateWindowWave() {
        int rate = 8000;
        int len = (int)(rate * 0.12);
        byte[] buf = new byte[len];
        for (int i = 0; i < len; i++) {
            double t = (double) i / rate;
            double freq = 392.0 + (t * 3000); // Rising swoop
            double amp = Math.exp(-t * 8);
            buf[i] = (byte)(Math.sin(2 * Math.PI * freq * t) * 127 * amp);
        }
        return buf;
    }

    // --- PLAYBACK METHODS ---

    public static void playCursor() {
        play(sfxCursor);
    }

    public static void playDecide() { play(sfxDecide); }
    public static void playCancel() { play(sfxCancel); }
    public static void playWindow() { play(sfxWindow); }

    public static void playButtonSound() {
        File file = new File(GamePaths.BUNDLED_ROOT, "audio/ClickButton.wav");
        if (file.exists()) {
            try {
                AudioInputStream stream = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) clip.close();
                });
                clip.start();
            } catch (Exception e) {
                playDecide();
            }
        } else {
            playDecide();
        }
    }

    private static void play(Clip clip) {
        if (clip == null) return;
        new Thread(() -> {
            synchronized (clip) {
                clip.setFramePosition(0);
                clip.start();
            }
        }).start();
    }

    // --- BGM METHODS ---

    public static void playBgm() {
        Platform.runLater(() -> {
            if (bgmPlayer != null) {
                bgmPlayer.play();
            }
        });
    }

    public static void pauseBgm() {
        Platform.runLater(() -> {
            if (bgmPlayer != null) {
                bgmPlayer.pause();
            }
        });
    }

    public static void setBgmVolume(double vol) {
        Platform.runLater(() -> {
            if (bgmPlayer != null) {
                bgmPlayer.setVolume(vol);
            }
        });
    }

    // --- BATTLE SFX METHODS ---

    public static void playHitMelee()    { play(sfxHitMelee); }
    public static void playHitLance()    { play(sfxHitLance); }
    public static void playHitCritical() { play(sfxHitCritical); }
    public static void playHitKill()     { play(sfxHitKill); }
    public static void playNoDamage()    { play(sfxNoDamage); }
    public static void playArrowHit()    { play(sfxArrowHit); }
    public static void playMiss()        { play(sfxMiss); }
    public static void playSwordSwing()  { play(sfxSwordSwing); }
    public static void playAxeSwing()    { play(sfxAxeSwing); }
    public static void playSpearSpin()   { play(sfxSpearSpin); }
    public static void playBowRelease()  { play(sfxBowRelease); }
    public static void playStepHeavy()   { play(sfxStepHeavy); }
    public static void playHumanFall()   { play(sfxHumanFall); }
    public static void playFadeDieAway1(){ play(sfxFadeDieAway1); }
    public static void playHpTick()      { play(sfxHpTick); }
    public static void playLevelUp()     { play(sfxLevelUp); }
    public static void playStepGrass()   { play(sfxStepGrass); }
    public static void playStepStone()   { play(sfxStepStone); }
    public static void playStepInfantry(){ play(sfxStepInfantry); }
    public static void playStepHorse()   { play(sfxStepHorse); }

    /**
     * Plays the correct hit SFX based on weapon type and combat outcome.
     * Matches FE8 logic: crit > kill > miss > no damage > weapon-specific hit.
     */
    public static void playBattleHitSfx(String weaponType, boolean isCrit, boolean isMiss, int damage, boolean isKill) {
        if (isMiss)              { playMiss(); return; }
        if (isCrit)              { playHitCritical(); return; }
        if (isKill)              { playHitKill(); return; }
        if (damage == 0)         { playNoDamage(); return; }
        if (weaponType == null)  { playHitMelee(); return; }
        switch (weaponType.toUpperCase()) {
            case "LANCE":
            case "SPEAR":  playHitLance(); break;
            case "BOW":    playArrowHit(); break;
            default:       playHitMelee(); break;
        }
    }

    /** Backward-compatible overload */
    public static void playBattleHitSfx(String weaponType, boolean isCrit, boolean isMiss) {
        playBattleHitSfx(weaponType, isCrit, isMiss, isMiss ? 0 : 1, false);
    }

    /**
     * Plays the correct SFX for a FE8 animation script command code.
     * Called by handleFE8Command() in VersusGameplayScreen for non-C01 commands.
     * @param code the command code string from script.txt (e.g. "C1B", "C22")
     * @param weaponType the attacker's weapon type for context
     */
    public static void playScriptCommandSfx(String code, String weaponType) {
        if (code == null) return;
        String wpn = (weaponType != null) ? weaponType.toUpperCase() : "";
        switch (code) {
            case "C1B": playStepHeavy(); break;       // Heavy footstep / charge
            case "C22":                                // Short weapon swing
            case "C23":                                // Shorter weapon swing
                switch (wpn) {
                    case "AXE":   playAxeSwing(); break;
                    case "LANCE":
                    case "SPEAR": playSpearSpin(); break;
                    default:      playSwordSwing(); break;
                }
                break;
            case "C38": playSpearSpin(); break;        // Spear spin / lance whoosh
            case "C25":                                // Ranged weapon launch
                switch (wpn) {
                    case "LANCE":
                    case "SPEAR": playSpearSpin(); break;   // Javelin throw
                    case "AXE":   playAxeSwing(); break;    // Hand Axe throw
                    default:      playBowRelease(); break;  // Bow / magic
                }
                break;
            case "C1A":                                // Normal hit marker (no sound)
                break;
            case "C21":                                // Start attack #2 marker
                break;
            default: break;
        }
    }

    /**
     * Plays a map footstep sound for unit movement.
     */
    public static void playFootstep() {
        play(sfxStepGrass);
    }

    // --- GLOBAL SWING AUTO-INTEGRATION HOOKS ---

    public static void attachToContainer(Container container) {
        if (container == null) return;
        for (Component c : container.getComponents()) {
            boolean isButton = c instanceof JButton || c instanceof JMenuItem;
            if (isButton) {
                c.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (c.isEnabled()) playCursor();
                    }
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (c.isEnabled()) playButtonSound();
                    }
                });
            } else if (c instanceof JComboBox) {
                c.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        playCursor();
                    }
                });
                ((JComboBox<?>) c).addActionListener(e -> playButtonSound());
            }
            if (c instanceof Container) {
                attachToContainer((Container) c);
            }
        }
    }
}
