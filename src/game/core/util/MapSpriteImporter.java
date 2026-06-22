package game.core.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

/**
 * Standalone utility to auto-import GBA FE8 Map Sprites from the decomp ROM graphics
 * and slice them into the TurnBasedGame moving animation directory structure.
 */
public class MapSpriteImporter {

    public static class ImportResult {
        public int classesProcessed = 0;
        public int framesCreated = 0;
        public int errors = 0;
    }

    /**
     * Imports all map sprites from the specified GBA decomp directories into the game's unit assets.
     */
    public static ImportResult importAll(File decompRootDir, File gameDataDir) {
        ImportResult result = new ImportResult();
        
        File waitDir = new File(decompRootDir, "graphics/unit_icon/wait");
        File moveDir = new File(decompRootDir, "graphics/unit_icon/move");
        
        if (!waitDir.isDirectory() || !moveDir.isDirectory()) {
            System.err.println("Error: Decomp graphic directories not found under " + decompRootDir.getPath());
            result.errors++;
            return result;
        }

        File targetUnitsDir = new File(gameDataDir, "units/Unit");
        targetUnitsDir.mkdirs();

        // Scan wait directory for wait sheets
        File[] waitFiles = waitDir.listFiles((dir, name) -> name.startsWith("unit_icon_wait_") && name.endsWith("_sheet.png"));
        if (waitFiles == null) return result;

        for (File waitFile : waitFiles) {
            // Extract class name: unit_icon_wait_[Class]_sheet.png
            String fileName = waitFile.getName();
            String className = fileName.substring("unit_icon_wait_".length(), fileName.length() - "_sheet.png".length());
            
            File moveFile = new File(moveDir, "unit_icon_move_" + className + "_sheet.png");
            if (!moveFile.exists()) {
                System.out.println("Skipping " + className + " because corresponding move sheet was not found.");
                continue;
            }

            try {
                System.out.println("Processing map sprite class: " + className);
                boolean success = importClass(className, waitFile, moveFile, targetUnitsDir);
                if (success) {
                    result.classesProcessed++;
                    result.framesCreated += 15; // 3 standing + 4 * 3 walking
                } else {
                    result.errors++;
                }
            } catch (Exception e) {
                System.err.println("Error processing class " + className + ": " + e.getMessage());
                e.printStackTrace();
                result.errors++;
            }
        }

        return result;
    }

    /**
     * Slices and imports wait and move sheets for a single class.
     */
    public static boolean importClass(String className, File waitFile, File moveFile, File targetUnitsDir) throws IOException {
        BufferedImage waitImg = ImageIO.read(waitFile);
        BufferedImage moveImg = ImageIO.read(moveFile);

        if (waitImg == null || moveImg == null) {
            return false;
        }

        File classDir = new File(targetUnitsDir, className + "/MovingAnimation");
        classDir.mkdirs();

        // 1. Standing frames (Wait sheet: 3 frames stacked vertically)
        int waitW = waitImg.getWidth();
        int waitH = waitImg.getHeight() / 3;
        File standingDir = new File(classDir, "Standing");
        standingDir.mkdirs();
        
        String[] standingFrameNames = new String[3];
        for (int i = 0; i < 3; i++) {
            BufferedImage sub = waitImg.getSubimage(0, i * waitH, waitW, waitH);
            BufferedImage processed = cleanImageBackground(sub);
            String frameName = "frame_" + i + ".png";
            standingFrameNames[i] = frameName;
            ImageIO.write(processed, "PNG", new File(standingDir, frameName));
        }
        writeMetadataAndScript(standingDir, standingFrameNames, 5);

        // 2. Walking frames (Move sheet: 15 frames stacked vertically)
        int moveW = moveImg.getWidth();
        int moveH = moveImg.getHeight() / 15;

        // Down Walk (frames 0 to 3)
        File downDir = new File(classDir, "Walk_Down");
        downDir.mkdirs();
        String[] downFrameNames = new String[4];
        for (int i = 0; i < 4; i++) {
            BufferedImage sub = moveImg.getSubimage(0, i * moveH, moveW, moveH);
            BufferedImage processed = cleanImageBackground(sub);
            String frameName = "frame_" + i + ".png";
            downFrameNames[i] = frameName;
            ImageIO.write(processed, "PNG", new File(downDir, frameName));
        }
        writeMetadataAndScript(downDir, downFrameNames, 5);

        // Up Walk (frames 4 to 7)
        File upDir = new File(classDir, "Walk_Up");
        upDir.mkdirs();
        String[] upFrameNames = new String[4];
        for (int i = 0; i < 4; i++) {
            BufferedImage sub = moveImg.getSubimage(0, (i + 4) * moveH, moveW, moveH);
            BufferedImage processed = cleanImageBackground(sub);
            String frameName = "frame_" + i + ".png";
            upFrameNames[i] = frameName;
            ImageIO.write(processed, "PNG", new File(upDir, frameName));
        }
        writeMetadataAndScript(upDir, upFrameNames, 5);

        // Side Walk (frames 8 to 11)
        File sideDir = new File(classDir, "Walk_Side");
        sideDir.mkdirs();
        String[] sideFrameNames = new String[4];
        for (int i = 0; i < 4; i++) {
            BufferedImage sub = moveImg.getSubimage(0, (i + 8) * moveH, moveW, moveH);
            BufferedImage processed = cleanImageBackground(sub);
            String frameName = "frame_" + i + ".png";
            sideFrameNames[i] = frameName;
            ImageIO.write(processed, "PNG", new File(sideDir, frameName));
        }
        writeMetadataAndScript(sideDir, sideFrameNames, 5);

        return true;
    }

    /**
     * Keying out chroma greens and restoring full alpha transparency.
     */
    private static BufferedImage cleanImageBackground(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha == 0) {
                    dst.setRGB(x, y, 0);
                    continue;
                }

                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                // Robust green key detection
                boolean isGreen = (g > 115 && g > r + 30 && g > b + 30);

                // Pure/dull GBA palette background green variants
                if (Math.abs(r - 168) < 25 && Math.abs(g - 208) < 25 && Math.abs(b - 160) < 25) {
                    isGreen = true;
                }
                if (g > 190 && r < 100 && b < 100) {
                    isGreen = true;
                }

                if (isGreen) {
                    dst.setRGB(x, y, 0); // fully transparent
                } else {
                    dst.setRGB(x, y, argb);
                }
            }
        }
        return dst;
    }

    private static void writeMetadataAndScript(File targetDir, String[] frameNames, int duration) throws IOException {
        // Write metadata.json
        File metaFile = new File(targetDir, "metadata.json");
        try (FileWriter fw = new FileWriter(metaFile)) {
            fw.write("{\n  \"frames\": [\n");
            for (int i = 0; i < frameNames.length; i++) {
                fw.write("    {\"file\": \"" + frameNames[i] + "\", \"duration\": " + duration + "}");
                if (i < frameNames.length - 1) {
                    fw.write(",\n");
                }
            }
            fw.write("\n  ]\n}");
        }

        // Write script.txt
        File scriptFile = new File(targetDir, "script.txt");
        try (FileWriter fw = new FileWriter(scriptFile)) {
            for (String fName : frameNames) {
                fw.write(duration + " " + fName + "\n");
            }
        }
    }
}
