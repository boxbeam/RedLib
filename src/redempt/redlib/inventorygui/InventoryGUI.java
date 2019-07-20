package redempt.cmdmgr2.inventorygui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import redempt.cmdmgr2.CmdMgr;

/**
 * @author Redempt
 *
 */
public class InventoryGUI implements Listener {
	
	private final Inventory inventory;
	private List<ItemButton> buttons = new ArrayList<>();
	
	/**
	 * Creates a new GUI from an inventory
	 * @param inventory The inventory to create a GUI from
	 */
	public InventoryGUI(Inventory inventory) {
		this.inventory = inventory;
		Bukkit.getPluginManager().registerEvents(this, CmdMgr.plugin);
	}
	
	/**
	 * Gets the inventory this GUI is wrapping
	 * @return The wrapped inventory
	 */
	public Inventory getInventory() {
		return inventory;
	}
	
	/**
	 * Add a button to the GUI in the given slot
	 * @param slot The slot to add the button to
	 * @param button The button to be added
	 */
	public void addButton(int slot, ItemButton button) {
		button.setSlot(slot);
		buttons.add(button);
		inventory.setItem(slot, button.getItem());
	}
	
	/**
	 * Remove a button from the inventory
	 * @param button The button to be removed
	 */
	public void removeButton(ItemButton button) {
		buttons.remove(button);
		inventory.remove(button.getItem());
	}
	
	/**
	 * Refresh the inventory.
	 */
	public void update() {
		for (ItemButton button : buttons) {
			inventory.setItem(button.getSlot(), button.getItem());
		}
	}
	
	/**
	 * Remove this inventory as a listener and clean everything up to prevent memory leaks.
	 * Call this when the GUI is no longer being used.
	 */
	public void destroy() {
		HandlerList.unregisterAll(this);
		inventory.clear();
		buttons.clear();
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (e.getInventory().equals(inventory)) {
			e.setCancelled(true);
			for (ItemButton button : buttons) {
				if (button.getItem().equals(e.getCurrentItem()) && e.getSlot() == button.getSlot()) {
					button.onClick(e);
					break;
				}
			}
		}
	}
	
}
