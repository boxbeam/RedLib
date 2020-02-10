package redempt.redlib.inventorygui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
	private Set<Integer> openSlots = new HashSet<>();
	private Consumer<InventoryClickEvent> onClickOpenSlot = (e) -> {};
	private boolean returnItems = true;
	private boolean destroyOnClose = true;
	
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
	 * Fill a section of the inventory with the given item
	 * @param start The starting index to fill from, inclusive
	 * @param The ending index to fill to, exclusive
	 * @param item The item to set in these slots
	 */
	public void fill(int start, int end, ItemStack item) {
		for (int i = start; i < end; i++) {
			inventory.setItem(i, item.clone());
		}
	}
	
	/**
	 * Fill a section of the inventory with the given item
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
	 * Opens a slot so that items can be placed in it
	 * @param slot The slot to open
	 */
	public void openSlot(int slot) {
		openSlots.add(slot);
	}
	
	/**
	 * Opens slots so that items can be placed in them
	 * @param start The start of the open slot section, inclusive
	 * @param end The end of the open slot section, exclusive
	 */
	public void openSlots(int start, int end) {
		for (int i = start; i < end; i++) {
			openSlots.add(i);
		}
	}
	
	/**
	 * Closes a slot so that items can't be placed in it
	 * @param slot The slot to open
	 */
	public void closeSlot(int slot) {
		openSlots.remove(slot);
	}
	
	/**
	 * Closes slots so that items can't be placed in them
	 * @param start The start of the closed slot section, inclusive
	 * @param end The end of the open closed section, exclusive
	 */
	public void closeSlots(int start, int end) {
		for (int i = start; i < end; i++) {
			openSlots.remove(i);
		}
	}
	
	/**
	 * Gets the open slots
	 * @return The set of open slots
	 */
	public Set<Integer> getOpenSlots() {
		return openSlots;
	}
	
	/**
	 * Returns whether or not items in open slots are returned to the player when this inventory is destroyed
	 * @return Whether or not items in open slots are returned to the player when this inventory is destroyed
	 */
	public boolean returnsItems() {
		return returnItems;
	}
	
	/**
	 * Sets whether items in open slots are returned to the player when this inventory is destroyed
	 * @param returnItems Whether items in open slots should be returned to the player when this inventory is destroyed
	 */
	public void setReturnsItems(boolean returnItems) {
		this.returnItems = returnItems;
	}
	
	/**
	 * Returns whether this GUI is destroyed when it has been closed by all viewers
	 * @return Whether this GUI is destroyed when it has been closed by all viewers
	 */
	public boolean destroysOnClose() {
		return destroyOnClose;
	}
	
	/**
	 * Sets whether this GUI is destroyed when it has been closed by all viewers
	 * @param destroyOnClose Whether this GUI is destroyed when it has been closed by all viewers
	 */
	public void setDestroyOnClose(boolean destroyOnClose) {
		this.destroyOnClose = destroyOnClose;
	}
	
	/**
	 * Sets the handler for when an open slot is clicked
	 * @param handler The handler for when an open slot is clicked
	 */
	public void setOnClickOpenSlot(Consumer<InventoryClickEvent> handler) {
		this.onClickOpenSlot = handler;
	}
	
	/**
	 * Remove this inventory as a listener and clean everything up to prevent memory leaks.
	 * Call this when the GUI is no longer being used.
	 * @param lastViewer The last Player who was viewing this GUI, to have the items returned to them.
	 */
	public void destroy(Player lastViewer) {
		HandlerList.unregisterAll(this);
		if (returnItems && lastViewer != null) {
			for (int slot : openSlots) {
				ItemStack item = inventory.getItem(slot);
				if (item == null) {
					continue;
				}
				lastViewer.getInventory().addItem(item).values().forEach(i -> lastViewer.getWorld().dropItem(lastViewer.getLocation(), i));
			}
		}
		inventory.clear();
		buttons.clear();
	}
	
	/**
	 * Remove this inventory as a listener and clean everything up to prevent memory leaks.
	 * Call this when the GUI is no longer being used.
	 */
	public void destroy() {
		destroy(null);
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
		if (inventory.equals(e.getView().getTopInventory()) && !inventory.equals(e.getClickedInventory()) && e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			if (openSlots.size() > 0) {
				onClickOpenSlot.accept(e);
				return;
			}
		}
		if (inventory.equals(e.getClickedInventory())) {
			if (openSlots.contains(e.getSlot())) {
				onClickOpenSlot.accept(e);
				return;
			}
			e.setCancelled(true);
			for (ItemButton button : buttons) {
				if (e.getSlot() == button.getSlot()) {
					button.onClick(e);
					break;
				}
			}
		}
	}
	
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if (e.getInventory().equals(inventory) && destroyOnClose) {
			if (e.getViewers().size() <= 1) {
				destroy((Player) e.getPlayer());
			}
		}
	}
	
	/**
	 * Gets the state of the GUI, which can be restored later
	 * @return The state of this GUI
	 */
	public GUIState getState() {
		return new GUIState(buttons, openSlots, inventory.getContents(), this);
	}
	
	public static class GUIState {
		
		private List<ItemButton> buttons;
		private Set<Integer> openSlots;
		private ItemStack[] contents;
		private InventoryGUI gui;
		
		private GUIState(List<ItemButton> buttons, Set<Integer> openSlots, ItemStack[] contents, InventoryGUI gui) {
			this.buttons = new ArrayList<>(buttons);
			this.openSlots = new HashSet<>(openSlots);
			this.contents = contents.clone();
			this.gui = gui;
		}
		
		/**
		 * Restore the GUI to this state
		 */
		public void restore() {
			gui.clear();
			gui.buttons = new ArrayList<>(buttons);
			gui.openSlots = new HashSet<>(openSlots);
			gui.inventory.setContents(contents.clone());
		}
		
	}
	
}
