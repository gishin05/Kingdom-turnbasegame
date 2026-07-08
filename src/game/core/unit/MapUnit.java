package game.core.unit;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

/**
 * A runtime unit placed on the tactical map.
 * Wraps UnitStats + live position + inventory + HP.
 * Resets each time Test Mode is opened (not saved to map JSON).
 */
public class MapUnit {

    // ── Identity ──────────────────────────────────────────────
    public UnitStats stats;       // loaded from stats.json (Category/UnitName)
    public String category;       // "Champion" or "Unit"
    public String unitName;       // e.g. "Ephraim", "Knight"
    public String weaponFolder;   // current active weapon folder in anims, e.g. "spear"

    // ── Faction (mirrors FE8's FACTION_* constants) ───────────
    public enum Faction { PLAYER, ENEMY, ALLY }
    public Faction faction = Faction.PLAYER;

    // ── Multiplayer Ownership ─────────────────────────────────
    public int ownerIndex = 0;           // which player owns this (0=P1, 1=P2...)
    public java.awt.Color teamColor = new java.awt.Color(0, 162, 232);  // player's assigned color

    // ── Map State ─────────────────────────────────────────────
    public Point position = new Point(0, 0);  // tile col, row
    public java.awt.geom.Point2D.Double renderPos = new java.awt.geom.Point2D.Double(0, 0);
    public boolean renderMirrorX = false;
    public List<Point> movePath = new ArrayList<>();
    public int animFrame = 0;
    public int animTimer = 0;
    public int   currentHp;                   // live HP; starts at stats.maxHp
    public boolean hasMoved  = false;         // used move this turn
    public boolean hasActed  = false;         // used action this turn (attacked/waited)
    public boolean isDead    = false;

    // ── Inventory & Cargo ─────────────────────────────────────
    /** Up to 5 items — mirrors FE8's UNIT_ITEM_COUNT = 5 */
    public List<WeaponItem> inventory = new ArrayList<>();
    public int equippedSlot = 0;
    
    /** Units loaded inside this unit (e.g. for transports like Fleet) */
    public List<MapUnit> loadedUnits = new ArrayList<>();

    /** Returns the equipped weapon, or null if inventory is empty */
    public WeaponItem getEquipped() {
        if (inventory.isEmpty() || equippedSlot >= inventory.size()) return null;
        return inventory.get(equippedSlot);
    }

    /** Equip the first usable weapon automatically (mirrors FE8's auto-equip) */
    public void autoEquip() {
        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.get(i).isBroken() && inventory.get(i).isWeapon()) {
                equippedSlot = i;
                return;
            }
        }
        equippedSlot = 0;
    }

    /** Add a weapon to inventory (max 5 slots) */
    public boolean addItem(WeaponItem item) {
        if (inventory.size() >= 5) return false;
        inventory.add(item);
        return true;
    }

    // ── Constructors ──────────────────────────────────────────
    public MapUnit() {}

    public MapUnit(String category, String unitName, Faction faction, Point startPos) {
        this.category  = category;
        this.unitName  = unitName;
        this.faction   = faction;
        this.position  = new Point(startPos);
        this.renderPos = new java.awt.geom.Point2D.Double(startPos.x, startPos.y);
        this.stats     = UnitStats.load(category, unitName);
        
        // Dynamic fallback overrides for units if stats.json doesn't exist on disk
        if ("Air Unit".equalsIgnoreCase(category)) {
            if ("Land Unit".equals(this.stats.unitType)) {
                this.stats.unitType = "Air Unit";
                this.stats.move = 7;
            }
            if ("Infantry".equalsIgnoreCase(this.stats.subUnitType) && "Pegasus".equalsIgnoreCase(unitName)) {
                this.stats.subUnitType = "pegasus";
            }
        } else if ("Ocean Unit".equalsIgnoreCase(category)) {
            if ("Land Unit".equals(this.stats.unitType)) {
                this.stats.unitType = "Ocean Unit";
                this.stats.move = 6;
            }
        }
        
        this.currentHp = stats.maxHp;
    }

    // ── Computed Battle Stats ─────────────────────────────────
    /** Attack Speed = Speed - max(0, weapon.weight - Str) */
    public int getAttackSpeed() {
        WeaponItem w = getEquipped();
        if (w == null) return stats.speed;
        int penalty = Math.max(0, w.weight - stats.strength);
        return stats.speed - penalty;
    }

    /** Battle Attack = Str/Mag + weapon.might */
    public int getBattleAtk() {
        WeaponItem w = getEquipped();
        if (w == null) return stats.strength;
        boolean isMagic = w.weaponType.equalsIgnoreCase("Anima")
                       || w.weaponType.equalsIgnoreCase("Light")
                       || w.weaponType.equalsIgnoreCase("Dark");
        return (isMagic ? stats.magic : stats.strength) + w.might;
    }

    /** Hit Rate = Skill*2 + Luck/2 + weapon.hit */
    public int getBattleHit() {
        WeaponItem w = getEquipped();
        int base = stats.skill * 2 + stats.luck / 2;
        return base + (w != null ? w.hit : 0);
    }

    /** Critical = Skill/2 + weapon.crit */
    public int getBattleCrit() {
        WeaponItem w = getEquipped();
        return stats.skill / 2 + (w != null ? w.crit : 0);
    }

    /** Avoid = AttackSpeed*2 + Luck/2 */
    public int getBattleAvoid() { return getAttackSpeed() * 2 + stats.luck / 2; }

    /** Dodge (crit dodge) = Luck */
    public int getBattleDodge() { return stats.luck; }

    // ── Turn Management ───────────────────────────────────────
    public boolean isExhausted() { return hasMoved && hasActed; }
    public void resetTurn()      { hasMoved = false; hasActed = false; }

    /** Apply damage; returns true if unit died */
    public boolean takeDamage(int dmg) {
        currentHp = Math.max(0, currentHp - dmg);
        if (currentHp == 0) isDead = true;
        return isDead;
    }

    public void heal(int amount) {
        currentHp = Math.min(stats.maxHp, currentHp + amount);
    }

    // ── Utility ───────────────────────────────────────────────
    public boolean isPlayerUnit() { return faction == Faction.PLAYER; }
    public boolean isEnemyUnit()  { return faction == Faction.ENEMY;  }

    /** Manhattan distance to another unit */
    public int distanceTo(MapUnit other) {
        return Math.abs(position.x - other.position.x) + Math.abs(position.y - other.position.y);
    }

    /** Can this unit attack 'target' with its equipped weapon? */
    public boolean canAttack(MapUnit other) {
        WeaponItem w = getEquipped();
        if (w == null || !w.isWeapon()) return false;
        return w.coversRange(distanceTo(other));
    }

    /** Can a counter-attack happen (target can reach back)? */
    public boolean canCounter(MapUnit attacker) {
        return canAttack(attacker);
    }

    @Override
    public String toString() {
        return "[" + faction + "] " + unitName + " HP:" + currentHp + "/" + stats.maxHp
             + " @(" + position.x + "," + position.y + ")";
    }
}
