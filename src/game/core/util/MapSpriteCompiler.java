package game.core.util;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;

/**
 * Utility to compile images into GBA-compatible map sprite data.
 * Handles 4bpp tile conversion and LZ77 compression.
 */
public class MapSpriteCompiler {

    /**
     * Compiles an image into a compressed GBA map sprite blob.
     */
    public static byte[] compile(BufferedImage img) {
        byte[] tiledData = convertTo4bppTiles(img);
        return compressLZ77(tiledData);
    }

    /**
     * Converts a BufferedImage to 4bpp GBA tile format (8x8 tiles).
     */
    public static byte[] convertTo4bppTiles(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        
        // Ensure dimensions are multiples of 8
        int tilesX = (width + 7) / 8;
        int tilesY = (height + 7) / 8;
        
        byte[] data = new byte[tilesX * tilesY * 32];
        int dataIdx = 0;

        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                // Process one 8x8 tile
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x += 2) {
                        int px = tx * 8 + x;
                        int py = ty * 8 + y;
                        
                        int p1 = 0;
                        int p2 = 0;
                        
                        if (px < width && py < height) {
                            p1 = getPixelIndex(img, px, py);
                        }
                        if (px + 1 < width && py < height) {
                            p2 = getPixelIndex(img, px + 1, py);
                        }
                        
                        // GBA 4bpp: Byte contains two pixels. 
                        // Low nibble is first pixel, High nibble is second pixel.
                        data[dataIdx++] = (byte) ((p1 & 0x0F) | ((p2 & 0x0F) << 4));
                    }
                }
            }
        }
        return data;
    }

    private static int getPixelIndex(BufferedImage img, int x, int y) {
        int argb = img.getRGB(x, y);
        if ((argb >>> 24) < 128) return 0; // Transparent
        
        if (img.getColorModel() instanceof IndexColorModel) {
            // If it's already indexed, we should ideally use the raster
            // but for simplicity and robustness with various inputs, 
            // we'll assume the user has set up the palette or we'll map to indices.
            // For this project, we'll try to find the best match in the palette if it's not simple.
            // However, a simple implementation just uses the green channel or similar if not careful.
            // Let's assume we want to support any image by mapping colors to 16-color indices.
        }
        
        // Custom color mapping (Simplified for this project)
        // In a real FE tool, we'd use a color-to-index map.
        // Here we'll just use a basic luminance-based index if not indexed, 
        // but ideally we want to preserve the palette indices if they exist.
        Object data = img.getRaster().getDataElements(x, y, null);
        if (img.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
            return ((byte[]) data)[0] & 0xFF;
        }
        
        // Fallback for non-indexed images (not ideal for GBA)
        return (argb & 0xFF) / 16; 
    }

    /**
     * Compresses data using GBA-compliant LZ77.
     */
    public static byte[] compressLZ77(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Header: 0x10 | Length(24-bit)
        int len = data.length;
        out.write(0x10);
        out.write(len & 0xFF);
        out.write((len >> 8) & 0xFF);
        out.write((len >> 16) & 0xFF);

        int pos = 0;
        while (pos < len) {
            int flags = 0;
            byte[] buffer = new byte[16]; // Max 8 blocks, each max 2 bytes
            int bufIdx = 0;

            for (int i = 0; i < 8; i++) {
                if (pos >= len) break;

                Match match = findMatch(data, pos);
                if (match != null && match.length >= 3) {
                    flags |= (1 << (7 - i));
                    int disp = pos - match.distance - 1;
                    buffer[bufIdx++] = (byte) (((match.length - 3) << 4) | ((disp >> 8) & 0x0F));
                    buffer[bufIdx++] = (byte) (disp & 0xFF);
                    pos += match.length;
                } else {
                    buffer[bufIdx++] = data[pos++];
                }
            }
            out.write(flags);
            out.write(buffer, 0, bufIdx);
        }

        return out.toByteArray();
    }

    private static class Match {
        int distance;
        int length;
    }

    private static Match findMatch(byte[] data, int pos) {
        Match best = null;
        int maxDist = Math.min(pos, 4096);
        int maxLen = Math.min(data.length - pos, 18);

        for (int d = 1; d <= maxDist; d++) {
            int len = 0;
            while (len < maxLen && data[pos - d + (len % d)] == data[pos + len]) {
                len++;
            }
            if (len >= 3 && (best == null || len > best.length)) {
                best = new Match();
                best.distance = pos - d;
                best.length = len;
                if (len == 18) break;
            }
        }
        return best;
    }
}
