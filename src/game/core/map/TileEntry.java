package game.core.map;

import java.io.Serializable;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

/**
 * Represents a single entry in a TSA-based map grid.
 * Matches the GBA 2-byte structure.
 */
public class TileEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int tileId;      // 10 bits in GBA (0-1023)
    public boolean flipX;   // 1 bit
    public boolean flipY;   // 1 bit
    public int paletteId;   // 4 bits (0-15)
    public String tilesetName = "DEFAULT";

    public TileEntry(int tileId) {
        this.tileId = tileId;
    }

    public TileEntry(int tileId, boolean flipX, boolean flipY, int paletteId) {
        this.tileId = tileId;
        this.flipX = flipX;
        this.flipY = flipY;
        this.paletteId = paletteId;
    }
    
    /**
     * Converts to GBA short format (2 bytes).
     */
    public short toShort() {
        int val = (tileId & 0x3FF);
        if (flipX) val |= 0x400;
        if (flipY) val |= 0x800;
        val |= (paletteId & 0xF) << 12;
        return (short) val;
    }
    
    public static TileEntry fromShort(short s) {
        int val = s & 0xFFFF;
        int id = val & 0x3FF;
        boolean fx = (val & 0x400) != 0;
        boolean fy = (val & 0x800) != 0;
        int pal = (val >> 12) & 0xF;
        return new TileEntry(id, fx, fy, pal);
    }
}
