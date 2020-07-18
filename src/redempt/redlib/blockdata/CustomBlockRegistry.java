package redempt.redlib.blockdata;

import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import redempt.redlib.misc.EventListener;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads and registers CustomBlockTypes
 */
public class CustomBlockRegistry {
	
	private BlockDataManager manager;
	private Map<String, CustomBlockType<?>> types = new HashMap<>();
	private Plugin plugin;
	
	/**
	 * Construct a CustomBlockRegistry without passing a plugin. Use this constructor if you plan to use
	 * {@link CustomBlockRegistry#registerAll(Plugin)}.
	 * @param manager The BlockDataManager to use for managing block data
	 */
	public CustomBlockRegistry(BlockDataManager manager) {
		this.manager = manager;
	}
	
	/**
	 * Construct a CustomBlockRegistry, passing a plugin. Use this constructor if you plan to use
	 * {@link CustomBlockRegistry#register(CustomBlockType)}
	 * @param manager The BlockDataManager to use for managing block data
	 * @param plugin The Plugin to register events with
	 */
	public CustomBlockRegistry(BlockDataManager manager, Plugin plugin) {
		this.manager = manager;
		this.plugin = plugin;
		new EventListener<>(plugin, PlayerInteractEvent.class, e -> {
			Block block = e.getClickedBlock();
			if (block == null) {
				return;
			}
			CustomBlock cb = getCustomBlock(block);
			if (cb != null) {
				cb.click(e);
			}
		});
	}
	
	/**
	 * @return The BlockDataManager managing block data for this CustomBlockRegistry
	 */
	public BlockDataManager getManager() {
		return manager;
	}
	
	/**
	 * Looks through all classes in a Plugin, registering all classes which extend CustomBlockType that are not
	 * interfaces or abstract. Each one must have a constructor that takes no arguments in order to be registered.
	 * @param plugin The Plugin to register the CustomBlockTypes with
	 */
	public void registerAll(Plugin plugin) {
		if (this.plugin != null) {
			throw new IllegalStateException("This CustomBlockRegistry has already been registered! Try calling the constructor that doesn't require a Plugin.");
		}
		this.plugin = plugin;
		try {
			ClassLoader loader = plugin.getClass().getClassLoader();
			JarFile jar = new JarFile(new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (entry.isDirectory() || !name.endsWith(".class")) {
					continue;
				}
				name = name.replace("/", ".").substring(0, name.length() - 6);
				Class<?> clazz = Class.forName(name, true, loader);
				if (!CustomBlockType.class.isAssignableFrom(clazz) || clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
					continue;
				}
				try {
					Constructor<?> constructor = clazz.getConstructor();
					CustomBlockType type = (CustomBlockType) constructor.newInstance();
					register(type);
				} catch (NoSuchMethodException e) {
					throw new IllegalStateException("Class " + clazz.getName() + " does not have a default constructor and could not be loaded");
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		new EventListener<>(plugin, PlayerInteractEvent.class, e -> {
			Block block = e.getClickedBlock();
			if (block == null) {
				return;
			}
			CustomBlock cb = getCustomBlock(block);
			if (cb != null) {
				cb.click(e);
			}
		});
	}
	
	/**
	 * Registers a single CustomBlockType into this CustomBlockRegistry
	 * @param type The CustomBlockType to register
	 */
	public void register(CustomBlockType<?> type) {
		type.register(manager, plugin);
		types.put(type.getName(), type);
	}
	
	/**
	 * Gets a CustomBlockType by name
	 * @param name The name of the CustomBlockType
	 * @return The CustomBlockType with the given name
	 */
	public CustomBlockType<?> getByName(String name) {
		return types.get(name);
	}
	
	/**
	 * @return A collection of all CustomBlockTypes in this registry
	 */
	public Collection<CustomBlockType<?>> getTypes() {
		return types.values();
	}
	
	/**
	 * Gets a CustomBlock instance with the correct CustomBlockType
	 * @param block The Block to check
	 * @return The CustomBlock, or null if it was not a custom block
	 */
	public <T extends CustomBlock> T getCustomBlock(Block block) {
		DataBlock db = manager.getExisting(block);
		if (db == null) {
			return null;
		}
		String type = db.getString("custom-type");
		CustomBlockType<?> ctype = types.get(type);
		if (ctype == null) {
			return null;
		}
		return (T) ctype.get(block);
	}
	
}
