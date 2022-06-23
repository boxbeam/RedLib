package redempt.redlib.itemutils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * A utility class to easily create items
 * @author Redempt
 *
 */
public class ItemBuilder extends ItemStack {
	
	/**
	 * Constructs a new ItemBuilder. An ItemBuilder extends ItemStack, an can be used as such.
	 * @param material The type of the item
	 * @param amount The amount of the item
	 */
	public ItemBuilder(Material material, int amount) {
		super(material, amount);
	}
	
	/**
	 * Constructs a new ItemBuilder. An ItemBuilder extends ItemStack, an can be used as such.
	 * @param material The type of the item
	 */
	public ItemBuilder(Material material) {
		super(material);
	}
	
	/**
	 * Constructs an ItemBuilder using a pre-existing item
	 * @param item The item to copy
	 */
	public ItemBuilder(ItemStack item) {
		super(item);
	}
	
	/**
	 * Sets the stack size of this ItemBuilder
	 * @param amount The number of items in the stack
	 * @return The ItemBuilder with the new stack size
	 */
	public ItemBuilder setCount(int amount) {
		setAmount(amount);
		return this;
	}
	
	/**
	 * Adds an enchantment to this ItemBuilder
	 * @param enchant The enchantment to add
	 * @param level The level of the enchantment
	 * @return The enchanted ItemBuilder
	 */
	public ItemBuilder addEnchant(Enchantment enchant, int level) {
		ItemUtils.addEnchant(this, enchant, level);
		return this;
	}
	
	/**
	 * Converts this ItemBuilder to a normal ItemStack. Useful because there are some inconsistencies within Spigot using this class.
	 * @return An ItemStack copy of this ItemBuilder
	 */
	public ItemStack toItemStack() {
		return new ItemStack(this);
	}
	
	/**
	 * Set the lore of this ItemBuilder
	 * @param lore The lines of lore
	 * @return The ItemBuilder with lore added
	 */
	public ItemBuilder setLore(String... lore) {
		ItemUtils.setLore(this, lore);
		return this;
	}
	
	/**
	 * Add a line of lore to this ItemBuilder
	 * @param line The line of lore
	 * @return The ItemBuilder with lore added
	 */
	public ItemBuilder addLore(String line) {
		ItemUtils.addLore(this, line);
		return this;
	}
	
	/**
	 * Add multiple lines of lore to this ItemBuilder
	 * @param lines The lines of lore
	 * @return The ItemBuilder with lore added
	 */
	public ItemBuilder addLore(Iterable<String> lines) {
		ItemUtils.addLore(this, lines);
		return this;
	}

	/**
	 * Remove a String of lore if present from this ItemBuilder
	 * @param line The line of lore to remove
	 * @return The ItemBuilder with lore removed if present
	 */
	public ItemBuilder removeLore(String line) {
		ItemUtils.removeLoreLine(this, line);
		return this;
	}

	/**
	 * Remove a line of lore if present from this ItemBuilder
	 * @param index The index of the line of lore to remove
	 * @return The ItemBuilder with lore removed if present
	 */
	public ItemBuilder removeLore(int index) {
		ItemUtils.removeLoreLine(this, index);
		return this;
	}
	
	/**
	 * Renames this ItemBuilder
	 * @param name The name to set
	 * @return The renamed ItemBuilder
	 */
	public ItemBuilder setName(String name) {
		ItemUtils.rename(this, name);
		return this;
	}
	
	/**
	 * Set the durability (damage) of the ItemBuilder
	 * @param durability The durability to set
	 * @return The ItemBuilder with its durability changed
	 */
	@SuppressWarnings("deprecation")
	public ItemBuilder setDurability(int durability) {
		this.setDurability((short) durability);
		return this;
	}
	
	/**
	 * Adds an attribute to this ItemBuilder
	 * @param attribute The Attribute to be added
	 * @param modifier The AttributeModifier to be added
	 * @return The ItemBuilder with the attribute added
	 */
	public ItemBuilder addAttribute(Attribute attribute, AttributeModifier modifier) {
		ItemUtils.addAttribute(this, attribute, modifier);
		return this;
	}
	
	/**
	 * Adds an attribute to this ItemBuilder
	 * @param attribute The attribute to be added
	 * @param amount The amount of the modifier
	 * @param operation The operation of the modifier 
	 * @return The ItemBuilder with the attribute added
	 */
	public ItemBuilder addAttribute(Attribute attribute, double amount, Operation operation) {
		ItemUtils.addAttribute(this, attribute, amount, operation);
		return this;
	}
	
	/**
	 * Adds an attribute to this ItemBuilder
	 * @param attribute The attribute to be added
	 * @param amount The amount of the modifier
	 * @param operation The operation of the modifier 
	 * @param slot The slot the modifier affects
	 * @return The ItemBuilder with the attribute added
	 */
	public ItemBuilder addAttribute(Attribute attribute, double amount, Operation operation, EquipmentSlot slot) {
		ItemUtils.addAttribute(this, attribute, amount, operation, slot);
		return this;
	}
	
	/**
	 * Adds ItemFlags to this ItemBuilder
	 * @param flags The ItemFlags to add
	 * @return The ItemBuilder with the flags added
	 */
	public ItemBuilder addItemFlags(ItemFlag... flags) {
		ItemUtils.addItemFlags(this, flags);
		return this;
	}
	
	/**
	 * Adds damage to this ItemBuilder
	 * @param damage The amount of damage to apply
	 * @return The ItemBuilder with the damage applied
	 */
	public ItemBuilder addDamage(int damage) {
		ItemUtils.damage(this, damage);
		return this;
	}
	
	/**
	 * Sets the custom model data of this ItemBuilder
	 * @param customModelData The custom model data to set
	 * @return The ItemBuilder with the custom model data set
	 */
	public ItemBuilder setCustomModelData(int customModelData) {
		ItemUtils.setCustomModelData(this, customModelData);
		return this;
	}
	
	/**
	 * Add persistent tags to this ItemBuilder
	 * @param key The key to add the data under
	 * @param type The type of the data
	 * @param data The data to store
	 * @param <T> The primary object type
	 * @param <Z> The retrieved object type
	 * @return The ItemBuilder with the persistent data added
	 */
	public <T, Z> ItemBuilder addPersistentTag(NamespacedKey key, PersistentDataType<T, Z> type, Z data) {
		ItemUtils.addPersistentTag(this, key, type, data);
		return this;
	}
	
	/**
	 * Sets this ItemBuilder to be unbreakable
	 * @return The ItemBuilder with the unbreakable tag added
	 */
	public ItemBuilder unbreakable() {
		ItemUtils.setUnbreakable(this);
		return this;
	}
	
}
