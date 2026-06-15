package game.core.animation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

public class AnimationScript {
    private Map<Integer, List<AnimationCommand>> modes = new HashMap<>();
    private File scriptFile;
    private int currentParsingMode = -1;

    public AnimationScript(File scriptFile) {
        this.scriptFile = scriptFile;
        parse();
    }

    public File getScriptFile() {
        return scriptFile;
    }

    private void parse() {
        if (!scriptFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Strip inline comments
                if (line.contains("#")) {
                    line = line.substring(0, line.indexOf("#")).trim();
                }
                
                // Detect Mode Header: /// - Mode X
                if (line.startsWith("/// - Mode")) {
                    try {
                        String modeStr = line.substring(line.indexOf("Mode") + 4).trim();
                        currentParsingMode = Integer.parseInt(modeStr);
                        modes.putIfAbsent(currentParsingMode, new ArrayList<>());
                    } catch (Exception e) {}
                    continue;
                }

                if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) continue;

                // Typical format: [duration] [flags] [frameName]
                String[] parts = line.split("\\s+");
                AnimationCommand cmd = null;

                if (parts.length >= 3 && isNumeric(parts[0])) {
                    int duration = Integer.parseInt(parts[0]);
                    String frameName = parts[2];
                    String secondary = (parts.length >= 4) ? parts[3] : null;
                    cmd = new AnimationCommand(AnimationCommand.Type.FRAME, duration, frameName, secondary);
                } else if (parts.length == 1 && parts[0].startsWith("C")) {
                    cmd = new AnimationCommand(AnimationCommand.Type.COMMAND, parts[0]);
                }

                if (cmd != null) {
                    if (currentParsingMode == -1) {
                        // Fallback for scripts without modes (Mode 1)
                        currentParsingMode = 1;
                        modes.putIfAbsent(1, new ArrayList<>());
                    }
                    modes.get(currentParsingMode).add(cmd);
                }
            }
            
            // Add END to each mode
            for (List<AnimationCommand> cmdList : modes.values()) {
                cmdList.add(new AnimationCommand(AnimationCommand.Type.END, null));
            }
        } catch (Exception e) {
            System.err.println("Error parsing script: " + scriptFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    private boolean isNumeric(String str) {
        return str != null && str.matches("\\d+");
    }

    public List<AnimationCommand> getCommands() {
        return getCommands(1); // Default to Mode 1
    }

    public List<AnimationCommand> getCommands(int mode) {
        return modes.getOrDefault(mode, new ArrayList<>());
    }

    public List<Integer> getAvailableModes() {
        return new ArrayList<>(modes.keySet());
    }
    
    public String getActionTypeForMode(int mode) {
        List<AnimationCommand> cmds = modes.get(mode);
        if (cmds == null) return "Unknown";
        
        boolean hasNormalHit = false;
        boolean hasCritHit = false;
        boolean hasSpell = false;
        boolean hasDodge = false;
        
        for (AnimationCommand cmd : cmds) {
            if (cmd.getType() == AnimationCommand.Type.COMMAND) {
                String code = cmd.getCommandCode();
                if (code == null) continue;
                // Extended hit detection for FE GBA scripts
                if (code.equals("C1A") || code.equals("C21") || code.equals("C01") || code.equals("C03") || code.equals("C07")) hasNormalHit = true;
                if (code.equals("C0C") || code.equals("C38")) hasCritHit = true;
                if (code.equals("C05")) hasSpell = true;
                if (code.equals("C02")) hasDodge = true;
            }
        }
        
        if (hasDodge) return "Dodge";
        if (hasSpell && hasCritHit) return "Crit Ranged Attack";
        if (hasSpell) return "Basic Ranged Attack";
        if (hasCritHit) return "Crit Melee Attack";
        if (hasNormalHit) return "Basic Melee Attack";
        
        // Fallback to standard FE GBA mode mapping
        switch(mode) {
            case 1: return "Basic Melee Attack";
            case 2: return "Melee Attack (Close)";
            case 3: return "Crit Melee Attack";
            case 4: return "Crit Melee Attack (Close)";
            case 5: return "Basic Ranged Attack";
            case 6: return "Crit Ranged Attack";
            case 7: return "Dodge";
            case 8: return "Dodge (Ranged)";
            case 9: return "Standing";
            case 11: return "Miss";
            case 12: return "Melee Attack (Finisher)";
            default: return "Mode " + mode;
        }
    }

    public static final int MODE_NORMAL_ATTACK = 1;
    public static final int MODE_CRIT_ATTACK = 3;
    public static final int MODE_RANGED_ATTACK = 5;
    public static final int MODE_DODGE = 7;
    public static final int MODE_STANDING = 9;
    public static final int MODE_MISS = 11;

    public boolean isEmpty() {
        return modes.isEmpty();
    }
}
