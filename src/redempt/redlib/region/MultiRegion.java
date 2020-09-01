package redempt.redlib.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a collection of Regions forming any shape
 *
 * @author Redempt
 */
public class MultiRegion extends Region {
	
	private static BlockFace[] faces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
	private static Vector[] adjacent = {new Vector(.5, .5, .5), new Vector(.5, .5, -.5), new Vector(.5, -.5, .5), new Vector(-.5, .5, .5),
										new Vector(-.5, -.5, .5), new Vector(-.5, .5, -.5), new Vector(.5, -.5, -.5), new Vector(-.5, -.5, -.5)};
	
	private List<Region> regions = new ArrayList<>();
	private List<Region> subtract = new ArrayList<>();
	private boolean clustered = false;
	
	/**
	 * Construct a MultiRegion using a list of Regions
	 *
	 * @param regions The list of Regions
	 */
	public MultiRegion(List<Region> regions) {
		if (regions.size() == 0) {
			throw new IllegalArgumentException("Cannot create MultiRegion from 0 regions");
		}
		World world = regions.get(0).getWorld();
		for (Region region : regions) {
			if (region.isMulti()) {
				clustered = true;
			}
			if (!region.getWorld().equals(world)) {
				throw new IllegalArgumentException("All regions must be in the same world");
			}
			this.regions.add(region);
		}
		fixCorners(null);
	}
	
	private void fixCorners(Region r) {
		if (r == null) {
			World world = regions.get(0).getWorld();
			double minX = this.regions.stream().min((a, b) -> (int) Math.signum(a.getStart().getX() - b.getStart().getX())).get().getStart().getX();
			double minY = this.regions.stream().min((a, b) -> (int) Math.signum(a.getStart().getY() - b.getStart().getY())).get().getStart().getY();
			double minZ = this.regions.stream().min((a, b) -> (int) Math.signum(a.getStart().getZ() - b.getStart().getZ())).get().getStart().getZ();
			
			double maxX = this.regions.stream().max((a, b) -> (int) Math.signum(a.getEnd().getX() - b.getEnd().getX())).get().getEnd().getX();
			double maxY = this.regions.stream().max((a, b) -> (int) Math.signum(a.getEnd().getY() - b.getEnd().getY())).get().getEnd().getY();
			double maxZ = this.regions.stream().max((a, b) -> (int) Math.signum(a.getEnd().getZ() - b.getEnd().getZ())).get().getEnd().getZ();
			
			start = new Location(world, minX, minY, minZ);
			end = new Location(world, maxX, maxY, maxZ);
			return;
		}
		World world = regions.get(0).getWorld();
		double minX = Math.min(start.getX(), r.getStart().getX());
		double minY = Math.min(start.getY(), r.getStart().getY());
		double minZ = Math.min(start.getZ(), r.getStart().getZ());
		
		double maxX = Math.max(end.getX(), r.getEnd().getX());
		double maxY = Math.max(end.getY(), r.getEnd().getY());
		double maxZ = Math.max(end.getZ(), r.getEnd().getZ());
		
		start = new Location(world, minX, minY, minZ);
		end = new Location(world, maxX, maxY, maxZ);
		return;
	}
	
	/**
	 * Construct a MultiRegion using a vararg of Regions
	 *
	 * @param regions The vararg of Regions
	 */
	public MultiRegion(Region... regions) {
		this(Arrays.stream(regions).collect(Collectors.toList()));
	}
	
	/**
	 * Adds a Region to this MultiRegion
	 *
	 * @param region The Region to add
	 */
	public void add(Region region) {
		if (!region.getWorld().equals(getWorld())) {
			throw new IllegalArgumentException("Region is not in the same world as this MultiRegion");
		}
		if (region.isMulti() && !clustered) {
			MultiRegion multi = (MultiRegion) region;
			for (Region r : multi.getRegions()) {
				regions.add(r.clone());
			}
			fixCorners(region);
			return;
		}
		regions.add(region.clone());
		fixCorners(region);
	}
	
	/**
	 * Subtracts a Region from this MultiRegion. A subtracted Region overrides all positive Regions,
	 * meaning adding a Region that overlaps a previously subtracted Region will not add the overlapping blocks.
	 * Calling {@link MultiRegion#recalculate()} will coalesce into only added Regions.
	 *
	 * @param region The Region to subtract
	 */
	public void subtract(Region region) {
		if (!region.getWorld().equals(getWorld())) {
			throw new IllegalArgumentException("Region is not in the same world as this MultiRegion");
		}
		subtract.add(region.clone());
	}
	
	/**
	 * Checks whether this MultiRegion contains the given Location
	 *
	 * @param location The Location to check
	 * @return Whether the Location is contained within this MultiRegion
	 */
	public boolean contains(Location location) {
		if (!getWorld().equals(location.getWorld())) {
			return false;
		}
		if (location.getX() < start.getX() || location.getY() < start.getY() || location.getZ() < start.getZ()
				|| location.getX() > end.getX() || location.getY() > end.getY() || location.getZ() > end.getZ()) {
			return false;
		}
		return contains(regions, location) && !contains(subtract, location);
	}
	
	private static boolean contains(List<Region> regions, Location loc) {
		return regions.stream().anyMatch(r -> r.contains(loc));
	}
	
	/**
	 * Gets all the cuboid Regions that form this MultiRegion
	 *
	 * @return The list of Regions that form this MultiRegion
	 */
	public List<Region> getRegions() {
		return regions;
	}
	
	/**
	 * Sums the block volume of all the Regions that make up this MultiRegion.
	 * Will be inaccurate if any of the Regions overlap. Call {@link MultiRegion#recalculate()} first.
	 *
	 * @return The volume of this MultiRegion
	 */
	public int getBlockVolume() {
		int total = 0;
		for (Region region : regions) {
			total += region.getBlockVolume();
		}
		for (Region region : subtract) {
			total -= region.getBlockVolume();
		}
		return total;
	}
	
	/**
	 * Sums the volume of all the Regions that make up this MultiRegion.
	 * Will be inaccurate if any of the Regions overlap. Call {@link MultiRegion#recalculate()} first.
	 *
	 * @return The volume of this MultiRegion
	 */
	@Override
	public double getVolume() {
		double total = 0;
		for (Region region : regions) {
			total += region.getVolume();
		}
		for (Region region : subtract) {
			total -= region.getVolume();
		}
		return total;
	}
	
	/**
	 * @return Whether this Region is a non-cuboid variant
	 */
	@Override
	public boolean isMulti() {
		return true;
	}
	
	/**
	 * Clones this MultiRegion
	 *
	 * @return A clone of this MultiRegion
	 */
	@Override
	public MultiRegion clone() {
		List<Region> clone = new ArrayList<>();
		regions.stream().map(Region::clone).forEach(clone::add);
		MultiRegion multi = new MultiRegion(clone);
		subtract.forEach(multi::subtract);
		return multi;
	}
	
	/**
	 * Expands the MultiRegion in a given direction, or retracts if negative.
	 * Expanding takes a one-block wide slice on the face of the direction given, and duplicates it forward
	 * in that direction the given number of times. Retracting subtracts a region of n width in the direction
	 * given on the face of the direction. It is highly recommended to call {@link MultiRegion#recalculate()}
	 * after calling this, especially if it is a retraction. This is a fairly expensive operation,
	 * so use it sparingly.
	 *
	 * @param direction The direction to expand the region in
	 * @param amount    The amount to expand the region in the given direction
	 */
	@Override
	public void expand(BlockFace direction, int amount) {
		if (amount == 0) {
			return;
		}
		if (amount < 0) {
			Region r = new Region(start, end);
			r.expand(direction.getOppositeFace(), -r.measureBlocks(direction));
			r.expand(direction.getOppositeFace(), Math.abs(amount));
			subtract.add(r);
			return;
		}
		Region r = new Region(start, end);
		r.expand(direction.getOppositeFace(), -(r.measureBlocks(direction) - 1));
		MultiRegion slice = getIntersection(r);
		slice.move(direction.getDirection());
		for (int i = 0; i < amount; i++) {
			MultiRegion clone = slice.clone();
			clone.move(direction.getDirection().multiply(i));
			add(clone);
		}
		fixCorners(null);
	}
	
	/**
	 * Expands the region, or retracts if negative. This makes 6 calls to {@link MultiRegion#expand(BlockFace, int)},
	 * meaning it is very expensive. Avoid calling this method if possible.
	 *
	 * @param posX The amount to expand the region in the positive X direction
	 * @param negX The amount to expand the region in the negative X direction
	 * @param posY The amount to expand the region in the positive Y direction
	 * @param negY The amount to expand the region in the negative Y direction
	 * @param posZ The amount to expand the region in the positive Z direction
	 * @param negZ The amount to expand the region in the negative Z direction
	 */
	public void expand(int posX, int negX, int posY, int negY, int posZ, int negZ) {
		expand(BlockFace.EAST, posX);
		expand(BlockFace.WEST, negX);
		expand(BlockFace.SOUTH, posZ);
		expand(BlockFace.NORTH, negZ);
		expand(BlockFace.UP, posY);
		expand(BlockFace.DOWN, negY);
	}
	
	/**
	 * Turns this MultiRegion into a cuboid Region using the extreme corners
	 *
	 * @return A cuboid region guaranteed to have equal or greater coverage compared to this MultiRegion
	 */
	public Region toCuboid() {
		return new Region(this.start, this.end);
	}
	
	/**
	 * automatically clusters regions in clusters of 10 until there are less than 25 top-level regions
	 */
	public void autoCluster() {
		while (regions.size() > 25) {
			cluster(10);
		}
	}
	
	/**
	 * Smartly groups Regions in close proximity into clusters. Since {@link MultiRegion#contains(Location)} first checks
	 * if the given Location is within the extreme corners of a MultiRegion, this will significantly increase the performance
	 * of {@link MultiRegion#contains(Location)} for MultiRegions which contain many Regions by allowing it to skip having
	 * to check {@link Region#contains(Location)} on many nearby Regions. This method can be called multiple times, which
	 * will further cluster the clusters. It is recommended to use {@link MultiRegion#autoCluster()} in most cases.
	 *
	 * @param per The number of Regions that should be in each cluster
	 */
	public void cluster(int per) {
		clustered = true;
		Region smallest = null;
		for (Region region : regions) {
			if (smallest == null || region.getBlockVolume() < smallest.getBlockVolume()) {
				smallest = region;
			}
		}
		Location center = smallest.getCenter();
		regions.sort((a, b) -> (int) Math.signum(a.getCenter().distanceSquared(center) - b.getCenter().distanceSquared(center)));
		List<Region> list = new ArrayList<>();
		Set<Region> set = new HashSet<>();
		set.add(smallest);
		list.add(smallest);
		while (set.size() < regions.size()) {
			Region prev = list.get(list.size() - 1);
			double radius = getApproxRadius(prev);
			Location middle = prev.getCenter();
			Region closest = null;
			double distance = 0;
			for (Region region : regions) {
				if (set.contains(region)) {
					continue;
				}
				double dist = middle.distance(region.getCenter());
				if (dist / 1.5 <= getApproxRadius(region) + radius) {
					closest = region;
					break;
				}
				if (closest == null || dist < distance) {
					distance = dist;
					closest = region;
				}
			}
			set.add(closest);
			list.add(closest);
		}
		List<Region> cluster = new ArrayList<>();
		int count = 0;
		MultiRegion current = null;
		for (int i = 0; i < list.size(); i++) {
			Region reg = list.get(i);
			if (current == null) {
				current = new MultiRegion(reg);
			} else {
				current.add(reg);
			}
			count++;
			if (count >= per) {
				cluster.add(current);
				current = null;
				count = 0;
			}
		}
		if (current != null) {
			cluster.add(current);
		}
		this.regions = cluster;
	}
	
	private double getApproxRadius(Region region) {
		double[] dim = region.getDimensions();
		return (dim[0] + dim[1] + dim[2]) / 3;
	}
	
	/**
	 * Flattens any clusters in this MultiRegion so that it is composed only of cuboid Regions
	 */
	public void decluster() {
		if (!clustered) {
			return;
		}
		clustered = false;
		List<Region> regions = new ArrayList<>();
		for (Region region : this.regions) {
			if (region.isMulti()) {
				MultiRegion multi = (MultiRegion) region.clone();
				multi.decluster();
				regions.addAll(multi.getRegions());
				continue;
			}
			regions.add(region);
		}
		this.regions = regions;
	}
	
	/**
	 * @return Whether this MultiRegion has been clustered into smaller Regions
	 */
	public boolean isClustered() {
		return clustered;
	}
	
	/**
	 * Recursively gets the number of Regions in this MultiRegion
	 *
	 * @return The total number of cuboid Regions composing this MultiRegion
	 */
	public int getRegionCount() {
		if (!clustered) {
			return regions.size();
		}
		int count = 0;
		for (Region region : regions) {
			if (region.isMulti()) {
				count += ((MultiRegion) region).getRegionCount();
				continue;
			}
			count++;
		}
		return count;
	}
	
	/**
	 * Check if this Region overlaps with another.
	 *
	 * @param o The Region to check against
	 * @return Whether this Region overlaps with the given Region
	 */
	@Override
	public boolean overlaps(Region o) {
		if (!o.getWorld().equals(getWorld())) {
			return false;
		}
		if (o.isMulti()) {
			MultiRegion multi = (MultiRegion) o;
			return multi.getRegions().stream().anyMatch(r -> r.overlaps(this));
		}
		return regions.stream().anyMatch(r -> r.overlaps(o));
	}
	
	/**
	 * Gets a MultiRegion representing the overlap. This is somewhat expensive.
	 *
	 * @param other The Region to check for overlap with
	 * @return The overlapping portions of the Regions
	 */
	public MultiRegion getIntersection(Region other) {
		MultiRegion region = null;
		for (Region r : regions) {
			Region intersect = other.getIntersection(r);
			if (intersect == null) {
				continue;
			}
			if (region == null) {
				region = new MultiRegion(intersect);
				continue;
			}
			region.add(intersect);
		}
		return region;
	}
	
	/**
	 * Moves this MultiRegion using the given vector
	 * @param v The vector to be applied to both corners of the region
	 */
	@Override
	public void move(Vector v) {
		regions.forEach(r -> r.move(v));
		start = start.add(v);
		end = end.add(v);
	}
	
	/**
	 * Rotates this MultiRegion and all of its sub-regions around the given point
	 * @param center The point to rotate this Region around
	 * @param rotations The number of clockwise rotations to apply
	 */
	@Override
	public void rotate(Location center, int rotations) {
		for (Region region : regions) {
			region.rotate(center, rotations);
		}
		super.rotate(center, rotations);
	}
	
	/**
	 * Sets the world of this MultiRegion and all of its sub-regions, while keeping the coordinates
	 * the same
	 * @param world The world to set
	 */
	@Override
	public void setWorld(World world) {
		regions.forEach(r -> r.setWorld(world));
	}
	
	/**
	 * Recalculates this region to ensure it is using close to the least possible number of sub-regions with no overlaps.
	 * This will coalesce the MultiRegion into only added Regions, but subtracted Regions will not be included
	 * in any of the Regions. Calling this method is somewhat expensive, but will make all other operations
	 * on this MultiRegion faster. After recalculating the regions, automatically clusters them in clusters of 10
	 * until there are less than 25 top-level regions.
	 */
	public void recalculate() {
		recalculate(true);
	}
	
	/**
	 * Recalculates this region to ensure it is using close to the least possible number of sub-regions with no overlaps.
	 * This will coalesce the MultiRegion into only added Regions, but subtracted Regions will not be included
	 * in any of the Regions. Calling this method is somewhat expensive, but will make all other operations
	 * on this MultiRegion faster.
	 *
	 * @param autoCluster Whether to automatically cluster regions in clusters of 10 until there are less than 25 top-level regions
	 */
	public void recalculate(boolean autoCluster) {
		List<Region> regions = this.regions;
		List<Region> newRegions = new ArrayList<>();
		MultiRegion[] blocks = {null};
		subtract.forEach(r -> {
			if (blocks[0] == null) {
				blocks[0] = new MultiRegion(r);
				return;
			}
			blocks[0].add(r);
		});
		newRegions.addAll(subtract);
		Location center = start.clone().add(end).multiply(0.5).getBlock().getLocation();
		Region r = new Region(center, center.clone().add(1, 1, 1));
		if (contains(center) && expandToMax(r, null)) {
			newRegions.add(r);
			blocks[0] = new MultiRegion(r);
		}
		boolean[] added = {true};
		List<Region> toRemove = new ArrayList<>();
		while (added[0]) {
			added[0] = false;
			for (Region region : regions) {
				Location loc = findFreePoint(region, newRegions);
				if (loc != null) {
					Region reg = new Region(loc, loc.clone().add(1, 1, 1));
					if (expandToMax(reg, blocks[0])) {
						if (blocks[0] == null) {
							blocks[0] = new MultiRegion(reg);
						} else {
							blocks[0].add(reg);
						}
						newRegions.add(reg);
						added[0] = true;
					}
					continue;
				}
				toRemove.add(region);
			}
			regions.removeAll(toRemove);
			toRemove.clear();
		}
		newRegions.removeAll(subtract);
		this.regions = newRegions;
		subtract.clear();
		if (!autoCluster) {
			return;
		}
		autoCluster();
	}
	
	private Location findFreePoint(Region check, List<Region> exclude) {
		List<Region> intersects = exclude.stream().map(check::getIntersection).filter(Objects::nonNull).collect(Collectors.toList());
		if (intersects.size() == 0) {
			return check.getCenter().getBlock().getLocation();
		}
		for (Region region : intersects) {
			for (Location corner : region.getCorners()) {
				for (Vector vec : adjacent) {
					Location loc = corner.clone().add(vec).getBlock().getLocation();
					if (check.contains(loc) && !contains(intersects, loc)) {
						return loc;
					}
				}
			}
		}
		return null;
	}
	
	private boolean expandToMax(Region r, MultiRegion exclude) {
		List<BlockFace> faces = new ArrayList<>(6);
		List<BlockFace> toRemove = new ArrayList<>();
		Collections.addAll(faces, MultiRegion.faces);
		int step = 1;
		boolean expanded = false;
		while (faces.size() > 0) {
			for (int i = 0; i < faces.size(); i++) {
				BlockFace face = faces.get(i);
				r.expand(face, step);
				if (r.getBlockVolume() == getNonIntersectingVolume(r)
						&& (exclude == null || exclude.getNonIntersectingVolume(r) == 0)) {
					expanded = true;
					continue;
				}
				r.expand(face, -step);
				if (step > 1) {
					step = Math.max(1, step / 2);
					i--;
					continue;
				}
				toRemove.add(face);
			}
			if (toRemove.size() == 0) {
				step *= 2;
			}
			faces.removeAll(toRemove);
			toRemove.clear();
		}
		if (r.getBlockVolume() == getNonIntersectingVolume(r)
				&& (exclude == null || exclude.getNonIntersectingVolume(r) == 0)) {
			expanded = true;
		}
		return expanded;
	}
	
	private int getNonIntersectingVolume(Region r) {
		MultiRegion intersection = getIntersection(r);
		if (intersection == null) {
			return 0;
		}
		int volume = intersection.getBlockVolume();
		List<Region> intersections = intersection.getRegions();
		int overlap = 0;
		while (intersections.size() > 0) {
			Region region = intersections.get(intersections.size() - 1);
			for (int i = 0; i < intersections.size() - 1; i++) {
				Region other = intersections.get(i);
				Region tmp = region.getIntersection(other);
				if (tmp != null) {
					tmp = tmp.getIntersection(r);
					if (tmp != null) {
						overlap += tmp.getBlockVolume();
					}
				}
			}
			intersections.remove(intersections.size() - 1);
		}
		return volume - overlap;
	}
	
	/**
	 * A Stream of all the blocks in all of the Regions within this MultiRegion. May iterate the same block multiple
	 * times if any of the Regions overlap. Call {@link MultiRegion#recalculate()} first.
	 *
	 * @return A Stream of all the blocks in this MultiRegion
	 */
	public Stream<Block> stream() {
		Stream<Block> stream = Stream.empty();
		for (Region region : regions) {
			stream = Stream.concat(stream, region.stream());
		}
		return stream.filter(b -> subtract.stream().noneMatch(r -> r.contains(b.getLocation())));
	}
	
	/**
	 * Converts this MultiRegion to a String which can be converted back to a MultiRegion using {@link MultiRegion#fromString(String)}
	 * Please use this to persist MultiRegions, as most of the operations for manipulating a MultiRegion are far more
	 * expensive than the same operations would be for a Region. If its shape is static, and it needs to be reused, save it.
	 *
	 * @return The String representation of this MultiRegion
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder(getWorld().getName()).append(" ");
		for (Region region : regions) {
			String str = region.toString();
			str = str.substring(str.indexOf(' ') + 1);
			builder.append(str).append(",");
		}
		return builder.substring(0, builder.length() - 1);
	}
	
	/**
	 * Turns a String generated by {@link MultiRegion#toString()} back into a MultiRegion
	 *
	 * @param input The String representation of a MultiRegion
	 * @return The MultiRegion
	 */
	public static MultiRegion fromString(String input) {
		int pos = input.indexOf(' ');
		World world = Bukkit.getWorld(input.substring(0, pos));
		input = input.substring(pos + 1);
		String[] split = input.split(",");
		List<Region> regions = new ArrayList<>(split.length);
		for (String string : split) {
			regions.add(Region.fromString(new StringBuilder(world.getName()).append(" ").append(string).toString()));
		}
		return new MultiRegion(regions);
	}
	
}
