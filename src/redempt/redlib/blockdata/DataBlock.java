package redempt.redlib.blockdata;

import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Block with various data attached to it
 * @author Redempt
 */
public class DataBlock {
	
	protected Map<String, Object> data = new HashMap<>();
	private Block block;
	private BlockDataManager manager;
	
	protected DataBlock(Block block, BlockDataManager manager) {
		this.block = block;
		this.manager = manager;
	}
	
	/**
	 * Sets a data value in this DataBlock. If this is the first piece of data attached to this
	 * DataBlock, it will be added to the map of the BlockDataManager it was created by.
	 * @param key The key to put the data at
	 * @param data The data to put
	 */
	public void set(String key, Object data) {
		if (this.data.put(key, data) == null && this.data.size() == 1) {
			manager.map.set(block.getLocation(), this);
		}
	}
	
	/**
	 * Gets the object mapped to a certain key
	 * @param key The key
	 * @return The object mapped to the key
	 */
	public Object get(String key) {
		return this.data.get(key);
	}
	
	/**
	 * Gets an int mapped to a certain key
	 * @param key The key
	 * @return The int mapped to the key
	 */
	public int getInt(String key) {
		return (int) this.data.get(key);
	}
	
	/**
	 * Gets a String mapped to a certain key
	 * @param key The key
	 * @return The String mapped to the key
	 */
	public String getString(String key) {
		return (String) this.data.get(key);
	}
	
	/**
	 * Gets a boolean mapped to a certain key
	 * @param key The key
	 * @return The boolean mapped to the key
	 */
	public boolean getBoolean(String key) {
		return (boolean) this.data.get(key);
	}
	
	/**
	 * Gets a double mapped to a certain key
	 * @param key The key
	 * @return The double mapped to the key
	 */
	public double getDouble(String key) {
		return (double) this.data.get(key);
	}
	
	/**
	 * Removes the object associated with a certain key.
	 * If this object is the last one stored in this DataBlock,
	 * this DataBlock will be removed from its BlockDataManager.
	 * @param key The key
	 */
	public void remove(String key) {
		this.data.remove(key);
		if (this.data.size() == 0) {
			manager.map.remove(block.getLocation(), this);
		}
	}
	
	/**
	 * @return The Block this DataBlock stores data for
	 */
	public Block getBlock() {
		return block;
	}
	
	protected void setBlock(Block block) {
		this.block = block;
	}
	
	/**
	 * @return The BlockDataManager managing this DataBlock
	 */
	public BlockDataManager getManager() {
		return manager;
	}
	
}
