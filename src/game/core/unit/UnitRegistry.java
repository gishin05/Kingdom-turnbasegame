package game.core.unit;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Singleton registry that caches UnitStats for all units.
 * Scans Champion/ and Unit/ directories at first access.
 * Call UnitRegistry.loadAll() once on startup.
 */
public class UnitRegistry {

    private static final String ANIMS_BASE =
        game.core.util.GamePaths.BATTLE.getAbsolutePath();

    private static final Map<String, UnitStats> cache       = new LinkedHashMap<>();
    private static final Map<String, String>    categoryMap = new LinkedHashMap<>();
    private static boolean loaded = false;

    // ── Public API ────────────────────────────────────────────

    /** Load all units from disk (Champion/ and Unit/ categories). Call once at startup. */
    public static synchronized void loadAll() {
        if (loaded) return;
        cache.clear();
        categoryMap.clear();
        scanCategory("Champion");
        scanCategory("Unit");
        loaded = true;
        System.out.println("[UnitRegistry] Loaded " + cache.size() + " units: " + cache.keySet());
    }

    /** Force a reload (call after adding new unit folders) */
    public static synchronized void reload() {
        loaded = false;
        loadAll();
    }

    /** Get cached stats for a unit by name. Returns defaults if not found. */
    public static UnitStats get(String unitName) {
        ensureLoaded();
        UnitStats s = cache.get(unitName);
        if (s == null) {
            System.err.println("[UnitRegistry] Unit not found: " + unitName);
            return new UnitStats();
        }
        return s;
    }

    /** Get category ("Champion" or "Unit") for a unit name. */
    public static String getCategory(String unitName) {
        ensureLoaded();
        return categoryMap.getOrDefault(unitName, "Unit");
    }

    /** Load stats directly from a specific category/unit path (bypasses cache). */
    public static UnitStats load(String category, String unitName) {
        File f = new File(ANIMS_BASE + "/" + category + "/" + unitName + "/stats.json");
        if (f.exists()) {
            try {
                String json = new String(Files.readAllBytes(f.toPath()));
                return UnitStats.fromJson(json);
            } catch (Exception e) {
                System.err.println("[UnitRegistry] Error loading " + f.getPath() + ": " + e.getMessage());
            }
        }
        return new UnitStats();
    }

    /** Get all unit names for a given category */
    public static List<String> getUnitsForCategory(String category) {
        ensureLoaded();
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> e : categoryMap.entrySet()) {
            if (e.getValue().equalsIgnoreCase(category)) result.add(e.getKey());
        }
        return result;
    }

    /** Get all known unit names */
    public static Set<String> getAllUnitNames() {
        ensureLoaded();
        return Collections.unmodifiableSet(cache.keySet());
    }

    /** Get the available weapon folders for a given category/unit */
    public static List<String> getWeaponFolders(String category, String unitName) {
        List<String> weapons = new ArrayList<>();
        File unitDir = new File(ANIMS_BASE + "/" + category + "/" + unitName);
        if (!unitDir.exists()) return weapons;
        File[] subs = unitDir.listFiles(File::isDirectory);
        if (subs != null) {
            Arrays.sort(subs);
            for (File f : subs) weapons.add(f.getName());
        }
        return weapons;
    }

    // ── Internal ──────────────────────────────────────────────

    private static void ensureLoaded() {
        if (!loaded) loadAll();
    }

    private static void scanCategory(String category) {
        File catDir = new File(ANIMS_BASE + "/" + category);
        if (!catDir.exists() || !catDir.isDirectory()) return;

        File[] units = catDir.listFiles(File::isDirectory);
        if (units == null) return;

        for (File unitDir : units) {
            String name = unitDir.getName();
            UnitStats stats = load(category, name);
            stats.unitName = name;  // ensure name is set even if stats.json is missing
            cache.put(name, stats);
            categoryMap.put(name, category);
        }
    }
}
