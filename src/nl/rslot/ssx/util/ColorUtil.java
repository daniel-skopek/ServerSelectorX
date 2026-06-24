package nl.rslot.ssx.util;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Color;

import com.google.common.base.Preconditions;

public class ColorUtil {

    /**
	 * Converts all & characters belonging to a color code in a string list to ChatColor.COLOR_CHAR
	 * @see #parseColors(String)
	 * @param list
	 * @return Converted list
	 */
	public static List<String> parseColors(final List<String> list) {
		return list.stream().map(ColorUtil::parseColors).collect(Collectors.toList());
	}

	/**
	 * Converts all & characters belonging to a color code in a string to ChatColor.COLOR_CHAR
	 * @param string String to convert
	 * @see #parseColors(List)
	 */
	public static String parseColors(final String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}

    public static Color hexToColor(String hex){
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		}
		Preconditions.checkArgument(hex.length() == 6,
				"Length of hex string must be 6 (it is %s)", hex.length());

		return Color.fromRGB(
				Integer.valueOf(hex.substring(1, 3), 16),
	            Integer.valueOf(hex.substring(3, 5), 16),
	            Integer.valueOf(hex.substring(5, 7), 16)
		);
	}

}
