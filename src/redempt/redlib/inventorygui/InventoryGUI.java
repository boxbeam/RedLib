package redempt.redlib.inventorygui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.RedLib;
import redempt.redlib.itemutils.ItemBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Redempt
 */
public class InventoryGUI implements Listener {
	
	/**
	 * A gray stained glass pane with no name. Good for filling empty slots in GUIs.
	 */
	public static final ItemStack FILLER;
	
	static {
		if (RedLib.MID_VERSION >= 13) {
			FILLER = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ");
		} else {
			FILLER = new ItemBuilder(Material.valueOf("STAINED_GLASS_PANE")).setDurability(7).setName(" ");
		}
	}
	
	private final Inventory inventory;
	private Set<Integer> openSlots = new LinkedHashSet<>();
	private Runnable onDestroy;
	private BiConsumer<InventoryClickEvent, List<Integer>> onClickOpenSlot = (e, i) -> {};
	private Consumer<InventoryDragEvent> onDragOpenSlot = e -> {};
	private Map<Integer, ItemButton> buttons = new HashMap<>();
	
	private boolean returnItems = true;
	private boolean destroyOnClose = true;
	
	/**
	 * Creates a new GUI from an inventory
	 *
	 * @param inventory The inventory to create a GUI from
	 */
	public InventoryGUI(Inventory inventory) {
		this.inventory = inventory;
		Bukkit.getPluginManager().registerEvents(this, RedLib.getInstance());
	}
	
	/**
	 * Creates a new GUI, instantiating a new inventory with the given size and name
	 *
	 * @param size The size of the inventory
	 * @param name The name of the inventory
	 */
	public InventoryGUI(int size, String name) {
		this(Bukkit.createInventory(null, size, name));
	}
	
	/**
	 * Gets the inventory this GUI is wrapping
	 *
	 * @return The wrapped inventory
	 */
	public Inventory getInventory() {
		return inventory;
	}
	
	/**
	 * Gets the inventory size
	 *
	 * @return The inventory size
	 */
	public int getSize() {
		return this.getInventory().getSize();
	}

	/**
	 * Add a button to the GUI in the given slot
	 *
	 * @param button The button to be added
	 * @param slot   The slot to add the button to
	 */
	public void addButton(ItemButton button, int slot) {
		button.setSlot(slot);
		inventory.setItem(slot, button.getItem());
		buttons.put(slot, button);
	}
	
	/**
	 * Add a button to the GUI in the given slot
	 *
	 * @param button The button to be added
	 * @param slot   The slot to add the button to
	 */
	public void addButton(int slot, ItemButton button) {
		addButton(button, slot);
	}
	
	/**
	 * Add a button at the given position in the inventory
	 *
	 * @param button The button to be added
	 * @param x      The X position to add the button at
	 * @param y      The Y position to add the button at
	 */
	public void addButton(ItemButton button, int x, int y) {
		int slot = x + (y * 9);
		addButton(button, slot);
	}
	
	/**
	 * Fill a section of the inventory with the given item
	 *
	 * @param start The starting index to fill from, inclusive
	 * @param end   The ending index to fill to, exclusive
	 * @param item  The item to set in these slots
	 */
	public void fill(int start, int end, ItemStack item) {
		for (int i = start; i < end; i++) {
			inventory.setItem(i, item == null ? null : item.clone());
		}
	}
	
	/**
	 * Fill a section of the inventory with the given item
	 *
	 * @param x1   The X position to fill from, inclusive
	 * @param y1   The Y position to fill from, inclusive
	 * @param x2   The X position to fill to, exclusive
	 * @param y2   The Y position to fill to, exclusive
	 * @param item The item to set in these slots
	 */
	public void fill(int x1, int y1, int x2, int y2, ItemStack item) {
		for (int x = x1; x < x2; x++) {
			for (int y = y1; y < y2; y++) {
				inventory.setItem(x + (y * 9), item == null ? null : item.clone());
			}
		}
	}
	
	/**
	 * Remove a button from the inventory
	 *
	 * @param button The button to be removed
	 */
	public void removeButton(ItemButton button) {
		inventory.setItem(button.getSlot(), new ItemStack(Material.AIR));
		buttons.remove(button.getSlot());
	}
	
	/**
	 * @return All the ItemButtons in this GUI
	 */
	public List<ItemButton> getButtons() {
		return new ArrayList<>(buttons.values());
	}
	
	/**
	 * Gets the ItemButton in a given slot
	 * @param slot The slot the button is in
	 * @return The ItemButton, or null if there is no button in that slot
	 */
	public ItemButton getButton(int slot) {
		return buttons.get(slot);
	}
	
	/**
	 * Clears a single slot, removing a button if it is present
	 * @param slot The slot to clear
	 */
	public void clearSlot(int slot) {
		ItemButton button = buttons.get(slot);
		if (button != null) {
			removeButton(button);
			return;
		}
		inventory.setItem(slot, new ItemStack(Material.AIR));
	}
	
	/**
	 * Refresh the inventory.
	 */
	public void update() {
		for (ItemButton button : buttons.values()) {
			inventory.setItem(button.getSlot(), button.getItem());
		}
	}
	
	/**
	 * Opens a slot so that items can be placed in it
	 *
	 * @param slot The slot to open
	 */
	public void openSlot(int slot) {
		openSlots.add(slot);
	}
	
	/**
	 * Opens slots so that items can be placed in them
	 *
	 * @param start The start of the open slot section, inclusive
	 * @param end   The end of the open slot section, exclusive
	 */
	public void openSlots(int start, int end) {
		for (int i = start; i < end; i++) {
			openSlots.add(i);
		}
	}
	
	/**
	 * Opens slots so that items can be placed in them
	 *
	 * @param x1 The x position to open from, inclusive
	 * @param y1 The y position to open from, inclusive
	 * @param x2 The x position to open to, exclusive
	 * @param y2 The y position to open to, exclusive
	 */
	public void openSlots(int x1, int y1, int x2, int y2) {
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++) {
				openSlots.add(y * 9 + x);
			}
		}
	}
	
	/**
	 * Closes a slot so that items can't be placed in it
	 *
	 * @param slot The slot to open
	 */
	public void closeSlot(int slot) {
		openSlots.remove(slot);
	}
	
	/**
	 * Closes slots so that items can't be placed in them
	 *
	 * @param start The start of the closed slot section, inclusive
	 * @param end   The end of the open closed section, exclusive
	 */
	public void closeSlots(int start, int end) {
		for (int i = start; i < end; i++) {
			openSlots.remove(i);
		}
	}
	
	/**
	 * Closes slots so that items can't be placed in them
	 *
	 * @param x1 The x position to close from, inclusive
	 * @param y1 The y position to close from, inclusive
	 * @param x2 The x position to close to, exclusive
	 * @param y2 The y position to close to, exclusive
	 */
	public void closeSlots(int x1, int y1, int x2, int y2) {
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++) {
				openSlots.remove(y * 9 + x);
			}
		}
	}
	
	/**
	 * Gets the open slots
	 *
	 * @return The set of open slots
	 */
	public Set<Integer> getOpenSlots() {
		return openSlots;
	}
	
	/**
	 * Opens this GUI for a player
	 *
	 * @param player The player to open this GUI for
	 */
	public void open(Player player) {
		player.openInventory(inventory);
	}
	
	/**
	 * Returns whether or not items in open slots are returned to the player when this inventory is destroyed
	 *
	 * @return Whether or not items in open slots are returned to the player when this inventory is destroyed
	 */
	public boolean returnsItems() {
		return returnItems;
	}
	
	/**
	 * Sets whether items in open slots are returned to the player when this inventory is destroyed
	 *
	 * @param returnItems Whether items in open slots should be returned to the player when this inventory is destroyed
	 */
	public void setReturnsItems(boolean returnItems) {
		this.returnItems = returnItems;
	}
	
	/**
	 * Returns whether this GUI is destroyed when it has been closed by all viewers
	 *
	 * @return Whether this GUI is destroyed when it has been closed by all viewers
	 */
	public boolean destroysOnClose() {
		return destroyOnClose;
	}
	
	/**
	 * Sets whether this GUI is destroyed when it has been closed by all viewers
	 *
	 * @param destroyOnClose Whether this GUI is destroyed when it has been closed by all viewers
	 */
	public void setDestroyOnClose(boolean destroyOnClose) {
		this.destroyOnClose = destroyOnClose;
	}
	
	/**
	 * Sets a callback to be run when this GUI is destroyed
	 *
	 * @param onDestroy The callback
	 */
	public void setOnDestroy(Runnable onDestroy) {
		this.onDestroy = onDestroy;
	}
	
	/**
	 * Sets the handler for when an open slot is clicked
	 *
	 * @param handler The handler for when an open slot is clicked
	 */
	public void setOnClickOpenSlot(Consumer<InventoryClickEvent> handler) {
		this.onClickOpenSlot = (e, i) -> handler.accept(e);
	}
	
	/**
	 * Sets the handler for when an open slot is clicked
	 *
	 * @param handler The handler for when an open slot is clicked, taking the event and list
	 *                of affected slots
	 */
	public void setOnClickOpenSlot(BiConsumer<InventoryClickEvent, List<Integer>> handler) {
		this.onClickOpenSlot = handler;
	}
	
	/**
	 * Remove this inventory as a listener and clean everything up to prevent memory leaks.
	 * Call this when the GUI is no longer being used.
	 *
	 * @param lastViewer The last Player who was viewing this GUI, to have the items returned to them.
	 */
	public void destroy(Player lastViewer) {
		if (onDestroy != null) {
			onDestroy.run();
		}
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
	
	/**
	 * Sets the handler for when items are drag-clicked into open slots
	 *
	 * @param onDrag The handler
	 */
	public void setOnDragOpenSlot(Consumer<InventoryDragEvent> onDrag) {
		this.onDragOpenSlot = onDrag;
	}
	
	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		List<Integer> slots = e.getRawSlots().stream().filter(s -> getInventory(e.getView(), s).equals(inventory)).collect(Collectors.toList());
		if (slots.size() == 0) {
			return;
		}
		if (!openSlots.containsAll(slots)) {
			e.setCancelled(true);
			return;
		}
		onDragOpenSlot.accept(e);
	}
	
	private Inventory getInventory(InventoryView view, int rawSlot) {
		return rawSlot < view.getTopInventory().getSize() ? view.getTopInventory() : view.getBottomInventory();
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (!inventory.equals(e.getView().getTopInventory())) {
			return;
		}
		if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR && !e.getClickedInventory().equals(inventory)) {
			e.setCancelled(true);
			return;
		}
		if (!inventory.equals(e.getClickedInventory()) && e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			if (openSlots.size() > 0) {
				Map<Integer, ItemStack> slots = new HashMap<>();
				int amount = e.getCurrentItem().getAmount();
				for (int slot : openSlots) {
					if (amount <= 0) {
						break;
					}
					ItemStack item = inventory.getItem(slot);
					if (item == null) {
						int diff = Math.min(amount, e.getCurrentItem().getType().getMaxStackSize());
						amount -= diff;
						ItemStack clone = e.getCurrentItem().clone();
						clone.setAmount(diff);
						slots.put(slot, clone);
						continue;
					}
					if (e.getCurrentItem().isSimilar(item)) {
						int max = item.getType().getMaxStackSize() - item.getAmount();
						int diff = Math.min(max, e.getCurrentItem().getAmount());
						amount -= diff;
						ItemStack clone = item.clone();
						clone.setAmount(clone.getAmount() + diff);
						slots.put(slot, clone);
					}
				}
				if (slots.size() == 0) {
					return;
				}
				onClickOpenSlot.accept(e, new ArrayList<>(slots.keySet()));
				if (e.isCancelled()) {
					return;
				}
				e.setCancelled(true);
				ItemStack item = e.getCurrentItem();
				item.setAmount(amount);
				e.setCurrentItem(item);
				slots.forEach(inventory::setItem);
				Bukkit.getScheduler().scheduleSyncDelayedTask(RedLib.getInstance(), () -> {
					((Player) e.getWhoClicked()).updateInventory();
				});
				return;
			}
			e.setCancelled(true);
		}
		if (e.getInventory().equals(e.getClickedInventory())) {
			if (openSlots.contains(e.getSlot())) {
				List<Integer> list = new ArrayList<>();
				list.add(e.getSlot());
				onClickOpenSlot.accept(e, list);
				return;
			}
			e.setCancelled(true);
			ItemButton button = buttons.get(e.getSlot());
			if (button != null) {
				button.onClick(e);
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
	
}
