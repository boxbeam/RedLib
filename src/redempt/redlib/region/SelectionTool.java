package redempt.redlib.region;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import redempt.redlib.RedLib;
import redempt.redlib.commandmanager.Messages;
import redempt.redlib.itemutils.ItemUtils;
import redempt.redlib.misc.Path;

/**
 * A tool which can be given to players and used to select a Region, or just any two points
 * @author Redempt
 *
 */
public class SelectionTool implements Listener {
	
	private ItemStack item;
	private Map<UUID, Location[]> selections = new HashMap<>();
	
	/**
	 * Create a SelectionTool with the given item
	 * @param item The item to use
	 */
	public SelectionTool(ItemStack item) {
		this.item = item;
		Bukkit.getPluginManager().registerEvents(this, RedLib.getInstance());
	}
	
	@EventHandler
	public void onClick(PlayerInteractEvent e) {
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getItem() == null) {
			return;
		}
		if (!ItemUtils.compare(e.getItem(), item)) {
			return;
		}
		Location[] locations = selections.getOrDefault(e.getPlayer().getUniqueId(), new Location[2]);
		if (locations[0] != null && locations[1] != null) {
			locations[0] = null;
			locations[1] = null;
		}
		if (locations[0] == null) {
			locations[0] = e.getClickedBlock().getLocation();
			e.getPlayer().sendMessage(Messages.msg("firstLocationSet"));
		} else if (locations[1] == null) {
			locations[1] = e.getClickedBlock().getLocation();
			e.getPlayer().sendMessage(Messages.msg("secondLocationSet"));
		}
		selections.put(e.getPlayer().getUniqueId(), locations);
	}
	
	/**
	 * Gets the item used by this SelectionTool
	 * @return The item
	 */
	public ItemStack getItem() {
		return item.clone();
	}
	
	/**
	 * Get the locations selected by the given player
	 * @param uuid The UUID of the player
	 * @return The locations selected by the given player
	 */
	public Location[] getLocations(UUID uuid) {
		return selections.getOrDefault(uuid, new Location[2]);
	}
	
	/**
	 * Creates and returns a Region based on the locations selected by the player
	 * @param uuid The UUID of the player
	 * @return The Region selected by the player, or null if the player has not selected 2 locations
	 */
	public Region getRegion(UUID uuid) {
		Location[] locations = selections.get(uuid);
		if (locations[0] == null || locations[1] == null) {
			return null;
		}
		Region region = new Region(locations[0], locations[1]);
		region.expand(1, 0, 1, 0, 1, 0);
		return region;
	}
	
	/**
	 * Creates a path of Locations, one block apart, based on the locations selected by the player
	 * @param uuid The UUID of the player
	 * @return The Path selected by the player, or null if the player has not selected 2 locations
	 */
	public List<Location> getPath(UUID uuid) {
		Location[] locations = selections.get(uuid);
		if (locations[0] == null || locations[1] == null) {
			return null;
		}
		return Path.getPath(locations[0], locations[1]);
	}
	
}
