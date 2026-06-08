package game.core.battle;

import game.core.unit.MapUnit;
import game.core.unit.WeaponItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Core battle engine.
 * Implements FE8-accurate combat formulas from bmbattle.h / ComputeBattleUnitStats.
 *
 * Formula reference (from FE8 source):
 *   Attack     = Str (or Mag for magic) + weapon.might
 *   Hit Rate   = Skill*2 + Luck/2 + weapon.hit  [+ weapon triangle]
 *   Avoid      = AttackSpeed*2 + Luck/2
 *   Crit Rate  = Skill/2 + weapon.crit
 *   Dodge      = Luck
 *   Eff. Hit   = HitRate - Avoid          (clamped 0-100)
 *   Eff. Crit  = CritRate - Dodge         (clamped 0-100)
 *   Crit dmg   = normal damage * 3
 *   Doubleattack when |AtkSpd_A - AtkSpd_D| >= 4
 */
public class BattleManager {

    private static final Random RNG = new Random();

    // ── Result structures ──────────────────────────────────────

    public static class BattleResult {
        public Combatant attacker;
        public Combatant defender;
        public List<BattleHit> hits = new ArrayList<>();

        public BattleResult(Combatant a, Combatant d) {
            this.attacker = a;
            this.defender = d;
        }

        /** True if the defender was killed */
        public boolean defenderDied() { return defender.hp <= 0; }
        /** True if the attacker was killed by a counter */
        public boolean attackerDied() { return attacker.hp <= 0; }
    }

    public static class Combatant {
        public MapUnit mapUnit;
        public String name;
        public int hp, maxHp;
        public int attack, defense, speed, skill, luck, magic, con;
        public WeaponType weaponType;
        public int weaponWeight, weaponAtk, weaponHit, weaponCrit;
        public int weaponMinRange, weaponMaxRange;
        public String effectiveAgainst = "";

        // Computed
        public int battleAtk, battleHit, battleCrit, battleAvoid, battleDodge, atkSpeed;

        public Combatant(MapUnit mapUnit, String name, int hp, int maxHp) {
            this.mapUnit = mapUnit;
            this.name  = name;
            this.hp    = hp;
            this.maxHp = maxHp;
        }
    }

    public static class BattleHit {
        public boolean isAttacker;
        public int damage;
        public boolean isCrit;
        public boolean isMiss;

        public BattleHit(boolean isAttacker, int damage, boolean isCrit, boolean isMiss) {
            this.isAttacker = isAttacker;
            this.damage     = damage;
            this.isCrit     = isCrit;
            this.isMiss     = isMiss;
        }
    }

    public enum WeaponType { SWORD, LANCE, AXE, BOW, ANIMA, LIGHT, DARK, STAFF, UNARMED }

    // ── Primary API ───────────────────────────────────────────

    /**
     * Generate a full battle between two MapUnits (preferred API).
     * Uses the exact FE8 formulas with weapon data from MapUnit.inventory.
     */
    public BattleResult generateBattle(MapUnit attacker, MapUnit defender) {
        int distance = Math.abs(attacker.position.x - defender.position.x) + Math.abs(attacker.position.y - defender.position.y);
        Combatant a = fromMapUnit(attacker);
        Combatant d = fromMapUnit(defender);
        return generateBattle(a, d, distance);
    }

    public BattleResult generateBattle(Combatant a, Combatant d, int distance) {
        BattleResult result = new BattleResult(a, d);
        computeStats(a, d);
        computeStats(d, a);
        applyWeaponTriangle(a, d);
        simulateExchange(result, distance);
        return result;
    }

    // ── MapUnit → Combatant conversion ───────────────────────

    private Combatant fromMapUnit(MapUnit u) {
        Combatant c = new Combatant(u, u.unitName, u.currentHp, u.stats.maxHp);
        c.attack   = u.stats.strength;
        c.magic    = u.stats.magic;
        c.defense  = u.stats.defense;
        c.speed    = u.stats.speed;
        c.skill    = u.stats.skill;
        c.luck     = u.stats.luck;
        c.con      = u.stats.con;

        WeaponItem w = u.getEquipped();
        if (w != null && !w.isBroken()) {
            c.weaponAtk      = w.might;
            c.weaponHit      = w.hit;
            c.weaponCrit     = w.crit;
            c.weaponWeight   = w.weight;
            c.weaponMinRange = w.minRange;
            c.weaponMaxRange = w.maxRange;
            c.weaponType     = toWpnType(w.weaponType);
            c.effectiveAgainst = w.effectiveAgainst;
        } else {
            c.weaponType = WeaponType.UNARMED;
            c.effectiveAgainst = "";
        }
        return c;
    }

    private WeaponType toWpnType(String t) {
        switch (t.toLowerCase()) {
            case "sword":  return WeaponType.SWORD;
            case "lance":
            case "spear":  return WeaponType.LANCE;
            case "axe":    return WeaponType.AXE;
            case "bow":    return WeaponType.BOW;
            case "anima":  return WeaponType.ANIMA;
            case "light":  return WeaponType.LIGHT;
            case "dark":   return WeaponType.DARK;
            case "staff":  return WeaponType.STAFF;
            default:       return WeaponType.UNARMED;
        }
    }

    // ── Stat computation (FE8 ComputeBattleUnitStats) ─────────

    private void computeStats(Combatant c, Combatant target) {
        // Attack Speed = Speed - max(0, weight - CON)
        c.atkSpeed = c.speed - Math.max(0, c.weaponWeight - c.con);

        int effectiveBonus = 1;
        if (c.effectiveAgainst != null && !c.effectiveAgainst.isEmpty() && target.mapUnit != null) {
            String subType = target.mapUnit.stats.subUnitType;
            if (subType != null) {
                for (String type : c.effectiveAgainst.split(",")) {
                    if (type.trim().equalsIgnoreCase(subType)) {
                        effectiveBonus = 3;
                        break;
                    }
                }
            }
        }

        boolean isMagic = c.weaponType == WeaponType.ANIMA
                       || c.weaponType == WeaponType.LIGHT
                       || c.weaponType == WeaponType.DARK;
        // Attack Power = Strength/Magic + (Weapon Might * Effective Multiplier)
        c.battleAtk  = (isMagic ? c.magic : c.attack) + (c.weaponAtk * effectiveBonus);

        // Hit Rate = Skill*2 + Luck/2 + weapon.hit
        c.battleHit  = c.skill * 2 + c.luck / 2 + c.weaponHit;

        // Avoid = AtkSpd*2 + Luck/2
        c.battleAvoid = c.atkSpeed * 2 + c.luck / 2;

        // Crit = Skill/2 + weapon.crit
        c.battleCrit = c.skill / 2 + c.weaponCrit;

        // Dodge (crit dodge) = Luck
        c.battleDodge = c.luck;
    }

    // ── Weapon Triangle (FE8 applyWeaponTriangle) ─────────────
    // Sword > Axe > Lance > Sword   |  Anima > Light > Dark > Anima

    private void applyWeaponTriangle(Combatant a, Combatant d) {
        int aBonus = getTriangleBonus(a.weaponType, d.weaponType);
        int dBonus = getTriangleBonus(d.weaponType, a.weaponType);
        a.battleHit += aBonus * 15;  a.battleAtk += aBonus;
        d.battleHit += dBonus * 15;  d.battleAtk += dBonus;
    }

    private int getTriangleBonus(WeaponType atk, WeaponType def) {
        if (atk == WeaponType.SWORD  && def == WeaponType.AXE)   return  1;
        if (atk == WeaponType.AXE   && def == WeaponType.LANCE)  return  1;
        if (atk == WeaponType.LANCE  && def == WeaponType.SWORD)  return  1;
        if (atk == WeaponType.SWORD  && def == WeaponType.LANCE)  return -1;
        if (atk == WeaponType.LANCE  && def == WeaponType.AXE)   return -1;
        if (atk == WeaponType.AXE   && def == WeaponType.SWORD)  return -1;
        if (atk == WeaponType.ANIMA  && def == WeaponType.DARK)   return  1;
        if (atk == WeaponType.DARK   && def == WeaponType.LIGHT)  return  1;
        if (atk == WeaponType.LIGHT  && def == WeaponType.ANIMA)  return  1;
        if (atk == WeaponType.ANIMA  && def == WeaponType.LIGHT)  return -1;
        if (atk == WeaponType.LIGHT  && def == WeaponType.DARK)   return -1;
        if (atk == WeaponType.DARK   && def == WeaponType.ANIMA)  return -1;
        return 0;
    }

    // ── Battle exchange (FE8 BattleGenerateRoundHits) ─────────

    private void simulateExchange(BattleResult result, int distance) {
        Combatant a = result.attacker;
        Combatant d = result.defender;

        // Round 1: Attacker
        executeHit(result, a, d, true);
        if (d.hp <= 0) return;

        // Round 2: Defender counter (if armed and has range)
        if (d.weaponType != WeaponType.UNARMED) {
            boolean canCounter = (distance >= d.weaponMinRange && distance <= d.weaponMaxRange);
            if (canCounter) {
                executeHit(result, d, a, false);
                if (a.hp <= 0) return;
            }
        }

        // Round 3: Double-attack (BATTLE_FOLLOWUP_SPEED_THRESHOLD = 4)
        if (a.atkSpeed - d.atkSpeed >= 4) {
            executeHit(result, a, d, true);
        } else if (d.atkSpeed - a.atkSpeed >= 4 && d.hp > 0) {
            boolean canCounter = (distance >= d.weaponMinRange && distance <= d.weaponMaxRange);
            if (canCounter) executeHit(result, d, a, false);
        }
    }

    private void executeHit(BattleResult res, Combatant atk, Combatant def, boolean isAtk) {
        int hitChance  = Math.max(0, Math.min(100, atk.battleHit  - def.battleAvoid));
        int critChance = Math.max(0, Math.min(100, atk.battleCrit - def.battleDodge));

        // FE8 uses 2-RN system for hit, 1-RN for crit
        boolean miss = roll2RN(hitChance);
        boolean crit = !miss && roll1RN(critChance);

        int dmg = Math.max(0, atk.battleAtk - def.defense);
        if (crit) dmg *= 3;
        if (miss) dmg = 0;

        def.hp = Math.max(0, def.hp - dmg);
        res.hits.add(new BattleHit(isAtk, dmg, crit, miss));
    }

    /** 2-RN system: avg of two d100 rolls vs threshold (FE8 uses this to make extreme hit/miss rarer) */
    private boolean roll2RN(int threshold) {
        int r1 = RNG.nextInt(100);
        int r2 = RNG.nextInt(100);
        return (r1 + r2) / 2 >= threshold;
    }

    /** 1-RN system: single d100 roll (used for crits) */
    private boolean roll1RN(int threshold) {
        return RNG.nextInt(100) < threshold;
    }
}
