package redempt.redlib.blockdata;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import redempt.redlib.json.JSONList;
import redempt.redlib.json.JSONMap;
import redempt.redlib.misc.LocationUtils;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a Block with various data attached to it
 * @author Redempt
 */
public class DataBlock {
	
	protected JSONMap data = new JSONMap();
	protected boolean exists = false;
	private Block block;
	private BlockDataManager manager;
	private boolean modified;
	
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
		modified = true;
		this.data.put(key, data);
		if (!exists) {
			save();
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
	
	public JSONMap getMap(String key) {
		return this.data.getMap(key);
	}
	
	public JSONList getList(String key) {
		return this.data.getList(key);
	}
	
	/**
	 * Removes the object associated with a certain key.
	 * If this object is the last one stored in this DataBlock,
	 * this DataBlock will be removed from its BlockDataManager.
	 * @param key The key
	 */
	public void remove(String key) {
		this.data.remove(key);
	}
	
	/**
	 * @return All the data stored in this DataBlock
	 */
	public JSONMap getData() {
		return data;
	}
	
	protected void save() {
		if (!modified) {
			return;
		}
		modified = false;
		if (exists) {
			manager.sql.execute("UPDATE blocks SET data=? WHERE x=? AND y=? AND z=? AND world=?;",
					data.toString(), block.getX(), block.getY(), block.getZ(), getWorld().getName());
		} else {
			
			int[] pos = getChunkCoordinates();
			manager.sql.execute("INSERT INTO blocks VALUES (?, ?, ?, ?, ?, ?, ?);",
					getWorld().getName(), pos[0], pos[1], block.getX(), block.getY(), block.getZ(), data.toString());
			exists = true;
		}
	}
	
	
	/**
	 * @return Whether the chunk this DataBlock is in is loaded
	 */
	public boolean isLoaded() {
		int[] pos = getChunkCoordinates();
		return getWorld().isChunkLoaded(pos[0], pos[1]);
	}
	
	/**
	 * @return The chunk coordinates [x, z] of this DataBlock
	 */
	public int[] getChunkCoordinates() {
		return LocationUtils.getChunkCoordinates(block.getLocation());
	}
	
	/**
	 * @return The World this DataBlock is in
	 */
	public World getWorld() {
		return block.getWorld();
	}
	
	/**
	 * Removes this DataBlock and all the data associated with it
	 */
	public void remove() {
		manager.remove(this);
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
	
	@Override
	public int hashCode() {
		return Objects.hash(block, manager);
	}
	
	@Override
	public boolean equals(Object o) {
		return o.hashCode() == hashCode();
	}
	
}
