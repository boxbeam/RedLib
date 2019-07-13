package redempt.cmdmgr2.inventorygui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class ItemButton {
	
	protected ItemStack item;
	private int slot;
	
	public ItemButton(ItemStack item) {
		this.item = item;
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	protected int getSlot() {
		return slot;
	}
	
	protected void setSlot(int slot) {
		this.slot = slot;
	}
	
	public void setItem(ItemStack item) {
		this.item = item;
	}
	
	public abstract void onClick(InventoryClickEvent e);
	
}
