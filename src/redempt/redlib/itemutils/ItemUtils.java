package redempt.redlib.itemutils;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectOutputStream;
import redempt.redlib.json.JSONList;
import redempt.redlib.json.JSONMap;
import redempt.redlib.json.JSONParser;
import redempt.redlib.nms.NMSHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A utility class to easily modify items
 * @author Redempt
 *
 */
public class ItemUtils {
	
	/**
	 * Renames an ItemStack, functionally identical to {@link ItemUtils#setName(ItemStack, String)} but kept for legacy reasons
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
	 * Renames an ItemStack
	 * @param item The ItemStack to be renamed
	 * @param name The name to give the ItemStack
	 * @return The renamed ItemStack
	 */
	public static ItemStack setName(ItemStack item, String name) {
		return rename(item, name);
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
	 * Sets an item to be unbreakable
	 * @param item The item to make unbreakable
	 * @return The unbreakable item
	 */
	public static ItemStack setUnbreakable(ItemStack item) {
		item = item.clone();
		ItemMeta meta = item.getItemMeta();
		meta.setUnbreakable(true);
		item.setItemMeta(meta);
		return item;
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
	 * Adds ItemFlags to the item
	 * @param item The item to add ItemFlags to
	 * @param flags The ItemFlags to add
	 * @return The modified item
	 */
	public static ItemStack addItemFlags(ItemStack item, ItemFlag... flags) {
		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(flags);
		ItemStack clone = item.clone();
		clone.setItemMeta(meta);
		return clone;
	}
	
	/**
	 * Adds persistent data to the item
	 * @param item The item to add persistent data to
	 * @param key The key to add the data under
	 * @param type The type of the data
	 * @param data The data to store
	 * @return The modified item
	 */
	public static <T, Z> ItemStack addPersistentTag(ItemStack item, NamespacedKey key, PersistentDataType<T, Z> type, Z data) {
		ItemMeta meta = item.getItemMeta();
		meta.getPersistentDataContainer().set(key, type, data);
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
	 * Counts the number of items of the given type in the given inventory
	 * @param inv The inventory to count the items in
	 * @param type The type of item to count
	 * @return The number of items found
	 */
	public static int count(Inventory inv, Material type) {
		type = new ItemStack(type).getType();
		int count = 0;
		for (ItemStack i : inv) {
			if (i != null && i.getType() == type) {
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
	
	/**
	 * Removes the specified amount of the given item type from the given inventory
	 * @param inv The inventory to remove the items from
	 * @param type The item type to be removed
	 * @param amount The amount of items to remove
	 * @return Whether the amount specified could be removed. False if it removed less than specified.
	 */
	public static boolean remove(Inventory inv, Material type, int amount) {
		type = new ItemStack(type).getType();
		ItemStack[] contents = inv.getContents();
		for (int i = 0; i < contents.length && amount > 0; i++) {
			if (contents[i] == null || contents[i].getType() != type) {
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
	
	/**
	 * Remove all matching items, returning the number that were removed
	 * @param inv The inventory to count and remove items from
	 * @param item The item to count and remove
	 * @return How many items were removed
	 */
	public static int countAndRemove(Inventory inv, ItemStack item) {
		int count = count(inv, item);
		remove(inv, item, count);
		return count;
	}
	
	/**
	 * Remove all items of a specified type, returning the number that were removed
	 * @param inv The inventory to count and remove items from
	 * @param type The item type to count and remove
	 * @return How many items were removed
	 */
	public static int countAndRemove(Inventory inv, Material type) {
		int count = count(inv, type);
		remove(inv, type, count);
		return count;
	}
	
	/**
	 * Give the player the specified items, dropping them on the ground if there is not enough room
	 * @param player The player to give the items to
	 * @param items The items to be given
	 */
	public static void give(Player player, ItemStack... items) {
		player.getInventory().addItem(items).values().forEach(i -> player.getWorld().dropItem(player.getLocation(), i));
	}
	
	/**
	 * Gives the player the specified amount of the specified item, dropping them on the ground if there is not enough room
	 * @param player The player to give the items to
	 * @param item The item to be given to the player
	 * @param amount The amount the player should be given
	 */
	public static void give(Player player, ItemStack item, int amount) {
		if (amount < 1) {
			throw new IllegalArgumentException("Amount must be greater than 0");
		}
		int stackSize = item.getType().getMaxStackSize();
		while (amount > stackSize) {
			ItemStack clone = item.clone();
			clone.setAmount(stackSize);
			give(player, clone);
			amount -= stackSize;
		}
		ItemStack clone = item.clone();
		clone.setAmount(amount);
		give(player, clone);
	}
	
	/**
	 * Gives the player the specified amount of the specified item type, dropping them on the ground if there is not enough room
	 * @param player The player to give the items to
	 * @param type The item type to be given to the player
	 * @param amount The amount the player should be given
	 */
	public static void give(Player player, Material type, int amount) {
		give(player, new ItemStack(type), amount);
	}
	
	/**
	 * Compares the type, name, and lore of two items
	 * @param first The first ItemStack
	 * @param second The second ItemStack
	 * @return Whether the two items are identical in terms of type, name, and lore. Returns true if both items are null, and false if only one is null.
	 */
	public static boolean compare(ItemStack first, ItemStack second) {
		if (first == second) {
			return true;
		}
		if (first == null || second == null) {
			return false;
		}
		if (first.getType() != second.getType()) {
			return false;
		}
		ItemMeta firstMeta = first.getItemMeta();
		ItemMeta secondMeta = second.getItemMeta();
		if (firstMeta.hasDisplayName() != secondMeta.hasDisplayName()) {
			return false;
		}
		if (firstMeta.hasDisplayName()) {
			if (!firstMeta.getDisplayName().equals(secondMeta.getDisplayName())) {
				return false;
			}
		}
		if (firstMeta.hasLore() != secondMeta.hasLore()) {
			return false;
		}
		if (firstMeta.hasLore()) {
			List<String> firstLore = firstMeta.getLore();
			List<String> secondLore = secondMeta.getLore();
			if (firstLore.size() != secondLore.size()) {
				return false;
			}
			for (int i = 0; i < firstLore.size(); i++) {
				if (!firstLore.get(i).equals(secondLore.get(i))) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Creates a mock inventory clone of the given inventory. Do not try to open this inventory for players,
	 * it will throw an error.
	 * @param inv The inventory to clone
	 * @return A mock clone inventory
	 */
	public static Inventory cloneInventory(Inventory inv) {
		ItemStack[] contents = new ItemStack[inv.getSize()];
		for (int i = 0; i < inv.getSize(); i++) {
			ItemStack item = inv.getItem(i);
			if (item == null) {
				continue;
			}
			contents[i] = item.clone();
		}
		return new MockInventory(contents, inv.getHolder(), inv.getType());
	}
	
	/**
	 * Converts an ItemStack to a JSON string
	 * @param item The ItemStack to convert to a string
	 * @return A JSON string representing the given item
	 */
	public static String toString(ItemStack item) {
		return item == null ? null : toJSON(item, ItemStack.class).toString();
	}
	
	/**
	 * Constructs an ItemStack from a previously-serialized JSON string
	 * @param json The JSON string created using {@link ItemUtils#toString(ItemStack)}
	 * @return The deserialized ItemStack
	 */
	public static ItemStack fromString(String json) {
		JSONMap map = JSONParser.parseMap(json);
		ItemStack item = (ItemStack) deserialize(map);
		ItemMeta meta = item.getItemMeta();
		// Everything except enchantments works. Not sure why, but it has to be done manually.
		if (map.containsKey("meta")) {
			JSONMap jsonMeta = map.getMap("meta");
			JSONMap enchants = jsonMeta.getMap("enchants");
			if (enchants != null) {
				enchants.forEach((k, v) -> {
					meta.addEnchant(Enchantment.getByName(k), (int) v, true);
				});
			}
		}
		item.setItemMeta(meta);
		return item;
	}
	
	private static Object deserialize(JSONMap map) {
		try {
			Map<String, Object> dmap = new HashMap<>();
			map.forEach((k, v) -> {
				if (v instanceof JSONMap) {
					JSONMap json = (JSONMap) v;
					if (json.containsKey("==")) {
						dmap.put(k, deserialize(json));
					}
					return;
				}
				dmap.put(k, v);
			});
			Class<?> clazz = Class.forName(map.getString("==").replace("%version%", NMSHelper.getNMSVersion()));
			DelegateDeserialization annotation = clazz.getAnnotation(DelegateDeserialization.class);
			if (annotation != null) {
				clazz = annotation.value();
			}
			Method deserialize = clazz.getDeclaredMethod("deserialize", Map.class);
			Object o = deserialize.invoke(null, dmap);
			return o;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static JSONMap toJSON(ConfigurationSerializable s, Class<?> clazz) {
		Map<String, Object> map = s.serialize();
		JSONMap json = new JSONMap();
		json.put("==", clazz.getName().replace(NMSHelper.getNMSVersion(), "%version%"));
		map.forEach((k, v) -> {
			json.put(k, serialize(v));
		});
		return json;
	}
	
	private static Object serialize(Object o) {
		if (o instanceof ConfigurationSerializable) {
			Class<?> clazz = o.getClass();
			return toJSON((ConfigurationSerializable) o, clazz);
		} else if (o instanceof Map) {
			Map map = (Map) o;
			JSONMap json = new JSONMap();
			map.forEach((k, v) -> {
				json.put(k.toString(), serialize(v));
			});
			return json;
		} else if (o instanceof List) {
			List list = (List) o;
			JSONList json = new JSONList();
			list.stream().map(ItemUtils::serialize).forEach(json::add);
			return json;
		}
		return o;
	}
	
}
