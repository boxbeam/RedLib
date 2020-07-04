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
	
	/**
	 * Maps a Region to an object
	 * @param region The Region to map the object to
	 * @param object The object to set
	 */
	public void set(Region region, T object) {
		Coordinate start = new Coordinate(region.getStart());
		Coordinate end = new Coordinate(region.getEnd());
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
	 * Removes a mapping
	 * @param region The region to remove the mapping from
	 * @param object The object to remove
	 */
	public void remove(Region region, T object) {
		Coordinate start = new Coordinate(region.getStart());
		Coordinate end = new Coordinate(region.getEnd());
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
	 * Gets all objects mapped to an approximate location
	 * @param location The location to check nearby objects for
	 * @return A set of objects mapped near the given location
	 */
	public Set<T> get(Location location) {
		return map.getOrDefault(new Coordinate(location), new HashSet<>());
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
		
		public Coordinate(Location location) {
			this.x = location.getBlockX() / 100;
			this.z = location.getBlockZ() / 100;
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
