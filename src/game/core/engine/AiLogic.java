package game.core.engine;

import game.ui.screens.VersusGameplayScreen;
import game.ui.screens.VersusScreen;
import game.core.unit.MapUnit;
import game.core.unit.WeaponItem;
import java.awt.Point;
import java.util.*;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

public class AiLogic {
    int aiState = 0;
    int aiTimer = 0;
    MapUnit currentAiUnit = null;
    List<MapUnit> aiUnactedUnits = new ArrayList<>();
    
    private VersusGameplayScreen screen;
    
    public AiLogic(VersusGameplayScreen screen) {
        this.screen = screen;
    }

    public void updateAI() {
        if (aiTimer > 0) { aiTimer--; return; }

        if (aiState == 0) { // Init Turn
            VersusScreen.PlayerSettings p = screen.players.get(screen.currentPlayerIdx);
            
            for (Map.Entry<Point, VersusGameplayScreen.EventInfo> entry : screen.eventMap.entrySet()) {
                VersusGameplayScreen.EventInfo ev = entry.getValue();
                if (ev.owner == screen.currentPlayerIdx && ("BASE".equalsIgnoreCase(ev.type) || "ARMORY".equalsIgnoreCase(ev.type) || "FORT".equalsIgnoreCase(ev.type) || "AERIE".equalsIgnoreCase(ev.type))) {
                    boolean occupied = false;
                    for (MapUnit u : screen.units) {
                        if (!u.isDead && u.position.equals(entry.getKey())) {
                            occupied = true; break;
                        }
                    }
                    if (!occupied) {
                        String cat = "Unit";
                        List<String> aiUnitChoices = new ArrayList<>();
                        
                        // Dynamically scan available units just like the deploy menu does
                        File battleDirFile = new File(game.core.util.GamePaths.BATTLE, cat);
                        File unitsDirFile = new File(game.core.util.GamePaths.UNITS, cat);
                        if (battleDirFile.exists() && battleDirFile.isDirectory() && unitsDirFile.exists() && unitsDirFile.isDirectory()) {
                            File[] battleSubs = battleDirFile.listFiles(File::isDirectory);
                            if (battleSubs != null) {
                                for (File bs : battleSubs) {
                                    String name = bs.getName();
                                    File us = new File(unitsDirFile, name);
                                    if (us.exists() && us.isDirectory()) {
                                        game.core.unit.UnitStats stats = game.core.unit.UnitRegistry.get(name);
                                        if ("ARMORY".equalsIgnoreCase(ev.type) && !"Land Unit".equalsIgnoreCase(stats.unitType)) continue;
                                        if ("AERIE".equalsIgnoreCase(ev.type) && !"Air Unit".equalsIgnoreCase(stats.unitType)) continue;
                                        if ("FORT".equalsIgnoreCase(ev.type) && !"Ocean Unit".equalsIgnoreCase(stats.unitType)) continue;
                                        if ("BASE".equalsIgnoreCase(ev.type) && !"Land Unit".equalsIgnoreCase(stats.unitType)) continue;
                                        
                                        aiUnitChoices.add(name);
                                    }
                                }
                            }
                        }
                        
                        // Fallback in case directory scanning is empty
                        if (aiUnitChoices.isEmpty()) {
                            if ("AERIE".equalsIgnoreCase(ev.type)) {
                                aiUnitChoices.add("Pegasus Knight");
                            } else if ("FORT".equalsIgnoreCase(ev.type)) {
                                aiUnitChoices.add("Fleet");
                            } else {
                                aiUnitChoices.addAll(Arrays.asList("Soldier", "Assassin", "Cavalier", "Knight", "Sentinel"));
                            }
                        }
                        
                        java.util.Collections.shuffle(aiUnitChoices);
                        for (String uName : aiUnitChoices) {
                            int cost = game.core.engine.DeploymentEngine.calculatePrice(cat, uName);
                            if (p.gold >= cost) {
                                screen.deployUnit(cat, uName, cost, ev);
                                break;
                            }
                        }
                    }
                }
            }

            aiUnactedUnits.clear();
            for (MapUnit u : screen.units) {
                if (!u.isDead && u.ownerIndex == screen.currentPlayerIdx && !u.hasActed) {
                    aiUnactedUnits.add(u);
                }
            }
            if (aiUnactedUnits.isEmpty()) {
                screen.nextTurn();
                return;
            }
            currentAiUnit = aiUnactedUnits.get(0);
            aiState = 1;
            aiTimer = 30; // Wait before acting
        }
        else if (aiState == 1) { // Camera focus
            int ry = (int) Math.round(currentAiUnit.renderPos.y);
            int rx = (int) Math.round(currentAiUnit.renderPos.x);
            boolean visible = false;
            if (screen.fogOfWarEnabled && screen.visibleTiles != null && ry >= 0 && ry < screen.mapH && rx >= 0 && rx < screen.mapW) {
                visible = screen.visibleTiles[ry][rx] || screen.isPlayerVisionActive(currentAiUnit.ownerIndex);
            } else if (!screen.fogOfWarEnabled) {
                visible = true;
            } else if (screen.isPlayerVisionActive(currentAiUnit.ownerIndex)) {
                visible = true;
            }
            if (visible) {
                screen.cameraTargetX = currentAiUnit.renderPos.x * screen.TILE_SIZE * screen.zoomScale;
                screen.cameraTargetY = currentAiUnit.renderPos.y * screen.TILE_SIZE * screen.zoomScale;
            }
            aiState = 2;
            aiTimer = 15;
        }
        else if (aiState == 2) { // Decide Move & Attack
            screen.calculateMoveRange(currentAiUnit);
            // minR and maxR calculations removed, as AI now checks all inventory weapons during move scoring
            
            boolean unitCanCapture = screen.canCapture(currentAiUnit);
            boolean isFleet = "Fleet".equalsIgnoreCase(currentAiUnit.unitName);
            boolean isAir = "Air Unit".equalsIgnoreCase(currentAiUnit.stats.unitType);
            boolean hasLoadedUnits = isFleet && currentAiUnit.loadedUnits != null && !currentAiUnit.loadedUnits.isEmpty();
            
            // Build Distance Field from enemies using Land BFS to find reachable land tiles
            Map<Point, Integer> landDistField = new HashMap<>();
            Queue<Point> queue = new LinkedList<>();
            for (MapUnit e : screen.units) {
                if (e.ownerIndex != screen.currentPlayerIdx && !e.isDead) {
                    landDistField.put(e.position, 0);
                    queue.add(e.position);
                }
            }
            int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
            while (!queue.isEmpty()) {
                Point curr = queue.poll();
                int currDist = landDistField.get(curr);
                for (int[] d : dirs) {
                    Point next = new Point(curr.x + d[0], curr.y + d[1]);
                    if (next.x >= 0 && next.x < screen.mapW && next.y >= 0 && next.y < screen.mapH) {
                        int cost = game.core.engine.MovementEngine.getTerrainCost(next.x, next.y, "Land Unit", screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                        if (cost != -1 && !landDistField.containsKey(next)) {
                            landDistField.put(next, currDist + 1);
                            queue.add(next);
                        }
                    }
                }
            }
            
            // Build Unit-specific Distance Field from enemies to find paths for this specific unit type
            Map<Point, Integer> unitDistField = new HashMap<>();
            Queue<Point> uQueue = new LinkedList<>();
            for (MapUnit e : screen.units) {
                if (e.ownerIndex != screen.currentPlayerIdx && !e.isDead) {
                    unitDistField.put(e.position, 0);
                    uQueue.add(e.position);
                }
            }
            while (!uQueue.isEmpty()) {
                Point curr = uQueue.poll();
                int currDist = unitDistField.get(curr);
                for (int[] d : dirs) {
                    Point next = new Point(curr.x + d[0], curr.y + d[1]);
                    if (next.x >= 0 && next.x < screen.mapW && next.y >= 0 && next.y < screen.mapH) {
                        int cost = game.core.engine.MovementEngine.getTerrainCost(next.x, next.y, currentAiUnit.stats.unitType, screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                        if (cost != -1 && !unitDistField.containsKey(next)) {
                            unitDistField.put(next, currDist + 1);
                            uQueue.add(next);
                        }
                    }
                }
            }
            
            // Build HQ and Event Distance Fields for capturing units
            Map<Point, Integer> hqDistField = new HashMap<>();
            Map<Point, Integer> eventDistField = new HashMap<>();
            if (unitCanCapture) {
                Queue<Point> hQueue = new LinkedList<>();
                Queue<Point> eQueue = new LinkedList<>();
                for (Map.Entry<Point, VersusGameplayScreen.EventInfo> entry : screen.eventMap.entrySet()) {
                    VersusGameplayScreen.EventInfo ev = entry.getValue();
                    if (ev.owner != screen.currentPlayerIdx) {
                        eventDistField.put(entry.getKey(), 0);
                        eQueue.add(entry.getKey());
                        if ("HQ".equals(ev.type)) {
                            hqDistField.put(entry.getKey(), 0);
                            hQueue.add(entry.getKey());
                        }
                    }
                }
                while (!eQueue.isEmpty()) {
                    Point curr = eQueue.poll();
                    int currDist = eventDistField.get(curr);
                    for (int[] d : dirs) {
                        Point next = new Point(curr.x + d[0], curr.y + d[1]);
                        if (next.x >= 0 && next.x < screen.mapW && next.y >= 0 && next.y < screen.mapH) {
                            int cost = game.core.engine.MovementEngine.getTerrainCost(next.x, next.y, currentAiUnit.stats.unitType, screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                            if (cost != -1 && !eventDistField.containsKey(next)) {
                                eventDistField.put(next, currDist + 1);
                                eQueue.add(next);
                            }
                        }
                    }
                }
                while (!hQueue.isEmpty()) {
                    Point curr = hQueue.poll();
                    int currDist = hqDistField.get(curr);
                    for (int[] d : dirs) {
                        Point next = new Point(curr.x + d[0], curr.y + d[1]);
                        if (next.x >= 0 && next.x < screen.mapW && next.y >= 0 && next.y < screen.mapH) {
                            int cost = game.core.engine.MovementEngine.getTerrainCost(next.x, next.y, currentAiUnit.stats.unitType, screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                            if (cost != -1 && !hqDistField.containsKey(next)) {
                                hqDistField.put(next, currDist + 1);
                                hQueue.add(next);
                            }
                        }
                    }
                }
            }
            
            boolean isStrictlyStranded = !isFleet && !isAir && !landDistField.containsKey(currentAiUnit.position);
            boolean isFar = !isFleet && !isAir && landDistField.containsKey(currentAiUnit.position) && landDistField.get(currentAiUnit.position) > 8;
            
            boolean isStranded = isStrictlyStranded;
            
            if (!isStranded && isFar) {
                List<MapUnit> availableFleets = new ArrayList<>();
                int totalCapacity = 0;
                for (MapUnit f : screen.units) {
                    if (f.ownerIndex == currentAiUnit.ownerIndex && "Fleet".equalsIgnoreCase(f.unitName) && !f.isDead && (f.loadedUnits == null || f.loadedUnits.size() < 3)) {
                        availableFleets.add(f);
                        totalCapacity += 3 - (f.loadedUnits == null ? 0 : f.loadedUnits.size());
                    }
                }
                
                // Deduct strictly stranded units from capacity since they have absolute priority
                for (MapUnit u : screen.units) {
                    if (u.ownerIndex == currentAiUnit.ownerIndex && !u.isDead && !"Air Unit".equalsIgnoreCase(u.stats.unitType) && !"Fleet".equalsIgnoreCase(u.unitName)) {
                        if (!landDistField.containsKey(u.position)) {
                            totalCapacity--;
                        }
                    }
                }
                
                if (totalCapacity > 0) {
                    List<MapUnit> farCandidates = new ArrayList<>();
                    for (MapUnit u : screen.units) {
                        if (u.ownerIndex == currentAiUnit.ownerIndex && !u.isDead && !"Air Unit".equalsIgnoreCase(u.stats.unitType) && !"Fleet".equalsIgnoreCase(u.unitName)) {
                            if (landDistField.containsKey(u.position) && landDistField.get(u.position) > 8) {
                                farCandidates.add(u);
                            }
                        }
                    }
                    
                    farCandidates.sort((a, b) -> {
                        int distA = 9999;
                        int distB = 9999;
                        for (MapUnit f : availableFleets) {
                            distA = Math.min(distA, Math.abs(a.position.x - f.position.x) + Math.abs(a.position.y - f.position.y));
                            distB = Math.min(distB, Math.abs(b.position.x - f.position.x) + Math.abs(b.position.y - f.position.y));
                        }
                        return Integer.compare(distA, distB);
                    });
                    
                    int myIndex = farCandidates.indexOf(currentAiUnit);
                    if (myIndex != -1 && myIndex < totalCapacity) {
                        isStranded = true;
                    }
                }
            }
            
            // Build Fleet Distance Field if this unit is stranded
            Map<Point, Integer> fleetDistField = new HashMap<>();
            if (isStranded) {
                Queue<Point> fQueue = new LinkedList<>();
                for (MapUnit f : screen.units) {
                    if (f.ownerIndex == currentAiUnit.ownerIndex && "Fleet".equalsIgnoreCase(f.unitName) && !f.isDead && (f.loadedUnits == null || f.loadedUnits.size() < 3)) {
                        for (int[] d : dirs) {
                            Point adj = new Point(f.position.x + d[0], f.position.y + d[1]);
                            if (adj.x >= 0 && adj.x < screen.mapW && adj.y >= 0 && adj.y < screen.mapH) {
                                int cost = game.core.engine.MovementEngine.getTerrainCost(adj.x, adj.y, currentAiUnit.stats.unitType, screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                                if (cost != -1 && !fleetDistField.containsKey(adj)) {
                                    fleetDistField.put(adj, 0);
                                    fQueue.add(adj);
                                }
                            }
                        }
                    }
                }
                while (!fQueue.isEmpty()) {
                    Point curr = fQueue.poll();
                    int currDist = fleetDistField.get(curr);
                    for (int[] d : dirs) {
                        Point next = new Point(curr.x + d[0], curr.y + d[1]);
                        if (next.x >= 0 && next.x < screen.mapW && next.y >= 0 && next.y < screen.mapH) {
                            int cost = game.core.engine.MovementEngine.getTerrainCost(next.x, next.y, currentAiUnit.stats.unitType, screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                            if (cost != -1 && !fleetDistField.containsKey(next)) {
                                fleetDistField.put(next, currDist + 1);
                                fQueue.add(next);
                            }
                        }
                    }
                }
            }

            // Build Stranded Distance Field if this is an empty fleet
            Map<Point, Integer> strandedDistField = new HashMap<>();
            if (isFleet && !hasLoadedUnits) {
                Queue<Point> sQueue = new LinkedList<>();
                for (MapUnit u : screen.units) {
                    if (u.ownerIndex == screen.currentPlayerIdx && !u.isDead && !"Air Unit".equalsIgnoreCase(u.stats.unitType) && !"Fleet".equalsIgnoreCase(u.unitName)) {
                        int uDist = landDistField.getOrDefault(u.position, 9999);
                        if (uDist > 8) {
                            for (int[] d : dirs) {
                                Point adj = new Point(u.position.x + d[0], u.position.y + d[1]);
                                if (adj.x >= 0 && adj.x < screen.mapW && adj.y >= 0 && adj.y < screen.mapH) {
                                    int cost = game.core.engine.MovementEngine.getTerrainCost(adj.x, adj.y, currentAiUnit.stats.unitType, screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                                    if (cost != -1 && !strandedDistField.containsKey(adj)) {
                                        strandedDistField.put(adj, 0);
                                        sQueue.add(adj);
                                    }
                                }
                            }
                        }
                    }
                }
                while (!sQueue.isEmpty()) {
                    Point curr = sQueue.poll();
                    int currDist = strandedDistField.get(curr);
                    for (int[] d : dirs) {
                        Point next = new Point(curr.x + d[0], curr.y + d[1]);
                        if (next.x >= 0 && next.x < screen.mapW && next.y >= 0 && next.y < screen.mapH) {
                            int cost = game.core.engine.MovementEngine.getTerrainCost(next.x, next.y, currentAiUnit.stats.unitType, screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                            if (cost != -1 && !strandedDistField.containsKey(next)) {
                                strandedDistField.put(next, currDist + 1);
                                sQueue.add(next);
                            }
                        }
                    }
                }
            }

            // Build Coast Distance Field if this is a loaded fleet
            Map<Point, Integer> coastDistField = new HashMap<>();
            if (isFleet && hasLoadedUnits) {
                Queue<Point> cQueue = new LinkedList<>();
                for (Point landPoint : landDistField.keySet()) {
                    for (int[] d : dirs) {
                        Point adj = new Point(landPoint.x + d[0], landPoint.y + d[1]);
                        if (adj.x >= 0 && adj.x < screen.mapW && adj.y >= 0 && adj.y < screen.mapH) {
                            int cost = game.core.engine.MovementEngine.getTerrainCost(adj.x, adj.y, currentAiUnit.stats.unitType, screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                            if (cost != -1 && !coastDistField.containsKey(adj)) {
                                coastDistField.put(adj, 0);
                                cQueue.add(adj);
                            }
                        }
                    }
                }
                while (!cQueue.isEmpty()) {
                    Point curr = cQueue.poll();
                    int currDist = coastDistField.get(curr);
                    for (int[] d : dirs) {
                        Point next = new Point(curr.x + d[0], curr.y + d[1]);
                        if (next.x >= 0 && next.x < screen.mapW && next.y >= 0 && next.y < screen.mapH) {
                            int cost = game.core.engine.MovementEngine.getTerrainCost(next.x, next.y, currentAiUnit.stats.unitType, screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                            if (cost != -1 && !coastDistField.containsKey(next)) {
                                coastDistField.put(next, currDist + 1);
                                cQueue.add(next);
                            }
                        }
                    }
                }
            }
            
            // Find target enemy
            double bestScore = -99999;
            Point bestMovePos = currentAiUnit.position;
            
            for (Point p : screen.moveRange) {
                // If there's another unit here, we can't end our turn here (unless it's us)
                boolean occupied = false;
                for (MapUnit u : screen.units) {
                    if (u != currentAiUnit && !u.isDead && u.position.equals(p)) {
                        occupied = true; break;
                    }
                }
                if (occupied) continue;
                
                double score = 0;
                
                if (isStranded) {
                    // Stranded unit goal: find closest friendly Fleet
                    int closestFleetDist = fleetDistField.getOrDefault(p, 9999);
                    if (closestFleetDist != 9999) {
                        score = -closestFleetDist * 5;
                        if (closestFleetDist == 0) score += 2000; // Bonus for being adjacent to fleet
                    } else {
                        // Fallback: no fleet available, just walk
                        int trueDist = unitDistField.getOrDefault(p, 9999);
                        score = -trueDist * 5;
                    }
                } else if (isFleet && !hasLoadedUnits) {
                    // Empty Fleet goal: find closest stranded land unit
                    int closestStrandedDist = strandedDistField.getOrDefault(p, 9999);
                    score = -closestStrandedDist * 5;
                } else if (isFleet && hasLoadedUnits) {
                    // Loaded Fleet goal: find coastline connected to landDistField
                    int closestCoastDist = coastDistField.getOrDefault(p, 9999);
                    score = -closestCoastDist * 5;
                    if (closestCoastDist == 0) score += 2000; // Bonus for being ready to drop
                } else {
                    // Normal unit goal: move towards enemy using unitDistField
                    int trueDist = unitDistField.getOrDefault(p, 9999);
                    score = -trueDist * 5;
                    
                    for (MapUnit e : screen.units) {
                        if (e.ownerIndex != screen.currentPlayerIdx && !e.isDead) {
                            int mDist = Math.abs(e.position.x - p.x) + Math.abs(e.position.y - p.y);
                            // If we can attack from here, huge score!
                            boolean canAttackAtDist = false;
                            for (WeaponItem wi : currentAiUnit.inventory) {
                                if (wi.isWeapon() && !wi.isBroken() && game.core.unit.UnitWeapon.canUseWeapon(currentAiUnit, wi)) {
                                    if (mDist >= wi.minRange && mDist <= wi.maxRange) {
                                        canAttackAtDist = true;
                                        break;
                                    }
                                }
                            }
                            if (canAttackAtDist) {
                                score += 1000 - e.currentHp; // prefer low HP targets
                            }
                        }
                    }
                }
                
                // Capture priority and event navigation for all capturing units (even stranded ones)
                if (unitCanCapture) {
                    int hqDist = hqDistField.getOrDefault(p, 9999);
                    int evDist = eventDistField.getOrDefault(p, 9999);
                    
                    score -= hqDist * 10;
                    score -= evDist * 8;
                    
                    VersusGameplayScreen.EventInfo ev = screen.eventMap.get(p);
                    if (ev != null && ev.owner != screen.currentPlayerIdx) {
                        score += 50000;
                        if ("HQ".equals(ev.type)) score += 100000;
                    }
                }
                
                // Tiebreaker: prefer tiles closer to our current position
                score -= (Math.abs(p.x - currentAiUnit.position.x) + Math.abs(p.y - currentAiUnit.position.y));
                
                if (score > bestScore) {
                    bestScore = score;
                    bestMovePos = p;
                }
            }
            
            // Reconstruct path
            screen.reconstructPath(bestMovePos, currentAiUnit);
            currentAiUnit.hasMoved = true;
            screen.moveRange.clear();
            screen.attackRange.clear();
            aiState = 3;
        }
        else if (aiState == 3) { // Wait for movement to finish
            if (currentAiUnit.movePath.isEmpty()) {
                aiState = 4;
                aiTimer = 15;
            }
        }
        else if (aiState == 4) { // Combat
            boolean actionTaken = false;
            
            // 1. Check Capture
            VersusGameplayScreen.EventInfo ev = screen.eventMap.get(currentAiUnit.position);
            if (ev != null && ev.owner != screen.currentPlayerIdx && screen.canCapture(currentAiUnit)) {
                screen.performCapture(currentAiUnit, ev);
                actionTaken = true;
            }
            
            // 2. Check Fleet Drop
            if (!actionTaken && "Fleet".equalsIgnoreCase(currentAiUnit.unitName) && currentAiUnit.loadedUnits != null && !currentAiUnit.loadedUnits.isEmpty()) {
                // Recalculate landDistField to ensure we drop on a landmass that can reach the enemy
                Map<Point, Integer> landDistField = new HashMap<>();
                Queue<Point> queue = new LinkedList<>();
                for (MapUnit e : screen.units) {
                    if (e.ownerIndex != screen.currentPlayerIdx && !e.isDead) {
                        landDistField.put(e.position, 0);
                        queue.add(e.position);
                    }
                }
                int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
                while (!queue.isEmpty()) {
                    Point curr = queue.poll();
                    int currDist = landDistField.get(curr);
                    for (int[] d : dirs) {
                        Point next = new Point(curr.x + d[0], curr.y + d[1]);
                        if (next.x >= 0 && next.x < screen.mapW && next.y >= 0 && next.y < screen.mapH) {
                            int cost = game.core.engine.MovementEngine.getTerrainCost(next.x, next.y, "Land Unit", screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                            if (cost != -1 && !landDistField.containsKey(next)) {
                                landDistField.put(next, currDist + 1);
                                queue.add(next);
                            }
                        }
                    }
                }
                
                boolean droppedAny = false;
                Iterator<MapUnit> it = currentAiUnit.loadedUnits.iterator();
                while (it.hasNext()) {
                    MapUnit toDrop = it.next();
                    Point bestDrop = null;
                    int bestDropDist = 9999;
                    
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (Math.abs(dx) + Math.abs(dy) == 1) {
                                int nx = currentAiUnit.position.x + dx;
                                int ny = currentAiUnit.position.y + dy;
                                if (nx >= 0 && nx < screen.mapW && ny >= 0 && ny < screen.mapH) {
                                    int cost = game.core.engine.MovementEngine.getTerrainCost(nx, ny, toDrop.stats.unitType, screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                                    if (cost != -1) {
                                        boolean occ = false;
                                        for (MapUnit occU : screen.units) if (!occU.isDead && occU.position.x == nx && occU.position.y == ny) { occ = true; break; }
                                        if (!occ) {
                                            // Pick drop point that has the best path to enemy via landDistField
                                            Point dropP = new Point(nx, ny);
                                            int dist = landDistField.getOrDefault(dropP, 9999);
                                            if (dist < bestDropDist) {
                                                bestDropDist = dist;
                                                bestDrop = dropP;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (bestDrop != null && bestDropDist != 9999) {
                        toDrop.position = bestDrop;
                        toDrop.renderPos = new java.awt.geom.Point2D.Double(bestDrop.x, bestDrop.y);
                        toDrop.hasActed = true;
                        toDrop.hasMoved = true;
                        screen.units.add(toDrop);
                        it.remove();
                        screen.unitOrderDirty = true;
                        screen.fogDirty = true;
                        droppedAny = true;
                    }
                }
                if (droppedAny) {
                    actionTaken = true;
                }
            }
            
            // 3. Check Attack
            if (!actionTaken && !"Fleet".equalsIgnoreCase(currentAiUnit.unitName)) {
                MapUnit target = null;
                int bestHp = 9999;
                int bestWeaponSlot = -1;
                
                for (MapUnit e : screen.units) {
                    if (e.ownerIndex != screen.currentPlayerIdx && !e.isDead) {
                        int dist = Math.abs(e.position.x - currentAiUnit.position.x) + Math.abs(e.position.y - currentAiUnit.position.y);
                        
                        // Find a weapon in inventory that can hit at this distance
                        int matchingSlot = -1;
                        int bestDmg = -1;
                        for (int sIdx = 0; sIdx < currentAiUnit.inventory.size(); sIdx++) {
                            WeaponItem wi = currentAiUnit.inventory.get(sIdx);
                            if (wi.isWeapon() && !wi.isBroken() && game.core.unit.UnitWeapon.canUseWeapon(currentAiUnit, wi)) {
                                if (dist >= wi.minRange && dist <= wi.maxRange) {
                                    if (wi.power > bestDmg) {
                                        bestDmg = wi.power;
                                        matchingSlot = sIdx;
                                    }
                                }
                            }
                        }
                        
                        if (matchingSlot != -1) {
                            if (e.currentHp < bestHp) {
                                bestHp = (int)e.currentHp;
                                target = e;
                                bestWeaponSlot = matchingSlot;
                            }
                        }
                    }
                }
                
                if (target != null && bestWeaponSlot != -1) {
                    currentAiUnit.equippedSlot = bestWeaponSlot;
                    screen.beginBattleTransition(currentAiUnit, target);
                    actionTaken = true;
                }
            }
            
            // 4. Check Fleet Load
            if (!actionTaken && !"Fleet".equalsIgnoreCase(currentAiUnit.unitName) && !"Air Unit".equalsIgnoreCase(currentAiUnit.stats.unitType) && (currentAiUnit.category == null || !currentAiUnit.category.equalsIgnoreCase("Champion"))) {
                for (MapUnit other : screen.units) {
                    if (other.ownerIndex == currentAiUnit.ownerIndex && "Fleet".equalsIgnoreCase(other.unitName) && !other.isDead) {
                        int d = Math.abs(other.position.x - currentAiUnit.position.x) + Math.abs(other.position.y - currentAiUnit.position.y);
                        if (d <= 1 && (other.loadedUnits == null || other.loadedUnits.size() < 3)) {
                            // Check if unit is stranded or far from enemies
                            Map<Point, Integer> landDistField = new HashMap<>();
                            Queue<Point> queue = new LinkedList<>();
                            for (MapUnit e : screen.units) {
                                if (e.ownerIndex != screen.currentPlayerIdx && !e.isDead) {
                                    landDistField.put(e.position, 0);
                                    queue.add(e.position);
                                }
                            }
                            int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
                            while (!queue.isEmpty()) {
                                Point curr = queue.poll();
                                int currDist = landDistField.get(curr);
                                for (int[] dir : dirs) {
                                    Point next = new Point(curr.x + dir[0], curr.y + dir[1]);
                                    if (next.x >= 0 && next.x < screen.mapW && next.y >= 0 && next.y < screen.mapH) {
                                        int cost = game.core.engine.MovementEngine.getTerrainCost(next.x, next.y, "Land Unit", screen.mapData, screen.mapTSData, screen.loadedTilesets, screen.mapW, screen.mapH);
                                        if (cost != -1 && !landDistField.containsKey(next)) {
                                            landDistField.put(next, currDist + 1);
                                            queue.add(next);
                                        }
                                    }
                                }
                            }
                            
                            boolean isStranded = !landDistField.containsKey(currentAiUnit.position);
                            int trueDist = landDistField.getOrDefault(currentAiUnit.position, 9999);
                            
                            if (isStranded || trueDist > 8) {
                                if (other.loadedUnits == null) other.loadedUnits = new ArrayList<>();
                                other.loadedUnits.add(currentAiUnit);
                                screen.units.remove(currentAiUnit);
                                screen.unitOrderDirty = true;
                                screen.fogDirty = true;
                                actionTaken = true;
                                break;
                            }
                        }
                    }
                }
            }
            
            currentAiUnit.hasActed = true;
            aiState = 5;
            aiTimer = 30; // Wait before next unit
        }
        else if (aiState == 5) {
            // Check if battle is still active
            if (!screen.isBattleActive && !screen.isCaptureAnimActive) {
                aiState = 0;
            }
        }
    }
}
