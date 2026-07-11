package game.core.unit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnitWeapon {
    private static final Map<String, List<String>> unitAllowedWeapons = new HashMap<>();

    static {
        unitAllowedWeapons.put("Soldier", Arrays.asList("Lance"));
        unitAllowedWeapons.put("Swordsmen", Arrays.asList("Sword"));
        unitAllowedWeapons.put("Swordmen", Arrays.asList("Sword")); // Support for misspelled JSON name
        unitAllowedWeapons.put("Sentinel", Arrays.asList("Axe", "Sword", "Lance"));
        unitAllowedWeapons.put("Archer", Arrays.asList("Bow"));
        unitAllowedWeapons.put("Cavalier", Arrays.asList("Sword", "Lance"));
        unitAllowedWeapons.put("Knight", Arrays.asList("Lance"));
        unitAllowedWeapons.put("Pegasus Knight", Arrays.asList("Lance"));
        unitAllowedWeapons.put("Assassin", Arrays.asList("Sword"));
        unitAllowedWeapons.put("Ballista", Arrays.asList("Bow"));
        
        // Champions
        unitAllowedWeapons.put("Ephraim", Arrays.asList("Lance"));
    }

    /**
     * Checks if a unit is allowed to use a specific weapon based on their class configuration.
     * Returns false (default deny) if the unit is not explicitly mapped.
     */
    public static boolean canUseWeapon(MapUnit unit, WeaponItem weapon) {
        if (unit == null || weapon == null) return false;
        
        String name = unit.stats != null ? unit.stats.unitName : unit.unitName;
        if (name == null) return false;
        
        List<String> allowed = unitAllowedWeapons.get(name);
        
        if (allowed == null) {
            // Default deny if the class is not registered
            return false;
        }
        
        for (String type : allowed) {
            if (type.equalsIgnoreCase(weapon.weaponType)) {
                return true;
            }
        }
        
        return false;
    }
}
