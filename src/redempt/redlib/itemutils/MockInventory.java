package redempt.redlib.itemutils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

class MockInventory implements Inventory {
	
	private ItemStack[] items;
	private InventoryHolder holder;
	private InventoryType type;
	
	public MockInventory(ItemStack[] items, InventoryHolder holder, InventoryType type) {
		this.items = items;
		this.holder = holder;
		this.type = type;
	}
	
	@Override
	public int getSize() {
		return items.length;
	}
	
	@Override
	public int getMaxStackSize() {
		return 64;
	}
	
	@Override
	public void setMaxStackSize(int i) {
	}
	
	@Override
	public ItemStack getItem(int i) {
		return items[i];
	}
	
	@Override
	public void setItem(int i, ItemStack itemStack) {
		items[i] = itemStack;
	}
	
	@Override
	public HashMap<Integer, ItemStack> addItem(ItemStack... itemStacks) throws IllegalArgumentException {
		return null;
	}
	
	@Override
	public HashMap<Integer, ItemStack> removeItem(ItemStack... itemStacks) throws IllegalArgumentException {
		return null;
	}
	
	@Override
	public ItemStack[] getContents() {
		return items;
	}
	
	@Override
	public void setContents(ItemStack[] itemStacks) throws IllegalArgumentException {
		this.items = itemStacks;
	}
	
	@Override
	public ItemStack[] getStorageContents() {
		return items;
	}
	
	@Override
	public void setStorageContents(ItemStack[] itemStacks) throws IllegalArgumentException {
		this.items = itemStacks;
	}
	
	@Override
	public boolean contains(Material material) throws IllegalArgumentException {
		return Arrays.stream(items).anyMatch(i -> i.getType() == material);
	}
	
	@Override
	public boolean contains(ItemStack itemStack) {
		return Arrays.stream(items).anyMatch(i -> i.equals(itemStack));
	}
	
	@Override
	public boolean contains(Material material, int amount) throws IllegalArgumentException {
		return Arrays.stream(items).filter(i -> i.getType() == material).collect(Collectors.summarizingInt(ItemStack::getAmount)).getSum() >= amount;
	}
	
	@Override
	public boolean contains(ItemStack itemStack, int amount) {
		return Arrays.stream(items).filter(i -> i.equals(itemStack)).count() >= amount;
	}
	
	@Override
	public boolean containsAtLeast(ItemStack itemStack, int amount) {
		return Arrays.stream(items).filter(i -> i.equals(itemStack)).collect(Collectors.summarizingInt(ItemStack::getAmount)).getSum() >= amount;
	}
	
	@Override
	public HashMap<Integer, ? extends ItemStack> all(Material material) throws IllegalArgumentException {
		HashMap<Integer, ItemStack> items = new HashMap<>();
		for (int i = 0; i < this.items.length; i++) {
			if (this.items[i].getType() == material) {
				items.put(i, this.items[i]);
			}
		}
		return items;
	}
	
	@Override
	public HashMap<Integer, ? extends ItemStack> all(ItemStack itemStack) {
		HashMap<Integer, ItemStack> items = new HashMap<>();
		for (int i = 0; i < this.items.length; i++) {
			if (this.items[i].equals(itemStack)) {
				items.put(i, this.items[i]);
			}
		}
		return items;
	}
	
	@Override
	public int first(Material material) throws IllegalArgumentException {
		for (int i = 0; i < items.length; i++) {
			if (items[i].getType() == material) {
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public int first(ItemStack itemStack) {
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals(itemStack)) {
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public int firstEmpty() {
		for (int i = 0; i < items.length; i++) {
			if (items[i] == null || items[i].getType() == Material.AIR) {
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public void remove(Material material) throws IllegalArgumentException {
		for (int i = 0; i < items.length; i++) {
			if (items[i].getType() == material) {
				items[i] = null;
			}
		}
	}
	
	@Override
	public void remove(ItemStack itemStack) {
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals(itemStack)) {
				items[i] = null;
			}
		}
	}
	
	@Override
	public void clear(int i) {
		items[i] = null;
	}
	
	@Override
	public void clear() {
		items = new ItemStack[items.length];
	}
	
	@Override
	public List<HumanEntity> getViewers() {
		return new ArrayList<>();
	}
	
	@Override
	public InventoryType getType() {
		return type;
	}
	
	@Override
	public InventoryHolder getHolder() {
		return holder;
	}
	
	@Override
	public ListIterator<ItemStack> iterator() {
		List<ItemStack> itemList = new ArrayList<>();
		Collections.addAll(itemList, items);
		return itemList.listIterator();
	}
	
	@Override
	public ListIterator<ItemStack> iterator(int i) {
		List<ItemStack> itemList = new ArrayList<>();
		Collections.addAll(itemList, items);
		return itemList.listIterator(i);
	}
	
	@Override
	public Location getLocation() {
		if (holder instanceof Entity) {
			return ((Entity) holder).getLocation();
		}
		if (holder instanceof BlockState) {
			return ((BlockState) holder).getLocation();
		}
		return null;
	}
	
}
