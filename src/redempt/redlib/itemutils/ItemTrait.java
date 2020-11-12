package redempt.redlib.itemutils;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * A set of comparable traits items have and can be compared with {@link ItemUtils#compare(ItemStack, ItemStack, ItemTrait...)}
 * @author Redempt
 */
public enum ItemTrait {
	
	/**
	 * For comparing the durability of two items
	 */
	DURABILITY((a, b) -> a.getDurability() == b.getDurability()),
	/**
	 * For comparing the amount of two items
	 */
	AMOUNT((a, b) -> a.getAmount() == b.getAmount()),
	/**
	 * For comparing the display name of two items
	 */
	NAME((a, b) -> {
		if (a.hasItemMeta() != b.hasItemMeta()) {
			return false;
		}
		if (!a.hasItemMeta()) {
			return true;
		}
		if (a.getItemMeta().hasDisplayName() != b.getItemMeta().hasDisplayName()) {
			return false;
		}
		if (!a.getItemMeta().hasDisplayName()) {
			return true;
		}
		return a.getItemMeta().getDisplayName().equals(b.getItemMeta().getDisplayName());
	}),
	/**
	 * For comparing the lore of two items
	 */
	LORE((a, b) -> {
		if (a.hasItemMeta() != b.hasItemMeta()) {
			return false;
		}
		if (!a.hasItemMeta()) {
			return true;
		}
		if (a.getItemMeta().hasLore() != b.getItemMeta().hasLore()) {
			return false;
		}
		if (!a.getItemMeta().hasLore()) {
			return true;
		}
		List<String> lore1 = a.getItemMeta().getLore();
		List<String> lore2 = b.getItemMeta().getLore();
		if (lore1.size() != lore2.size()) {
			return false;
		}
		for (int i = 0; i < lore1.size(); i++) {
			if (!lore1.get(i).equals(lore2.get(i))) {
				return false;
			}
		}
		return true;
	}),
	/**
	 * For comparing the enchantments of two items
	 */
	ENCHANTMENTS((a, b) -> {
		Map<Enchantment, Integer> ench1 = a.getEnchantments();
		Map<Enchantment, Integer> ench2 = b.getEnchantments();
		if (ench1.size() != ench2.size() || !ench1.keySet().containsAll(ench2.keySet())) {
			return false;
		}
		for (Enchantment ench : ench1.keySet()) {
			if (!ench1.get(ench).equals(ench2.get(ench))) {
				return false;
			}
		}
		return true;
	}),
	/**
	 * For comparing the types of two items
	 */
	TYPE((a, b) -> a.getType() == b.getType());
	
	private BiPredicate<ItemStack, ItemStack> compare;
	
	ItemTrait(BiPredicate<ItemStack, ItemStack> compare) {
		this.compare = compare;
	}
	
	/**
	 * Compares this trait on the two items
	 * @param a The first item
	 * @param b The second item
	 * @return True if the trait is the same on both items, false otherwise
	 */
	public boolean compare(ItemStack a, ItemStack b) {
		return compare.test(a, b);
	}
	
}
