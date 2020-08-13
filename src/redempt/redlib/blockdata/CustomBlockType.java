package redempt.redlib.blockdata;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Represents a type of a CustomBlock that can be set
 * @param <T> The type of the CustomBlock represented by this CustomBlockType
 * @author Redempt
 */
public abstract class CustomBlockType<T extends CustomBlock> implements Listener {
	
	private BlockDataManager manager;
	private String typeName;
	
	/**
	 * Construct a CustomBlockType with the type name. You should only call this if you don't use
	 * {@link CustomBlockRegistry#registerAll(Plugin)} to load custom block types.
	 * @param typeName The name of this type
	 */
	public CustomBlockType(String typeName) {
		this.typeName = typeName;
	}
	
	/**
	 * Checks whether the item given matches the item for this CustomBlockType
	 * @param item The ItemStack to check
	 * @return Whether the item matches
	 */
	public boolean itemMatches(ItemStack item) {
		return item.hasItemMeta() && getBaseItemName().equals(item.getItemMeta().getDisplayName());
	}
	
	/**
	 * Called when this CustomBlockType is placed. Use it to initialize any fields that are needed.
	 * @param player The player who placed the CustomBlock
	 * @param item The ItemStack in their hand when it was placed
	 * @param block The CustomBlock storing the data
	 */
	public abstract void place(Player player, ItemStack item, T block);
	
	/**
	 * Gets the item to be dropped when this block is mined
	 * @param block The CustomBlock that was mined
	 * @return The ItemStack to drop
	 */
	public abstract ItemStack getItem(T block);
	
	/**
	 * @return A unique item name that the item for this CustomBlockType will have
	 */
	public abstract String getBaseItemName();
	
	protected final void register(BlockDataManager manager) {
		this.manager = manager;
	}
	
	/**
	 * Checks whether the type of a block matches this CustomBlockType. Always returns true by default.
	 * @param material The Material to check
	 * @return Whether this Material matches the type for this CustomBlockType
	 */
	public boolean typeMatches(Material material) {
		return true;
	}
	
	/**
	 * @return The name of this CustomBlockType
	 */
	public String getName() {
		return typeName;
	}
	
	/**
	 * Defines a custom return for a class extending {@link CustomBlock}
	 * @param db The DataBlock to be passed to the constructor
	 * @return The CustomBlock sub-class instance
	 */
	public T getCustom(DataBlock db) {
		return null;
	}
	
	/**
	 * Gets a {@link CustomBlock} of this type at the given block
	 * @param block The Block to get the CustomBlock at
	 * @return The CustomBlock of this type at this Block, or null if it is not present
	 */
	public final T get(Block block) {
		return get(manager.getExisting(block));
	}
	
	/**
	 * Gets a {@link CustomBlock} of this type from the given DataBlock
	 * @param db The DataBlock to get the CustomBlock at
	 * @return The CustomBlock of this type represented by this DataBlock, or null if it is not present
	 */
	public final T get(DataBlock db) {
		if (db == null || !db.getString("custom-type").equals(typeName) || !typeMatches(db.getBlock().getType())) {
			return null;
		}
		CustomBlock custom = getCustom(db);
		if (custom != null) {
			return (T) custom;
		}
		return (T) new CustomBlock(this, db);
	}
	
	/**
	 * Initializes the placement of this CustomBlockType for the given Block. Does not change the block's vanilla type.
	 * @param block The block to initialize
	 * @return The initialized CustomBlock
	 */
	public final T initialize(Block block) {
		DataBlock db = manager.getDataBlock(block);
		db.set("custom-type", typeName);
		return getCustom(db);
	}
	
}
