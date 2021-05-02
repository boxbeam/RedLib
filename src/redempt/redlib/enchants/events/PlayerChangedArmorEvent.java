package redempt.redlib.enchants.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.RedLib;
import redempt.redlib.misc.Task;

/**
 * Called when a player changes any piece of armor
 * @author Redempt
 */
public class PlayerChangedArmorEvent extends PlayerEvent {
	
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
		Task.syncRepeating(RedLib.getInstance(), () -> Bukkit.getOnlinePlayers().forEach(PlayerChangedArmorEvent::check), 1, 1);
	}
	
	private static void check(Player player) {
		ItemStack[] armor = player.getInventory().getArmorContents().clone();
		Task.syncDelayed(RedLib.getInstance(), () -> {
			ItemStack[] newArmor = player.getInventory().getArmorContents();
			for (int i = 0; i < armor.length; i++) {
				if (armor[i] == null && newArmor[i] == null) {
					continue;
				}
				if ((armor[i] == null) || (newArmor[i] == null) || !armor[i].equals(newArmor[i])) {
					Bukkit.getPluginManager().callEvent(new PlayerChangedArmorEvent(player, armor, newArmor));
				}
			}
		}, 1);
	}

	private ItemStack[] previous;
	private ItemStack[] current;
	
	/**
	 * Constructs a new PlayerChangedArmorEvent
	 * @param player The Player who changed their armor
	 * @param previous The armor the Player was previously wearing
	 * @param current The armor the Player is now wearing
	 */
	public PlayerChangedArmorEvent(Player player, ItemStack[] previous, ItemStack[] current) {
		super(player);
		this.previous = previous;
		this.current = current;
	}
	
	/**
	 * @return The armor the Player was previously wearing
	 */
	public ItemStack[] getPreviousArmor() {
		return previous;
	}
	
	/**
	 * @return The armor the Player is now wearing
	 */
	public ItemStack[] getNewArmor() {
		return current;
	}
	

	public HandlerList getHandlers() {
		return handlers;
	}
	
}