package game.core.engine;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

public class DeploymentEngine {
    public static int calculatePrice(String category, String name) {
        // Static assignment of unit prices
        switch (name) {
            // Champions
            case "Ephraim": return 1000;
            
            // Land Units
            case "Knight": return 700;
            case "Cavalier": return 600;
            case "Assassin": return 300;
            case "Sentinel": return 1100;
            case "Soldier": return 200;
            case "Archer": return 300;
            case "Swordsmen": return 200;
            case "Supplier": return 500;
            
            // Air Units
            case "Pegasus Knight": return 600;
            
            // Siege Units
            case "Ballista": return 1300;
            
            // Fallback default prices based on category
            default: return "Champion".equalsIgnoreCase(category) ? 1000 : 500;
        }
    }
}
