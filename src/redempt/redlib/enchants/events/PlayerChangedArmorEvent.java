package redempt.redlib.enchants.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.RedLib;
import redempt.redlib.misc.EventListener;

/**
 * Called when a player changes any piece of armor
 * @author Redempt
 */
public class PlayerChangedArmorEvent extends Event {
	
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
		new ArmorListener();
	}
	
	private Player player;
	private ItemStack[] previous;
	private ItemStack[] current;
	
	/**
	 * Constructs a new PlayerChangedArmorEvent
	 * @param player The Player who changed their armor
	 * @param previous The armor the Player was previously wearing
	 * @param current The armor the Player is now wearing
	 */
	public PlayerChangedArmorEvent(Player player, ItemStack[] previous, ItemStack[] current) {
		this.player = player;
		this.previous = previous;
		this.current = current;
	}
	
	/**
	 * @return The Player who changed their armor
	 */
	public Player getPlayer() {
		return player;
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
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
}
class ArmorListener implements Listener {
	
	public ArmorListener() {
		Bukkit.getPluginManager().registerEvents(this, RedLib.getInstance());
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		ItemStack[] armor = e.getWhoClicked().getInventory().getArmorContents().clone();
		Bukkit.getScheduler().scheduleSyncDelayedTask(RedLib.getInstance(), () -> {
			ItemStack[] newArmor = e.getWhoClicked().getInventory().getArmorContents();
			for (int i = 0; i < armor.length; i++) {
				if (armor[i] == null && newArmor[i] == null) {
					continue;
				}
				if ((armor[i] == null) || (newArmor[i] == null) || !armor[i].equals(newArmor[i])) {
					Bukkit.getPluginManager().callEvent(new PlayerChangedArmorEvent((Player) e.getWhoClicked(), armor, newArmor));
				}
			}
		});
	}
	
}
