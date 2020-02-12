package redempt.redlib.itemutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * A utility class to easily modify items
 * @author Redempt
 *
 */
public class ItemUtils {
	
	/**
	 * Renames an ItemStack
	 * @param item The ItemStack to be renamed
	 * @param name The name to give the ItemStack
	 * @return The renamed ItemStack
	 */
	public static ItemStack rename(ItemStack item, String name) {
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		ItemStack clone = item.clone();
		clone.setItemMeta(meta);
		return clone;
	}
	
	/**
	 * Set a single line of lore for an ItemStack
	 * @param item The ItemStack to be given lore
	 * @param line The line of lore to be given
	 * @return The modified ItemStack
	 */
	public static ItemStack setLore(ItemStack item, String line) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = new ArrayList<>();
		lore.add(line);
		meta.setLore(lore);
		ItemStack clone = item.clone();
		clone.setItemMeta(meta);
		return clone;
	}
	
	/**
	 * Set multiple lines of lore for an ItemStack
	 * @param item The ItemStack to be given lore
	 * @param lore The lines of lore to be given
	 * @return The modified ItemStack
	 */
	public static ItemStack setLore(ItemStack item, List<String> lore) {
		ItemMeta meta = item.getItemMeta();
		meta.setLore(lore);
		ItemStack clone = item.clone();
		clone.setItemMeta(meta);
		return clone;
	}
	
	/**
	 * Add a line of lore to an ItemStack
	 * @param item The ItemStack to be given lore
	 * @param line The line of lore to add
	 * @return The modified ItemStack
	 */
	public static ItemStack addLore(ItemStack item, String line) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		if (lore == null) {
			lore = new ArrayList<>();
		}
		lore.add(line);
		meta.setLore(lore);
		ItemStack clone = item.clone();
		clone.setItemMeta(meta);
		return clone;
	}
	
	/**
	 * Set multiple lines of lore for an ItemStack
	 * @param item The ItemStack to be given lore
	 * @param lore The lines of lore to be given
	 * @return The modified ItemStack
	 */
	public static ItemStack setLore(ItemStack item, String... lore) {
		return setLore(item, Arrays.asList(lore));
	}
	
	/**
	 * Add an enchantment to an ItemStack
	 * @param item The ItemStack to be enchanted
	 * @param enchant The Enchantment to add to the ItemStack
	 * @param level The level of the Enchantment
	 * @return The enchanted ItemStack
	 */
	public static ItemStack addEnchant(ItemStack item, Enchantment enchant, int level) {
		ItemMeta meta = item.getItemMeta();
		meta.addEnchant(enchant, level, true);
		if (level == 0) {
			meta.removeEnchant(enchant);
		}
		ItemStack clone = item.clone();
		clone.setItemMeta(meta);
		return clone;
	}
	
	/**
	 * Add an attribute to the item
	 * @param item The item to have an attribute added
	 * @param attribute The Attribute to be added
	 * @param modifier The AttributeModifier to be added
	 * @return The modified ItemStack
	 */
	public static ItemStack addAttribute(ItemStack item, Attribute attribute, AttributeModifier modifier) {
		ItemMeta meta = item.getItemMeta();
		meta.addAttributeModifier(attribute, modifier);
		ItemStack clone = item.clone();
		clone.setItemMeta(meta);
		return clone;
	}
	
	/**
	 * Add an attribute to the item
	 * @param item The item to have an attribute added
	 * @param attribute The Attribute to be added
	 * @param amount The amount to modify it by
	 * @param operation The operation by which the value will be modified
	 * @return The modified item
	 */
	public static ItemStack addAttribute(ItemStack item, Attribute attribute, double amount, Operation operation) {
		ItemMeta meta = item.getItemMeta();
		AttributeModifier modifier = new AttributeModifier(attribute.toString(), amount, operation);
		meta.addAttributeModifier(attribute, modifier);
		ItemStack clone = item.clone();
		clone.setItemMeta(meta);
		return clone;
	}
	
	/**
	 * Add an attribute to the item
	 * @param item The item to have an attribute added
	 * @param attribute The Attribute to be added
	 * @param amount The amount to modify it by
	 * @param operation The operation by which the value will be modified
	 * @param slot The slot this attribute will be effective in
	 * @return The modified item
	 */
	public static ItemStack addAttribute(ItemStack item, Attribute attribute, double amount, Operation operation, EquipmentSlot slot) {
		ItemMeta meta = item.getItemMeta();
		AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), attribute.toString(), amount, operation, slot);
		meta.addAttributeModifier(attribute, modifier);
		ItemStack clone = item.clone();
		clone.setItemMeta(meta);
		return clone;
	}
	
	/**
	 * Counts the number of the given item in the given inventory
	 * @param inv The inventory to count the items in
	 * @param item The item to count
	 * @return The number of items found
	 */
	public static int count(Inventory inv, ItemStack item) {
		int count = 0;
		for (ItemStack i : inv) {
			if (item.isSimilar(i)) {
				count += i.getAmount();
			}
		}
		return count;
	}
	
	/**
	 * Removes the specified amount of the given item from the given inventory
	 * @param inv The inventory to remove the items from
	 * @param item The item to be removed
	 * @param amount The amount of items to remove
	 * @return Whether the amount specified could be removed. False if it removed less than specified.
	 */
	public static boolean remove(Inventory inv, ItemStack item, int amount) {
		ItemStack[] contents = inv.getContents();
		for (int i = 0; i < contents.length && amount > 0; i++) {
			if (!item.isSimilar(contents[i])) {
				continue;
			}
			if (amount >= contents[i].getAmount()) {
				amount -= contents[i].getAmount();
				contents[i] = null;
				if (amount == 0) {
					inv.setContents(contents);
					return true;
				}
				continue;
			}
			contents[i].setAmount(contents[i].getAmount() - amount);
			inv.setContents(contents);
			return true;
		}
		inv.setContents(contents);
		return false;
	}
	
}
