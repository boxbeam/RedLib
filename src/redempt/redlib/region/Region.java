package redempt.redlib.region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import redempt.redlib.RedLib;
import redempt.redlib.region.ProtectionPolicy.ProtectionType;
import redempt.redlib.region.events.RegionEnterEvent;
import redempt.redlib.region.events.RegionEnterEvent.EnterCause;
import redempt.redlib.region.events.RegionExitEvent;
import redempt.redlib.region.events.RegionExitEvent.ExitCause;

/**
 * Represents a cuboid region in a world
 * @author Redempt
 */
public class Region implements Listener {
	
	protected Location start;
	protected Location end;
	
	/**
	 * Construct a Region using 2 corners
	 * @param start The first corner
	 * @param end The second corner
	 */
	public Region(Location start, Location end) {
		setLocations(start, end);
	}
	
	protected Region() {}
	
	protected void setLocations(Location start, Location end) {
		if (!start.getWorld().equals(end.getWorld())) {
			throw new IllegalArgumentException("Locations must be in the same world");
		}
		double minX = Math.min(start.getX(), end.getX());
		double minY = Math.min(start.getY(), end.getY());
		double minZ = Math.min(start.getZ(), end.getZ());
		
		double maxX = Math.max(start.getX(), end.getX());
		double maxY = Math.max(start.getY(), end.getY());
		double maxZ = Math.max(start.getZ(), end.getZ());
		
		this.start = new Location(start.getWorld(), minX, minY, minZ);
		this.end = new Location(end.getWorld(), maxX, maxY, maxZ);
	}
	
	/**
	 * Enable RegionEnterEvent and RegionExitEvent for this region
	 */
	public void enableEvents() {
		Bukkit.getPluginManager().registerEvents(this, RedLib.getInstance());
	}
	
	/**
	 * Disable RegionEnterEvent and RegionExitEvent for this region
	 */
	public void disableEvents() {
		HandlerList.unregisterAll(this);
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		if (!contains(e.getFrom()) && contains(e.getTo())) {
			Bukkit.getPluginManager().callEvent(new RegionEnterEvent(e.getPlayer(), this, EnterCause.MOVE, e));
		}
		if (!contains(e.getTo()) && contains(e.getFrom())) {
			Bukkit.getPluginManager().callEvent(new RegionExitEvent(e.getPlayer(), this, ExitCause.MOVE, e));
		}
	}
	
	@EventHandler
	public void onTeleport(PlayerTeleportEvent e) {
		if (!contains(e.getFrom()) && contains(e.getTo())) {
			Bukkit.getPluginManager().callEvent(new RegionEnterEvent(e.getPlayer(), this, EnterCause.TELEPORT, e));
		}
		if (!contains(e.getTo()) && contains(e.getFrom())) {
			Bukkit.getPluginManager().callEvent(new RegionExitEvent(e.getPlayer(), this, ExitCause.TELEPORT, e));
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (contains(e.getPlayer().getLocation())) {
			Bukkit.getPluginManager().callEvent(new RegionEnterEvent(e.getPlayer(), this, EnterCause.JOIN, null));
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		if (contains(e.getPlayer().getLocation())) {
			Bukkit.getPluginManager().callEvent(new RegionExitEvent(e.getPlayer(), this, ExitCause.QUIT, null));
		}
	}
	
	/**
	 * Get the minimum corner of this Region
	 * @return The corner with the lowest X, Y, and Z values
	 */
	public Location getStart() {
		return start.clone();
	}
	
	/**
	 * Get the maximum corner of this Region
	 * @return The corner with the highest X, Y, and Z values
	 */
	public Location getEnd() {
		return end.clone();
	}
	
	/**
	 * @return An array of size 8 containing all corners of this Region
	 */
	public Location[] getCorners() {
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
	 * Gets the center of this Region
	 * @return A Location representing the center of this Region
	 */
	public Location getCenter() {
		return start.clone().add(end).multiply(0.5);
	}
	
	/**
	 * Check whether a location is inside this Region
	 * @param loc The location to check
	 * @return Whether the location is inside this Region
	 * @deprecated Unclear name, use {@link Region#contains(Location)}
	 */
	public boolean isInside(Location loc) {
		return contains(loc);
	}
	
	/**
	 * Check whether a location is inside this Region
	 * @param loc The location to check
	 * @return Whether this Region contains the given Location
	 */
	public boolean contains(Location loc) {
		return loc.getWorld().getName().equals(start.getWorld().getName()) &&
				loc.getX() >= start.getX() && loc.getY() >= start.getY() && loc.getZ() >= start.getZ() &&
				loc.getX() < end.getX() && loc.getY() < end.getY() && loc.getZ() < end.getZ();
	}
	
	/**
	 * Get the dimensions of this Region [x, y, z] in blocks
	 * @return The dimensions of this Region
	 */
	public int[] getBlockDimensions() {
		return new int[] {end.getBlockX() - start.getBlockX(), end.getBlockY() - start.getBlockY(), end.getBlockZ() - start.getBlockZ()};
	}
	
	/**
	 * @return The volume of this Region, in cubic meters
	 */
	public double getVolume() {
		double[] dim = getDimensions();
		return dim[0] * dim[1] * dim[2];
	}
	
	/**
	 * @return The volume of this Region, in blocks
	 */
	public int getBlockVolume() {
		int[] dim = getBlockDimensions();
		return dim[0] * dim[1] * dim[2];
	}
	
	/**
	 * Get the dimensions of this Region [x, y, z]
	 * @return The dimensions of this Region
	 */
	public double[] getDimensions() {
		return new double[] {end.getX() - start.getX(), end.getY() - start.getY(), end.getZ() - start.getZ()};
	}
	
	/**
	 * Gets the length of this Region along a given axis
	 * @param direction The BlockFace representing the axis - opposites will act the same (i.e. UP, DOWN)
	 * @return The length of this Region along the given axis
	 */
	public double measure(BlockFace direction) {
		Vector v = direction.getDirection();
		double[] dimensions = getDimensions();
		if (Math.abs(v.getX()) > 0.5) {
			return dimensions[0];
		}
		if (Math.abs(v.getY()) > 0.5) {
			return dimensions[1];
		}
		return dimensions[2];
	}
	
	/**
	 * Gets the block length of this Region along a given axis
	 * @param direction The BlockFace representing the axis - opposites will act the same (i.e. UP, DOWN)
	 * @return The block length of this Region along the given axis
	 */
	public int measureBlocks(BlockFace direction) {
		Vector v = direction.getDirection();
		int[] dimensions = getBlockDimensions();
		if (Math.abs(v.getX()) > 0.5) {
			return dimensions[0];
		}
		if (Math.abs(v.getY()) > 0.5) {
			return dimensions[1];
		}
		return dimensions[2];
	}
	
	/**
	 * Gets the current state of this Region
	 * @return The state of this Region
	 */
	public RegionState getState() {
		return new RegionState(this);
	}
	
	/**
	 * Clones this Region
	 * @return A clone of this Region
	 */
	public Region clone() {
		return new Region(start.clone(), end.clone());
	}
	
	/**
	 * Expands the region in all directions, or retracts if negative. If this is a MultiRegion,
	 * makes 6 calls to {@link MultiRegion#expand(BlockFace, int)}, meaning it is very expensive.
	 * Check if this is a MultiRegion before expanding.
	 * @param amount The amount to expand the region by
	 */
	public void expand(int amount) {
		expand(amount, amount, amount, amount, amount, amount);
	}
	
	/**
	 * Expands the region, or retracts where negative values are passed
	 * @param posX The amount to expand the region in the positive X direction
	 * @param negX The amount to expand the region in the negative X direction
	 * @param posY The amount to expand the region in the positive Y direction
	 * @param negY The amount to expand the region in the negative Y direction
	 * @param posZ The amount to expand the region in the positive Z direction
	 * @param negZ The amount to expand the region in the negative Z direction
	 */
	public void expand(int posX, int negX, int posY, int negY, int posZ, int negZ) {
		start = start.subtract(negX, negY, negZ);
		end = end.add(posX, posY, posZ);
		setLocations(start, end);
	}
	
	/**
	 * Expand the region in a given direction, or retracts if negative.
	 * @param direction The direction to expand the region in
	 * @param amount The amount to expand the region in the given direction
	 */
	public void expand(BlockFace direction, int amount) {
		Vector vec = direction.getDirection();
		if (vec.getX() + vec.getY() + vec.getZ() > 0) {
			vec = vec.multiply(amount);
			end = end.add(vec);
		} else {
			vec = vec.multiply(amount);
			start = start.add(vec);
		}
		setLocations(start, end);
	}
	
	/**
	 * Move the region
	 * @param v The vector to be applied to both corners of the region
	 */
	public void move(Vector v) {
		start = start.add(v);
		end = end.add(v);
	}
	
	/**
	 * @return Whether this is a MultiRegion
	 */
	public boolean isMulti() {
		return false;
	}
	
	/**
	 * Move the region
	 * @param x How much to move the region on the X axis
	 * @param y How much to move the region on the Y axis
	 * @param z How much to move the region on the Z axis
	 */
	public void move(int x, int y, int z) {
		move(new Vector(x, y, z));
	}
	
	/**
	 * Protect this region with the given protection types
	 * @param types The events to protect against. See {@link ProtectionType#ALL} and {@link ProtectionType#allExcept(ProtectionType...)}
	 * @return The {@link ProtectedRegion}
	 */
	public ProtectedRegion protect(ProtectionType... types) {
		return new ProtectedRegion(this, types);
	}
	
	/**
	 * Run a lambda on every Block in this Region
	 * @param lambda The lambda to be run on each Block
	 */
	public void forEachBlock(Consumer<Block> lambda) {
		stream().forEach(lambda);
	}
	
	/**
	 * @return The World this Region is in
	 */
	public World getWorld() {
		return start.getWorld();
	}
	
	/**
	 * Check if this Region overlaps with another.
	 * @param o The Region to check against
	 * @return Whether this Region overlaps with the given Region
	 */
	public boolean overlaps(Region o) {
		if (!o.getWorld().equals(getWorld())) {
			return false;
		}
		if (o.isMulti()) {
			MultiRegion multi = (MultiRegion) o;
			return multi.getRegions().stream().anyMatch(r -> r.overlaps(this));
		}
		return (!(start.getX() > o.end.getX() || o.start.getX() > end.getX()
				|| start.getY() > o.end.getY() || o.start.getY() > end.getY()
				|| start.getZ() > o.end.getZ() || o.start.getZ() > end.getZ()));
	}
	
	/**
	 * Gets all entities contained in this Region
	 * @return The entities in this Region
	 */
	public List<Entity> getEntities() {
		List<Entity> entities = new ArrayList<>();
		for (int cx = start.getChunk().getX(); cx <= end.getChunk().getX(); cx++) {
			for (int cz = start.getChunk().getZ(); cz <= end.getChunk().getZ(); cz++) {
				Chunk chunk = start.getWorld().getChunkAt(cx, cz);
				Arrays.stream(chunk.getEntities()).filter(e -> contains(e.getLocation())).forEach(entities::add);
			}
		}
		return entities;
	}
	
	/**
	 * Gets all players contained in this Region
	 * @return The players in this Region
	 */
	public List<Player> getPlayers() {
		return getEntities().stream().filter(e -> e instanceof Player).map(e -> (Player) e).collect(Collectors.toList());
	}
	
	/**
	 * @return All the Chunks this Region overlaps
	 */
	public Set<Chunk> getChunks() {
		Set<Chunk> chunks = new HashSet<>();
		for (int cx = start.getChunk().getX(); cx <= end.getChunk().getX(); cx++) {
			for (int cz = start.getChunk().getZ(); cz <= end.getChunk().getZ(); cz++) {
				chunks.add(start.getWorld().getChunkAt(cx, cz));
			}
		}
		return chunks;
	}
	
	/**
	 * @return A Stream of all the blocks in this Region
	 */
	public Stream<Block> stream() {
		int[] dimensions = this.getBlockDimensions();
		RegionIterator iterator = new RegionIterator(dimensions[0], dimensions[1], dimensions[2]);
		Stream<Block> stream = Stream.generate(() -> {
			int[] pos = iterator.getPosition();
			Block block = start.clone().add(pos[0], pos[1], pos[2]).getBlock();
			iterator.next();
			return block;
		});
		return stream.sequential().limit(getBlockVolume());
	}
	
	/**
	 * Converts this Region to a String which can be converted back with {@link Region#fromString(String)} later
	 * @return The String representation of this Region
	 */
	public String toString() {
		return getWorld().getName() + " " + start.getX() + " " + start.getY() + " " + start.getZ() + " "
				+ end.getX() + " " + end.getY() + " " + end.getZ();
	}
	
	/**
	 * Converts a String generated by {@link Region#toString()} back to a Region
	 * @param input The String representation of a Region
	 * @return The Region
	 */
	public static Region fromString(String input) {
		String[] split = input.split(" ");
		World world = Bukkit.getWorld(split[0]);
		double minX = Double.parseDouble(split[1]);
		double minY = Double.parseDouble(split[2]);
		double minZ = Double.parseDouble(split[3]);
		double maxX = Double.parseDouble(split[4]);
		double maxY = Double.parseDouble(split[5]);
		double maxZ = Double.parseDouble(split[6]);
		return new Region(new Location(world, minX, minY, minZ), new Location(world, maxX, maxY, maxZ));
	}
	
	/**
	 * Represents a state of a Region, not necessarily at the current point in time
	 * @author Redempt
	 *
	 */
	public static class RegionState {
		
		BlockState[][][] blocks;
		private Region region;
			
		private RegionState(Region region) {
			this.region = region;
			int[] dimensions = region.getBlockDimensions();
			blocks = new BlockState[dimensions[0]][dimensions[1]][dimensions[2]];
			for (int x = 0; x <= dimensions[0]; x++) {
				for (int y = 0; y <= dimensions[1]; y++) {
					for (int z = 0; z <= dimensions[2]; z++) {
						Location loc = region.getStart().add(x, y, z);
						blocks[x][y][z] = loc.getBlock().getState();
					}
				}
			}
		}
		
		/**
		 * Restores the Region to this state
		 */
		public void restore() {
			int[] dimensions = region.getBlockDimensions();
			for (int x = 0; x <= dimensions[0]; x++) {
				for (int y = 0; y <= dimensions[1]; y++) {
					for (int z = 0; z <= dimensions[2]; z++) {
						blocks[x][y][z].update(true, false);
					}
				}
			}
		}
		
		/**
		 * Gets all the BlockStates in this RegionState
		 * @return The 3-dimensional array of BlockStates
		 */
		public BlockState[][][] getBlocks() {
			return blocks.clone();
		}
		
	}
	
	private static class RegionIterator {
		
		private int maxX;
		private int maxY;
		private int maxZ;
		private int x = 0;
		private int y = 0;
		private int z = 0;
		
		public RegionIterator(int x, int y, int z) {
			this.maxX = x;
			this.maxY = y;
			this.maxZ = z;
		}
		
		public int[] getPosition() {
			return new int[] {x, y, z};
		}
		
		public boolean next() {
			x++;
			if (x >= maxX) {
				x = 0;
				y++;
				if (y >= maxY) {
					y = 0;
					z++;
					if (z >= maxZ) {
						return false;
					}
				}
			}
			return true;
		}
		
	}
	
}
