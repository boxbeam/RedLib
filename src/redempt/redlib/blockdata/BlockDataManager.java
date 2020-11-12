package redempt.redlib.blockdata;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import redempt.redlib.RedLib;
import redempt.redlib.blockdata.events.DataBlockDestroyEvent;
import redempt.redlib.blockdata.events.DataBlockDestroyEvent.DestroyCause;
import redempt.redlib.blockdata.events.DataBlockMoveEvent;
import redempt.redlib.json.JSONMap;
import redempt.redlib.json.JSONParser;
import redempt.redlib.misc.LocationUtils;
import redempt.redlib.sql.SQLHelper;
import redempt.redlib.misc.Task;

import java.nio.file.Path;
import java.util.*;

/**
 * Manages {@link DataBlock} instances, which allow you to attach persistent metadata to blocks,
 * Keeps track of managed blocks, removing data if a block is destroyed or moving it if a block is pushed
 * by a piston.
 * @author Redempt
 */
public class BlockDataManager implements Listener {

	private static List<BlockDataManager> managers = new ArrayList<>();
	
	/**
	 * @return The list of all active BlockDataManagers
	 */
	public static List<BlockDataManager> getAllManagers() {
		return managers;
	}
	
	protected Map<World, Map<ChunkPosition, Set<DataBlock>>> blocks = new HashMap<>();
	protected SQLHelper sql;
	
	/**
	 * Create a BlockDataManager instance with a save file location, to be saved to and loaded from. This constructor
	 * immediately loads from the given file.
	 * @param saveFile The Path to load from immediately, and save to when save is called
	 */
	public BlockDataManager(Path saveFile) {
		Bukkit.getPluginManager().registerEvents(this, RedLib.getInstance());
		sql = new SQLHelper(SQLHelper.openSQLite(saveFile));
		sql.setAutoCommit(false);
		sql.execute("CREATE TABLE IF NOT EXISTS blocks (world TEXT, cx INT, cz INT, x INT, y INT, z INT, data TEXT, PRIMARY KEY (world, x, y, z));");
		managers.add(this);
		setAutoSave(true);
	}
	
	/**
	 * Sets whether this BlockDataManager will automatically save every 5 minutes. Defaults to true.
	 * @param autoSave Whether to save automatically every 5 minutes
	 */
	public void setAutoSave(boolean autoSave) {
		sql.setCommitInterval(autoSave ? 20 * 60 * 5 : -1);
	}
	
	/**
	 * Saves all data to the save file.
	 */
	public void save() {
		getAllLoaded().forEach(DataBlock::save);
		sql.commit();
	}
	
	/**
	 * Saves all data to the save file, and closes the SQL connection. Call this in your onDisable.
	 */
	public void saveAndClose() {
		setAutoSave(false);
		save();
		sql.close();
		managers.remove(this);
		HandlerList.unregisterAll(this);
	}
	
	/**
	 * Gets an existing DataBlock, returning null if that Block has no data attached to it.
	 * @param block The block to check
	 * @return A DataBlock, or null
	 */
	public DataBlock getExisting(Block block) {
		int[] pos = LocationUtils.getChunkCoordinates(block.getLocation());
		blocks.putIfAbsent(block.getWorld(), new HashMap<>());
		Set<DataBlock> set = load(block.getWorld(), pos[0], pos[1]);
		for (DataBlock db : set) {
			if (db.getBlock().equals(block)) {
				return db;
			}
		}
		return null;
	}
	
	/**
	 * Gets a DataBlock from a given Block, creating a new one if that Block had no data attached to it.
	 * @param block The block to check or create a DataBlock from
	 * @return An existing or new DataBlock
	 */
	public DataBlock getDataBlock(Block block) {
		DataBlock db = getExisting(block);
		if (db != null) {
			return db;
		}
		db = new DataBlock(block, this);
		blocks.putIfAbsent(block.getWorld(), new HashMap<>());
		Map<ChunkPosition, Set<DataBlock>> map = blocks.get(block.getWorld());
		int[] pos = db.getChunkCoordinates();
		ChunkPosition cpos = new ChunkPosition(pos[0], pos[1]);
		map.putIfAbsent(cpos, new HashSet<>());
		Set<DataBlock> set = map.get(cpos);
		set.add(db);
		return db;
	}
	
	protected void register(DataBlock db) {
		int[] pos = db.getChunkCoordinates();
		if (!isChunkLoaded(db.getWorld(), pos[0], pos[1])) {
			return;
		}
		Map<ChunkPosition, Set<DataBlock>> map = blocks.get(db.getWorld());
		ChunkPosition cpos = new ChunkPosition(pos[0], pos[1]);
		Set<DataBlock> set = map.get(cpos);
		set.add(db);
	}
	
	/**
	 * Removes a DataBlock from this DataBlockManager
	 * @param db The DataBlock to remove
	 */
	public void remove(DataBlock db) {
		sql.execute("DELETE FROM blocks WHERE x=? AND y=? AND z=? AND world=?;",
				db.getBlock().getX(), db.getBlock().getY(), db.getBlock().getZ(), db.getWorld().getName());
		int[] pos = db.getChunkCoordinates();
		if (isChunkLoaded(db.getWorld(), pos[0], pos[1])) {
			getLoaded(db.getWorld(), pos[0], pos[1]).remove(db);
		}
	}
	
	/**
	 * Gets all the DataBlocks near an approximate location
	 * @param loc The location to check near
	 * @param radius The radius to check in
	 * @return The nearby DataBlocks
	 */
	public Set<DataBlock> getNearby(Location loc, int radius) {
		radius /= 16;
		radius += 1;
		Set<DataBlock> set = new HashSet<>();
		int[] pos = LocationUtils.getChunkCoordinates(loc);
		for (int x = pos[0] - radius; x <= pos[0] + radius; x++) {
			for (int z = pos[1] - radius; z <= pos[1] + radius; z++) {
				if (isChunkLoaded(loc.getWorld(), x, z)) {
					set.addAll(getLoaded(loc.getWorld(), x, z));
				} else {
					set.addAll(load(loc.getWorld(), x, z));
				}
			}
		}
		return set;
	}
	
	/**
	 * Gets all the loaded DataBlocks in a chunk
	 * @param chunk The chunk to get the loaded DataBlocks in
	 * @return A set of DataBlocks in the chunk, or an empty set if the chunk is not loaded
	 */
	public Set<DataBlock> getLoaded(Chunk chunk) {
		return getLoaded(chunk.getWorld(), chunk.getX(), chunk.getZ());
	}
	
	/**
	 * Gets all the loaded DataBlocks in a chunk
	 * @param world The world the chunk is in
	 * @param cx The chunk X
	 * @param cz The chunk Z
	 * @return A set of DataBlocks in the chunk, or an empty set if the chunk is not loaded
	 */
	public Set<DataBlock> getLoaded(World world, int cx, int cz) {
		ChunkPosition pos = new ChunkPosition(cx, cz);
		Map<ChunkPosition, Set<DataBlock>> worldMap = blocks.get(world);
		if (worldMap == null) {
			return new HashSet<>();
		}
		return worldMap.getOrDefault(pos, new HashSet<>());
	}
	
	/**
	 * Loads all of the DataBlocks in a given chunk, or retrieves the already-loaded set of DataBlocks
	 * @param world The world the chunk is in
	 * @param cx The chunk X
	 * @param cz The chunk Z
	 * @return The set of DataBlocks in the chunk
	 */
	public Set<DataBlock> load(World world, int cx, int cz) {
		if (isChunkLoaded(world, cx, cz)) {
			return getLoaded(world, cx, cz);
		}
		Set<DataBlock> set = new HashSet<>();
		blocks.putIfAbsent(world, new HashMap<>());
		Map<ChunkPosition, Set<DataBlock>> worldMap = blocks.get(world);
		ChunkPosition pos = new ChunkPosition(cx, cz);
		sql.queryResults("SELECT x,y,z,data FROM blocks WHERE world=? AND cx=? AND cz=?;", world.getName(), cx, cz).forEach(r -> {
			int x = r.get(1);
			int y = r.get(2);
			int z = r.get(3);
			JSONMap data = JSONParser.parseMap(r.getString(4));
			Block block = world.getBlockAt(x, y, z);
			DataBlock db = new DataBlock(block, this);
			db.exists = true;
			db.data = data;
			set.add(db);
		});
		worldMap.put(pos, set);
		return set;
	}
	
	/**
	 * Loads all of the DataBlocks in a given chunk, or retrieves the already-loaded set of DataBlocks
	 * @param chunk The chunk to load DataBlocks in
	 * @return The set of DataBlocks in the chunk
	 */
	public Set<DataBlock> load(Chunk chunk) {
		return load(chunk.getWorld(), chunk.getX(), chunk.getZ());
	}
	
	/**
	 * Saves and unloads all of the DataBlocks in a chunk
	 * @param world The world the chunk is in
	 * @param cx The chunk X
	 * @param cz The chunk Z
	 */
	public void unload(World world, int cx, int cz) {
		Map<ChunkPosition, Set<DataBlock>> worldMap = blocks.get(world);
		if (worldMap == null) {
			return;
		}
		ChunkPosition pos = new ChunkPosition(cx, cz);
		Set<DataBlock> set = worldMap.get(pos);
		if (set == null) {
			return;
		}
		set.forEach(DataBlock::save);
		worldMap.remove(pos);
	}
	
	/**
	 * Saves and unloads all of the DataBlocks in a chunk
	 * @param chunk The chunk to unload the DataBlocks in
	 */
	public void unload(Chunk chunk) {
		unload(chunk.getWorld(), chunk.getX(), chunk.getZ());
	}
	
	/**
	 * Saves and unloads all DataBlocks from this BlockDataManager
	 */
	public void unloadAll() {
		save();
		blocks = new HashMap<>();
	}
	
	/**
	 * @param chunk The chunk to check
	 * @return Whether the DataBlocks in the chunk are loaded in this BlockDataManager
	 */
	public boolean isChunkLoaded(Chunk chunk) {
		return isChunkLoaded(chunk.getWorld(), chunk.getX(), chunk.getZ());
	}
	
	/**
	 *
	 * @param world The world the chunk is in
	 * @param cx The X coordinate of the chunk
	 * @param cz The Z coordinate of the chunk
	 * @return Whether the DataBlocks in the chunk are loaded in this BlockDataManager
	 */
	public boolean isChunkLoaded(World world, int cx, int cz) {
		return blocks.containsKey(world) && blocks.get(world).containsKey(new ChunkPosition(cx, cz));
	}
	
	/**
	 * @return The set of all loaded DataBlocks
	 */
	public Set<DataBlock> getAllLoaded() {
		Set<DataBlock> set = new HashSet<>();
		blocks.values().forEach(v -> {
			v.values().forEach(set::addAll);
		});
		return set;
	}
	
	/**
	 * Loads and returns a set of all DataBlocks managed by this BlockDataManager. Avoid calling this if possible.
	 * @return The set of all DataBlocks managed by this BlockDataManager
	 */
	public Set<DataBlock> getAll() {
		Set<DataBlock> set = new HashSet<>();
		sql.queryResults("SELECT world,cx,cz,x,y,z,data FROM blocks;").forEach(r -> {
			World world = Bukkit.getWorld(r.getString(1));
			int cx = r.get(2);
			int cz = r.get(3);
			if (isChunkLoaded(world, cx, cz)) {
				set.addAll(getLoaded(world, cx, cz));
				return;
			}
			int x = r.get(4);
			int y = r.get(5);
			int z = r.get(6);
			JSONMap data = JSONParser.parseMap(r.get(7));
			Block block = world.getBlockAt(x, y, z);
			DataBlock db = new DataBlock(block, this);
			db.exists = true;
			db.data = data;
			blocks.putIfAbsent(world, new HashMap<>());
			ChunkPosition pos = new ChunkPosition(cx, cz);
			blocks.get(world).putIfAbsent(pos, new HashSet<>());
			Set<DataBlock> chunk = blocks.get(world).get(pos);
			chunk.add(db);
			set.add(db);
		});
		return set;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBreakBlock(BlockBreakEvent e) {
		DataBlock db = getExisting(e.getBlock());
		if (db == null) {
			return;
		}
		DataBlockDestroyEvent event = new DataBlockDestroyEvent(db, e.getPlayer(), DestroyCause.PLAYER, e);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			e.setCancelled(true);
			return;
		}
		remove(db);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBurnBlock(BlockBurnEvent e) {
		DataBlock db = getExisting(e.getBlock());
		if (db == null) {
			return;
		}
		DataBlockDestroyEvent event = new DataBlockDestroyEvent(db, null, DestroyCause.FIRE, e);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			e.setCancelled(true);
			return;
		}
		remove(db);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent e) {
		List<Block> toRemove = new ArrayList<>();
		e.blockList().forEach(block -> {
			DataBlock db = getExisting(block);
			if (db == null) {
				return;
			}
			DataBlockDestroyEvent event = new DataBlockDestroyEvent(db, null, DestroyCause.EXPLOSION, e);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				toRemove.add(block);
				return;
			}
			remove(db);
		});
		e.blockList().removeAll(toRemove);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent e) {
		List<Block> toRemove = new ArrayList<>();
		e.blockList().forEach(block -> {
			DataBlock db = getExisting(block);
			if (db == null) {
				return;
			}
			DataBlockDestroyEvent event = new DataBlockDestroyEvent(db, null, DestroyCause.EXPLOSION, e);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				toRemove.add(block);
				return;
			}
			remove(db);
		});
		e.blockList().removeAll(toRemove);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPush(BlockPistonExtendEvent e) {
		e.getBlocks().forEach(block -> {
			DataBlock db = getExisting(block);
			if (db == null) {
				return;
			}
			Location to = block.getRelative(e.getDirection()).getLocation();
			DataBlockMoveEvent event = new DataBlockMoveEvent(db, to);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				e.setCancelled(true);
				return;
			}
			db.move(to.getBlock());
		});
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPull(BlockPistonRetractEvent e) {
		e.getBlocks().forEach(block -> {
			DataBlock db = getExisting(block);
			if (db == null) {
				return;
			}
			Location to = block.getRelative(e.getDirection()).getLocation();
			DataBlockMoveEvent event = new DataBlockMoveEvent(db, to);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				e.setCancelled(true);
				return;
			}
			db.move(to.getBlock());
		});
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onChunkUnload(ChunkUnloadEvent e) {
		unload(e.getChunk());
	}
	
	private static class ChunkPosition {
		
		public int x;
		public int z;
		
		public ChunkPosition(int x, int z) {
			this.x = x;
			this.z = z;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(x, z);
		}
		
		@Override
		public boolean equals(Object o) {
			return o.hashCode() == hashCode();
		}
		
	}
	
}
