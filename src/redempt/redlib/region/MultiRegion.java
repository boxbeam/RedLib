package redempt.redlib.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a collection of Regions forming any shape
 * @author Redempt
 */
public class MultiRegion extends Region {
	
	private static BlockFace[] faces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
	
	private List<Region> regions = new ArrayList<>();
	private List<Region> subtract = new ArrayList<>();
	private Location start;
	private Location end;
	
	/**
	 * Construct a MultiRegion using a list of Regions
	 * @param regions The list of Regions
	 */
	public MultiRegion(List<Region> regions) {
		if (regions.size() == 0) {
			throw new IllegalArgumentException("Cannot create MultiRegion from 0 regions");
		}
		World world = regions.get(0).getWorld();
		for (Region region : regions) {
			if (!region.getWorld().equals(world)) {
				throw new IllegalArgumentException("All regions must be in the same world");
			}
			this.regions.add(region);
		}
		fixCorners();
	}
	
	private void fixCorners() {
		World world = regions.get(0).getWorld();
		double minX = this.regions.stream().min((a, b) -> (int) Math.signum(a.getStart().getX() - b.getStart().getX())).get().getStart().getX();
		double minY = this.regions.stream().min((a, b) -> (int) Math.signum(a.getStart().getY() - b.getStart().getY())).get().getStart().getY();
		double minZ = this.regions.stream().min((a, b) -> (int) Math.signum(a.getStart().getZ() - b.getStart().getZ())).get().getStart().getZ();
		
		double maxX = this.regions.stream().max((a, b) -> (int) Math.signum(a.getEnd().getX() - b.getEnd().getX())).get().getEnd().getX();
		double maxY = this.regions.stream().max((a, b) -> (int) Math.signum(a.getEnd().getY() - b.getEnd().getY())).get().getEnd().getY();
		double maxZ = this.regions.stream().max((a, b) -> (int) Math.signum(a.getEnd().getZ() - b.getEnd().getZ())).get().getEnd().getZ();
		
		start = new Location(world, minX, minY, minZ);
		end = new Location(world, maxX, maxY, maxZ);
		setLocations(start, end);
	}
	
	/**
	 * Construct a MultiRegion using a vararg of Regions
	 * @param regions The vararg of Regions
	 */
	public MultiRegion(Region... regions) {
		this(Arrays.stream(regions).collect(Collectors.toList()));
	}
	
	/**
	 * Adds a Region to this MultiRegion
	 * @param region The Region to add
	 */
	public void add(Region region) {
		if (region.isMulti()) {
			MultiRegion multi = (MultiRegion) region;
			for (Region r : multi.getRegions()) {
				regions.add(r.clone());
			}
			fixCorners();
			return;
		}
		regions.add(region.clone());
		fixCorners();
	}
	
	/**
	 * Subtracts a Region from this MultiRegion. A subtracted Region overrides all positive Regions,
	 * meaning adding a Region that overlaps a previously subtracted Region will not add the overlapping blocks.
	 * Calling {@link MultiRegion#recalculate()} will coalesce into only added Regions,
	 * @param region The Region to subtract
	 */
	public void subtract(Region region) {
		if (region.isMulti()) {
			MultiRegion multi = (MultiRegion) region;
			for (Region r : multi.getRegions()) {
				subtract.add(r.clone());
			}
			return;
		}
		subtract.add(region.clone());
	}
	
	@Override
	public List<Entity> getEntities() {
		List<Entity> entities = new ArrayList<>();
		regions.stream().map(Region::getEntities).forEach(entities::addAll);
		return entities;
	}
	
	/**
	 * Checks whether this MultiRegion contains the given Location
	 * @param location The Location to check
	 * @return Whether the Location is contained within this MultiRegion
	 */
	public boolean contains(Location location) {
		if (!getWorld().equals(location.getWorld())) {
			return false;
		}
		return contains(regions, location) && !contains(subtract, location);
	}
	
	private static boolean contains(List<Region> regions, Location loc) {
		return regions.stream().anyMatch(r -> r.contains(loc));
	}
	
	/**
	 * @return The World this MultiRegion is in
	 */
	public World getWorld() {
		return start.getWorld();
	}
	
	/**
	 * Gets all the cuboid Regions that form this MultiRegion
	 * @return The list of Regions that form this MultiRegion
	 */
	public List<Region> getRegions() {
		return regions;
	}
	
	/**
	 * Sums the block volume of all the Regions that make up this MultiRegion.
	 * Will be inaccurate if any of the Regions overlap. Call {@link MultiRegion#recalculate()} first.
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
	 * @return The center of this MultiRegion as defined by the point in the center of the two extreme corners
	 */
	@Override
	public Location getCenter() {
		return start.clone().add(end).multiply(0.5);
	}
	
	/**
	 * @return The lower extreme corner - the Location representing the minimum of all coordinates covered by this MultiRegion
	 */
	@Override
	public Location getStart() {
		return start.clone();
	}
	
	/**
	 * @return The upper extreme corner - the Location representing the maximum of all coordinates covered by this MultiRegion
	 */
	@Override
	public Location getEnd() {
		return end.clone();
	}
	
	/**
	 * @return Whether this is a MultiRegion
	 */
	@Override
	public boolean isMulti() {
		return true;
	}
	
	/**
	 * Clones this MultiRegion
	 * @return A clone of this MultiRegion
	 */
	@Override
	public MultiRegion clone() {
		List<Region> clone = new ArrayList<>();
		regions.stream().map(Region::clone).forEach(clone::add);
		return new MultiRegion(clone);
	}
	
	/**
	 * Expands the MultiRegion in a given direction, or retracts if negative.
	 * Expanding takes a one-block wide slice on the face of the direction given, and duplicates it forward
	 * in that direction the given number of times. Retracting subtracts a region of n width in the direction
	 * given on the face of the direction. It is highly recommended to call {@link MultiRegion#recalculate()}
	 * after calling this, especially if it is a retraction. This is a fairly expensive operation,
	 * so use it sparingly.
	 * @param direction The direction to expand the region in
	 * @param amount The amount to expand the region in the given direction
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
		fixCorners();
	}
	
	/**
	 * Expands the region, or retracts if negative. This makes 6 calls to {@link MultiRegion#expand(BlockFace, int)},
	 * meaning it is very expensive. Avoid calling this method if possible.
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
	 * @return A cuboid region guaranteed to have equal or greater coverage compared to this MultiRegion
	 */
	public Region toCuboid() {
		return new Region(start, end);
	}
	
	/**
	 * Check if this Region overlaps with another.
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
	 * @param other The Region to check for overlap with
	 * @return The overlapping portions of the Regions
	 */
	public MultiRegion getIntersection(Region other) {
		MultiRegion[] start = {null};
		other.stream().map(Block::getLocation).forEach(l -> {
			if (contains(l)) {
				Region r = new Region(l, l.clone().add(1, 1, 1));
				if (start[0] == null) {
					start[0] = new MultiRegion(r);
				} else {
					start[0].add(r);
				}
			}
		});
		if (start[0] == null) {
			return null;
		}
		start[0].recalculate();
		return start[0];
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
	 * Recalculates this region to ensure it is using close to the least possible number of sub-regions with no overlaps.
	 * This will coalesce the MultiRegion into only added Regions, but subtracted Regions will not be included
	 * in any of the Regions. Calling this method is somewhat expensive, but will make all other operations
	 * on this MultiRegion faster.
	 */
	public void recalculate() {
		List<Region> newRegions = new ArrayList<>();
		newRegions.addAll(subtract);
		Location center = start.clone().add(end).multiply(0.5).getBlock().getLocation();
		Region r = new Region(center, center.clone().add(1, 1, 1));
		expandToMax(r, newRegions);
		if (contains(center)) {
			newRegions.add(r);
		}
		boolean[] added = {true};
		while (added[0]) {
			added[0] = false;
			for (Region region : regions) {
				region.stream().map(Block::getLocation)
						.filter(l -> this.contains(l) && !contains(newRegions, l))
						.sorted((a, b) -> (int) Math.signum(b.distanceSquared(center) - a.distanceSquared(center)))
						.findFirst().ifPresent(l -> {
							added[0] = true;
							Region reg = new Region(l, l.clone().add(1, 1, 1));
							expandToMax(reg, newRegions);
							newRegions.add(reg);
				});
			}
		}
		newRegions.removeAll(subtract);
		regions = newRegions;
		subtract.clear();
	}
	
	private void expandToMax(Region r, List<Region> exclude) {
		Set<BlockFace> faces = new HashSet<>(6);
		List<BlockFace> toRemove = new ArrayList<>();
		Arrays.stream(MultiRegion.faces).forEach(faces::add);
		while (faces.size() > 0) {
			for (BlockFace face : faces) {
				Region clone = r.clone();
				clone.expand(face.getOppositeFace(), -(clone.measureBlocks(face) - 1));
				clone.move(face.getDirection());
				if (clone.stream().map(Block::getLocation).allMatch(l -> this.contains(l) && !contains(exclude, l))) {
					r.expand(face, 1);
					continue;
				}
				toRemove.add(face);
			}
			faces.removeAll(toRemove);
			toRemove.clear();
		}
	}
	
	/**
	 * A Stream of all the blocks in all of the Regions within this MultiRegion. May iterate the same block multiple
	 * times if any of the Regions overlap. Call {@link MultiRegion#recalculate()} first.
	 * @return A Stream of all the blocks in this MultiRegion
	 */
	public Stream<Block> stream() {
		Stream<Block> stream = Stream.empty();
		for (Region region : regions) {
			stream = Stream.concat(stream, region.stream());
		}
		return stream;
	}
	
	/**
	 * Converts this MultiRegion to a String which can be converted back to a MultiRegion using {@link MultiRegion#fromString(String)}
	 * Please use this to persist MultiRegions, as most of the operations for manipulating a MultiRegion are far more
	 * expensive than the same operations would be for a Region. If its shape is static, and it needs to be reused, save it.
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
