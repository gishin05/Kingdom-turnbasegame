package game.core.unit;

import java.io.File;
import java.nio.file.Files;

/**
 * Weapon / item data.
 * Directly mirrors FE8's struct ItemData from bmitem.h.
 * Real stat values taken from constants/items.h + FE8 data tables.
 */
public class WeaponItem {

    // ── Identity ──────────────────────────────────────────────
    public String name       = "Iron Lance";
    /** "Sword","Lance","Axe","Bow","Anima","Light","Dark","Staff","Item" */
    public String weaponType = "Lance";

    // ── Combat Stats (from ItemData) ──────────────────────────
    public int might   = 7;   // base attack power added to Str/Mag
    public int hit     = 80;  // base hit rate bonus
    public int crit    = 0;   // base critical rate bonus
    public int weight  = 8;   // reduces attack speed when > unit CON

    // ── Range ─────────────────────────────────────────────────
    public int minRange = 1;
    public int maxRange = 1;

    // ── Durability ────────────────────────────────────────────
    public int maxUses     = 35;
    public int currentUses = 35;

    // ── Animation folder link ─────────────────────────────────
    /** Matches the subfolder in the unit's anim directory, e.g. "spear", "lance" */
    public String animWeaponFolder = "lance";

    // ── Weapon Triangle (matches FE8 ITYPE_* enum) ───────────
    public enum WpnType { SWORD, LANCE, AXE, BOW, ANIMA, LIGHT, DARK, STAFF, ITEM }

    // ── Constructors ──────────────────────────────────────────
    public WeaponItem() {}

    public WeaponItem(String name, String type, int might, int hit, int crit,
                      int weight, int minR, int maxR, int maxUses, String animFolder) {
        this.name             = name;
        this.weaponType       = type;
        this.might            = might;
        this.hit              = hit;
        this.crit             = crit;
        this.weight           = weight;
        this.minRange         = minR;
        this.maxRange         = maxR;
        this.maxUses          = maxUses;
        this.currentUses      = maxUses;
        this.animWeaponFolder = animFolder;
    }

    public boolean isWeapon()  { return !weaponType.equalsIgnoreCase("Staff") && !weaponType.equalsIgnoreCase("Item"); }
    public boolean coversRange(int r) { return r >= minRange && r <= maxRange; }
    public boolean isBroken()  { return currentUses <= 0; }
    public void useOnce()      { if (currentUses > 0) currentUses--; }

    // ── Serialization ─────────────────────────────────────────
    public String toJson() {
        return "{\n  \"name\":\"" + name + "\",\n  \"type\":\"" + weaponType
             + "\",\n  \"might\":" + might + ",\n  \"hit\":" + hit
             + ",\n  \"crit\":" + crit + ",\n  \"weight\":" + weight
             + ",\n  \"minRange\":" + minRange + ",\n  \"maxRange\":" + maxRange
             + ",\n  \"maxUses\":" + maxUses + ",\n  \"currentUses\":" + currentUses
             + ",\n  \"animFolder\":\"" + animWeaponFolder + "\"\n}";
    }

    public static WeaponItem fromJson(String json) {
        WeaponItem w = new WeaponItem();
        w.name             = str(json, "name");
        w.weaponType       = str(json, "type");
        w.might            = num(json, "might",   7);
        w.hit              = num(json, "hit",    80);
        w.crit             = num(json, "crit",    0);
        w.weight           = num(json, "weight",  8);
        w.minRange         = num(json, "minRange",1);
        w.maxRange         = num(json, "maxRange",1);
        w.maxUses          = num(json, "maxUses",35);
        w.currentUses      = num(json, "currentUses",35);
        w.animWeaponFolder = str(json, "animFolder");
        return w;
    }

    public static WeaponItem load(String weaponType, String name) {
        File f = new File(game.core.util.GamePaths.WEAPONS, weaponType + "/" + name + ".json");
        if (!f.exists()) return defaultForType(weaponType);
        try { return fromJson(new String(Files.readAllBytes(f.toPath()))); }
        catch (Exception e) { return defaultForType(weaponType); }
    }

    public void save() {
        File dir = new File(game.core.util.GamePaths.WEAPONS, weaponType);
        if (!dir.exists()) dir.mkdirs();
        try { Files.write(new File(dir, name + ".json").toPath(), toJson().getBytes()); }
        catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Pre-built weapon table from FE8 data.
     * Stats sourced from bmitem.h ItemData fields:
     *   might, hit, crit, weight, encodedRange, maxUses
     */
    public static WeaponItem[] FE8_WEAPONS = {
        // ── SWORDS ─────────────────────────────── Mt  Hit Crit Wt  minR maxR Uses  animFolder
        new WeaponItem("Iron Sword",    "Sword",     5,  90,  0,  5,   1,   1,  46, "sword"),
        new WeaponItem("Slim Sword",    "Sword",     4, 100,  0,  2,   1,   1,  30, "sword"),
        new WeaponItem("Steel Sword",   "Sword",     8,  75,  0,  9,   1,   1,  30, "sword"),
        new WeaponItem("Silver Sword",  "Sword",    13,  80,  0,  7,   1,   1,  15, "sword"),
        new WeaponItem("Brave Sword",   "Sword",     9,  75,  0,  7,   1,   1,  30, "sword"),
        new WeaponItem("Killer Sword",  "Sword",     9,  75, 30,  7,   1,   1,  20, "sword"),
        new WeaponItem("Armorslayer",   "Sword",     8,  80,  0,  9,   1,   1,  18, "sword"),
        new WeaponItem("Wyrmslayer",    "Sword",     8,  80,  0,  9,   1,   1,  20, "sword"),
        new WeaponItem("Light Brand",   "Sword",    11,  80,  0,  7,   1,   2,  30, "sword"),
        new WeaponItem("Runesword",     "Sword",    12,  65,  0,  9,   1,   2,  15, "sword"),
        new WeaponItem("Lancereaver",   "Sword",    10,  75,  0,  9,   1,   1,  15, "sword"),
        new WeaponItem("Zanbato",       "Sword",     8,  80,  0,  9,   1,   1,  18, "sword"),
        new WeaponItem("Rapier",        "Sword",     5, 100,  5,  2,   1,   1,  40, "sword"),
        new WeaponItem("Sieglinde",     "Sword",    17,  90,  0,  9,   1,   2,  30, "sword"),
        new WeaponItem("Audhulma",      "Sword",    18,  75,  0,  9,   1,   1,  30, "sword"),

        // ── LANCES ─────────────────────────────── Mt  Hit Crit Wt  minR maxR Uses  animFolder
        new WeaponItem("Iron Lance",    "Lance",     7,  80,  0,  8,   1,   1,  35, "lance"),
        new WeaponItem("Slim Lance",    "Lance",     6,  85,  0,  4,   1,   1,  30, "lance"),
        new WeaponItem("Steel Lance",   "Lance",    11,  70,  0, 13,   1,   1,  30, "lance"),
        new WeaponItem("Silver Lance",  "Lance",    14,  80,  0, 11,   1,   1,  15, "lance"),
        new WeaponItem("Brave Lance",   "Lance",    10,  75,  0, 12,   1,   1,  30, "lance"),
        new WeaponItem("Killer Lance",  "Lance",    10,  75, 30, 12,   1,   1,  20, "lance"),
        new WeaponItem("Horseslayer",   "Lance",     7,  70,  0, 10,   1,   1,  16, "lance"),
        new WeaponItem("Javelin",       "Lance",     6,  65,  0, 10,   1,   2,  20, "lance"),
        new WeaponItem("Spear",         "Lance",    14,  70,  0, 15,   1,   2,  12, "lance"),
        new WeaponItem("Axereaver",     "Lance",     9,  70,  0, 10,   1,   1,  15, "lance"),
        new WeaponItem("Reginleif",     "Lance",     7,  80,  0, 10,   1,   1,  25, "lance"),
        new WeaponItem("Vidofnir",      "Lance",    20,  80,  0, 13,   1,   1,  30, "lance"),
        new WeaponItem("Siegmund",      "Lance",    17,  80,  0, 10,   1,   2,  30, "lance"),

        // ── AXES ───────────────────────────────── Mt  Hit Crit Wt  minR maxR Uses  animFolder
        new WeaponItem("Iron Axe",      "Axe",       8,  75,  0, 10,   1,   1,  35, "axe"),
        new WeaponItem("Steel Axe",     "Axe",      12,  65,  0, 15,   1,   1,  30, "axe"),
        new WeaponItem("Silver Axe",    "Axe",      15,  75,  0, 13,   1,   1,  15, "axe"),
        new WeaponItem("Brave Axe",     "Axe",      11,  65,  0, 15,   1,   1,  30, "axe"),
        new WeaponItem("Killer Axe",    "Axe",      11,  65, 30, 15,   1,   1,  20, "axe"),
        new WeaponItem("Halberd",       "Axe",       8,  70,  0, 12,   1,   1,  16, "axe"),
        new WeaponItem("Hammer",        "Axe",      10,  65,  0, 16,   1,   1,  20, "axe"),
        new WeaponItem("Devil Axe",     "Axe",      18,  55,  0, 18,   1,   1,  20, "axe"),
        new WeaponItem("Hand Axe",      "Axe",       7,  60,  0, 12,   1,   2,  20, "axe"),
        new WeaponItem("Tomahawk",      "Axe",      14,  65,  0, 14,   1,   2,  15, "axe"),
        new WeaponItem("Swordreaver",   "Axe",      10,  70,  0, 15,   1,   1,  15, "axe"),
        new WeaponItem("Garm",          "Axe",      20,  75,  0, 14,   1,   2,  30, "axe"),

        // ── BOWS ───────────────────────────────── Mt  Hit Crit Wt  minR maxR Uses  animFolder
        new WeaponItem("Iron Bow",      "Bow",       6,  85,  0,  5,   2,   2,  35, "bow"),
        new WeaponItem("Steel Bow",     "Bow",       9,  70,  0,  8,   2,   2,  30, "bow"),
        new WeaponItem("Silver Bow",    "Bow",      12,  80,  0,  6,   2,   2,  15, "bow"),
        new WeaponItem("Killer Bow",    "Bow",       9,  75, 30,  7,   2,   2,  20, "bow"),
        new WeaponItem("Brave Bow",     "Bow",       8,  75,  0,  7,   2,   2,  30, "bow"),
        new WeaponItem("Short Bow",     "Bow",       5,  90,  5,  4,   1,   2,  30, "bow"),
        new WeaponItem("Longbow",       "Bow",       5,  65,  0,  9,   2,   3,  20, "bow"),
        new WeaponItem("Nidhogg",       "Bow",      20,  70,  0,  9,   2,   3,  30, "bow"),

        // ── ANIMA ──────────────────────────────── Mt  Hit Crit Wt  minR maxR Uses  animFolder
        new WeaponItem("Fire",          "Anima",     5,  90,  0,  4,   1,   2,  40, "anima"),
        new WeaponItem("Thunder",       "Anima",     8,  80,  5,  7,   1,   2,  35, "anima"),
        new WeaponItem("Elfire",        "Anima",    12,  85,  0,  9,   1,   2,  30, "anima"),
        new WeaponItem("Bolting",       "Anima",    12,  60,  0,  9,   3,   10, 8,  "anima"),
        new WeaponItem("Fimbulvetr",    "Anima",    14,  75,  0, 12,   1,   2,  20, "anima"),
        new WeaponItem("Forblaze",      "Anima",    21,  75,  0, 14,   1,   2,  20, "anima"),
        new WeaponItem("Excalibur",     "Anima",    10,  90, 15,  4,   1,   2,  20, "anima"),

        // ── LIGHT ──────────────────────────────── Mt  Hit Crit Wt  minR maxR Uses  animFolder
        new WeaponItem("Lightning",     "Light",     4,  95,  0,  4,   1,   2,  35, "light"),
        new WeaponItem("Shine",         "Light",     6,  90,  0,  6,   1,   2,  30, "light"),
        new WeaponItem("Divine",        "Light",     7,  80,  5,  8,   1,   2,  25, "light"),
        new WeaponItem("Purge",         "Light",    10,  60,  0,  7,   3,   10, 10, "light"),
        new WeaponItem("Aura",          "Light",    14,  85,  0,  8,   1,   2,  20, "light"),
        new WeaponItem("Luce",          "Light",    20,  85,  0, 11,   1,   2,  20, "light"),
        new WeaponItem("Ivaldi",        "Light",    20,  90,  0,  7,   1,   2,  30, "light"),

        // ── DARK ───────────────────────────────── Mt  Hit Crit Wt  minR maxR Uses  animFolder
        new WeaponItem("Flux",          "Dark",      7,  80,  0,  8,   1,   2,  35, "dark"),
        new WeaponItem("Luna",          "Dark",      0,  80,  0,  8,   1,   2,  15, "dark"),
        new WeaponItem("Nosferatu",     "Dark",     10,  70,  0, 10,   1,   2,  15, "dark"),
        new WeaponItem("Eclipse",       "Dark",      0,  30,  0,  7,   1,   2,   5, "dark"),
        new WeaponItem("Fenrir",        "Dark",     14,  80,  0, 15,   1,   2,  20, "dark"),
        new WeaponItem("Gleipnir",      "Dark",     20,  80,  0, 12,   1,   2,  20, "dark"),
        new WeaponItem("Naglfar",       "Dark",     23,  70,  0, 18,   1,   2,  20, "dark"),

        // ── STAVES ─────────────────────────────── Mt  Hit Crit Wt  minR maxR Uses  animFolder
        new WeaponItem("Heal",          "Staff",     0, 100,  0,  3,   1,   1,  30, "staff"),
        new WeaponItem("Mend",          "Staff",     0, 100,  0,  4,   1,   1,  20, "staff"),
        new WeaponItem("Recover",       "Staff",     0, 100,  0,  4,   1,   1,  15, "staff"),
        new WeaponItem("Physic",        "Staff",     0, 100,  0,  5,   1,   10, 15, "staff"),
        new WeaponItem("Fortify",       "Staff",     0, 100,  0,  5,   1,   15,  8, "staff"),
        new WeaponItem("Warp",          "Staff",     0, 100,  0,  3,   1,    5, 10, "staff"),
    };

    public static WeaponItem defaultForType(String type) {
        for (WeaponItem w : FE8_WEAPONS) {
            if (w.weaponType.equalsIgnoreCase(type)) return copy(w);
        }
        return new WeaponItem("Iron Lance","Lance",7,80,0,8,1,1,35,"lance");
    }

    public static WeaponItem byName(String name) {
        for (WeaponItem w : FE8_WEAPONS) {
            if (w.name.equalsIgnoreCase(name)) return copy(w);
        }
        return defaultForType("Lance");
    }

    private static WeaponItem copy(WeaponItem src) {
        return new WeaponItem(src.name, src.weaponType, src.might, src.hit, src.crit,
                              src.weight, src.minRange, src.maxRange, src.maxUses, src.animWeaponFolder);
    }

    // ── JSON helpers ──────────────────────────────────────────
    private static String str(String json, String key) {
        try {
            int s = json.indexOf("\"" + key + "\":");
            if (s < 0) return "";
            String sub = json.substring(json.indexOf(":", s) + 1).trim();
            if (sub.startsWith("\"")) { int e = sub.indexOf("\"",1); return sub.substring(1,e); }
            int e = sub.indexOf(","); if (e<0) e = sub.indexOf("}");
            return sub.substring(0,e).trim();
        } catch (Exception e) { return ""; }
    }
    private static int num(String json, String key, int def) {
        try { return Integer.parseInt(str(json,key).trim()); }
        catch (Exception e) { return def; }
    }

    @Override public String toString() {
        return name + " [" + weaponType + "] Mt:" + might + " Hit:" + hit
             + " Wt:" + weight + " Rng:" + minRange + "-" + maxRange
             + " (" + currentUses + "/" + maxUses + ")";
    }
}
