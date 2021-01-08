package redempt.redlib.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import redempt.redlib.misc.LocationUtils;
import redempt.redlib.multiblock.Rotator;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a collection of Regions forming any shape
 *
 * @author Redempt
 */
public class MultiRegion extends Region implements Overlappable {
	
	private static Vector[] adjacent = {new Vector(.1, .1, .1), new Vector(.1, .1, -.1), new Vector(.1, -.1, .1), new Vector(-.1, .1, .1),
										new Vector(-.1, -.1, .1), new Vector(-.1, .1, -.1), new Vector(.1, -.1, -.1), new Vector(-.1, -.1, -.1)};
	
	private List<Region> regions = new ArrayList<>();
	private List<Region> subtract = new ArrayList<>();
	private boolean clustered = false;
	private Location start;
	private Location end;
	
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
			if (region instanceof MultiRegion) {
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
	 * Construct a MultiRegion using a vararg of Regions
	 *
	 * @param regions The vararg of Regions
	 */
	public MultiRegion(Region... regions) {
		this(Arrays.stream(regions).collect(Collectors.toList()));
	}
	
	/**
	 * @return The least extreme corner of the region, representing minimum X, Y, and Z coordinates
	 */
	public Location getStart() {
		return start;
	}
	
	/**
	 * @return The most extreme corner of the region, representing maximum X, Y, and Z coordinates
	 */
	public Location getEnd() {
		return end;
	}
	
	/**
	 * Adds a Region to this MultiRegion
	 *
	 * @param region The Overlappable Region to add
	 * @throws IllegalArgumentException if the Region is not Overlappable or is in another world
	 */
	public void add(Region region) {
		if (!(region instanceof Overlappable)) {
			throw new IllegalArgumentException("Cannot add non-Overlappable Region to MultiRegion");
		}
		if (!region.getWorld().equals(getWorld())) {
			throw new IllegalArgumentException("Region is not in the same world as this MultiRegion");
		}
		if (region instanceof MultiRegion && !clustered) {
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
	public MultiRegion expand(BlockFace direction, double amount) {
		if (amount == 0) {
			return this;
		}
		if (amount < 0) {
			Region r = new CuboidRegion(start, end);
			r.expand(direction.getOppositeFace(), -r.measureBlocks(direction));
			r.expand(direction.getOppositeFace(), Math.abs(amount));
			subtract.add(r);
			return this;
		}
		Region r = new CuboidRegion(start, end);
		r.expand(direction.getOppositeFace(), -(r.measureBlocks(direction) - 1));
		MultiRegion slice = getIntersection((Overlappable) r);
		slice.move(direction.getDirection());
		for (int i = 0; i < amount; i++) {
			MultiRegion clone = slice.clone();
			clone.move(direction.getDirection().multiply(i));
			add(clone);
		}
		fixCorners(null);
		return this;
	}
	
	/**
	 * Expands the region, or retracts if negative. This makes 6 calls to {@link MultiRegion#expand(BlockFace, double)},
	 * meaning it is expensive. Avoid calling this method if possible.
	 *
	 * @param posX The amount to expand the region in the positive X direction
	 * @param negX The amount to expand the region in the negative X direction
	 * @param posY The amount to expand the region in the positive Y direction
	 * @param negY The amount to expand the region in the negative Y direction
	 * @param posZ The amount to expand the region in the positive Z direction
	 * @param negZ The amount to expand the region in the negative Z direction
	 */
	public MultiRegion expand(double posX, double negX, double posY, double negY, double posZ, double negZ) {
		expand(BlockFace.EAST, posX);
		expand(BlockFace.WEST, negX);
		expand(BlockFace.SOUTH, posZ);
		expand(BlockFace.NORTH, negZ);
		expand(BlockFace.UP, posY);
		expand(BlockFace.DOWN, negY);
		return this;
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
			if (region instanceof MultiRegion) {
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
			if (region instanceof MultiRegion) {
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
	 * @param overlap The Region to check against
	 * @return Whether this Region overlaps with the given Region
	 */
	@Override
	public boolean overlaps(Overlappable overlap) {
		Region o = (Region) overlap;
		if (!o.getWorld().equals(getWorld())) {
			return false;
		}
		if (o instanceof MultiRegion) {
			MultiRegion multi = (MultiRegion) o;
			return multi.getRegions().stream().anyMatch(r -> ((Overlappable) r).overlaps(this));
		}
		return regions.stream().anyMatch(r -> overlap.overlaps((Overlappable) r));
	}
	
	/**
	 * Gets a MultiRegion representing the overlap. This is somewhat expensive.
	 *
	 * @param other The Region to check for overlap with
	 * @return The overlapping portions of the Regions
	 */
	public MultiRegion getIntersection(Overlappable other) {
		MultiRegion region = null;
		for (Region r : regions) {
			Region intersect = other.getIntersection((Overlappable) r);
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
	 * @return Itself
	 */
	@Override
	public MultiRegion move(Vector v) {
		regions.forEach(r -> r.move(v));
		start = start.add(v);
		end = end.add(v);
		return this;
	}
	
	@Override
	public Region move(double x, double y, double z) {
		return move(new Vector(x, y, z));
	}
	
	/**
	 * Rotates this MultiRegion and all of its sub-regions around the given point
	 * @param center The point to rotate this Region around
	 * @param rotations The number of clockwise rotations to apply
	 * @return Itself
	 */
	@Override
	public MultiRegion rotate(Location center, int rotations) {
		for (Region region : regions) {
			region.rotate(center, rotations);
		}
		Location start = getStart();
		Location end = getEnd();
		start.subtract(center);
		end.subtract(center);
		Rotator rotator = new Rotator(rotations, false);
		rotator.setLocation(start.getX(), start.getZ());
		start.setX(rotator.getRotatedX());
		start.setZ(rotator.getRotatedZ());
		rotator.setLocation(end.getX(), end.getZ());
		end.setX(rotator.getRotatedX());
		end.setZ(rotator.getRotatedZ());
		start.add(center);
		end.add(center);
		setLocations(start, end);
		return this;
	}
	
	/**
	 * Sets the world of this MultiRegion and all of its sub-regions, while keeping the coordinates
	 * the same
	 * @param world The world to set
	 * @return Itself
	 */
	@Override
	public MultiRegion setWorld(World world) {
		regions.forEach(r -> r.setWorld(world));
		start.setWorld(world);
		end.setWorld(world);
		return this;
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
		MultiRegionMeta summary = new MultiRegionMeta(regions);
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
		Region r = new CuboidRegion(center, center);
		if (contains(center) && expandToMax(r, null, summary)) {
			newRegions.add(r);
			blocks[0] = new MultiRegion(r);
		}
		boolean[] added = {true};
		List<Region> toRemove = new ArrayList<>();
		while (added[0]) {
			added[0] = false;
			for (Region region : regions) {
				Location loc = findFreePoint((CuboidRegion) region, newRegions);
				if (loc != null) {
					Region reg = new CuboidRegion(loc, loc);
					if (expandToMax(reg, blocks[0], summary)) {
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
	
	private Location findFreePoint(CuboidRegion check, List<Region> exclude) {
		List<Region> intersects = exclude.stream().map(r -> check.getIntersection((Overlappable) r)).filter(Objects::nonNull).collect(Collectors.toList());
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
	
	private boolean expandToMax(Region r, MultiRegion exclude, MultiRegionMeta summary) {
		List<BlockFace> faces = new ArrayList<>(6);
		Collections.addAll(faces, LocationUtils.PRIMARY_BLOCK_FACES);
		boolean expanded = false;
		while (faces.size() > 0) {
			for (int i = 0; i < faces.size(); i++) {
				BlockFace face = faces.get(i);
				double step = summary.getCurrentStep(r, face);
				double next = summary.getNextStep(face, step);
				if (step == next) {
					faces.remove(i);
					continue;
				}
				r.expand(face, Math.abs(next - step));
				if (compare(r.getVolume(), getNonIntersectingVolume(r))
						&& (exclude == null || exclude.getNonIntersectingVolume(r) == 0)) {
					expanded = true;
					continue;
				}
				r.expand(face, -Math.abs(next - step));
				faces.remove(i);
			}
		}
		return r.getVolume() > 0 && expanded;
	}
	
	private boolean compare(double first, double second) {
		return Math.abs(first - second) < 0.00001;
	}
	
	private double getNonIntersectingVolume(Region r) {
		MultiRegion intersection = getIntersection((Overlappable) r);
		if (intersection == null) {
			return 0;
		}
		double volume = intersection.getVolume();
		List<Region> intersections = intersection.getRegions();
		int overlap = 0;
		while (intersections.size() > 0) {
			Region region = intersections.get(intersections.size() - 1);
			for (int i = 0; i < intersections.size() - 1; i++) {
				Region other = intersections.get(i);
				Region tmp = ((Overlappable) region).getIntersection((Overlappable) other);
				if (tmp != null) {
					tmp = ((Overlappable) tmp).getIntersection((Overlappable) r);
					if (tmp != null) {
						overlap += tmp.getVolume();
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
			regions.add(CuboidRegion.fromString(new StringBuilder(world.getName()).append(" ").append(string).toString()));
		}
		return new MultiRegion(regions);
	}
	
}
