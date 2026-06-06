package game.core.map;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import game.core.util.AssetManager;
import game.core.util.JsonUtil;

/**
 * Manages a GBA-style tileset, providing 8x8 tile extraction and terrain metadata.
 */
public class Tileset {
    private String name;
    private BufferedImage image;
    private int tileWidth = 8;
    private int tileHeight = 8;
    
    // Terrain metadata mapping tile index to properties
    private Map<Integer, TerrainProperty> terrainMap = new HashMap<>();
    
    // Cache of individual tile images — independent copies for GPU acceleration
    private Map<Integer, BufferedImage> tileCache = new HashMap<>();

    public Tileset(String name, File file) {
        this(name, file, 16, 16);
    }

    public Tileset(String name, File file, int tw, int th) {
        this.name = name;
        this.image = AssetManager.getImage(file.getPath());
        this.tileWidth = tw;
        this.tileHeight = th;
        loadMetadata(file.getPath().replace(".png", ".json"));
    }

    public int getTileWidth() { return tileWidth; }
    public int getTileHeight() { return tileHeight; }

    public BufferedImage getTile(int id) {
        BufferedImage cached = tileCache.get(id);
        if (cached != null) return cached;
        
        int cols = image.getWidth() / tileWidth;
        int x = (id % cols) * tileWidth;
        int y = (id / cols) * tileHeight;
        
        if (x + tileWidth > image.getWidth() || y + tileHeight > image.getHeight()) {
            return null;
        }
        // Create an independent copy instead of getSubimage() view —
        // subimage views share the parent DataBuffer and prevent GPU acceleration.
        BufferedImage tile = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D tg = tile.createGraphics();
        tg.drawImage(image, 0, 0, tileWidth, tileHeight, x, y, x + tileWidth, y + tileHeight, null);
        tg.dispose();
        tileCache.put(id, tile);
        return tile;
    }

    public int getMaxTileId() {
        return (image.getWidth() / tileWidth) * (image.getHeight() / tileHeight);
    }

    public String getName() {
        return name;
    }

    public BufferedImage getFullImage() {
        return image;
    }

    public static class TerrainProperty {
        public String type; // e.g., "PLAIN", "FOREST"
        public Map<String, Integer> moveCosts = new HashMap<>(); // Key: UnitType, Value: Cost (-1 for blocked)
        public int defBonus;
        public int avoBonus;

        public TerrainProperty(String type, int defBonus, int avoBonus) {
            this.type = type;
            this.defBonus = defBonus;
            this.avoBonus = avoBonus;
            // Default costs
            moveCosts.put("Land Unit", 1);
            moveCosts.put("Ocean Unit", -1);
            moveCosts.put("Air Unit", 1);
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"").append(type).append("\",");
            sb.append("\"def\":").append(defBonus).append(",");
            sb.append("\"avo\":").append(avoBonus).append(",");
            sb.append("\"costs\":{");
            int count = 0;
            for (Map.Entry<String, Integer> entry : moveCosts.entrySet()) {
                sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
                if (++count < moveCosts.size()) sb.append(",");
            }
            sb.append("}}");
            return sb.toString();
        }
    }
    
    public void setTerrain(int tileId, TerrainProperty prop) {
        terrainMap.put(tileId, prop);
    }
    
    /**
     * Returns the terrain property for the given tile ID, or null if no metadata is defined.
     */
    public TerrainProperty getTerrain(int tileId) {
        return terrainMap.get(tileId);
    }

    /**
     * Returns true if this tileset has explicit terrain metadata for the given tile ID.
     */
    public boolean hasTerrain(int tileId) {
        return terrainMap.containsKey(tileId);
    }

    /**
     * Returns the terrain property for the given tile ID, or a default PLAIN property if none is defined.
     */
    public TerrainProperty getTerrainOrDefault(int tileId) {
        return terrainMap.getOrDefault(tileId, new TerrainProperty("PLAIN", 0, 0));
    }

    public void saveMetadata(String path) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"tiles\": {\n");
            int count = 0;
            for (Map.Entry<Integer, TerrainProperty> entry : terrainMap.entrySet()) {
                sb.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue().toJson());
                if (++count < terrainMap.size()) sb.append(",");
                sb.append("\n");
            }
            sb.append("  }\n}");
            java.nio.file.Files.write(java.nio.file.Paths.get(path), sb.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMetadata(String path) {
        File f = new File(path);
        if (!f.exists()) return;
        try {
            String content = new String(java.nio.file.Files.readAllBytes(f.toPath()));
            // Very basic manual parsing for simplicity
            int tilesStart = content.indexOf("\"tiles\":");
            if (tilesStart == -1) return;
            String tilesPart = content.substring(content.indexOf("{", tilesStart) + 1, content.lastIndexOf("}"));
            String[] entries = tilesPart.split("(?<=\\}),\\s*");
            for (String entry : entries) {
                if (!entry.contains(":")) continue;
                String idStr = entry.substring(0, entry.indexOf(":")).replaceAll("[\"\\s]", "");
                int id = Integer.parseInt(idStr);
                String propJson = entry.substring(entry.indexOf(":") + 1).trim();
                
                String type = JsonUtil.extractJsonVal(propJson, "type");
                int def = JsonUtil.parseInt(JsonUtil.extractJsonVal(propJson, "def"), 0);
                int avo = JsonUtil.parseInt(JsonUtil.extractJsonVal(propJson, "avo"), 0);
                TerrainProperty prop = new TerrainProperty(type, def, avo);
                
                int costsStart = propJson.indexOf("\"costs\":");
                if (costsStart != -1) {
                    String costsPart = propJson.substring(propJson.indexOf("{", costsStart) + 1, propJson.indexOf("}", costsStart));
                    String[] costPairs = costsPart.split(",");
                    for (String cp : costPairs) {
                        String[] kv = cp.split(":");
                        if (kv.length == 2) {
                            String uType = kv[0].replace("\"", "").trim();
                            int cost = Integer.parseInt(kv[1].trim());
                            prop.moveCosts.put(uType, cost);
                        }
                    }
                }
                terrainMap.put(id, prop);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
