package redempt.redlib.enchants.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.RedLib;
import redempt.redlib.misc.Task;

/**
 * Called when a player changes the item they are holding, or a property of their held item changes
 * @author Redempt
 */
public class PlayerChangedHeldItemEvent extends PlayerEvent {
	
	private static HandlerList handlers = new HandlerList();
	private static boolean registered = false;
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		Task.syncRepeating(RedLib.getInstance(), () -> Bukkit.getOnlinePlayers().forEach(PlayerChangedHeldItemEvent::check), 1, 1);
	}
	
	private static void check(Player player) {
		ItemStack held = player.getItemInHand();
		Task.syncDelayed(RedLib.getInstance(), () -> {
			ItemStack now = player.getItemInHand();
			if ((held == null && now != null) || !held.equals(now)) {
				Bukkit.getPluginManager().callEvent(new PlayerChangedHeldItemEvent(player, held, now));
			}
		}, 1);
	}
	
	private ItemStack oldItem;
	private ItemStack newItem;
	private Player player;
	
	/**
	 * Constructs a new PlayerChangedHeldItemEvent
	 * @param player The Player who changed their held item
	 * @param oldItem The item they were previously holding
	 * @param newItem The item they are now holding
	 */
	public PlayerChangedHeldItemEvent(Player player, ItemStack oldItem, ItemStack newItem) {
		this.player = player;
		this.oldItem = oldItem;
		this.newItem = newItem;
	}
	
	/**
	 * @return The item the player was holding previously
	 */
	public ItemStack getPreviousItem() {
		return oldItem;
	}
	
	/**
	 * @return The item the player is now holding
	 */
	public ItemStack getNewItem() {
		return newItem;
	}
	
	/**
	 * @return The player
	 */
	public Player getPlayer() {
		return player;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
}
