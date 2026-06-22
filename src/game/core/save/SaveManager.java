package game.core.save;

import java.io.File;
import java.io.IOException;

import game.core.util.GamePaths;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

public class SaveManager {
    private static final int MAX_SLOTS = 3;
    private static final String VERSUS_SAVE_FILE = "versus.dat";
    private static final String SETTINGS_FILE = "settings.ini";

    static {
        GamePaths.ensureRuntimeDirs();
    }

    public static boolean hasAnySave() {
        for (int i = 1; i <= MAX_SLOTS; i++) {
            if (hasSave(i)) return true;
        }
        return false;
    }

    public static boolean hasSave(int slot) {
        return new File(GamePaths.SAVES, "save" + slot + ".dat").exists();
    }

    public static void deleteSave(int slot) {
        File file = new File(GamePaths.SAVES, "save" + slot + ".dat");
        if (file.exists()) {
            file.delete();
        }
    }

    public static void createNewSave(int slot) {
        try {
            File file = new File(GamePaths.SAVES, "save" + slot + ".dat");
            file.createNewFile(); // Just an empty file for now
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Versus (single save file) ---
    public static boolean hasVersusSave() {
        return new File(GamePaths.SAVES, VERSUS_SAVE_FILE).exists();
    }

    public static void deleteVersusSave() {
        File f = new File(GamePaths.SAVES, VERSUS_SAVE_FILE);
        if (f.exists()) f.delete();
    }

    public static void saveVersus(VersusSaveData data) throws IOException {
        File f = new File(GamePaths.SAVES, VERSUS_SAVE_FILE);
        data.writeTo(f);
    }

    public static VersusSaveData loadVersus() throws IOException {
        File f = new File(GamePaths.SAVES, VERSUS_SAVE_FILE);
        return VersusSaveData.readFrom(f);
    }

    // --- Settings ---
    public static void saveSettings(SettingsSaveData data) {
        data.writeTo(new File(GamePaths.SAVES, SETTINGS_FILE));
    }

    public static SettingsSaveData loadSettings() {
        return SettingsSaveData.readFrom(new File(GamePaths.SAVES, SETTINGS_FILE));
    }
}
