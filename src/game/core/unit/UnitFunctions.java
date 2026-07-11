package game.core.unit;

import java.util.ArrayList;
import java.util.List;

public class UnitFunctions {

    /**
     * Gets a list of friendly units adjacent to the supplier (distance exactly 1).
     */
    public static List<MapUnit> getSupplyTargets(MapUnit supplier, List<MapUnit> allUnits) {
        List<MapUnit> targets = new ArrayList<>();
        for (MapUnit other : allUnits) {
            if (other != supplier && other.ownerIndex == supplier.ownerIndex && !other.isDead) {
                int dist = Math.abs(other.position.x - supplier.position.x) + Math.abs(other.position.y - supplier.position.y);
                if (dist == 1) {
                    targets.add(other);
                }
            }
        }
        return targets;
    }

    /**
     * Supplies all friendly units adjacent to the supplier.
     * Restores weapon durability and resets ration to 40.
     */
    public static void performSupply(MapUnit supplier, List<MapUnit> allUnits) {
        List<MapUnit> targets = getSupplyTargets(supplier, allUnits);
        for (MapUnit target : targets) {
            // Restore weapon durability
            for (WeaponItem item : target.inventory) {
                item.currentUses = item.maxUses;
            }
            // Restore ration (Do not reset status)
            if (target.stats != null) {
                target.stats.ration = 40;
            }
        }
        if (!targets.isEmpty()) {
            supplier.supplyUses--;
        }
    }

    /**
     * Gets a list of adjacent transport units (e.g. Fleets) that have capacity to load.
     */
    public static List<MapUnit> getAdjacentTransports(MapUnit unit, List<MapUnit> allUnits) {
        List<MapUnit> transports = new ArrayList<>();
        if (!"Fleet".equalsIgnoreCase(unit.unitName) && 
            (unit.stats != null && !"Air Unit".equalsIgnoreCase(unit.stats.unitType)) && 
            unit.category != null && !unit.category.equalsIgnoreCase("Champion")) {
            
            for (MapUnit other : allUnits) {
                if (other.ownerIndex == unit.ownerIndex && "Fleet".equalsIgnoreCase(other.unitName) && !other.isDead) {
                    int dist = Math.abs(other.position.x - unit.position.x) + Math.abs(other.position.y - unit.position.y);
                    if (dist <= 1 && (other.loadedUnits == null || other.loadedUnits.size() < 3)) {
                        transports.add(other);
                    }
                }
            }
        }
        return transports;
    }

    /**
     * Calculates the gold cost for healing/resupplying based on the amount of points.
     * 10 points = 10 gold. Rounding: 15 -> 10, 16 -> 20. Minimum 10 gold.
     */
    public static int calculateResupplyCost(int amount) {
        if (amount <= 0) return 0;
        int tens = amount / 10;
        int remainder = amount % 10;
        if (remainder >= 6) {
            tens += 1;
        }
        int cost = tens * 10;
        return cost == 0 ? 10 : cost;
    }

    /**
     * Gets a list of valid drop points around the transport.
     */
    public static List<java.awt.Point> getValidDropPoints(MapUnit transport, List<MapUnit> allUnits, int[][] mapData, String[][] mapTSData, java.util.Map<String, game.core.map.Tileset> loadedTilesets, int mapW, int mapH) {
        List<java.awt.Point> validDropPoints = new ArrayList<>();
        if ("Fleet".equalsIgnoreCase(transport.unitName) && transport.loadedUnits != null && !transport.loadedUnits.isEmpty()) {
            MapUnit toDrop = transport.loadedUnits.get(0);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) == 1) {
                        int nx = transport.position.x + dx;
                        int ny = transport.position.y + dy;
                        if (nx >= 0 && nx < mapW && ny >= 0 && ny < mapH) {
                            int cost = game.core.engine.MovementEngine.getTerrainCost(nx, ny, toDrop.stats.unitType, mapData, mapTSData, loadedTilesets, mapW, mapH);
                            if (cost != -1) {
                                boolean occ = false;
                                for (MapUnit occU : allUnits) {
                                    if (!occU.isDead && occU.position.x == nx && occU.position.y == ny) {
                                        occ = true;
                                        break;
                                    }
                                }
                                if (!occ) {
                                    validDropPoints.add(new java.awt.Point(nx, ny));
                                }
                            }
                        }
                    }
                }
            }
        }
        return validDropPoints;
    }
}
