package redempt.redlib.enchants;

/**
 * Represents a CustomEnchant and level
 * @author Redempt
 */
public class EnchantInfo {
	
	private CustomEnchant<?> ench;
	private int level;
	
	/**
	 * Constructs an EnchantInfo from a CustomEnchant and level
	 * @param ench The CustomEnchant
	 * @param level The level
	 */
	public EnchantInfo(CustomEnchant<?> ench, int level) {
		this.ench = ench;
		this.level = level;
	}
	
	/**
	 * @return The level stored in this EnchantInfo
	 */
	public int getLevel() {
		return level;
	}
	
	/**
	 * @return The CustomEnchant stored in this EnchantInfo
	 */
	public CustomEnchant<?> getEnchant() {
		return ench;
	}
	
}
