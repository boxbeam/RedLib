package redempt.redlib.misc;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;

import java.util.regex.Pattern;

public class ColorUtils {
    /**
     * Convert color codes and hex codes to Color container (RGB Container)
     * @param value Must match one of these formats "#FFFFFF" or "&c"
     * @return org.bukkit.Color
     */
    public static Color getRGBFromCode(String value) {
        return value.replace("#", "").matches("^([a-fA-F0-9]{6})$") ? hex2Rgb(value) : minecraft2Rgb(value);
    }

    /**
     * Convert string to a ChatColor object
     * @param value Must match one of these formats "#FFFFFF" or "&c"
     * @return net.md_5.bungee.api.ChatColor
     */
    public static ChatColor getChatColorFromCode(String value) {
        if (value.matches("&([A-z0-9]){1}")) {
            if (value.length() <= 1) return ChatColor.WHITE;
            return ChatColor.getByChar(value.charAt(1));
        } else if (value.matches("^#([a-fA-F0-9]{6})$")) {
            return ChatColor.of(value);
        } else {
            return ChatColor.WHITE;
        }
    }

    /**
     * Convert a string to a Color object
     * @param value A properly formatted "R,G,B" string
     * @param separator Separator used to form the "R,G,B" string
     * @return org.bukkit.Color
     */
    public static Color rgbFromString(String value, String separator) {
        String[] split = value.split(Pattern.quote(separator));
        return Color.fromRGB(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    /**
     * Convert a string to a Color object
     * @param value A properly formatted "R G B" string
     * @return org.bukkit.Color
     */
    public static Color rgbFromString(String value) {
        return rgbFromString(value, " ");
    }

    /**
     * Convert a Color object to "R,G,B" readable format
     * @param color org.bukkit.Color
     * @param separator Separator used to form the "R,G,B" string
     * @return A readable "R,G,B" string
     */
    public static String rgbToString(Color color, String separator) {
        return separator + color.getGreen() + separator + color.getBlue();
    }

    /**
     * Convert a Color object to "R G B" readable format
     * @param color org.bukkit.Color
     * @return A readable "R G B" string
     */
    public static String rgbToString(Color color) {
        return rgbToString(color, " ");
    }


    /**
     * Converts "#FFFFFF" format to a Color object containing RGB values
     * @param value Must be in format "#FFFFFF"
     * @return org.bukkit.Color
     */
    public static Color hex2Rgb(String value) {
        try {
            Color color;
            if (value.contains("#")) {
                color = Color.fromRGB(
                        Integer.valueOf(value.substring(1, 3), 16),
                        Integer.valueOf(value.substring(3, 5), 16),
                        Integer.valueOf(value.substring(5, 7), 16));
            } else {
                color = Color.fromRGB(
                        Integer.valueOf(value.substring(0, 2), 16),
                        Integer.valueOf(value.substring(2, 4), 16),
                        Integer.valueOf(value.substring(4, 6), 16));
            }
            return color;
        } catch (IllegalArgumentException ignored) {
            return Color.WHITE;
        }
    }

    /**
     * Converts "&c" format to a Color object containing RGB values
     * @param value Must be in format "&0"
     * @return org.bukkit.Color
     */
    public static Color minecraft2Rgb(String value) {
        try {
            switch (value) {
                case "&0":
                    return Color.fromRGB(0, 0, 0);
                case "&1":
                    return Color.fromRGB(0, 0, 170);
                case "&2":
                    return Color.fromRGB(0, 170, 0);
                case "&3":
                    return Color.fromRGB(0, 170, 170);
                case "&4":
                    return Color.fromRGB(170, 0, 0);
                case "&5":
                    return Color.fromRGB(170, 0, 170);
                case "&6":
                    return Color.fromRGB(255, 170, 0);
                case "&7":
                    return Color.fromRGB(170, 170, 170);
                case "&8":
                    return Color.fromRGB(85, 85, 85);
                case "&9":
                    return Color.fromRGB(85, 85, 255);
                case "&a":
                    return Color.fromRGB(85, 255, 85);
                case "&b":
                    return Color.fromRGB(85, 255, 255);
                case "&c":
                    return Color.fromRGB(255, 85, 85);
                case "&d":
                    return Color.fromRGB(255, 85, 255);
                case "&e":
                    return Color.fromRGB(255, 255, 85);
                default:
                    return Color.fromRGB(255, 255, 255);
            }
        } catch (IllegalArgumentException ignored) {
            return Color.WHITE;
        }
    }
}