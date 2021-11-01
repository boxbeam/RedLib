package redempt.redlib.inventorygui;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

/**
 * A panel in an InventoryGUI which can be used to paginate items and buttons
 * @author Redempt
 */
public class PaginationPanel {
	
	private InventoryGUI gui;
	private int page = 1;
	private List<IntConsumer> buttons = new ArrayList<>();
	private Map<Object, IntConsumer> items = new HashMap<>();
	private Set<Integer> slots = new TreeSet<>();
	private Runnable onUpdate = () -> {};
	
	/**
	 * Constructs a PaginationPanel to work on a given InventoryGUI
	 * @param gui The InventoryGUI to paginate
	 */
	public PaginationPanel(InventoryGUI gui) {
		this.gui = gui;
	}
	
	/**
	 * Sets a task to be run whenever the page updates, can be used to update a page indicator or similar
	 * @param onUpdate The task to be run on update
	 */
	public void setOnUpdate(Runnable onUpdate) {
		this.onUpdate = onUpdate;
	}
	
	private void addPagedButton0(ItemButton button) {
		IntConsumer setter = i -> gui.addButton(button, i);
		items.put(button, setter);
		buttons.add(setter);
	}
	
	/**
	 * Adds a paged button to the panel
	 * @param button The button to add
	 */
	public void addPagedButton(ItemButton button) {
		addPagedButton0(button);
		updatePage();
	}
	
	private void addPagedItem0(ItemStack item) {
		IntConsumer setter = i -> gui.getInventory().setItem(i, item);
		items.put(item, setter);
		buttons.add(setter);
	}
	
	/**
	 * Adds a paged item to the panel
	 * @param item The item to add
	 */
	public void addPagedItem(ItemStack item) {
		addPagedItem0(item);
		updatePage();
	}
	
	/**
	 * Adds multiple buttons to the paged panel
	 * @param buttons The buttons to add
	 */
	public void addPagedButtons(Iterable<ItemButton> buttons) {
		for (ItemButton button : buttons) {
			addPagedButton0(button);
		}
		updatePage();
	}
	
	/**
	 * Adds multiple items to the paged panel
	 * @param items The items to add
	 */
	public void addPagedItems(Iterable<ItemStack> items) {
		for (ItemStack item : items) {
			addPagedItem0(item);
		}
		updatePage();
	}
	
	/**
	 * Removes an item from the paged panel.
	 * @param item The item to remove
	 */
	public void removePagedItem(ItemStack item) {
		buttons.remove(items.remove(item));
		updatePage();
	}
	
	/**
	 * Removes a button from the paged panel.
	 * @param button The button to remove
	 */
	public void removePagedButton(ItemButton button) {
		buttons.remove(items.remove(button));
		updatePage();
	}
	
	/**
	 * Removes multiple items from the paged panel
	 * @param items The items to remove
	 */
	public void removePagedItems(Iterable<ItemStack> items) {
		for (ItemStack item : items) {
			buttons.remove(this.items.remove(item));
		}
		updatePage();
	}
	
	/**
	 * Removes multiple buttons from the paged panel
	 * @param buttons The buttons to remove
	 */
	public void removePagedButtons(Iterable<ItemButton> buttons) {
		for (ItemButton button : buttons) {
			this.buttons.remove(items.remove(button));
		}
		updatePage();
	}
	
	/**
	 * @return The page this panel is currently on
	 */
	public int getPage() {
		return page;
	}
	
	/**
	 * @return The max number of elements displayed on each page
	 */
	public int getPageSize() {
		return slots.size();
	}
	
	/**
	 * @return The maximum page number of this panel with the current number of elements
	 */
	public int getMaxPage() {
		return (buttons.size() / Math.max(1, slots.size())) + 1;
	}
	
	/**
	 * Adds a slot which will be used to display elements
	 * @param slot The slot to add
	 */
	public void addSlot(int slot) {
		slots.add(slot);
		updatePage();
	}
	
	/**
	 * Adds a range of slots which will be used to display elements
	 * @param start The start index of slots to add, inclusive
	 * @param end The end index of slots to add, exclusive
	 */
	public void addSlots(int start, int end) {
		for (int i = start; i < end; i++) {
			slots.add(i);
		}
		updatePage();
	}
	
	/**
	 * Adds a rectangular area of slots which will be used to display elements
	 * @param x1 The starting X of slots to add, inclusive
	 * @param y1 The starting Y of slots to add, inclusive
	 * @param x2 The ending X of slots to add, exclusive
	 * @param y2 The ending Y of slots to add, exclusive
	 */
	public void addSlots(int x1, int y1, int x2, int y2) {
		for (int x = x1; x < x2; x++) {
			for (int y = y1; y < y2; y++) {
				slots.add(y * 9 + y);
			}
		}
		updatePage();
	}
	
	/**
	 * Removes a slot which will be used to display elements
	 * @param slot The slot to remove
	 */
	public void removeSlot(int slot) {
		slots.forEach(gui::clearSlot);
		slots.remove(slot);
		updatePage();
	}
	
	/**
	 * Removes a range of slots which will be used to display elements
	 * @param start The start index of slots to remove, inclusive
	 * @param end The end index of slots to remove, exclusive
	 */
	public void removeSlots(int start, int end) {
		slots.forEach(gui::clearSlot);
		for (int i = start; i < end; i++) {
			slots.remove(i);
		}
		updatePage();
	}
	
	/**
	 * Removes a rectangular area of slots which will be used to display elements
	 * @param x1 The starting X of slots to remove, inclusive
	 * @param y1 The starting Y of slots to remove, inclusive
	 * @param x2 The ending X of slots to remove, exclusive
	 * @param y2 The ending Y of slots to remove, exclusive
	 */
	public void removeSlots(int x1, int y1, int x2, int y2) {
		slots.forEach(gui::clearSlot);
		for (int x = x1; x < x2; x++) {
			for (int y = y1; y < y2; y++) {
				slots.remove(y * 9 + y);
			}
		}
		updatePage();
	}
	
	/**
	 * Updates the elements displayed on the current page
	 */
	public void updatePage() {
		if (getPageSize() == 0 || buttons.size() == 0) {
			onUpdate.run();
			return;
		}
		slots.forEach(gui::clearSlot);
		int start = (page - 1) * getPageSize();
		int end = Math.min(buttons.size(), page * getPageSize());
		Iterator<Integer> iter = slots.iterator();
		for (int i = start; i < end; i++) {
			buttons.get(i).accept(iter.next());
		}
		onUpdate.run();
	}
	
	/**
	 * Sets the page of this panel
	 * @param page The page to set
	 */
	public void setPage(int page) {
		if (page < 1 || page > getMaxPage()) {
			throw new IllegalArgumentException("Invalid page: " + page);
		}
		this.page = page;
		updatePage();
	}
	
	/**
	 * Removes all items and buttons from the panel
	 */
	public void clear() {
		buttons.clear();
		items.clear();
		updatePage();
	}
	
	/**
	 * @return All ItemStacks added to this panel
	 */
	public List<ItemStack> getItems() {
		return items.keySet().stream().filter(ItemStack.class::isInstance).map(ItemStack.class::cast).collect(Collectors.toList());
	}
	
	/**
	 * @return All ItemButtons added to this panel
	 */
	public List<ItemButton> getButtons() {
		return items.keySet().stream().filter(ItemButton.class::isInstance).map(ItemButton.class::cast).collect(Collectors.toList());
	}
	
	/**
	 * Navigates to the next page, if there is one
	 */
	public void nextPage() {
		page = Math.min(page + 1, getMaxPage());
		updatePage();
	}
	
	/**
	 * Navigates to the previous page, if there is one
	 */
	public void prevPage() {
		page = Math.max(1, page - 1);
		updatePage();
	}
	
}
