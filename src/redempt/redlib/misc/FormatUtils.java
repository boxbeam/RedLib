package redempt.redlib.misc;

import java.text.DecimalFormat;

/**
 * Contains utilities for formatting various information
 * @author Redempt
 */
public class FormatUtils {
	
	private static DecimalFormat format = new DecimalFormat("0.00");
	private static char[] suffixes = {'K', 'M', 'B', 'T', 'Q'};
	
	/**
	 * Formats a time offset like 1h3m8s (1 hour, 3 minutes, 8 seconds)
	 * @param millis The time offset, in milliseconds
	 * @return The formatted string
	 */
	public static String formatTimeOffset(long millis) {
		millis /= 1000;
		long hours = millis / 3600;
		millis %= 3600;
		long minutes = millis / 60;
		millis %= 60;
		StringBuilder output = new StringBuilder();
		if (hours > 0) {
			output.append(hours).append("h");
		}
		if (minutes > 0) {
			output.append(minutes).append("m");
		}
		output.append(millis).append("s");
		return output.toString();
	}
	
	/**
	 * Truncates a double using a DecimalFormat with 0.00 as its format string
	 * @param input The input double
	 * @return The formatted double
	 */
	public static String truncateDouble(double input) {
		return format.format(input);
	}
	
	/**
	 * Formats money like 3.5B representing 3.5 billion
	 * @param money The money
	 * @return The formatted output string
	 */
	public static String formatMoney(double money) {
		int i = -1;
		while (money >= 1000) {
			money /= 1000;
			i++;
		}
		if (i >= 0) {
			return truncateDouble(money) + suffixes[i];
		}
		return truncateDouble(money);
	}
	
}
