package redempt.redlib.misc;

import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import redempt.redlib.RedLib;

public class LocationUtils {
	
	/**
	 * Checks if a given block type is a hazard - whether it would damage the player if they were on top of it
	 * @param type The type to check
	 * @return Whether the block type is a hazard
	 */
	public static boolean isHazard(Material type) {
		type = new ItemStack(type).getType();
		if (type.toString().contains("LAVA") || type.toString().contains("WATER")) {
			return true;
		}
		if (type.toString().contains("PORTAL") && !type.toString().endsWith("PORTAL_FRAME")) {
			return true;
		}
		if (type.toString().equals("MAGMA_BLOCK") || type.toString().equals("CAMPFIRE")) {
			return true;
		}
		return false;
	}
	
	/**
	 * Checks whether the given location is safe to teleport a player to - that a player would not be damaged as a result of being moved to this location
	 * @param loc The location to check
	 * @return Whether the given location is safe
	 */
	public static boolean isSafe(Location loc) {
		Block under = loc.clone().subtract(0, 1, 0).getBlock();
		if (!isHazard(under.getType()) && isFullBlock(under)) {
			Block middle = loc.getBlock();
			Block above = loc.clone().add(0, 1, 0).getBlock();
			if (!isHazard(middle.getType()) && !isHazard(above.getType())) {
				if (!middle.getType().isSolid() && !above.getType().isSolid() && !middle.isLiquid() && !above.isLiquid()) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static boolean isFullBlock(Block block) {
		if (RedLib.midVersion >= 13) {
			return block.getType().isSolid() && block.getBoundingBox().getVolume() == 1;
		} else {
			return block.getType().isOccluding();
		}
	}
	
	/**
	 * Gets the nearest safe location to the given location within the given distance passing the given predicate check
	 * @param loc The location to find the nearest safe location to
	 * @param maxDistance The maximum distance to check from this location
	 * @param filter Used to filter safe locations that you still don't want to send the player to. Any locations this returns false for will be ignored.
	 * @return The nearest safe location, or null if one was not found
	 */
	public static Location getNearestSafeLocation(Location loc, int maxDistance, Predicate<Location> filter) {
		Vector direction = loc.getDirection();
		loc = loc.getBlock().getLocation().add(0.5, 0.1, 0.5);
		if (isSafe(loc) && filter.test(loc)) {
			loc.setDirection(direction);
			return loc;
		}
		for (int y = 0; Math.abs(y) <= maxDistance; y = y == 0 ? 1 : -y - Math.min(Integer.signum(y), 0)) {
			for (int x = 0; Math.abs(x) <= Math.abs(y); x = x == 0 ? 1 : -x - Math.min(Integer.signum(x), 0)) {
				for (int z = 0; Math.abs(z) <= Math.abs(x); z = z == 0 ? 1 : -z - Math.min(Integer.signum(z), 0)) {
					Location check = loc.clone().add(x, y, z);
					if (isSafe(check) && filter.test(check)) {
						check.setDirection(direction);
						return check;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Gets the nearest safe location to the given location within the given distance
	 * @param loc The location to find the nearest safe location to
	 * @param maxDistance The maximum distance to check from this location
	 * @return The nearest safe location, or null if one was not found
	 */
	public static Location getNearestSafeLocation(Location loc, int maxDistance) {
		return getNearestSafeLocation(loc, maxDistance, l -> true);
	}
	
}
