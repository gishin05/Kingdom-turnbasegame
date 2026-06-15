package game.core.animation;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

public class AnimationCommand {
    public enum Type {
        FRAME,      // Display a frame for N duration
        COMMAND,    // Special command like C01 (Hit), C06 (Return)
        END         // End of animation
    }

    private Type type;
    private int duration;
    private String frameName;
    private String commandCode;
    private String secondaryCommandCode;

    public AnimationCommand(Type type, int duration, String frameName) {
        this.type = type;
        this.duration = duration;
        this.frameName = frameName;
    }

    public AnimationCommand(Type type, int duration, String frameName, String secondaryCommand) {
        this.type = type;
        this.duration = duration;
        this.frameName = frameName;
        this.secondaryCommandCode = secondaryCommand;
    }

    public AnimationCommand(Type type, String commandCode) {
        this.type = type;
        this.commandCode = commandCode;
    }

    public Type getType() { return type; }
    public int getDuration() { return duration; }
    public String getFrameName() { return frameName; }
    public String getCommandCode() { return commandCode; }
    public String getSecondaryCommandCode() { return secondaryCommandCode; }
    
    public int getFrameIndex() {
        if (frameName == null) return 0;
        try {
            // Remove extension if present and parse number
            String name = frameName;
            if (name.contains(".")) name = name.substring(0, name.indexOf("."));
            return Integer.parseInt(name);
        } catch (Exception e) { return 0; }
    }

    @Override
    public String toString() {
        if (type == Type.FRAME) return "Frame: " + frameName + " (" + duration + ")";
        if (type == Type.COMMAND) return "Command: " + commandCode;
        return "END";
    }
}
