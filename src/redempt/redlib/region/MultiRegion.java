package redempt.redlib.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a collection of Regions forming any shape
 * @author Redempt
 */
public class MultiRegion {
	
	private List<Region> regions = new ArrayList<>();
	private BlockFace[] faces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
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
		double minX = this.regions.stream().min((a, b) -> (int) Math.signum(b.getStart().getX() - a.getStart().getX())).get().getStart().getX();
		double minY = this.regions.stream().min((a, b) -> (int) Math.signum(b.getStart().getY() - a.getStart().getY())).get().getStart().getY();
		double minZ = this.regions.stream().min((a, b) -> (int) Math.signum(b.getStart().getZ() - a.getStart().getZ())).get().getStart().getZ();
		
		double maxX = this.regions.stream().max((a, b) -> (int) Math.signum(b.getStart().getX() - a.getStart().getX())).get().getStart().getX();
		double maxY = this.regions.stream().max((a, b) -> (int) Math.signum(b.getStart().getY() - a.getStart().getY())).get().getStart().getY();
		double maxZ = this.regions.stream().max((a, b) -> (int) Math.signum(b.getStart().getZ() - a.getStart().getZ())).get().getStart().getZ();
		
		start = new Location(world, minX, minY, minZ);
		end = new Location(world, maxX, maxY, maxZ);
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
		regions.add(region);
	}
	
	/**
	 * Checks whether this MultiRegion contains the given Location
	 * @param location The Location to check
	 * @return Whether the Location is contained within this MultiRegion
	 */
	public boolean contains(Location location) {
		return contains(regions, location);
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
	 * Gets all the rectangular prism Regions that form this MultiRegion
	 * @return The list of Regions that form this MultiRegion
	 */
	public List<Region> getRegions() {
		return regions;
	}
	
	/**
	 * Sums the volume of all the Regions that make up this MultiRegion.
	 * Will be inaccurate if any of the Regions overlap. Call {@link MultiRegion#recalculate()} first.
 	 * @return The volume of this MultiRegion
	 */
	public int getVolume() {
		int total = 0;
		for (Region region : regions) {
			total += region.getBlockVolume();
		}
		return total;
	}
	
	/**
	 * Recalculates this region to ensure it is using the least possible number of sub-regions with no overlaps.
	 */
	public void recalculate() {
		List<Region> newRegions = new ArrayList<>();
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
		regions = newRegions;
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
	
	private void expandToMax(Region r, List<Region> exclude) {
		boolean expanded = true;
		while (expanded) {
			expanded = false;
			for (BlockFace face : faces) {
				Region clone = r.clone();
				clone.expand(face.getOppositeFace(), -(clone.measureBlocks(face) - 1));
				clone.move(face.getDirection());
				if (clone.stream().map(Block::getLocation).allMatch(l -> this.contains(l) && !contains(exclude, l))) {
					expanded = true;
					r.expand(face, 1);
				}
			}
		}
	}
	
}
