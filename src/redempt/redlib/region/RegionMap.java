package redempt.redlib.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

/**
 * Represents a spatial mapping which can retrieve objects by approximate location in O(1)
 * @author Redempt
 * @param <T> The type mapped by this RegionMap
 */
public class RegionMap<T> {
	
	private Map<Coordinate, Set<T>> map = new HashMap<>();
	private int scale;
	
	/**
	 * Constructs a RegionMap with scale 100
	 */
	public RegionMap() {
		this(100);
	}
	
	/**
	 * Constructs a RegionMap with the specified scale
	 * @param scale The distance between things being stored in the internal map. Higher values use less memory
	 *              but more CPU time. If the regions being used to store objects in this RegionMap are especially large,
	 *              use a larger scale. If they are very small and there are a lot of them, use a smaller scale.
	 */
	public RegionMap(int scale) {
		this.scale = scale;
	}
	
	/**
	 * @return The scale of this RegionMap
	 */
	public int getScale() {
		return scale;
	}
	
	/**
	 * Maps a Region to an object
	 * @param region The Region to map the object to
	 * @param object The object to set
	 */
	public void set(Region region, T object) {
		Coordinate start = new Coordinate(region.getStart(), scale);
		Coordinate end = new Coordinate(region.getEnd(), scale);
		for (int x = start.getX(); x <= end.getX(); x++) {
			for (int z = start.getZ(); z <= end.getZ(); z++) {
				Coordinate coord = new Coordinate(start.getWorld(), x, z);
				Set<T> list = map.get(coord);
				if (list == null) {
					list = new HashSet<>();
				}
				list.add(object);
				map.put(coord, list);
			}
		}
	}
	
	/**
	 * Maps a Location to an object
	 * @param loc The location to map the object to
	 * @param object The object to put at this approximate location
	 */
	public void set(Location loc, T object) {
		Coordinate coord = new Coordinate(loc, scale);
		Set<T> list = map.get(coord);
		if (list == null) {
			list = new HashSet<>();
		}
		list.add(object);
		map.put(coord, list);
	}
	
	/**
	 * Removes a mapping by region
	 * @param region The region to remove the mapping from
	 * @param object The object to remove
	 */
	public void remove(Region region, T object) {
		if (object == null) {
			return;
		}
		Coordinate start = new Coordinate(region.getStart(), scale);
		Coordinate end = new Coordinate(region.getEnd(), scale);
		for (int x = start.getX(); x <= end.getX(); x++) {
			for (int z = start.getZ(); z <= end.getZ(); z++) {
				Coordinate coord = new Coordinate(start.getWorld(), x, z);
				Set<T> list = map.get(coord);
				if (list != null) {
					list.remove(object);
					if (list.size() == 0) {
						map.remove(coord);
					}
				}
			}
		}
	}
	
	/**
	 * Removes a mapping by location
	 * @param loc The location to remove the mapping from
	 * @param object The object to remove
	 */
	public void remove(Location loc, T object) {
		if (object == null) {
			return;
		}
		Coordinate coord = new Coordinate(loc, scale);
		Set<T> list = map.get(coord);
		if (list != null) {
			list.remove(object);
			if (list.size() == 0) {
				map.remove(coord);
			}
		}
	}
	
	/**
	 * Gets all objects mapped to an approximate location
	 * @param location The location to check nearby objects for
	 * @return A set of objects mapped near the given location
	 */
	public Set<T> get(Location location) {
		return map.getOrDefault(new Coordinate(location, scale), new HashSet<>());
	}
	
	/**
	 * Gets all objects mapped near the given location
	 * @param location The location to check centered on
	 * @param radius The radius to check
	 * @return A set of all objects mapped near the given location
	 * Note: The radius is not exact, no distance checks are made. Make sure you do your own distance checks
	 * if needed.
	 */
	public Set<T> getNearby(Location location, int radius) {
		radius /= scale;
		radius += 1;
		Set<T> set = new HashSet<>();
		Coordinate center = new Coordinate(location, scale);
		for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
			for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
				Coordinate coord = new Coordinate(location.getWorld(), x, z);
				Set<T> tmp = map.get(coord);
				if (tmp != null) {
					set.addAll(tmp);
				}
			}
		}
		return set;
	}
	
	/**
	 * @return All objects mapped with this RegionMap
	 */
	public Set<T> getAll() {
		Set<T> set = new HashSet<>();
		map.values().forEach(set::addAll);
		return set;
	}
	
	/**
	 * Clears all data from this RegionMap
	 */
	public void clear() {
		map.clear();
	}
	
	private static class Coordinate {
		
		private int x;
		private int z;
		private World world;
		
		public Coordinate(World world, int x, int z) {
			this.x = x;
			this.z = z;
			this.world = world;
		}
		
		public Coordinate(Location location, int scale) {
			this.x = location.getBlockX() / scale;
			this.z = location.getBlockZ() / scale;
			this.world = location.getWorld();
		}
		
		public int getX() {
			return x;
		}
		
		public int getZ() {
			return z;
		}
		
		public World getWorld() {
			return world;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(x, z, world);
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof Coordinate) {
				Coordinate other = (Coordinate) o;
				return other.x == x && other.z == z && other.world.equals(world);
			}
			return super.equals(o);
		}
		
	}
	
}
