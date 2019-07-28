package redempt.redlib.inventorygui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import redempt.redlib.RedLib;

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
		Bukkit.getPluginManager().registerEvents(this, RedLib.plugin);
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
	 * @param button The button to be added
	 * @param slot The slot to add the button to
	 */
	public void addButton(ItemButton button, int slot) {
		button.setSlot(slot);
		buttons.add(button);
		inventory.setItem(slot, button.getItem());
	}
	
	/**
	 * Add a button at the given position in the inventory
	 * @param button The button to be added
	 * @param x The X position to add the button at
	 * @param y The Y position to add the button at
	 */
	public void addButton(ItemButton button, int x, int y) {
		int slot = x + (y * 9);
		addButton(button, slot);
	}
	
	/**
	 * Fill a section of the inventory with the given 
	 * @param x1 The X position to fill from, inclusive
	 * @param y1 The Y position to fill from, inclusive
	 * @param x2 The X position to fill to, exclusive
	 * @param y2 The Y position to fill to, exclusive
	 * @param item The item to set in these slots
	 */
	public void fill(int x1, int y1, int x2, int y2, ItemStack item) {
		for (int x = x1; x < x2; x++) {
			for (int y = y1; y < y2; y++) {
				inventory.setItem(x + (y * 9), item.clone());
			}
		}
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
	
	/**
	 * Clears the inventory and its buttons
	 */
	public void clear() {
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
	
	/**
	 * Gets the state of the GUI, which can be restored later
	 * @return The state of this GUI
	 */
	public GUIState getState() {
		return new GUIState(buttons, inventory.getContents(), this);
	}
	
	public static class GUIState {
		
		private List<ItemButton> buttons;
		private ItemStack[] contents;
		private InventoryGUI gui;
		
		private GUIState(List<ItemButton> buttons, ItemStack[] contents, InventoryGUI gui) {
			this.buttons = new ArrayList<>(buttons);
			this.contents = contents.clone();
			this.gui = gui;
		}
		
		/**
		 * Restore the GUI to this state
		 */
		public void restore() {
			gui.clear();
			gui.buttons = new ArrayList<>(buttons);
			gui.inventory.setContents(contents.clone());
		}
		
	}
	
}
