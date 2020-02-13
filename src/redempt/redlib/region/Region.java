package redempt.redlib.region;

import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

/**
 * Represents a cubic region in a world
 * @author Redempt
 *
 */
public class Region {
	
	private Location start;
	private Location end;
	
	/**
	 * Construct a Region using 2 corners
	 * @param start The first corner
	 * @param end The second corner
	 */
	public Region(Location start, Location end) {
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
	 * Check whether a location is inside this Region
	 * @param loc The location to check
	 * @return Whether the location is inside this Region
	 */
	public boolean isInside(Location loc) {
		return loc.getX() >= start.getX() && loc.getY() >= start.getY() && loc.getZ() >= start.getZ() &&
				loc.getX() <= end.getX() && loc.getY() <= end.getY() && loc.getZ() <= end.getZ();
	}
	
	/**
	 * Get the dimensions of this Region [x, y, z] in blocks
	 * @return The dimensions of this Region
	 */
	public int[] getBlockDimensions() {
		return new int[] {end.getBlockX() - start.getBlockX(), end.getBlockY() - start.getBlockY(), end.getBlockZ() - start.getBlockZ()};
	}
	
	/**
	 * Get the dimensions of this Region [x, y, z]
	 * @return The dimensions of this Region
	 */
	public double[] getDimensions() {
		return new double[] {end.getX() - start.getX(), end.getY() - start.getY(), end.getZ() - start.getZ()};
	}
	
	/**
	 * Gets the current state of this Region
	 * @return The state of this Region
	 */
	public RegionState getState() {
		return new RegionState(this);
	}
	
	/**
	 * Run a lambda on every Block in this Region
	 * @param lambda The lambda to be run on each Block
	 */
	public void forEachBlock(Consumer<Block> lambda) {
		int[] dimensions = this.getBlockDimensions();
		for (int x = 0; x < dimensions[0]; x++) {
			for (int y = 0; y < dimensions[1]; y++) {
				for (int z = 0; z < dimensions[2]; z++) {
					Location loc = getStart().add(x, y, z);
					lambda.accept(loc.getBlock());
				}
			}
		}
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
			for (int x = 0; x < dimensions[0]; x++) {
				for (int y = 0; y < dimensions[1]; y++) {
					for (int z = 0; z < dimensions[2]; z++) {
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
			for (int x = 0; x < dimensions[0]; x++) {
				for (int y = 0; y < dimensions[1]; y++) {
					for (int z = 0; z < dimensions[2]; z++) {
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
	
}
