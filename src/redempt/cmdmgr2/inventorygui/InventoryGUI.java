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

public class InventoryGUI implements Listener {
	
	private final Inventory inventory;
	private List<ItemButton> buttons = new ArrayList<>();
	
	public InventoryGUI(Inventory inventory) {
		this.inventory = inventory;
		Bukkit.getPluginManager().registerEvents(this, CmdMgr.plugin);
	}
	
	public Inventory getInventory() {
		return inventory;
	}
	
	public void addButton(int slot, ItemButton button) {
		button.setSlot(slot);
		buttons.add(button);
		inventory.setItem(slot, button.getItem());
	}
	
	public void removeButton(ItemButton button) {
		buttons.remove(button);
		inventory.remove(button.getItem());
	}
	
	public void update() {
		for (ItemButton button : buttons) {
			inventory.setItem(button.getSlot(), button.getItem());
		}
	}
	
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
