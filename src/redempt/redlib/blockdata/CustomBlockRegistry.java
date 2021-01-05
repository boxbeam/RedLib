package redempt.redlib.blockdata;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import redempt.redlib.RedLib;
import redempt.redlib.blockdata.events.CustomBlockPlaceEvent;
import redempt.redlib.blockdata.events.DataBlockDestroyEvent;
import redempt.redlib.blockdata.events.DataBlockDestroyEvent.DestroyCause;
import redempt.redlib.misc.Path;
import redempt.redlib.nms.NMSHelper;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads and registers CustomBlockTypes
 * @author Redempt
 */
public class CustomBlockRegistry implements Listener {
	
	private BlockDataManager manager;
	private Map<String, CustomBlockType<?>> types = new HashMap<>();
	private Map<String, CustomBlockType<?>> byItemName = new HashMap<>();
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
		Bukkit.getPluginManager().registerEvents(this, plugin);
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
					CustomBlockType<?> type = (CustomBlockType<?>) constructor.newInstance();
					register(type);
				} catch (NoSuchMethodException e) {
					throw new IllegalStateException("Class " + clazz.getName() + " does not have a default constructor and could not be loaded");
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Registers a single CustomBlockType into this CustomBlockRegistry
	 * @param type The CustomBlockType to register
	 */
	public void register(CustomBlockType<?> type) {
		String name = type.getBaseItemName();
		byItemName.put(name, type);
		type.register(manager);
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
	 * @param <T> The type of the CustomBlock
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
	
	@EventHandler
	public void onClick(PlayerInteractEvent e) {
		Block block = e.getClickedBlock();
		if (block == null) {
			return;
		}
		CustomBlock cb = getCustomBlock(block);
		if (cb != null) {
			cb.click(e);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public <T extends CustomBlock> void onPlace(BlockPlaceEvent e) {
		DataBlock db = manager.getExisting(e.getBlock());
		if (db != null) {
			db.remove();
		}
		ItemStack item = e.getItemInHand();
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
			return;
		}
		String name = item.getItemMeta().getDisplayName();
		CustomBlockType<T> type = (CustomBlockType<T>) byItemName.get(name);
		if (type == null) {
			return;
		}
		if (type.typeMatches(e.getBlock().getType()) && type.itemMatches(e.getItemInHand())) {
			CustomBlockPlaceEvent place = new CustomBlockPlaceEvent(e.getBlock(), e.getItemInHand(), type, e.getPlayer());
			Bukkit.getPluginManager().callEvent(place);
			if (place.isCancelled()) {
				e.setCancelled(true);
				return;
			}
			type.place(e.getPlayer(), e.getItemInHand(), type.initialize(e.getBlock()));
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public <T extends CustomBlock> void onBreak(DataBlockDestroyEvent e) {
		if (e.getCause() == DestroyCause.PLAYER) {
			if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
				return;
			}
			CustomBlock cb = getCustomBlock(e.getBlock());
			if (cb == null) {
				return;
			}
			CustomBlockType<T> type = (CustomBlockType<T>) cb.getType();
			DataBlock db = e.getDataBlock();
			ItemStack item = type.getItem(type.get(db));
			if (RedLib.MID_VERSION >= 12) {
				BlockBreakEvent parent = (BlockBreakEvent) e.getParent();
				if (!parent.isDropItems()) {
					return;
				}
				BlockState state = e.getBlock().getState();
				parent.setDropItems(false);
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
					List<Item> drops = new ArrayList<>();
					drops.add(e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), item));
					Event event = (Event) NMSHelper.getClass("org.bukkit.event.block.BlockDropItemEvent").getInstance(e.getBlock(), state, e.getPlayer(), drops).getObject();
					Bukkit.getPluginManager().callEvent(event);
					if (((Cancellable) event).isCancelled()) {
						drops.get(0).remove();
					}
				});
			} else {
				Collection<ItemStack> drops = e.getBlock().getDrops(e.getPlayer().getItemInHand());
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
					e.getBlock().getWorld().getNearbyEntities(e.getBlock().getLocation().add(0.5, 0.5, 0.5), 1, 1, 1).stream()
							.filter(en -> en instanceof Item && en.getTicksLived() < 2).map(en -> (Item) en)
							.filter(i -> drops.stream().anyMatch(it -> it.isSimilar(i.getItemStack())))
							.forEach(Entity::remove);
					e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0.5, 0.5), item);
				});
			}
		}
	}
	
	@EventHandler
	public <T extends CustomBlock> void onPickBlock(InventoryCreativeEvent e) {
		if (e.getCursor() == null || e.getCursor().getType() == Material.AIR
				|| e.getSlot() > 8 || e.getView().getTopInventory().getType() != InventoryType.CRAFTING
				|| e.getCursor().getAmount() != 1 || e.getAction() != InventoryAction.PLACE_ALL) {
			return;
		}
		List<Location> path = Path.getPath(e.getWhoClicked().getEyeLocation(), e.getWhoClicked().getLocation().getDirection(), 5);
		Block block = null;
		for (Location loc : path) {
			if (loc.getBlock().getType() != Material.AIR) {
				block = loc.getBlock();
				break;
			}
		}
		if (block == null) {
			return;
		}
		if (block.getType() == e.getCursor().getType()) {
			T cb = getCustomBlock(block);
			if (cb != null) {
				e.setCancelled(true);
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
					CustomBlockType<T> type = (CustomBlockType<T>) cb.getType();
					ItemStack item = type.getItem(cb);
					for (int i = 0; i < 9; i++) {
						if (item.isSimilar(e.getWhoClicked().getInventory().getItem(i))) {
							e.getWhoClicked().getInventory().setHeldItemSlot(i);
							return;
						}
					}
					e.getWhoClicked().getInventory().setItem(e.getSlot(), item);
				});
			}
		}
	}
	
}
