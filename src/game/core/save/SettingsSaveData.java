package game.core.save;

import java.io.*;
import java.util.Properties;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

public class SettingsSaveData {
    public double bgmVolume = 1.0;
    public float sfxVolume = 1.0f;
    public int resolutionIndex = 3; // Default 1920x1080
    public int mapSizeIndex = 0;
    public boolean touchOverlayEnabled = false;

    // Controls
    public int upKey1 = 38;    // KeyEvent.VK_UP
    public int upKey2 = 87;    // KeyEvent.VK_W
    public int downKey1 = 40;  // KeyEvent.VK_DOWN
    public int downKey2 = 83;  // KeyEvent.VK_S
    public int leftKey1 = 37;  // KeyEvent.VK_LEFT
    public int leftKey2 = 65;  // KeyEvent.VK_A
    public int rightKey1 = 39; // KeyEvent.VK_RIGHT
    public int rightKey2 = 68; // KeyEvent.VK_D
    public int enterKey = 10;  // KeyEvent.VK_ENTER
    public int escKey = 27;    // KeyEvent.VK_ESCAPE

    public void writeTo(File file) {
        Properties p = new Properties();
        p.setProperty("bgmVolume", String.valueOf(bgmVolume));
        p.setProperty("sfxVolume", String.valueOf(sfxVolume));
        p.setProperty("resolutionIndex", String.valueOf(resolutionIndex));
        p.setProperty("mapSizeIndex", String.valueOf(mapSizeIndex));
        p.setProperty("touchOverlayEnabled", String.valueOf(touchOverlayEnabled));
        
        p.setProperty("upKey1", String.valueOf(upKey1));
        p.setProperty("upKey2", String.valueOf(upKey2));
        p.setProperty("downKey1", String.valueOf(downKey1));
        p.setProperty("downKey2", String.valueOf(downKey2));
        p.setProperty("leftKey1", String.valueOf(leftKey1));
        p.setProperty("leftKey2", String.valueOf(leftKey2));
        p.setProperty("rightKey1", String.valueOf(rightKey1));
        p.setProperty("rightKey2", String.valueOf(rightKey2));
        p.setProperty("enterKey", String.valueOf(enterKey));
        p.setProperty("escKey", String.valueOf(escKey));
        
        file.getParentFile().mkdirs();
        try (OutputStream os = new FileOutputStream(file)) {
            p.store(os, "Game Settings");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SettingsSaveData readFrom(File file) {
        SettingsSaveData d = new SettingsSaveData();
        if (!file.exists()) return d;
        
        Properties p = new Properties();
        try (InputStream is = new FileInputStream(file)) {
            p.load(is);
            d.bgmVolume = Double.parseDouble(p.getProperty("bgmVolume", "1.0"));
            d.sfxVolume = Float.parseFloat(p.getProperty("sfxVolume", "1.0"));
            d.resolutionIndex = Integer.parseInt(p.getProperty("resolutionIndex", "3"));
            d.mapSizeIndex = Integer.parseInt(p.getProperty("mapSizeIndex", "0"));
            d.touchOverlayEnabled = Boolean.parseBoolean(p.getProperty("touchOverlayEnabled", "false"));
            
            d.upKey1 = Integer.parseInt(p.getProperty("upKey1", "38"));
            d.upKey2 = Integer.parseInt(p.getProperty("upKey2", "87"));
            d.downKey1 = Integer.parseInt(p.getProperty("downKey1", "40"));
            d.downKey2 = Integer.parseInt(p.getProperty("downKey2", "83"));
            d.leftKey1 = Integer.parseInt(p.getProperty("leftKey1", "37"));
            d.leftKey2 = Integer.parseInt(p.getProperty("leftKey2", "65"));
            d.rightKey1 = Integer.parseInt(p.getProperty("rightKey1", "39"));
            d.rightKey2 = Integer.parseInt(p.getProperty("rightKey2", "68"));
            d.enterKey = Integer.parseInt(p.getProperty("enterKey", "10"));
            d.escKey = Integer.parseInt(p.getProperty("escKey", "27"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return d;
    }
}
