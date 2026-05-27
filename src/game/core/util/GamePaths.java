package game.core.util;

import java.io.File;

/**
 * Central paths for game content. All loaders and editors should use this class.
 */
public final class GamePaths {

    private static final File PROJECT_ROOT = findProjectRoot();

    public static final File BUNDLED_ROOT = new File(PROJECT_ROOT, "assets/bundled");
    public static final File DATA_ROOT = new File(PROJECT_ROOT, "assets/data");
    public static final File RUNTIME_ROOT = new File(PROJECT_ROOT, "runtime");

    public static final File MAPS_VERSUS = new File(DATA_ROOT, "maps/versus");
    public static final File MAPS_CAMPAIGN = new File(DATA_ROOT, "maps/campaign");
    public static final File TILESETS = new File(DATA_ROOT, "tilesets");
    public static final File TILESETS_IMPORTED = new File(TILESETS, "imported");
    public static final File UNITS = new File(DATA_ROOT, "units");
    public static final File BATTLE = new File(DATA_ROOT, "battle");
    public static final File WEAPONS = new File(DATA_ROOT, "weapons");
    public static final File BATTLE_BACKGROUNDS = new File(DATA_ROOT, "battle-scenes/backgrounds");
    public static final File BATTLE_PLATFORMS = new File(DATA_ROOT, "battle-scenes/platforms");
    public static final File SAVES = new File(RUNTIME_ROOT, "saves");
    public static final File ARCHIVE_TILESETS = new File(PROJECT_ROOT, "assets/archive/tilesets-vendor");

    private GamePaths() {}

    /** Classpath resource path (leading slash) for bundled assets. */
    public static String bundledResource(String relative) {
        String p = relative.replace('\\', '/');
        return p.startsWith("/") ? p : "/" + p;
    }

    public static File bundledFile(String relative) {
        return new File(BUNDLED_ROOT, relative);
    }

    public static File dataFile(String relative) {
        return new File(DATA_ROOT, relative);
    }

    public static File mapVersusFile(String fileName) {
        return new File(MAPS_VERSUS, fileName);
    }

    public static File mapVersusFileForMap(String mapName) {
        String name = mapName.endsWith(".json") ? mapName : mapName + ".json";
        return mapVersusFile(name);
    }

    public static File battleTerrain(String terrainName) {
        return new File(DATA_ROOT, "battle-scenes/terrain/" + terrainName.toUpperCase());
    }

    public static File unitActionDir(String category, String unitName, String action) {
        return new File(UNITS, category + "/" + unitName + "/MovingAnimation/" + action);
    }

    public static File battleAnimDir(String category, String name, String weapon) {
        return new File(BATTLE, category + "/" + name + "/" + weapon);
    }

    /** Legacy overload for backwards compatibility */
    public static File battleAnimDir(String name, String weapon) {
        return new File(BATTLE, name + "/" + weapon);
    }

    public static String[] battleAssetSearchRoots() {
        return new String[] {
            BATTLE.getPath() + File.separator,
            UNITS.getPath() + File.separator
        };
    }

    public static void ensureRuntimeDirs() {
        SAVES.mkdirs();
        MAPS_VERSUS.mkdirs();
        MAPS_CAMPAIGN.mkdirs();
    }

    private static File findProjectRoot() {
        String cwd = System.getProperty("user.dir");
        File dir = new File(cwd);
        while (dir != null) {
            if (new File(dir, "assets").isDirectory() && new File(dir, "src").isDirectory()) {
                return dir;
            }
            if (new File(dir, "TurnBasedGame").isDirectory()) {
                return new File(dir, "TurnBasedGame");
            }
            dir = dir.getParentFile();
        }
        return new File(cwd);
    }
}
