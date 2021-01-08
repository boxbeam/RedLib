package redempt.redlib.region;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import redempt.redlib.RedLib;
import redempt.redlib.misc.LocationUtils;
import redempt.redlib.protection.ProtectedRegion;
import redempt.redlib.protection.ProtectionPolicy.ProtectionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a region of an unspecified shape in the world
 */
public abstract class Region implements Cloneable {
	
	/**
	 * @return The least extreme corner of this Region
	 */
	public abstract Location getStart();
	
	/**
	 * @return The most extreme corner of this Region
	 */
	public abstract Location getEnd();
	
	/**
	 * @return The volume of this Region, in cubic meters
	 */
	public abstract double getVolume();
	
	/**
	 * @return The volume of this Region, in whole blocks
	 */
	public abstract int getBlockVolume();
	
	/**
	 * Expands this Region by a specified amount in each direction
	 * @param posX The amount to increase in the positive X direction
	 * @param negX The amount to increase in the negative X direction
	 * @param posY The amount to increase in the positive Y direction
	 * @param negY The amount to increase in the negative Y direction
	 * @param posZ The amount to increase in the positive Z direction
	 * @param negZ The amount to increase in the negative Z direction
	 * @return Itself
	 */
	public abstract Region expand(double posX, double negX, double posY, double negY, double posZ, double negZ);
	
	/**
	 * Expands this Region in a specific direction
	 * @param face The BlockFace representing the direction to expand in
	 * @param amount The amount to expand
	 * @return Itself
	 */
	public abstract Region expand(BlockFace face, double amount);
	
	/**
	 * Moves this Region
	 * @param vec The vector representing the direction and amount to move
	 * @return Itself
	 */
	public abstract Region move(Vector vec);
	public abstract Region move(double x, double y, double z);
	
	/**
	 * Determines if this Region contains a Location
	 * @param loc The location to check
	 * @return Whether the location is contained by this Region
	 */
	public abstract boolean contains(Location loc);
	
	/**
	 * @return A clone of this Region
	 */
	public abstract Region clone();
	
	/**
	 * Rotates this Region around a central point
	 * @param center The center of rotation
	 * @param rotations The number of clockwise rotations
	 * @return Itself
	 */
	public abstract Region rotate(Location center, int rotations);
	
	/**
	 * Sets the World of this Region
	 * @param world The World
	 * @return Itself
	 */
	public abstract Region setWorld(World world);
	
	/**
	 * Streams all Blocks inside this Region
	 * @return The stream of all Blocks contained in this Region
	 */
	public abstract Stream<Block> stream();
	
	/**
	 * @return All the Chunks this Region overlaps
	 */
	public Set<Chunk> getChunks() {
		Set<Chunk> chunks = new HashSet<>();
		int[] cstart = LocationUtils.getChunkCoordinates(getStart());
		int[] cend = LocationUtils.getChunkCoordinates(getEnd());
		for (int cx = cstart[0]; cx <= cend[0]; cx++) {
			for (int cz = cstart[1]; cz <= cend[1]; cz++) {
				chunks.add(getWorld().getChunkAt(cx, cz));
			}
		}
		return chunks;
	}
	
	/**
	 * Checks whether a Block is contained by this Region
	 * @param block The Block
	 * @return Whether the Block is contained by this Region
	 */
	public boolean contains(Block block) {
		return contains(block.getLocation());
	}
	
	/**
	 * @return All the loaded Chunks this Region overlaps
	 */
	public Set<Chunk> getLoadedChunks() {
		Set<Chunk> chunks = new HashSet<>();
		int[] cstart = LocationUtils.getChunkCoordinates(getStart());
		int[] cend = LocationUtils.getChunkCoordinates(getEnd());
		for (int cx = cstart[0]; cx <= cend[0]; cx++) {
			for (int cz = cstart[1]; cz <= cend[1]; cz++) {
				if (getStart().getWorld().isChunkLoaded(cx, cz)) {
					chunks.add(getStart().getWorld().getChunkAt(cx, cz));
				}
			}
		}
		return chunks;
	}
	
	/**
	 * Enable RegionEnterEvent and RegionExitEvent for this region
	 */
	public void enableEvents() {
		RegionEnterExitListener.getRegionMap().set(this.toCuboid(), this);
	}
	
	/**
	 * Disable RegionEnterEvent and RegionExitEvent for this region
	 */
	public void disableEvents() {
		RegionEnterExitListener.getRegionMap().remove(this.toCuboid(), this);
	}
	
	/**
	 * Gets all players contained in this Region
	 * @return The players in this Region
	 */
	public List<Player> getPlayers() {
		return getEntities(false).stream().filter(e -> e instanceof Player).map(e -> (Player) e).collect(Collectors.toList());
	}
	
	/**
	 * Gets all entities contained in this Region in loaded chunks
	 * @return The entities in this Region
	 */
	public List<Entity> getEntities() {
		return getEntities(false);
	}
	
	/**
	 * Gets all entities contained in this Region
	 * @param load Whether to load chunks to check the entities inside them
	 * @return The entities in this Region
	 */
	public List<Entity> getEntities(boolean load) {
		List<Entity> entities = new ArrayList<>();
		Stream<Chunk> chunkStream = load ? getChunks().stream() : getLoadedChunks().stream();
		chunkStream.map(Chunk::getEntities).forEach(e -> Arrays.stream(e).filter(en -> this.contains(en.getLocation())).forEach(entities::add));
		return entities;
	}
	
	/**
	 * @return The World this Region is in
	 */
	public World getWorld() {
		return getStart().getWorld();
	}
	
	/**
	 * Streams every Block in this Region, running your lambda on it
	 * @param forEach What to run on each Block
	 */
	public void forEachBlock(Consumer<Block> forEach) {
		stream().forEach(forEach);
	}
	
	/**
	 * Get the dimensions of this Region [x, y, z] in blocks
	 * @return The dimensions of this Region
	 */
	public int[] getBlockDimensions() {
		return new int[] {getEnd().getBlockX() - getStart().getBlockX(),
				getEnd().getBlockY() - getStart().getBlockY(),
				getEnd().getBlockZ() - getStart().getBlockZ()};
	}
	
	/**
	 * Get the dimensions of this Region [x, y, z]
	 * @return The dimensions of this Region
	 */
	public double[] getDimensions() {
		return new double[] {getEnd().getX() - getStart().getX(),
				getEnd().getY() - getStart().getY(),
				getEnd().getZ() - getStart().getZ()};
	}
	
	/**
	 * @return All 8 cuboid corners of this Region
	 */
	public Location[] getCorners() {
		Location start = getStart();
		Location end = getEnd();
		return new Location[] {
				start,
				end,
				new Location(getWorld(), start.getX(), start.getY(), end.getZ()),
				new Location(getWorld(), start.getX(), end.getY(), start.getZ()),
				new Location(getWorld(), end.getX(), start.getY(), start.getZ()),
				new Location(getWorld(), start.getX(), end.getY(), end.getZ()),
				new Location(getWorld(), end.getX(), end.getY(), start.getZ()),
				new Location(getWorld(), end.getX(), start.getY(), end.getZ())
		};
	}
	
	/**
	 * @return A cuboid representation of this Region using the extreme corners
	 */
	public CuboidRegion toCuboid() {
		return new CuboidRegion(getStart().clone(), getEnd().clone());
	}
	
	/**
	 * @return The center of this Region, the midpoint of the two extreme corners
	 */
	public Location getCenter() {
		return getStart().clone().add(getEnd()).multiply(0.5);
	}
	
	/**
	 * Protects this Region
	 * @param plugin The plugin registering the protection
	 * @param types The ProtectionTypes to protect this Region with
	 * @return The ProtectedRegion using this Region and the given ProtectionTypes
	 */
	public ProtectedRegion protect(Plugin plugin, ProtectionType... types) {
		return new ProtectedRegion(plugin, this, types);
	}
	
	/**
	 * Protects this Region
	 * @param types The ProtectionTypes to protect this Region with
	 * @return The ProtectedRegion using this Region and the given ProtectionTypes
	 */
	public ProtectedRegion protect(ProtectionType... types) {
		return protect(RedLib.getCallingPlugin(), types);
	}
	
	/**
	 * Gets the length of this Region along a given axis
	 * @param direction The BlockFace representing the axis - opposites will act the same (i.e. UP, DOWN)
	 * @return The length of this Region along the given axis
	 */
	public double measure(BlockFace direction) {
		switch (direction) {
			case UP:
			case DOWN:
				return getDimensions()[1];
			case EAST:
			case WEST:
				return getDimensions()[0];
			case NORTH:
			case SOUTH:
				return getDimensions()[2];
			default:
				throw new IllegalArgumentException("Face must be one of UP, DOWN, NORTH, SOUTH, EAST, or WEST");
		}
	}
	
	/**
	 * Gets the block length of this Region along a given axis
	 * @param direction The BlockFace representing the axis - opposites will act the same (i.e. UP, DOWN)
	 * @return The block length of this Region along the given axis
	 */
	public int measureBlocks(BlockFace direction) {
		switch (direction) {
			case UP:
			case DOWN:
				return getBlockDimensions()[1];
			case EAST:
			case WEST:
				return getBlockDimensions()[0];
			case NORTH:
			case SOUTH:
				return getBlockDimensions()[2];
			default:
				throw new IllegalArgumentException("Face must be one of UP, DOWN, NORTH, SOUTH, EAST, or WEST");
		}
	}
	
}
