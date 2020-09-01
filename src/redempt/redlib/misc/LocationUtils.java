package redempt.redlib.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.world.WorldLoadEvent;
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
		if (under.getType().isSolid()) {
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
		Location nearest = null;
		double dist = 0;
		for (int y = 0; Math.abs(y) <= maxDistance; y = y == 0 ? 1 : -y - Math.min(Integer.signum(y), 0)) {
			for (int x = 0; Math.abs(x) <= maxDistance; x = x == 0 ? 1 : -x - Math.min(Integer.signum(x), 0)) {
				for (int z = 0; Math.abs(z) <= maxDistance; z = z == 0 ? 1 : -z - Math.min(Integer.signum(z), 0)) {
					Location check = loc.clone().add(x, y, z);
					if (isSafe(check) && filter.test(check)) {
						check.setDirection(direction);
						double distance = check.distanceSquared(loc);
						if (nearest == null || distance < dist) {
							nearest = check;
							dist = distance;
							if (dist <= 1) {
								return nearest;
							}
						}
					}
				}
			}
		}
		return nearest;
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
	
	/**
	 * Converts a Location to a String
	 * @param loc The Location to be stringified
	 * @param separator The separator to use between pieces of information
	 * @return The stringified Location
	 */
	public static String toString(Location loc, String separator) {
		return new StringBuilder().append(loc.getWorld().getName()).append(separator)
				.append(loc.getX()).append(separator)
				.append(loc.getY()).append(separator)
				.append(loc.getZ()).toString();
	}
	
	/**
	 * Converts a String back to a Location
	 * @param string The stringified Location
	 * @param separator The separator that was used in toString
	 * @return The Location
	 */
	public static Location fromString(String string, String separator) {
		String[] split = string.split(Pattern.quote(separator));
		World world = Bukkit.getWorld(split[0]);
		double x = Double.parseDouble(split[1]);
		double y = Double.parseDouble(split[2]);
		double z = Double.parseDouble(split[3]);
		Location location = new Location(world, x, y, z);
		if (world == null) {
			waitForWorld(split[0], location::setWorld);
		}
		return location;
	}
	
	/**
	 * Converts a Location to a String representing its location
	 * @param block The Block location to be stringified
	 * @param separator The separator to use between pieces of information
	 * @return The stringified location
	 */
	public static String toString(Block block, String separator) {
		return new StringBuilder().append(block.getWorld().getName()).append(separator)
				.append(block.getX()).append(separator)
				.append(block.getY()).append(separator)
				.append(block.getZ()).toString();
	}
	
	/**
	 * Converts a Location to a String representing its location
	 * @param block The Block location to be stringified
	 * @return The stringified location
	 */
	public static String toString(Block block) {
		return toString(block, " ");
	}
	
	/**
	 * Loads a Location from a String. If the world this Location is in is not yet loaded, waits for it to load, then passes
	 * the Location to the callback.
	 * @param string The String to be parsed into a Location
	 * @param separator The separator used when converting this Location to a String
	 * @param callback The callback to use the Location once it has been loaded
	 */
	public static void fromStringLater(String string, String separator, Consumer<Location> callback) {
		String[] split = string.split(Pattern.quote(separator));
		World world = Bukkit.getWorld(split[0]);
		double x = Double.parseDouble(split[1]);
		double y = Double.parseDouble(split[2]);
		double z = Double.parseDouble(split[3]);
		if (world != null) {
			callback.accept(new Location(world, x, y, z));
			return;
		}
		new EventListener<>(RedLib.getInstance(), WorldLoadEvent.class, (l, e) -> {
			if (e.getWorld().getName().equals(split[0])) {
				World w = Bukkit.getWorld(split[0]);
				callback.accept(new Location(w, x, y, z));
				l.unregister();
			}
		});
	}
	
	/**
	 * Loads a Location from a String. If the world this Location is in is not yet loaded, waits for it to load, then passes
	 * the Location to the callback.
	 * @param string The String to be parsed into a Location
	 * @param callback The callback to use the Location once it has been loaded
	 */
	public static void fromStringLater(String string, Consumer<Location> callback) {
		fromStringLater(string, " ", callback);
	}
	
	/**
	 * Converts a Location to a String. The same as calling toString(Location, " ")
	 * @param loc The Location to be stringified
	 * @return The stringified Location
	 */
	public static String toString(Location loc) {
		return toString(loc, " ");
	}
	
	/**
	 * Converts a String back to a Location. The same as calling fromString(String, " ")
	 * @param string The stringified Location
	 * @return The Location
	 */
	public static Location fromString(String string) {
		return fromString(string, " ");
	}
	
	private static Map<String, List<Consumer<World>>> waiting = new HashMap<>();
	private static boolean initialized = false;
	
	private static void initializeListener() {
		if (initialized) {
			return;
		}
		initialized = true;
		new EventListener<>(RedLib.getInstance(), WorldLoadEvent.class, e -> {
			List<Consumer<World>> list = waiting.remove(e.getWorld().getName());
			list.forEach(c -> c.accept(e.getWorld()));
		});
	}
	
	/**
	 * Waits for a world with the given name to load before calling the callback
	 * @param worldname The name of the world
	 * @param callback A callback to be passed the world when it loads
	 */
	public static void waitForWorld(String worldname, Consumer<World> callback) {
		World world = Bukkit.getWorld(worldname);
		if (world != null) {
			callback.accept(world);
			return;
		}
		waiting.putIfAbsent(worldname, new ArrayList<>());
		List<Consumer<World>> list = waiting.get(worldname);
		list.add(callback);
		initializeListener();
	}
	
	/**
	 * Gets the chunk X and Z of a location
	 * @param loc The location to get the chunk coordinates of
	 * @return An array containing the chunk coordinates [x, z]
	 */
	public static int[] getChunkCoordinates(Location loc) {
		return new int[] {loc.getBlockX() >> 4, loc.getBlockZ() >> 4};
	}
	
}
