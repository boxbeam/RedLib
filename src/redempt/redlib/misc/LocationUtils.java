package redempt.redlib.misc;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.util.Vector;

import redempt.redlib.RedLib;
import redempt.redlib.commandmanager.Messages;

public class LocationUtils {
	
	/**
	 * An array of all the block faces which face in a single direction (positive X, negative X, etc.)
	 */
	public static final BlockFace[] PRIMARY_BLOCK_FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
	
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
	 * Gets the Vector direction of a BlockFace. For use in versions below 1.13.
	 * @param face The block face
	 * @return The vector representing the direction
	 */
	public static Vector getDirection(BlockFace face) {
		return new Vector(face.getModX(), face.getModY(), face.getModZ());
	}
	
	private static DecimalFormat timeFormat = new DecimalFormat("0.#");
	
	/**
	 * Initiates a delayed teleport for a player which will be cancelled if the player moves. The messages
	 * relevant to this will be sent automatically.
	 * @param player The player to teleport
	 * @param loc The location to teleport the player to after the delay
	 * @param ticks The delay for the teleport, in ticks
	 * @param result A lambda to handle the result, given true if the teleport succeeded, false otherwise
	 */
	public static void delayedTeleport(Player player, Location loc, int ticks, Consumer<Boolean> result) {
		double seconds = ticks / 20d;
		player.sendMessage(Messages.msg("teleportDelay").replace("%seconds%", timeFormat.format(seconds)));
		Location start = player.getLocation();
		Task[] task = {null};
		EventListener<?> listener = new EventListener<>(RedLib.getInstance(), PlayerMoveEvent.class, (l, e) -> {
			if (!e.getPlayer().equals(player)) {
				return;
			}
			if (!start.getWorld().equals(e.getTo().getWorld()) || start.distanceSquared(e.getTo()) > 0.125) {
				player.sendMessage(Messages.msg("teleportCancelled"));
				task[0].cancel();
				l.unregister();
				result.accept(false);
			}
		});
		task[0] = Task.syncDelayed(RedLib.getInstance(), () -> {
			player.teleport(loc);
			listener.unregister();
			result.accept(true);
		}, ticks);
	}
	
	/**
	 * Initiates a delayed teleport for a player which will be cancelled if the player moves. The messages
	 * relevant to this will be sent automatically.
	 * @param player The player to teleport
	 * @param loc The location to teleport the player to after the delay
	 * @param ticks The delay for the teleport, in ticks
	 */
	public static void delayedTeleport(Player player, Location loc, int ticks) {
		delayedTeleport(player, loc, ticks, b -> {});
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
	 * Returns the Location at the center of a Block - shorthand
	 * @param block The Block to get the center of
	 * @return The center of the Block
	 */
	public static Location center(Block block) {
		return block.getLocation().add(.5, .5, .5);
	}
	
	/**
	 * Sets the location's coordinates to its block coordinates, then returns it
	 * @param loc The location
	 * @return The block location
	 */
	public static Location toBlockLocation(Location loc) {
		loc.setX(loc.getBlockX());
		loc.setY(loc.getBlockY());
		loc.setZ(loc.getBlockZ());
		return loc;
	}
	
	/**
	 * Sets the location's coordinates to the center point of its block coordinates, then returns it
	 * @param loc The location
	 * @return The block location
	 */
	public static Location center(Location loc) {
		return loc.add(.5, .5, .5);
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
			if (list == null) {
				return;
			}
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
	
	/**
	 * Finds the fastest path between a starting and ending location using A*, then removes unneeded steps for straight
	 * @param start The starting block
	 * @param end The ending block
	 * @param max The max number of locations to be checked - use to limit runtime
	 * @param filter A filter to determine which blocks are passable
	 * @return A List of locations leading from the start to the end, or the closest block if the path could not
	 * be completed
	 */
	public static List<Location> directPathfind(Block start, Block end, int max, Predicate<Block> filter) {
		List<Location> path = new ArrayList<>(pathfind(start, end, max, filter));
		for (int i = 0; i + 2 < path.size(); i += 2) {
			Location first = path.get(i);
			Location second = path.get(i + 2);
			if (Path.getPath(first, second, 0.25).stream().map(Location::getBlock).allMatch(filter)) {
				path.remove(i + 1);
				i -= 2;
			}
		}
		return path;
	}
	
	/**
	 * Finds the fastest path between a starting and ending location using A*, then removes unneeded steps for straight
	 * @param start The starting block
	 * @param end The ending block
	 * @param max The max number of locations to be checked - use to limit runtime
	 * @return A List of locations leading from the start to the end, or the closest block if the path could not
	 * be completed
	 */
	public static List<Location> directPathfind(Block start, Block end, int max) {
		return directPathfind(start, end, max, b -> !b.getType().isSolid());
	}
	
	/**
	 * Finds the fastest path between a starting and ending location using A*
	 * @param start The starting block
	 * @param end The ending block
	 * @param max The max number of locations to be checked - use to limit runtime
	 * @return A Deque of locations leading from the start to the end, or the closest block if the path could not
	 * be completed
	 */
	public static Deque<Location> pathfind(Block start, Block end, int max) {
		return pathfind(start, end, max, b -> !b.getType().isSolid());
	}
	
	/**
	 * Finds the fastest path between a starting and ending location using A*
	 * @param start The starting block
	 * @param end The ending block
	 * @param max The max number of locations to be checked - use to limit runtime
	 * @param filter A filter to determine which blocks are passable
	 * @return A Deque of locations leading from the start to the end, or the closest block if the path could not
	 * be completed
	 */
	public static Deque<Location> pathfind(Block start, Block end, int max, Predicate<Block> filter) {
		if (!start.getWorld().equals(end.getWorld())) {
			throw new IllegalArgumentException("Start and end must be in the same world");
		}
		Set<Block> nodes = new HashSet<>();
		PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.score));
		Set<Block> exclude = new HashSet<>();
		Node node = new Node(start, 0);
		node.score = score(node, start, end);
		nodes.add(node.block);
		queue.add(node);
		int iter = 0;
		Node least = node;
		int leastDist = distance(least.block, end);
		while (iter < max) {
			node = queue.poll();
			if (node == null) {
				return tracePath(least);
			}
			nodes.remove(node.block);
			int dist = distance(node.block, end);
			if (dist == 0 || (dist == 1 && !filter.test(end))) {
				return tracePath(node);
			} else {
				if (dist < leastDist) {
					leastDist = dist;
					least = node;
				}
			}
			exclude.add(node.block);
			getAdjacent(node, start, end, n -> {
				if (exclude.contains(n.block) || !filter.test(n.block)) {
					exclude.add(n.block);
					return;
				}
				if (nodes.add(n.block)) {
					queue.add(n);
				}
			});
			iter++;
		}
		return tracePath(least);
	}
	
	private static Deque<Location> tracePath(Node node) {
		Deque<Location> path = new ArrayDeque<>();
		while (node != null) {
			path.addFirst(node.block.getLocation().add(.5, .5, .5));
			node = node.parent;
		}
		return path;
	}
	
	private static void getAdjacent(Node block, Block start, Block end, Consumer<Node> lambda) {
		lambda.accept(getRelative(block, start, end, 1, 0, 0));
		lambda.accept(getRelative(block, start, end, -1, 0, 0));
		lambda.accept(getRelative(block, start, end, 0, 1, 0));
		lambda.accept(getRelative(block, start, end, 0, -1, 0));
		lambda.accept(getRelative(block, start, end, 0, 0, 1));
		lambda.accept(getRelative(block, start, end, 0, 0, -1));
	}
	
	private static Node getRelative(Node block, Block start, Block end, int x, int y, int z) {
		Block b = block.block.getRelative(x, y, z);
		int score = score(block, start, end);
		Node node = new Node(b, score);
		node.parent = block;
		return node;
	}
	
	private static int score(Node node, Block start, Block end) {
		return distance(node.block, start) + distance(node.block, end) * 2;
	}
	
	private static int distance(Block first, Block second) {
		return Math.abs(first.getX() - second.getX())
				+ Math.abs(first.getY() - second.getY())
				+ Math.abs(first.getZ() - second.getZ());
	}
	
	private static class Node {
		
		public Block block;
		public int score;
		public Node parent;
		
		public Node(Block block, int score) {
			this.block = block;
			this.score = score;
		}
		
	}
	
}
