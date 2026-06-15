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

    public void writeTo(File file) {
        Properties p = new Properties();
        p.setProperty("bgmVolume", String.valueOf(bgmVolume));
        p.setProperty("sfxVolume", String.valueOf(sfxVolume));
        p.setProperty("resolutionIndex", String.valueOf(resolutionIndex));
        
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return d;
    }
}
