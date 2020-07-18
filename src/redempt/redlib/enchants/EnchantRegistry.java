package redempt.redlib.enchants;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.Command.CommandArgumentType;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A registry for custom enchantments owned by a single plugin
 * @author Redempt
 */
public class EnchantRegistry {
	
	private static Map<Plugin, EnchantRegistry> registries = new HashMap<>();
	
	/**
	 * Gets the EnchantRegistry owned by the given plugin
	 * @param plugin The plugin owning the requested EnchantRegistry
	 * @return The EnchantRegistry owned by the plugin
	 */
	public static EnchantRegistry get(Plugin plugin) {
		return registries.get(plugin);
	}
	
	private Map<String, CustomEnchant<?>> enchants = new HashMap<>();
	private Map<String, CustomEnchant<?>> byDisplayName = new HashMap<>();
	private Plugin plugin;
	private Function<CustomEnchant<?>, String> namer;
	
	/**
	 * Gets a CustomEnchant by its name or ID
	 * @param name The name or ID of the enchantment
	 * @return The CustomEnchant
	 */
	public CustomEnchant<?> getByName(String name) {
		return enchants.get(name.toLowerCase().replace(" ", "_"));
	}
	
	/**
	 * @return A collection of all the CustomEnchants in this EnchantRegistry
	 */
	public Collection<CustomEnchant<?>> getEnchants() {
		return enchants.values();
	}
	
	protected int getLastSpace(String input) {
		for (int i = input.length() - 1; i >= 0; i--) {
			if (input.charAt(i) == ' ') {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Instantiates a new EnchantRegistry
	 * @param plugin The plugin that owns this EnchantRegistry
	 * @param namer A function which will generate a display name for any given CustomEnchant
	 */
	public EnchantRegistry(Plugin plugin, Function<CustomEnchant<?>, String> namer) {
		this.plugin = plugin;
		this.namer = namer;
		registries.put(plugin, this);
	}
	
	/**
	 * Instantiates a new EnchantRegistry
	 * @param plugin The plugin that owns this EnchantResgistry
	 * @param prefix The prefix to be prepended to the name of any given CustomEnchant to create the display name
	 */
	public EnchantRegistry(Plugin plugin, String prefix) {
		this(plugin, e -> prefix + e.getName());
	}
	
	/**
	 * Instantiates a new EnchantRegistry with a namer that prepends the gray chat color
	 * @param plugin The plugin that owns this EnchantRegistry
	 */
	public EnchantRegistry(Plugin plugin) {
		this(plugin, ChatColor.GRAY + "");
	}
	
	/**
	 * Registers a CustomEnchant in this EnchantRegistry
	 * @param ench The CustomEnchant to register
	 */
	public void register(CustomEnchant<?> ench) {
		ench.register(this);
		enchants.put(ench.getId(), ench);
		byDisplayName.put(ench.getDisplayName(), ench);
	}
	
	/**
	 * @return The plugin that owns this EnchantRegistry
	 */
	public Plugin getPlugin() {
		return plugin;
	}
	
	/**
	 * Removes all CustomEnchants from this EnchantRegistry
	 */
	public void clear() {
		enchants.clear();
		byDisplayName.clear();
	}
	
	/**
	 * Peeks inside a plugin's jar and registers all the classes which extend CustomEnchant inside it.
	 * Note: Custom enchantment classes MUST have a constructor with no arguments to be loaded by this method
	 * @param plugin The plugin to load all CustomEnchants from
	 */
	public void registerAll(Plugin plugin) {
		try {
			ClassLoader loader = plugin.getClass().getClassLoader();
			JarFile file = new JarFile(new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
			Enumeration<JarEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				String name = entry.getName();
				if (!name.endsWith(".class")) {
					continue;
				}
				name = name.substring(0, name.length() - 6).replace("/", ".");
				Class<?> clazz = Class.forName(name, true, loader);
				if (!CustomEnchant.class.isAssignableFrom(clazz) || Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
					continue;
				}
				try {
					Constructor<?> constructor = clazz.getConstructor();
					CustomEnchant<?> ench = (CustomEnchant<?>) constructor.newInstance();
					register(ench);
				} catch (NoSuchMethodException e) {
					throw new IllegalStateException("Class " + clazz.getName() + " does not have a default constructor and could not be loaded");
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * Gets all the CustomEnchants on an item
	 * @param item The item to get the CustomEnchants from
	 * @return A map of each CustomEnchant on this item to its level
	 */
	public Map<CustomEnchant<?>, Integer> getEnchants(ItemStack item) {
		Map<CustomEnchant<?>, Integer> map = new HashMap<>();
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
			return map;
		}
		item.getItemMeta().getLore().forEach(s -> {
			EnchantInfo info = fromLoreLine(s);
			if (info != null) {
				map.put(info.getEnchant(), info.getLevel());
			}
		});
		return map;
	}
	
	/**
	 * Combines two maps of CustomEnchants to their levels, in the same way that normal enchantments would be combined at an anvil
	 * @param first The first map of CustomEnchants to levels
	 * @param second The second map of CustomEnchants to levels - Incompatible enchants will be removed from this map
	 * @return A map of the combined CustomEnchants to their levels
	 */
	public Map<CustomEnchant<?>, Integer> combine(Map<CustomEnchant<?>, Integer> first, Map<CustomEnchant<?>, Integer> second) {
		Map<CustomEnchant<?>, Integer> ffirst = new HashMap<>(first);
		Map<CustomEnchant<?>, Integer> fsecond = new HashMap<>(second);
		fsecond.keySet().removeIf(e -> ffirst.keySet().stream().anyMatch(e2 -> !e2.isCompatible(e)));
		fsecond.forEach((ench, level) -> {
			if (!ffirst.containsKey(ench)) {
				ffirst.put(ench, level);
				return;
			}
			int firstLevel = ffirst.get(ench);
			if (firstLevel != level) {
				ffirst.put(ench, Math.max(level, firstLevel));
				return;
			}
			if (level + 1 <= ench.getMaxLevel()) {
				ffirst.put(ench, level + 1);
			}
		});
		return ffirst;
	}
	
	/**
	 * Applies all the enchantments in a map of CustomEnchants to their levels to an item
	 * @param enchants The map of CustomEnchants to their levels
	 * @param item The item to apply the enchants to
	 * @return The enchanted item
	 */
	public ItemStack applyAll(Map<CustomEnchant<?>, Integer> enchants, ItemStack item) {
		for (Entry<CustomEnchant<?>, Integer> entry : enchants.entrySet()) {
			item = entry.getKey().apply(item, entry.getValue());
		}
		return item;
	}
	
	/**
	 * Gets a CustomEnchant and its level from a line of lore
	 * @param line The line of lore
	 * @return The EnchantInfo containing the enchantment type and level, or null if there was no CustomEnchant on the given line of lore
	 */
	public EnchantInfo fromLoreLine(String line) {
		int lastSpace = getLastSpace(line);
		if (lastSpace == -1) {
			CustomEnchant<?> ench = byDisplayName.get(line);
			if (ench != null && ench.getMaxLevel() == 1) {
				return new EnchantInfo(ench, 1);
			}
			return null;
		}
		String name = line.substring(0, lastSpace);
		String level = line.substring(lastSpace + 1, line.length());
		CustomEnchant<?> ench = byDisplayName.get(name);
		if (ench == null) {
			return null;
		}
		int lvl = CustomEnchant.fromRomanNumerals(level);
		return new EnchantInfo(ench, lvl);
	}
	
	/**
	 * Gets the display name of a CustomEnchant
	 * @param enchant The Enchant to get the display name of
	 * @return The display name
	 */
	public String getDisplayName(CustomEnchant<?> enchant) {
		return namer.apply(enchant);
	}
	
	/**
	 * Gets the CommandArgumentType for CustomEnchants in this registry, with tab completion using IDs
	 * @param name The name to use for the argument type
	 * @return A CommandArgumentType for CustomEnchants in this registry
	 */
	public CommandArgumentType<? extends CustomEnchant<?>> getEnchantArgType(String name) {
		return new ArgType<>(name, this::getByName).tabStream(c -> enchants.keySet().stream());
	}
	
}
