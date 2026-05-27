package game.core.util;

/**
 * Centrally manages custom lightweight JSON value extraction and safe integer parsing.
 */
public class JsonUtil {

    /**
     * Safely parses a string into an integer with a fallback default value.
     */
    public static int parseInt(String s, int def) {
        if (s == null) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Extracts a string value matching the specified key from a JSON-like string block.
     */
    public static String extractJsonVal(String json, String key) {
        if (json == null || key == null) return "";
        try {
            int start = json.indexOf("\"" + key + "\":");
            if (start == -1) {
                // Alternative format search
                String search = "\"" + key + "\"";
                int s = json.indexOf(search);
                if (s == -1) return "";
                int colon = json.indexOf(":", s + search.length());
                if (colon == -1) return "";
                String sub = json.substring(colon + 1).trim();
                if (sub.startsWith("\"")) {
                    int end = sub.indexOf("\"", 1);
                    if (end == -1) return "";
                    return sub.substring(1, end);
                }
                int e = sub.indexOf(",");
                if (e == -1) e = sub.indexOf("}");
                if (e == -1) return sub.trim();
                return sub.substring(0, e).trim();
            }
            int startIdx = json.indexOf(":", start) + 1;
            String sub = json.substring(startIdx).trim();
            if (sub.startsWith("\"")) {
                int end = sub.indexOf("\"", 1);
                if (end == -1) return "";
                return sub.substring(1, end);
            } else {
                int end = sub.indexOf(",");
                if (end == -1) end = sub.indexOf("}");
                if (end == -1) return sub.trim();
                return sub.substring(0, end).trim();
            }
        } catch (Exception e) {
            return "";
        }
    }
}
