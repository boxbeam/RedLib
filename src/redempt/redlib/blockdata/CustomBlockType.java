package redempt.redlib.blockdata;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import redempt.redlib.RedLib;
import redempt.redlib.blockdata.events.DataBlockBreakEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class CustomBlockType<T extends CustomBlock> implements Listener {
	
	private BlockDataManager manager;
	private String typeName;
	private Plugin plugin;
	
	public CustomBlockType(String typeName) {
		this.typeName = typeName;
	}
	
	public abstract boolean itemMatches(ItemStack item);
	public abstract void place(Player player, ItemStack item, DataBlock block);
	public abstract ItemStack getItem(DataBlock block);
	
	public final void register(BlockDataManager manager, Plugin plugin) {
		this.manager = manager;
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public boolean typeMatches(Material material) {
		return true;
	}
	
	public String getName() {
		return typeName;
	}
	
	public T getCustom(CustomBlockType type, DataBlock db) {
		return null;
	}
	
	public final T get(Block block) {
		DataBlock db = manager.getExisting(block);
		if (db == null || !db.getString("custom-type").equals(typeName)) {
			return null;
		}
		CustomBlock custom = getCustom(this, db);
		if (custom != null) {
			return (T) custom;
		}
		return (T) new CustomBlock(this, db);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent e) {
		if (typeMatches(e.getBlock().getType()) && itemMatches(e.getItemInHand())) {
			DataBlock db = manager.getDataBlock(e.getBlock());
			db.set("custom-type", typeName);
			place(e.getPlayer(), e.getItemInHand(), db);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBreak(DataBlockBreakEvent e) {
		if (!typeMatches(e.getBlock().getType())) {
			return;
		}
		DataBlock db = e.getDataBlock();
		if (db == null || !db.getString("custom-type").equals(typeName)) {
			return;
		}
		ItemStack item = getItem(db);
		if (RedLib.midVersion >= 12) {
			BlockState state = e.getBlock().getState();
			e.getParent().setDropItems(false);
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
				List<Item> drops = new ArrayList<>();
				drops.add(e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0.5, 0.5), item));
				BlockDropItemEvent event = new BlockDropItemEvent(e.getBlock(), state, e.getPlayer(), drops);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
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
