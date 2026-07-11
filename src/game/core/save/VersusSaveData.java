package game.core.save;

import java.awt.Color;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

public class VersusSaveData {
    public static class PlayerData {
        public int index;
        public boolean isAI;
        public int gold;
        public Color color;
    }

    public static class UnitData {
        public String category = "";
        public String unitName = "";
        public int ownerIndex = 0;
        public int x = 0;
        public int y = 0;
        public int currentHp = 20;
        public int maxHp = 20;
        public int ration = 40;
        public String status = game.core.unit.UnitStatus.NONE;
        public boolean hasActed = false;
        public boolean hasMoved = false;
        public boolean isDead = false;
        public int equippedSlot;
        public List<String> inventoryNames = new ArrayList<>();
        public List<Integer> inventoryUses = new ArrayList<>();
        public List<UnitData> loadedUnits = new ArrayList<>();
    }

    public String mapPath;
    public int currentDay = 1;
    public int currentPlayerIdx = 0;
    public List<PlayerData> players = new ArrayList<>();
    public List<UnitData> units = new ArrayList<>();

    private static String enc(String s) {
        if (s == null) return "";
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String dec(String s) {
        if (s == null) return "";
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    public void writeTo(File file) throws IOException {
        Properties p = new Properties();
        p.setProperty("mapPath", enc(mapPath));
        p.setProperty("currentDay", String.valueOf(currentDay));
        p.setProperty("currentPlayerIdx", String.valueOf(currentPlayerIdx));

        p.setProperty("players.count", String.valueOf(players.size()));
        for (int i = 0; i < players.size(); i++) {
            PlayerData pd = players.get(i);
            p.setProperty("players." + i + ".index", String.valueOf(pd.index));
            p.setProperty("players." + i + ".isAI", String.valueOf(pd.isAI));
            p.setProperty("players." + i + ".gold", String.valueOf(pd.gold));
            p.setProperty("players." + i + ".colorRgb", String.valueOf(pd.color != null ? pd.color.getRGB() : Color.WHITE.getRGB()));
        }

        p.setProperty("units.count", String.valueOf(units.size()));
        for (int i = 0; i < units.size(); i++) {
            UnitData u = units.get(i);
            p.setProperty("units." + i + ".category", enc(u.category));
            p.setProperty("units." + i + ".unitName", enc(u.unitName));
            p.setProperty("units." + i + ".ownerIndex", String.valueOf(u.ownerIndex));
            p.setProperty("units." + i + ".x", String.valueOf(u.x));
            p.setProperty("units." + i + ".y", String.valueOf(u.y));
            p.setProperty("units." + i + ".currentHp", String.valueOf(u.currentHp));
            p.setProperty("units." + i + ".maxHp", String.valueOf(u.maxHp));
            p.setProperty("units." + i + ".ration", String.valueOf(u.ration));
            p.setProperty("units." + i + ".status", enc(u.status));
            p.setProperty("units." + i + ".hasActed", String.valueOf(u.hasActed));
            p.setProperty("units." + i + ".hasMoved", String.valueOf(u.hasMoved));
            p.setProperty("units." + i + ".isDead", String.valueOf(u.isDead));
            p.setProperty("units." + i + ".equippedSlot", String.valueOf(u.equippedSlot));
            
            p.setProperty("units." + i + ".inventory.count", String.valueOf(u.inventoryNames.size()));
            for (int k = 0; k < u.inventoryNames.size(); k++) {
                p.setProperty("units." + i + ".inventory." + k + ".name", enc(u.inventoryNames.get(k)));
                p.setProperty("units." + i + ".inventory." + k + ".uses", String.valueOf(u.inventoryUses.get(k)));
            }
            
            p.setProperty("units." + i + ".loaded.count", String.valueOf(u.loadedUnits.size()));
            for (int j = 0; j < u.loadedUnits.size(); j++) {
                UnitData lu = u.loadedUnits.get(j);
                p.setProperty("units." + i + ".loaded." + j + ".category", enc(lu.category));
                p.setProperty("units." + i + ".loaded." + j + ".unitName", enc(lu.unitName));
                p.setProperty("units." + i + ".loaded." + j + ".ownerIndex", String.valueOf(lu.ownerIndex));
                p.setProperty("units." + i + ".loaded." + j + ".currentHp", String.valueOf(lu.currentHp));
                p.setProperty("units." + i + ".loaded." + j + ".maxHp", String.valueOf(lu.maxHp));
                p.setProperty("units." + i + ".loaded." + j + ".ration", String.valueOf(lu.ration));
                p.setProperty("units." + i + ".loaded." + j + ".status", enc(lu.status));
                p.setProperty("units." + i + ".loaded." + j + ".equippedSlot", String.valueOf(lu.equippedSlot));
                p.setProperty("units." + i + ".loaded." + j + ".inventory.count", String.valueOf(lu.inventoryNames.size()));
                for (int k = 0; k < lu.inventoryNames.size(); k++) {
                    p.setProperty("units." + i + ".loaded." + j + ".inventory." + k + ".name", enc(lu.inventoryNames.get(k)));
                    p.setProperty("units." + i + ".loaded." + j + ".inventory." + k + ".uses", String.valueOf(lu.inventoryUses.get(k)));
                }
            }
        }

        file.getParentFile().mkdirs();
        try (OutputStream os = new FileOutputStream(file)) {
            p.store(os, "Versus Save");
        }
    }

    public static VersusSaveData readFrom(File file) throws IOException {
        Properties p = new Properties();
        try (InputStream is = new FileInputStream(file)) {
            p.load(is);
        }

        VersusSaveData d = new VersusSaveData();
        d.mapPath = dec(p.getProperty("mapPath", ""));
        d.currentDay = parseInt(p.getProperty("currentDay", "1"), 1);
        d.currentPlayerIdx = parseInt(p.getProperty("currentPlayerIdx", "0"), 0);

        int pc = parseInt(p.getProperty("players.count", "0"), 0);
        for (int i = 0; i < pc; i++) {
            PlayerData pd = new PlayerData();
            pd.index = parseInt(p.getProperty("players." + i + ".index", String.valueOf(i)), i);
            pd.isAI = Boolean.parseBoolean(p.getProperty("players." + i + ".isAI", "false"));
            pd.gold = parseInt(p.getProperty("players." + i + ".gold", "1000"), 1000);
            int rgb = parseInt(p.getProperty("players." + i + ".colorRgb", String.valueOf(Color.WHITE.getRGB())), Color.WHITE.getRGB());
            pd.color = new Color(rgb, true);
            d.players.add(pd);
        }

        int uc = parseInt(p.getProperty("units.count", "0"), 0);
        for (int i = 0; i < uc; i++) {
            UnitData u = new UnitData();
            u.category = dec(p.getProperty("units." + i + ".category", ""));
            u.unitName = dec(p.getProperty("units." + i + ".unitName", ""));
            u.ownerIndex = parseInt(p.getProperty("units." + i + ".ownerIndex", "0"), 0);
            u.x = parseInt(p.getProperty("units." + i + ".x", "0"), 0);
            u.y = parseInt(p.getProperty("units." + i + ".y", "0"), 0);
            u.currentHp = parseInt(p.getProperty("units." + i + ".currentHp", "20"), 20);
            u.maxHp = parseInt(p.getProperty("units." + i + ".maxHp", "20"), 20);
            u.ration = parseInt(p.getProperty("units." + i + ".ration", "40"), 40);
            u.status = dec(p.getProperty("units." + i + ".status", game.core.unit.UnitStatus.NONE));
            u.hasActed = Boolean.parseBoolean(p.getProperty("units." + i + ".hasActed", "false"));
            u.hasMoved = Boolean.parseBoolean(p.getProperty("units." + i + ".hasMoved", "false"));
            u.isDead = Boolean.parseBoolean(p.getProperty("units." + i + ".isDead", "false"));
            u.equippedSlot = parseInt(p.getProperty("units." + i + ".equippedSlot", "0"), 0);
            
            int ic = parseInt(p.getProperty("units." + i + ".inventory.count", "0"), 0);
            for (int k = 0; k < ic; k++) {
                u.inventoryNames.add(dec(p.getProperty("units." + i + ".inventory." + k + ".name", "")));
                u.inventoryUses.add(parseInt(p.getProperty("units." + i + ".inventory." + k + ".uses", "0"), 0));
            }
            
            int lc = parseInt(p.getProperty("units." + i + ".loaded.count", "0"), 0);
            for (int j = 0; j < lc; j++) {
                UnitData lu = new UnitData();
                lu.category = dec(p.getProperty("units." + i + ".loaded." + j + ".category", ""));
                lu.unitName = dec(p.getProperty("units." + i + ".loaded." + j + ".unitName", ""));
                lu.ownerIndex = parseInt(p.getProperty("units." + i + ".loaded." + j + ".ownerIndex", "0"), 0);
                lu.currentHp = parseInt(p.getProperty("units." + i + ".loaded." + j + ".currentHp", "20"), 20);
                lu.maxHp = parseInt(p.getProperty("units." + i + ".loaded." + j + ".maxHp", "20"), 20);
                lu.ration = parseInt(p.getProperty("units." + i + ".loaded." + j + ".ration", "40"), 40);
                lu.status = dec(p.getProperty("units." + i + ".loaded." + j + ".status", game.core.unit.UnitStatus.NONE));
                lu.equippedSlot = parseInt(p.getProperty("units." + i + ".loaded." + j + ".equippedSlot", "0"), 0);
                int lInvCount = parseInt(p.getProperty("units." + i + ".loaded." + j + ".inventory.count", "0"), 0);
                for (int k = 0; k < lInvCount; k++) {
                    lu.inventoryNames.add(dec(p.getProperty("units." + i + ".loaded." + j + ".inventory." + k + ".name", "")));
                    lu.inventoryUses.add(parseInt(p.getProperty("units." + i + ".loaded." + j + ".inventory." + k + ".uses", "0"), 0));
                }
                u.loadedUnits.add(lu);
            }
            
            d.units.add(u);
        }

        return d;
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}

