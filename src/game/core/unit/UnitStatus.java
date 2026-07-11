package game.core.unit;

/**
 * Lists the possible statuses a unit can have.
 */
public class UnitStatus {
    // Default / No status
    public static final String NONE = "--";
    
    // Starvation status: Applied when Ration reaches 0. Causes 5 true damage per turn.
    public static final String HUNGER = "Hunger";
    
    // Additional statuses can be added here for future mechanics
    public static final String POISON = "Poison";
    public static final String PARALYZED = "Paralyzed";
    public static final String SLEEP = "Sleep";
    public static final String SILENCE = "Silence";
    public static final String BERSERK = "Berserk";
}
