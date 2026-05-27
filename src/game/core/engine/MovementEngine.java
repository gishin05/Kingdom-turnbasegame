package game.core.engine;

import game.core.map.Tileset;
import game.core.unit.MapUnit;
import game.core.unit.WeaponItem;
import java.awt.Point;
import java.util.*;

public class MovementEngine {

    public static class MovementResult {
        public final Set<Point> moveRange = new HashSet<>();
        public final Set<Point> attackRange = new HashSet<>();
        public final Map<Point, Point> pathParent = new HashMap<>();
    }

    private static class Node {
        Point pos;
        int cost;
        Node(Point p, int c) { this.pos = p; this.cost = c; }
    }

    public static MovementResult calculateMovement(
        MapUnit u, 
        List<MapUnit> units, 
        int[][] mapData, 
        String[][] mapTSData, 
        Map<String, Tileset> loadedTilesets, 
        int mapW, 
        int mapH
    ) {
        MovementResult result = new MovementResult();
        if (u == null) return result;

        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        queue.add(new Node(u.position, 0));
        
        Map<Point, Integer> moveCosts = new HashMap<>();
        moveCosts.put(u.position, 0);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            
            if (current.cost > moveCosts.getOrDefault(current.pos, Integer.MAX_VALUE)) continue;
            if (current.cost > u.stats.move) continue;
            
            result.moveRange.add(current.pos);

            Point[] neighbors = {
                new Point(current.pos.x, current.pos.y - 1),
                new Point(current.pos.x, current.pos.y + 1),
                new Point(current.pos.x - 1, current.pos.y),
                new Point(current.pos.x + 1, current.pos.y)
            };

            for (Point next : neighbors) {
                if (next.x < 0 || next.x >= mapW || next.y < 0 || next.y >= mapH) continue;
                
                // Enemy blockage
                boolean blockedByEnemy = false;
                for (MapUnit other : units) {
                    if (other.position.equals(next) && isEnemy(u, other)) {
                        blockedByEnemy = true;
                        break;
                    }
                }
                if (blockedByEnemy) continue;

                int cost = getTerrainCost(next.x, next.y, u.stats.unitType, mapData, mapTSData, loadedTilesets, mapW, mapH);
                if (cost == -1) continue;

                int newCost = current.cost + cost;
                if (newCost <= u.stats.move && (!moveCosts.containsKey(next) || newCost < moveCosts.get(next))) {
                    moveCosts.put(next, newCost);
                    result.pathParent.put(next, current.pos);
                    queue.add(new Node(next, newCost));
                }
            }
        }

        // Calculate attack ranges based on move range
        calculateAttacks(u, result.moveRange, result.attackRange, mapW, mapH);

        return result;
    }

    private static boolean isEnemy(MapUnit u, MapUnit other) {
        if (u.ownerIndex >= 0 && other.ownerIndex >= 0) {
            return u.ownerIndex != other.ownerIndex;
        }
        return u.faction != other.faction;
    }

    public static int getTerrainCost(
        int x, int y, 
        String unitType, 
        int[][] mapData, 
        String[][] mapTSData, 
        Map<String, Tileset> loadedTilesets, 
        int mapW, int mapH
    ) {
        if (mapData == null || x < 0 || x >= mapW || y < 0 || y >= mapH) return -1;
        int tileId = mapData[y][x];
        String tsName = (mapTSData != null) ? mapTSData[y][x] : null;
        
        Tileset ts = null;
        if (loadedTilesets != null && !loadedTilesets.isEmpty()) {
            if (tsName != null) {
                ts = loadedTilesets.get(tsName);
                if (ts == null) {
                    // Try case-insensitive lookup
                    for (Map.Entry<String, Tileset> entry : loadedTilesets.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(tsName)) {
                            ts = entry.getValue();
                            break;
                        }
                    }
                }
            }
            
            // If still null, fallback to "Plain", "Desert", or "Snow" case-insensitively
            if (ts == null) {
                for (String fallbackKey : new String[]{"Plain", "Desert", "Snow"}) {
                    for (Map.Entry<String, Tileset> entry : loadedTilesets.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(fallbackKey)) {
                            ts = entry.getValue();
                            break;
                        }
                    }
                    if (ts != null) break;
                }
            }
            
            // If still null, just take the first available loaded tileset
            if (ts == null) {
                ts = loadedTilesets.values().iterator().next();
            }
        }

        if (ts == null) return 1;
        
        // Robust unit type resolution (case-insensitive and handles abbreviations/substrings)
        String normalizedUnitType = normalizeUnitType(unitType);
        
        // 1. Try direct lookup of terrain metadata for this tile ID
        Tileset.TerrainProperty prop = ts.getTerrain(tileId);
        
        // 2. No metadata for this tile ID — default to cost 1 (walkable plain)
        if (prop == null) return 1;
        
        // Look up cost for this unit type
        Integer cost = prop.moveCosts.get(normalizedUnitType);
        if (cost == null) {
            // Case-insensitive key search on prop.moveCosts
            for (Map.Entry<String, Integer> entry : prop.moveCosts.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(normalizedUnitType)) {
                    cost = entry.getValue();
                    break;
                }
            }
        }
        
        return (cost == null) ? 1 : cost;
    }

    /**
     * Normalizes a unit type string to one of the standard keys: "Land Unit", "Ocean Unit", "Air Unit".
     */
    private static String normalizeUnitType(String unitType) {
        if (unitType == null) return "Land Unit";
        String utUpper = unitType.toUpperCase();
        if (utUpper.contains("LAND")) {
            return "Land Unit";
        } else if (utUpper.contains("OCEAN") || utUpper.contains("WATER") || utUpper.contains("SEA")) {
            return "Ocean Unit";
        } else if (utUpper.contains("AIR") || utUpper.contains("FLY") || utUpper.contains("SKY")) {
            return "Air Unit";
        } else {
            return unitType;
        }
    }

    private static void calculateAttacks(MapUnit u, Set<Point> moveRange, Set<Point> attackRange, int mapW, int mapH) {
        int maxR = 0;
        int minR = 99;
        boolean hasWeapon = false;
        
        WeaponItem equipped = u.getEquipped();
        if (equipped != null) {
            hasWeapon = true;
            maxR = equipped.maxRange;
            minR = equipped.minRange;
        } else {
            for (WeaponItem wi : u.inventory) {
                if (wi.isWeapon() && !wi.isBroken()) {
                    hasWeapon = true;
                    if (wi.maxRange > maxR) maxR = wi.maxRange;
                    if (wi.minRange < minR) minR = wi.minRange;
                }
            }
        }

        if (hasWeapon) {
            for (Point p : moveRange) {
                for (int dy = -maxR; dy <= maxR; dy++) {
                    for (int dx = -maxR; dx <= maxR; dx++) {
                        int d = Math.abs(dx) + Math.abs(dy);
                        if (d >= minR && d <= maxR) {
                            Point ap = new Point(p.x + dx, p.y + dy);
                            if (ap.x >= 0 && ap.x < mapW && ap.y >= 0 && ap.y < mapH && !moveRange.contains(ap)) {
                                attackRange.add(ap);
                            }
                        }
                    }
                }
            }
        }
    }

    public static List<Point> reconstructPath(Point dest, Point start, Map<Point, Point> pathParent) {
        List<Point> path = new ArrayList<>();
        Point curr = dest;
        while (curr != null && !curr.equals(start)) {
            path.add(0, curr);
            curr = pathParent.get(curr);
        }
        return path;
    }
}
