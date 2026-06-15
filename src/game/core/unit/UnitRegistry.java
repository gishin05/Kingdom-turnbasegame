package game.core.unit;

import java.io.File;
import java.util.*;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

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
        return UnitStats.load(category, unitName);
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
        for (String root : game.core.util.GamePaths.battleAssetSearchRoots()) {
            File catDir = new File(root + category);
            if (!catDir.exists() || !catDir.isDirectory()) continue;

            File[] units = catDir.listFiles(File::isDirectory);
            if (units == null) continue;

            for (File unitDir : units) {
                String name = unitDir.getName();
                // Avoid overwriting if already found in a higher priority root
                if (!cache.containsKey(name)) {
                    UnitStats stats = load(category, name);
                    stats.unitName = name;  // ensure name is set even if stats.json is missing
                    cache.put(name, stats);
                    categoryMap.put(name, category);
                }
            }
        }
    }

    public static List<WeaponItem> getDefaultWeapons(String cat, String name) {
        List<WeaponItem> weapons = new ArrayList<>();
        if ("Fleet".equalsIgnoreCase(name)) return weapons;
        
        File battleDir = new File(game.core.util.GamePaths.BATTLE, cat + "/" + name);
        if (!battleDir.exists()) battleDir = new File(game.core.util.GamePaths.BATTLE, name);
        
        boolean hasWeapons = false;
        if (battleDir.exists()) {
            if (new File(battleDir, "Sword").exists() || new File(battleDir, "sword").exists()) {
                WeaponItem w = WeaponItem.byName("Iron Sword"); w.maxUses = 20; w.currentUses = 20; weapons.add(w); hasWeapons = true;
            }
            if (new File(battleDir, "Lance").exists() || new File(battleDir, "lance").exists() || new File(battleDir, "Spear").exists() || new File(battleDir, "spear").exists()) {
                if ("Ephraim".equalsIgnoreCase(name)) {
                    WeaponItem reginleif = WeaponItem.byName("Reginleif");
                    reginleif.maxUses = 45; reginleif.currentUses = 45;
                    weapons.add(reginleif);
                } else {
                    WeaponItem w1 = WeaponItem.byName("Iron Lance"); w1.maxUses = 20; w1.currentUses = 20; weapons.add(w1);
                }
                if (hasRangedAnimation(battleDir, "Lance", "lance", "Spear", "spear")) {
                    WeaponItem w2 = WeaponItem.byName("Javelin"); w2.maxUses = 10; w2.currentUses = 10; weapons.add(w2);
                }
                hasWeapons = true;
            }
            if (new File(battleDir, "Axe").exists() || new File(battleDir, "axe").exists()) {
                WeaponItem w1 = WeaponItem.byName("Iron Axe"); w1.maxUses = 20; w1.currentUses = 20; weapons.add(w1);
                if (hasRangedAnimation(battleDir, "Axe", "axe")) {
                    WeaponItem w2 = WeaponItem.byName("Hand Axe"); w2.maxUses = 10; w2.currentUses = 10; weapons.add(w2);
                }
                hasWeapons = true;
            }
            if (new File(battleDir, "Bow").exists() || new File(battleDir, "bow").exists()) {
                if ("Ballista".equalsIgnoreCase(name)) {
                    WeaponItem w = WeaponItem.byName("Ballista"); w.maxUses = 5; w.currentUses = 5; weapons.add(w); hasWeapons = true;
                } else {
                    WeaponItem w = WeaponItem.byName("Iron Bow"); w.maxUses = 20; w.currentUses = 20; weapons.add(w); hasWeapons = true;
                }
            }
            if (new File(battleDir, "Magic").exists() || new File(battleDir, "magic").exists()) {
                WeaponItem w = WeaponItem.byName("Fire"); w.maxUses = 20; w.currentUses = 20; weapons.add(w); hasWeapons = true;
            }
        }
        
        if (!hasWeapons) {
            WeaponItem ironLance = WeaponItem.byName("Iron Lance"); ironLance.maxUses = 20; ironLance.currentUses = 20; weapons.add(ironLance);
            WeaponItem javelin = WeaponItem.byName("Javelin"); javelin.maxUses = 10; javelin.currentUses = 10; weapons.add(javelin);
        }
        return weapons;
    }

    public static boolean hasRangedAnimation(File battleDir, String... folderNames) {
        for (String fName : folderNames) {
            File dir = new File(battleDir, fName);
            if (!dir.exists()) continue;
            File script = new File(dir, "script.txt");
            if (!script.exists()) continue;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(script))) {
                String line;
                boolean inRangedMode = false;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("/// - Mode 5") || line.startsWith("/// - Mode 6")) {
                        inRangedMode = true;
                    } else if (line.startsWith("/// - Mode")) {
                        inRangedMode = false;
                    } else if (inRangedMode && !line.isEmpty() && !line.startsWith("~") && !line.startsWith("//") && !line.startsWith("#")) {
                        return true;
                    }
                }
            } catch (Exception e) {}
        }
        return false;
    }
}
