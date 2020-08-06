package redempt.redlib.misc;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class Path {
	
	/**
	 * Get the locations between the start and end location
	 * @param start The start location
	 * @param end The end location
	 * @param step The step size to use
	 * @return A list of all the locations between the locations
	 */
	public static List<Location> getPath(Location start, Location end, double step) {
		List<Location> locs = new ArrayList<>();
		locs.add(start);
		Vector v = end.clone().subtract(start).toVector();
		v = v.normalize().multiply(step);
		Location current = start.clone();
		while (current.distance(end) > step) {
			locs.add(current.clone());
			current = current.add(v);
		}
		locs.add(end);
		return locs;
	}
	
	/**
	 * Get the locations between the start and end location
	 * @param start The start location
	 * @param end The end location
	 * @return A list of all the locations between the locations, equidistant
	 */
	public static List<Location> getPath(Location start, Location end) {
		return getPath(start, end, 1);
	}
	
	/**
	 * Get the locations from the start along a vector
	 * @param start The start location
	 * @param direction The vector indicating direction
	 * @param distance The length of the path
	 * @param step The step size to use
	 * @return A list of all the locations between the locations, equidistant
	 */
	public static List<Location> getPath(Location start, Vector direction, double distance, double step) {
		direction = direction.clone().normalize().multiply(distance);
		Location end = start.clone().add(direction);
		return getPath(start, end, step);
	}
	
	/**
	 * Get the locations from the start along a vector
	 * @param start The start location
	 * @param direction The vector indicating direction
	 * @param distance The max distance to step
	 * @return A list of all the locations between the locations, equidistant
	 */
	public static List<Location> getPath(Location start, Vector direction, double distance) {
		return getPath(start, direction, distance, 1);
	}
	
	/**
	 * Get the locations from the start along a vector
	 * @param start The start location
	 * @param direction The vector indicating direction and length
	 * @return A list of all the locations between the locations, equidistant
	 */
	public static List<Location> getPath(Location start, Vector direction) {
		return getPath(start, direction, direction.length(), 1);
	}
	
	/**
	 * Get the locations from the start along a vector
	 * @param start The start location whose direction vector will be used for direction and length
	 * @return A list of all the locations between the locations, equidistant
	 */
	public static List<Location> getPath(Location start) {
		return getPath(start, start.getDirection(), start.getDirection().length(), 1);
	}
	
	/**
	 * Get the locations from the start along a vector
	 * @param start The start location whose direction vector will be used for direction and length
	 * @param step The step size to use
	 * @return A list of all the locations between the locations, equidistant
	 */
	public static List<Location> getPath(Location start, double step) {
		return getPath(start, start.getDirection(), start.getDirection().length(), step);
	}
	
}
