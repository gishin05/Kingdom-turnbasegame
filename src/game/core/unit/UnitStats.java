package game.core.unit;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import game.core.util.JsonUtil;

/**
 * Data class for global character/unit statistics.
 * Load from disk via UnitStats.load(category, unitName).
 */
public class UnitStats implements Serializable {

    private static final String ANIMS_BASE =
        game.core.util.GamePaths.BATTLE.getAbsolutePath();

    private static final long serialVersionUID = 1L;

    public String unitName = "Unknown";
    public String unitType = "Land Unit"; // Land, Ocean, Air, etc.
    public String subUnitType = "Infantry"; // Armored, Mounted, Infantry, Siege
    
    // Core Info
    public int level = 1;
    public int exp = 0;
    public int maxHp = 20;
    
    // Fighting Skill
    public int strength = 5;
    public int magic = 0;
    public int skill = 5;
    public int speed = 5;
    public int luck = 5;
    public int defense = 5;
    public int resistance = 5;
    public String affinity = "Fire"; // Fire, Thunder, Wind, Water, Ice, Light, Anima, Dark

    // Personal Data
    public int move = 5;
    public int con = 5;
    public int aid = 4;
    public int trv = 0; // Index or Name of rescued unit
    public String cond = "Healthy";

    // Battle Record (B/W/L)
    public int battles = 0;
    public int wins = 0;
    public int losses = 0;

    public UnitStats() {}

    /**
     * Load stats from disk: assets/data/battle/{category}/{unitName}/stats.json
     * Returns defaults if the file doesn't exist.
     */
    public static UnitStats load(String category, String unitName) {
        try {
            File f = new File(ANIMS_BASE + "/" + category + "/" + unitName + "/stats.json");
            if (!f.exists()) {
                f = new File(ANIMS_BASE + "/" + unitName + "/stats.json");
            }
            if (!f.exists()) {
                UnitStats defaults = new UnitStats();
                defaults.unitName = unitName;
                return defaults;
            }
            String json = new String(Files.readAllBytes(f.toPath()));
            return fromJson(json);
        } catch (Exception e) {
            e.printStackTrace();
            UnitStats defaults = new UnitStats();
            defaults.unitName = unitName;
            return defaults;
        }
    }

    public void save(String category) {
        try {
            File dir = new File(ANIMS_BASE + "/" + category + "/" + unitName);
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "stats.json");
            Files.write(f.toPath(), toJson().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String toJson() {
        return "{\n" +
               "  \"name\": \"" + unitName + "\",\n" +
               "  \"type\": \"" + unitType + "\",\n" +
               "  \"subType\": \"" + subUnitType + "\",\n" +
               "  \"lvl\": " + level + ",\n" +
               "  \"exp\": " + exp + ",\n" +
               "  \"hp\": " + maxHp + ",\n" +
               "  \"str\": " + strength + ",\n" +
               "  \"mag\": " + magic + ",\n" +
               "  \"skl\": " + skill + ",\n" +
               "  \"spd\": " + speed + ",\n" +
               "  \"lck\": " + luck + ",\n" +
               "  \"def\": " + defense + ",\n" +
               "  \"res\": " + resistance + ",\n" +
               "  \"aff\": \"" + affinity + "\",\n" +
               "  \"mov\": " + move + ",\n" +
               "  \"con\": " + con + ",\n" +
               "  \"aid\": " + aid + ",\n" +
               "  \"trv\": " + trv + ",\n" +
               "  \"cond\": \"" + cond + "\",\n" +
               "  \"bwl\": { \"b\":" + battles + ",\"w\":" + wins + ",\"l\":" + losses + " }\n" +
               "}";
    }

    public static UnitStats fromJson(String json) {
        UnitStats stats = new UnitStats();
        stats.unitName = JsonUtil.extractJsonVal(json, "name");
        stats.unitType = JsonUtil.extractJsonVal(json, "type");
        stats.subUnitType = JsonUtil.extractJsonVal(json, "subType");
        if (stats.subUnitType == null || stats.subUnitType.isEmpty()) stats.subUnitType = "Infantry";
        stats.level = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "lvl"), 1);
        stats.exp = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "exp"), 0);
        stats.maxHp = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "hp"), 20);
        stats.strength = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "str"), 5);
        stats.magic = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "mag"), 0);
        stats.skill = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "skl"), 5);
        stats.speed = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "spd"), 5);
        stats.luck = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "lck"), 5);
        stats.defense = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "def"), 5);
        stats.resistance = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "res"), 5);
        stats.affinity = JsonUtil.extractJsonVal(json, "aff");
        stats.move = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "mov"), 5);
        stats.con = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "con"), 5);
        stats.aid = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "aid"), 4);
        stats.trv = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "trv"), 0);
        stats.cond = JsonUtil.extractJsonVal(json, "cond");
        
        // BWL
        stats.battles = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "b"), 0);
        stats.wins = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "w"), 0);
        stats.losses = JsonUtil.parseInt(JsonUtil.extractJsonVal(json, "l"), 0);
        
        return stats;
    }
}
