package game.core.engine;

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
            
            // Air Units
            case "Pegasus Knight": return 600;
            
            // Siege Units
            case "Ballista": return 1300;
            
            // Fallback default prices based on category
            default: return "Champion".equalsIgnoreCase(category) ? 1000 : 500;
        }
    }
}
