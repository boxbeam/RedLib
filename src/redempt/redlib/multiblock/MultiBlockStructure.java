package redempt.redlib.multiblock;

import org.bukkit.Location;
import org.bukkit.block.Block;

import redempt.redlib.RedLib;

/**
 * A utility class to create interactive multi-block structures
 * @author Redempt
 *
 */
public class MultiBlockStructure {
	
	/**
	 * Use this to get the info to build a multi-block structure.
	 * Should be hard-coded.
	 * @param start One bounding corner of the region
	 * @param end The other bounding corner of the region
	 * @return A string representing all of the block data for the region
	 */
	@SuppressWarnings("deprecation")
	public static String stringify(Location start, Location end) {
		if (!start.getWorld().equals(end.getWorld())) {
			throw new IllegalArgumentException("Locations must be in the same  world");
		}
		int minX = Math.min(start.getBlockX(), end.getBlockX());
		int minY = Math.min(start.getBlockY(), end.getBlockY());
		int minZ = Math.min(start.getBlockZ(), end.getBlockZ());
		
		int maxX = Math.max(start.getBlockX(), end.getBlockX());
		int maxY = Math.max(start.getBlockY(), end.getBlockY());
		int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());
		
		int midVersion = Integer.parseInt(RedLib.getServerVersion().split("\\.")[1]);
		
		String output = (maxX - minX) + "x" + (maxY - minY) + "x" + (maxZ - minZ) + ";";
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = new Location(start.getWorld(), x, y, z).getBlock();
					block.getType();
					if (midVersion >= 13) {
						output += block.getBlockData().getAsString() + ";";
					} else {
						output += block.getType() + ":" + block.getData();
					}
				}
			}
		}
		return output;
	}
	
	public static enum Axis {
		
		X,
		Y,
		Z;
		
	}
	
}
