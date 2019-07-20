package redempt.redlib.inventorygui;

import java.util.function.Consumer;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class ItemButton {
	
	protected ItemStack item;
	private int slot;
	
	/**
	 * Create an ItemButton from the given ItemStack and listener.
	 * Useful if you, like most people, would rather use lambdas than the anonymous class definition.
	 * @param item The ItemStack to be used as this button's icon
	 * @param listener The listener which will be called whenever this button is clicked
	 * @return The ItemButton, which can be added to an InventoryGUI
	 */
	public static ItemButton create(ItemStack item, Consumer<InventoryClickEvent> listener) {
		return new ItemButton(item) {
			
			@Override
			public void onClick(InventoryClickEvent e) {
				listener.accept(e);
			}
			
		};
	}
	
	/**
	 * Create a new ItemButton with the given ItemStack as the icon
	 * @param item The ItemStack to be used as the icon
	 */
	public ItemButton(ItemStack item) {
		this.item = item;
	}
	
	/**
	 * Get the ItemStack representing the icon for this button
	 * @return The ItemStack
	 */
	public ItemStack getItem() {
		return item;
	}
	
	protected int getSlot() {
		return slot;
	}
	
	protected void setSlot(int slot) {
		this.slot = slot;
	}
	
	/**
	 * Update the item of this button. Does not refresh the InventoryGUI; you must call {@link InventoryGUI#update()} for this change to be reflected in the GUI.
	 * @param item The item to become the icon for this button
	 */
	public void setItem(ItemStack item) {
		this.item = item;
	}
	
	public abstract void onClick(InventoryClickEvent e);
	
}
