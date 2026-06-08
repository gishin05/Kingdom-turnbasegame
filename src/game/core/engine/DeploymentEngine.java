package game.core.engine;

import game.core.unit.UnitRegistry;
import game.core.unit.UnitStats;

public class DeploymentEngine {
    public static int calculatePrice(String category, String name) {
        UnitStats stats = UnitRegistry.get(name);
        if (stats == null) return 500;
        int statsSum = stats.maxHp + stats.strength + stats.magic + stats.skill + 
                       stats.speed + stats.luck + stats.defense + stats.resistance + stats.move;
        int basePrice = "Champion".equalsIgnoreCase(category) ? 500 : 0;
        int statsCost = (statsSum * statsSum) / 6;
        int rawPrice = basePrice + statsCost;
        return ((rawPrice + 24) / 50) * 50;
    }
}
