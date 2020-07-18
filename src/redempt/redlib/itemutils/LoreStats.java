package redempt.redlib.itemutils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents stats or other info stored in item lore
 */
public class LoreStats {
	
	private Map<String, LorePlaceholder> placeholders = new HashMap<>();
	private List<String> lines;
	private boolean fixedLore = true;
	
	/**
	 * Create a LoreStats from a list of lore lines and a vararg of placeholder names
	 * @param lines The lines of lore to load. Placeholders should be surrounded by percent signs.
	 * @param placeholders Placeholder names, WITHOUT being surrounded by percent signs
	 */
	public LoreStats(List<String> lines, String... placeholders) {
		this.lines = lines;
		for (String name : placeholders) {
			String full = '%' + name + '%';
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				int posStart = line.indexOf(full);
				if (posStart == -1) {
					continue;
				}
				int posFromEnd = line.length() - (posStart + full.length());
				LorePlaceholder placeholder = new LorePlaceholder(name, i, posStart, posFromEnd);
				this.placeholders.put(name, placeholder);
			}
		}
	}
	
	/**
	 * True by default. Set to true if the lore lines on the item will always show up at the same
	 * positions as what was originally passed when constructing these LoreStats, false if the lore
	 * might be on different lines.
	 * @param fixedLore Whether the lines of the lore should be fixed
	 */
	public void setFixedLore(boolean fixedLore) {
		this.fixedLore = fixedLore;
	}
	
	private int getLine(ItemStack item, LorePlaceholder placeholder, String name) {
		if (placeholder == null) {
			throw new IllegalArgumentException("Placeholder '" + name + "' has not been registered");
		}
		List<String> lore = item.getItemMeta().getLore();
		int line = placeholder.getLine();
		if (!fixedLore) {
			String loreLine = lines.get(placeholder.getLine());
			String start = loreLine.substring(0, placeholder.getPosStart());
			String end = loreLine.substring(loreLine.length() - placeholder.getPosFromEnd());
			for (int i = 0; i < lore.size(); i++) {
				String str = lore.get(i);
				if (str.startsWith(start) && str.endsWith(end)) {
					line = i;
					break;
				}
			}
			line = -1;
		}
		if (line >= lore.size()) {
			return -1;
		}
		return line;
	}
	
	/**
	 * Gets a stat in String form from the lore of an item
	 * @param item The item to check the stat on
	 * @param placeholder The placeholder to use, without percent signs.
	 * @return The String stat found, or null if none was found
	 */
	public String getStat(ItemStack item, String placeholder) {
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
			return null;
		}
		LorePlaceholder lp = placeholders.get(placeholder);
		List<String> lore = item.getItemMeta().getLore();
		int line = getLine(item, lp, placeholder);
		if (line == -1) {
			return null;
		}
		String str = lore.get(line);
		return str.substring(lp.getPosStart(), str.length() - lp.getPosFromEnd());
	}
	
	/**
	 * Gets an int stat from lore
	 * @param item The item to check the stat on
	 * @param placeholder The placeholder to use, without percent signs
	 * @return The int stat found
	 * @throws NullPointerException if the stat was not found
	 */
	public int getInt(ItemStack item, String placeholder) {
		return Integer.parseInt(getStat(item, placeholder));
	}
	
	/**
	 * Gets an int stat from lore
	 * @param item The item to check the stat on
	 * @param placeholder The placeholder to use, without percent signs
	 * @param defaultValue The default value to use if no value was found in the lore
	 * @return The int stat found, or the default value
	 */
	public int getInt(ItemStack item, String placeholder, int defaultValue) {
		String str = getStat(item, placeholder);
		if (str == null) {
			return defaultValue;
		}
		return Integer.parseInt(str);
	}
	
	/**
	 * Gets a double stat from lore
	 * @param item The item to check the stat on
	 * @param placeholder The placeholder to use, without percent signs
	 * @return The double stat found
	 * @throws NullPointerException if the stat was not found
	 */
	public double getDouble(ItemStack item, String placeholder) {
		return Double.parseDouble(getStat(item, placeholder));
	}
	
	/**
	 * Gets a double stat from lore
	 * @param item The item to check the stat on
	 * @param placeholder The placeholder to use, without percent signs
	 * @param defaultValue The default value to use if no value was found in the lore
	 * @return The double stat found, or the default value
	 */
	public double getDouble(ItemStack item, String placeholder, double defaultValue) {
		String str = getStat(item, placeholder);
		if (str == null) {
			return defaultValue;
		}
		return Double.parseDouble(str);
	}
	
	/**
	 * Sets the stat on the given item to the given object, which will be cast to a String
	 * @param item The item to set the stat on
	 * @param placeholder The placeholder stat to set
	 * @param obj The object to set the stat to
	 * @return The modified ItemStack, or null if it could not be modified
	 */
	public ItemStack set(ItemStack item, String placeholder, Object obj) {
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
			return null;
		}
		LorePlaceholder lp = placeholders.get(placeholder);
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		int line = getLine(item, lp, placeholder);
		if (line == -1) {
			return null;
		}
		String str = lore.get(line);
		str = str.substring(0, lp.getPosStart()) + obj + str.substring(str.length() - lp.getPosFromEnd());
		lore.set(line, str);
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	private static class LorePlaceholder {
	
		private String name;
		private int line;
		private int posStart;
		private int posFromEnd;
		
		public LorePlaceholder(String name, int line, int posStart, int posFromEnd) {
			this.name = name;
			this.line = line;
			this.posStart = posStart;
			this.posFromEnd = posFromEnd;
		}
		
		public String getName() {
			return name;
		}
		
		public String getFullName() {
			return '%' + name + '%';
		}
		
		public int getLine() {
			return line;
		}
		
		public int getPosStart() {
			return posStart;
		}
		
		public int getPosFromEnd() {
			return posFromEnd;
		}
	
	}
	
}
