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
    private static float sfxVolume = 1.0f;
    private static double masterBgmVolume = 1.0;

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
    private static Clip sfxShadowFlash;
    private static Clip sfxSwordOut;
    private static Clip sfxSwordBack;
    private static Clip sfxBallista;

    // Battle SFX — Unit Feedback
    private static Clip sfxHumanFall;
    private static Clip sfxFadeDieAway1;
    private static Clip sfxHpTick;
    private static Clip sfxLevelUp;

    // Weather SFX
    private static Clip sfxRain;
    private static Clip sfxThunder;
    private static Clip sfxWind;

    // Map SFX — Footsteps
    private static Clip sfxStepGrass;
    private static Clip sfxStepStone;
    private static Clip sfxStepInfantry;
    private static Clip sfxStepHorse;
    private static Clip sfxStepFlier;
    private static Clip sfxStepSiege;
    private static Clip sfxStepShip;

    static {
        initSounds();
        initBattleSfx();
        initBgm();
    }

    private static void initSounds() {
        sfxCursor = loadAudioFile("HoverAudio.wav");
        sfxDecide = loadAudioFile("decide.wav");
        sfxCancel = loadAudioFile("cancel.wav");
        sfxWindow = loadAudioFile("window.wav");
        
        sfxRain = loadAudioFile("rain.wav");
        sfxThunder = loadAudioFile("thunder.aif");
        sfxWind = loadAudioFile("wind.wav");
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
                        bgmPlayer.setVolume(masterBgmVolume);
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
        sfxShadowFlash = loadAudioFile("shadow_flash.aif");
        sfxSwordOut    = loadAudioFile("sword_out.aif");
        sfxSwordBack   = loadAudioFile("sword_back.aif");
        sfxBallista    = loadAudioFile("Ballista.aif");

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
        sfxStepFlier   = loadAudioFile("Flier_Move.aif");
        sfxStepSiege   = loadAudioFile("siege_move.wav");
        sfxStepShip    = loadAudioFile("ship_move.aif");
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



    // --- PLAYBACK METHODS ---

    public static void playCursor() {
        play(sfxCursor);
    }

    public static void playDecide() { play(sfxDecide); }
    public static void playCancel() { play(sfxCancel); }
    public static void playWindow() { play(sfxWindow); }
    public static void playThunder() {
        new Thread(() -> {
            File file = new File(GamePaths.BUNDLED_ROOT, "audio/thunder.aif");
            if (file.exists()) {
                try {
                    AudioInputStream stream = AudioSystem.getAudioInputStream(file);
                    Clip clip = AudioSystem.getClip();
                    clip.open(stream);
                    try {
                        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                        float dB = (sfxVolume <= 0f) ? gain.getMinimum() : (float) (20.0 * Math.log10(sfxVolume));
                        dB = Math.max(gain.getMinimum(), Math.min(dB, gain.getMaximum()));
                        gain.setValue(dB);
                    } catch (Exception ignored) {}
                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) clip.close();
                    });
                    clip.start();
                } catch (Exception e) {
                    play(sfxThunder);
                }
            } else {
                play(sfxThunder);
            }
        }).start();
    }

    public static void setRainLoop(boolean active) {
        if (sfxRain == null) return;
        if (active) {
            if (!sfxRain.isRunning() && !sfxRain.isActive()) {
                try {
                    FloatControl gain = (FloatControl) sfxRain.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (sfxVolume <= 0f) ? gain.getMinimum() : (float) (20.0 * Math.log10(sfxVolume));
                    dB = Math.max(gain.getMinimum(), Math.min(dB, gain.getMaximum()));
                    gain.setValue(dB);
                } catch (Exception ignored) {}
                sfxRain.setFramePosition(0);
                sfxRain.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } else {
            if (sfxRain.isActive() || sfxRain.isRunning()) {
                sfxRain.stop();
            }
        }
    }

    public static void setWindLoop(boolean active) {
        if (sfxWind == null) return;
        if (active) {
            if (!sfxWind.isRunning() && !sfxWind.isActive()) {
                try {
                    FloatControl gain = (FloatControl) sfxWind.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (sfxVolume <= 0f) ? gain.getMinimum() : (float) (20.0 * Math.log10(sfxVolume));
                    dB = Math.max(gain.getMinimum(), Math.min(dB, gain.getMaximum()));
                    gain.setValue(dB);
                } catch (Exception ignored) {}
                sfxWind.setFramePosition(0);
                sfxWind.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } else {
            if (sfxWind.isActive() || sfxWind.isRunning()) {
                sfxWind.stop();
            }
        }
    }

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
                // Apply SFX volume via gain control
                try {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (sfxVolume <= 0f) ? gain.getMinimum() : (float) (20.0 * Math.log10(sfxVolume));
                    dB = Math.max(gain.getMinimum(), Math.min(dB, gain.getMaximum()));
                    gain.setValue(dB);
                } catch (Exception ignored) {}
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

    public static double getBgmVolume() {
        if (bgmPlayer != null) {
            return bgmPlayer.getVolume();
        }
        return masterBgmVolume;
    }

    public static double getMasterBgmVolume() {
        return masterBgmVolume;
    }

    public static void setMasterBgmVolume(double vol) {
        masterBgmVolume = Math.max(0.0, Math.min(1.0, vol));
    }

    public static float getSfxVolume() {
        return sfxVolume;
    }

    public static void setSfxVolume(float vol) {
        sfxVolume = Math.max(0f, Math.min(1f, vol));
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
    public static void playShadowFlash() { play(sfxShadowFlash); }
    public static void playSwordOut()    { play(sfxSwordOut); }
    public static void playSwordBack()   { play(sfxSwordBack); }
    public static void playBallista()    { play(sfxBallista); }
    public static void playHumanFall()   { play(sfxHumanFall); }
    public static void playFadeDieAway1(){ play(sfxFadeDieAway1); }
    public static void playHpTick()      { play(sfxHpTick); }
    public static void playLevelUp()     { play(sfxLevelUp); }
    public static void playStepGrass()   { play(sfxStepGrass); }
    public static void playStepStone()   { play(sfxStepStone); }
    public static void playStepInfantry(){ play(sfxStepInfantry); }
    public static void playStepHorse()   { play(sfxStepHorse); }
    public static void playStepSiege()   { play(sfxStepSiege); }
    public static void playStepShip() { 
        if (sfxStepShip != null && !sfxStepShip.isActive()) {
            play(sfxStepShip);
        }
    }
    
    public static void stopStepShip() {
        if (sfxStepShip != null && (sfxStepShip.isActive() || sfxStepShip.isRunning())) {
            sfxStepShip.stop();
        }
    }
    
    private static long lastFlierStepTime = 0;
    public static void playStepFlier() { 
        long now = System.currentTimeMillis();
        if (now - lastFlierStepTime >= 300) {
            play(sfxStepFlier);
            lastFlierStepTime = now;
        }
    }

    /**
     * Plays the correct hit SFX based on weapon type and combat outcome.
     * Matches FE8 logic: crit > kill > miss > no damage > weapon-specific hit.
     */
    public static void playBattleHitSfx(String weaponType, boolean isCrit, boolean isMiss, int damage, boolean isKill) {
        if (isMiss)              { playMiss(); return; }
        if (isCrit)              { playHitCritical(); return; }
        if (isKill) {
            if (sfxHitKill != null) {
                playHitKill(); 
                return;
            }
            // If hit_kill.aif is missing, fall through to play the normal weapon hit sound
        }
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
            case "C1B": playStepInfantry(); break;       // Infantry footstep / charge
            case "C13": playStepHeavy(); break;          // Armor footstep / charge
            case "C1C":                                // Horse trot / step
            case "C1D":                                // Horse gallop
            case "C1E": playStepHorse(); break;        // Horse gallop alternate
            case "C22":                                // Short weapon swing
            case "C23":                                // Shorter weapon swing
                switch (wpn) {
                    case "AXE":   playAxeSwing(); break;
                    case "LANCE":
                    case "SPEAR": playSpearSpin(); break;
                    default:      playSwordSwing(); break;
                }
                break;
            case "C1F": playStepFlier(); break;        // Flier Wing Flap / Move
            case "C38": playSpearSpin(); break;        // Spear spin / lance whoosh
            case "C36":
                if ("SWORD".equals(wpn)) playSwordOut();
                break;
            case "C14":
                if ("BOW".equals(wpn)) playBallista();
                break;
            case "C43":
                if ("SWORD".equals(wpn)) playSwordBack();
                break;
            case "C25":                                // Ranged weapon launch
                switch (wpn) {
                    case "LANCE":
                    case "SPEAR": playSpearSpin(); break;   // Javelin throw
                    case "AXE":   playAxeSwing(); break;    // Hand Axe throw
                    case "SWORD": playShadowFlash(); break; // Assassin shadow throw
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
