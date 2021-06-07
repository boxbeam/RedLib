package redempt.redlib.blockdata;

import org.bukkit.World;
import org.bukkit.block.Block;
import redempt.redlib.json.JSONList;
import redempt.redlib.json.JSONMap;
import redempt.redlib.misc.LocationUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a Block with various data attached to it
 * @author Redempt
 */
public class DataBlock {
	
	protected JSONMap data = new JSONMap();
	private Map<String, Object> transientProperties = new HashMap<>();
	protected boolean exists = false;
	private Block block;
	private BlockDataManager manager;
	private boolean modified;
	
	protected DataBlock(Block block, BlockDataManager manager) {
		this.block = block;
		this.manager = manager;
	}
	
	/**
	 * Gets a transient property associated with the given key
	 * @param key The key
	 * @return The transient property
	 */
	public Object getTransientProperty(String key) {
		return transientProperties.get(key);
	}
	
	/**
	 * Removes a transient property by its key
	 * @param key The key of the transient property
	 */
	public void removeTransientProperty(String key) {
		transientProperties.remove(key);
	}
	
	/**
	 * Attaches a transient property to this DataBlock. Transient properties
	 * are not saved and will be lost if the chunk is unloaded or the server is stopped
	 * @param key The key of the transient property
	 * @param o The value of the transient property
	 */
	public void setTransientProperty(String key, Object o) {
		transientProperties.put(key, o);
	}

	/**
	 * Attaches a transient property, if absent, to this DataBlock. Transient properties
	 * are not saved and will be lost if the chunk is unloaded or the server is stopped
	 * @param key The key of the transient property
	 * @param o The value of the transient property
	 */
	public void setTransientPropertyIfAbsent(String key, Object o) {
		if (!containsTransientProperty(key)) {
			transientProperties.put(key, o);
		}
	}

	/**
	 * Checks to see if a transient property is associated with the key provided.
	 * @param key The key
	 * @return true if this DataBlock contains a transient property for the specified key
	 */
	public boolean containsTransientProperty(String key) {
		return transientProperties.containsKey(key);
	}
	
	/**
	 * Sets a data value in this DataBlock. If this is the first piece of data attached to this
	 * DataBlock, it will be added to the map of the BlockDataManager it was created by.
	 * @param key The key to put the data at
	 * @param data The data to put
	 */
	public void set(String key, Object data) {
		modified = true;
		if (data == null) {
			remove(key);
			return;
		}
		this.data.put(key, data);
		if (!exists) {
			save();
		}
	}

	/**
	 * Sets a data value, if absent, in this DataBlock. If this is the first piece of data attached to this
	 * DataBlock, it will be added to the map of the BlockDataManager it was created by.
	 * @param key The key to put the data at
	 * @param data The data to put
	 */
	public void setIfAbsent(String key, Object data) {
		if (!contains(key)) {
			set(key, data);
		}
	}

	protected void setData(JSONMap data) {
		this.data = data;
		modified = true;
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
		return this.data.getInt(key);
	}
	
	/**
	 * Gets a String mapped to a certain key
	 * @param key The key
	 * @return The String mapped to the key
	 */
	public String getString(String key) {
		return this.data.getString(key);
	}
	
	/**
	 * Gets a boolean mapped to a certain key
	 * @param key The key
	 * @return The boolean mapped to the key
	 */
	public boolean getBoolean(String key) {
		return this.data.getBoolean(key);
	}
	
	/**
	 * Gets a double mapped to a certain key
	 * @param key The key
	 * @return The double mapped to the key
	 */
	public double getDouble(String key) {
		return this.data.getDouble(key);
	}
	
	/**
	 * Gets a double mapped to a certain key
	 * @param key The key
	 * @return The double mapped to the key
	 */
	public long getLong(String key) {
		return this.data.getLong(key);
	}
	
	/**
	 * Gets a JSONMap mapped to a certain key
	 * @param key The key
	 * @return The JSONMap mapped to the key
	 */
	public JSONMap getMap(String key) {
		return this.data.getMap(key);
	}
	
	/**
	 * Gets a JSONList mapped to a certain key
	 * @param key The key
	 * @return The JSONList mapped to the key
	 */
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
	 * Checks to see if an object is associated with the key provided.
	 * @param key The key
	 * @return true if this DataBlock contains an object for the specified key
	 */
	public boolean contains(String key) {
		return this.data.containsKey(key);
	}

	/**
	 * Removes all of the data from this block.
	 */

	public void clear() {
		this.data.clear();
	}

	/**
	 * Moves the data in this DataBlock to a new Block
	 * @param block The Block to move the data to
	 */
	public void move(Block block) {
		remove();
		this.block = block;
		manager.register(this);
		exists = false;
		modified = true;
		save();
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
			return;
		}
		int[] pos = getChunkCoordinates();
		manager.sql.execute("INSERT INTO blocks VALUES (?, ?, ?, ?, ?, ?, ?);",
				getWorld().getName(), pos[0], pos[1], block.getX(), block.getY(), block.getZ(), data.toString());
		exists = true;
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
		if (!(o instanceof DataBlock)) {
			return false;
		}
		return ((DataBlock) o).block.equals(block);
	}
	
}
