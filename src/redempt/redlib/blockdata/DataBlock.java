package redempt.redlib.blockdata;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import redempt.redlib.json.JSONList;
import redempt.redlib.json.JSONMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a Block with data attached to it
 * @author Redempt
 */
public class DataBlock {

	protected JSONMap data;
	private BlockDataManager manager;
	private BlockPosition block;
	private String world;
	private Map<String, Object> transientProperties;
	
	DataBlock(JSONMap data, BlockPosition block, String world, BlockDataManager manager) {
		this.data = data;
		this.block = block;
		this.manager = manager;
		this.world = world;
	}
	
	/**
	 * @return The BlockDataManager this DataBlock belongs to
	 */
	public BlockDataManager getManager() {
		return manager;
	}
	
	/**
	 * @return A map which can be used to store properties that do not persist
	 */
	public Map<String, Object> getTransientProperties() {
		if (transientProperties == null) {
			transientProperties = new HashMap<>();
		}
		return transientProperties;
	}
	
	/**
	 * @return The Block the data is attached to
	 */
	public Block getBlock() {
		return Bukkit.getWorld(world).getBlockAt(block.getX(), block.getY(), block.getZ());
	}
	
	protected ChunkPosition getChunkPosition() {
		return new ChunkPosition(block, world);
	}
	
	protected BlockPosition getBlockPosition() {
		return block;
	}
	
	/**
	 * Gets an object by key
	 * @param key The key the data is mapped to
	 * @return The data as an Object
	 */
	public Object getObject(String key) {
		return data.get(key);
	}
	
	/**
	 * Gets a string by key
	 * @param key The key the data is mapped to
	 * @return The data as a String
	 */
	public String getString(String key) {
		return data.getString(key);
	}
	
	/**
	 * Gets an int by key
	 * @param key The key the data is mapped to
	 * @return The data as an Integer
	 */
	public Integer getInt(String key) {
		return data.getInt(key);
	}
	
	/**
	 * Gets a long by key
	 * @param key The key the data is mapped to
	 * @return The data as a Long
	 */
	public Long getLong(String key) {
		return data.getLong(key);
	}
	
	/**
	 * Gets a Double by key
	 * @param key The key the data is mapped to
	 * @return The data as a Double
	 */
	public Double getDouble(String key) {
		return data.getDouble(key);
	}
	
	/**
	 * Gets a Boolean by key
	 * @param key The key the data is mapped to
	 * @return The data as a Boolean
	 */
	public Boolean getBoolean(String key) {
		return data.getBoolean(key);
	}
	
	/**
	 * Gets a JSONList by key
	 * @param key The key the data is mapped to
	 * @return The data as a JSONList
	 */
	public JSONList getList(String key) {
		return data.getList(key);
	}
	
	/**
	 * Gets a JSONMap by key
	 * @param key The key the data is mapped to
	 * @return The data as a JSONMap
	 */
	public JSONMap getMap(String key) {
		return data.getMap(key);
	}
	
	/**
	 * Checks if a certain key is used in this DataBlock
	 * @param key The key
	 * @return Whether the key is used
	 */
	public boolean contains(String key) {
		return data.containsKey(key);
	}
	
	/**
	 * Clears all data from this DataBlock
	 */
	public void clear() {
		data.clear();
	}
	
	/**
	 * Sets data in this DataBlock
	 * @param key The key to set the data with
	 * @param value The data
	 */
	public void set(String key, Object value) {
		manager.setModified(new ChunkPosition(block, world));
		if (value == null) {
			data.remove(key);
			return;
		}
		data.put(key, value);
	}
	
	/**
	 * Removes a key from this DataBlock
	 * @param key The key to remove
	 */
	public void remove(String key) {
		set(key, null);
	}
	
	/**
	 * @return All data stored in this DataBlock
	 */
	public Map<String, Object> getData() {
		return data;
	}
	
	/**
	 * @return All keys used in this DataBlock
	 */
	public Set<String> getKeys() {
		return data.keySet();
	}
	
}
