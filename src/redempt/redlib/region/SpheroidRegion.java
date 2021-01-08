package redempt.redlib.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import redempt.redlib.misc.LocationUtils;
import redempt.redlib.multiblock.Rotator;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents a spheroid region in a world
 * @author Redempt
 */
public class SpheroidRegion extends Region {

	private double xRad;
	private double yRad;
	private double zRad;
	private Location center;
	
	/**
	 * Creates a SpheroidRegion from two corners, using their midpoint as the center and their distance in each direction
	 * as the radius on each axis
	 * @param start The first corner
	 * @param end The second corner
	 * @throws IllegalArgumentException if the Locations are not in the same world
	 */
	public SpheroidRegion(Location start, Location end) {
		if (!start.getWorld().equals(end.getWorld())) {
			throw new IllegalArgumentException("Corners must be in the same world");
		}
		center = start.clone().add(end).multiply(0.5);
		xRad = Math.abs(start.getX() - end.getX()) / 2;
		yRad = Math.abs(start.getY() - end.getY()) / 2;
		zRad = Math.abs(start.getZ() - end.getZ()) / 2;
	}
	
	/**
	 * Creates a SpheroidRegion from a center and a radius in each direction
	 * @param center The center
	 * @param xRad The radius on the X axis
	 * @param yRad The radius on the Y axis
	 * @param zRad The radius on the Z axis
	 */
	public SpheroidRegion(Location center, double xRad, double yRad, double zRad) {
		this.center = center;
		this.xRad = xRad;
		this.yRad = yRad;
		this.zRad = zRad;
	}
	
	/**
	 * Creates a SpheroidRegion from a center and a radius
	 * @param center The center
	 * @param radius The radius
	 */
	public SpheroidRegion(Location center, double radius) {
		this(center, radius, radius, radius);
	}
	
	/**
	 * @return The volume of this SpheroidRegion
	 */
	@Override
	public double getVolume() {
		return Math.PI * xRad * yRad * zRad;
	}
	
	/**
	 * @return The *approximate* block volume of this SpheroidRegion
	 */
	@Override
	public int getBlockVolume() {
		return (int) getVolume();
	}
	
	/**
	 * Expands this SpheroidRegion in all directions
	 * @param posX The amount to increase in the positive X direction
	 * @param negX The amount to increase in the negative X direction
	 * @param posY The amount to increase in the positive Y direction
	 * @param negY The amount to increase in the negative Y direction
	 * @param posZ The amount to increase in the positive Z direction
	 * @param negZ The amount to increase in the negative Z direction
	 * @return Itself
	 */
	@Override
	public SpheroidRegion expand(double posX, double negX, double posY, double negY, double posZ, double negZ) {
		xRad += posX + negX;
		yRad += posY + negY;
		zRad += posZ + negZ;
		move(posX - negX, posY - negY, posZ - negZ);
		return this;
	}
	
	/**
	 * Expands this SpheroidRegion in a specific direction
	 * @param face The BlockFace representing the direction to expand in
	 * @param amount The amount to expand
	 * @return Itself
	 */
	@Override
	public SpheroidRegion expand(BlockFace face, double amount) {
		return this;
	}
	
	/**
	 * Expands this SpheroidRegion in all directions
	 * @param amount The amount to expand by in all directions
	 * @return Itself
	 */
	public SpheroidRegion expand(double amount) {
		xRad += amount;
		yRad += amount;
		zRad += amount;
		return this;
	}
	
	/**
	 * Moves this SpheroidRegion according to a Vector
	 * @param vec The vector representing the direction and amount to move
	 * @return Itself
	 */
	@Override
	public SpheroidRegion move(Vector vec) {
		center.add(vec);
		return this;
	}
	
	/**
	 * Moves this SpheroidRegion
	 * @param x The amount to move this SpheroidRegion on the X axis
	 * @param y The amount to move this SpheroidRegion on the Y axis
	 * @param z The amount to move this SpheroidRegion on the Z axis
	 * @return Itself
	 */
	@Override
	public SpheroidRegion move(double x, double y, double z) {
		return move(new Vector(x, y, z));
	}
	
	private double distanceSquared(Location loc) {
		return Math.pow((loc.getX() - center.getX()) / xRad, 2) +
				Math.pow((loc.getY() - center.getY()) / yRad, 2) +
				Math.pow((loc.getZ() - center.getZ()) / zRad, 2);
	}
	
	public boolean contains(Location loc) {
		return center.getWorld().equals(loc.getWorld()) &&
				distanceSquared(loc) <= 1;
	}
	
	/**
	 * @return True if the radius in all directions of this SpheroidRegion are the same
	 */
	public boolean isSphere() {
		return xRad == yRad && yRad == zRad;
	}
	
	/**
	 * @return A clone of this SpheroidRegion
	 */
	@Override
	public SpheroidRegion clone() {
		return new SpheroidRegion(center.clone(), xRad, yRad, zRad);
	}
	
	/**
	 * Rotates this SpheroidRegion
	 * @param center The center of rotation
	 * @param rotations The number of clockwise rotations
	 * @return Itself
	 */
	@Override
	public SpheroidRegion rotate(Location center, int rotations) {
		Rotator rotator = new Rotator(rotations, false);
		rotator.setLocation(center.getX(), center.getZ());
		center.setX(rotator.getRotatedX());
		center.setZ(rotator.getRotatedZ());
		if (rotations % 2 == 1) {
			double tmp = xRad;
			xRad = zRad;
			zRad = tmp;
		}
		return this;
	}
	
	/**
	 * Sets the world of this SpheroidRegion
	 * @param world The World
	 * @return Itself
	 */
	@Override
	public SpheroidRegion setWorld(World world) {
		center.setWorld(world);
		return this;
	}
	
	/**
	 * @return A stream of all Blocks contained by this SpheroidRegion
	 */
	@Override
	public Stream<Block> stream() {
		return toCuboid().stream().filter(b -> contains(b.getLocation().add(.5, .5, .5)));
	}
	
	/**
	 * @return The least extreme corner of this SpheroidRegion representing the minimum X, Y, and Z coordinates
	 */
	@Override
	public Location getStart() {
		return center.clone().subtract(xRad, yRad, zRad);
	}
	
	/**
	 * @return The move extreme corner of this SpheroidRegion representing the maximum X, Y, and Z coordinates
	 */
	@Override
	public Location getEnd() {
		return center.clone().add(xRad, yRad, zRad);
	}
	
	/**
	 * @return The World this SpheroidRegion is in
	 */
	@Override
	public World getWorld() {
		return center.getWorld();
	}
	
	/**
	 * @return The center of this SpheroidRegion
	 */
	@Override
	public Location getCenter() {
		return center.clone();
	}
	
	/**
	 * Gets a point on the surface of this SpheroidRegion in the given direction
	 * @param v The direction to get the point in
	 * @return The point on the surface of this SpheroidRegion
	 */
	public Location getSurfacePoint(Vector v) {
		v = v.clone().normalize();
		v.setX(v.getX() * xRad);
		v.setY(v.getY() * yRad);
		v.setZ(v.getZ() * zRad);
		return center.clone().add(v);
	}
	
	/**
	 * @return A Set containing all of the blocks on the surface of this SpheroidRegion
	 */
	public Set<Block> getSurface() {
		Set<Block> surface = new HashSet<>();
		double incPitch = 45 / yRad;
		double incYaw = 45 / Math.max(zRad, xRad);
		Location loc = getCenter();
		for (double pitch = 0; pitch < 360; pitch += incPitch) {
			for (double yaw = 0; yaw < 360; yaw += incYaw) {
				loc.setPitch((float) pitch);
				loc.setYaw((float) yaw);
				Block block = getSurfacePoint(loc.getDirection()).getBlock();
				if (surfaceContains(block)) {
					surface.add(block);
				}
			}
		}
		return surface;
	}
	
	/**
	 * Determines whether a block is on the surface of this SpheroidRegion
	 * @param block The block to check
	 * @return Whether the block is on the surface of this SpheroidRegion
	 */
	public boolean surfaceContains(Block block) {
		Vector v = LocationUtils.center(block).subtract(center).toVector();
		v.setX(v.getX() / xRad);
		v.setY(v.getY() / yRad);
		v.setZ(v.getZ() / zRad);
		Location l = getSurfacePoint(v);
		CuboidRegion region = new CuboidRegion(block.getLocation(), block.getLocation().add(1, 1, 1)).expand(0.06);
		return region.contains(l);
	}
	
	/**
	 * @return A String representation of this SpheroidRegion which can later be deserialized with {@link SpheroidRegion#fromString(String)}
	 */
	@Override
	public String toString() {
		return LocationUtils.toString(center) + "-" + xRad + " " + yRad + " " + zRad;
	}
	
	/**
	 * Deserializes a SpheroidRegion serialized with {@link SpheroidRegion#toString()}
	 * @param string The serialized SpheroidRegion String
	 * @return The deserialized SpheroidRegion
	 */
	public static SpheroidRegion fromString(String string) {
		String[] split = string.split("-");
		Location center = LocationUtils.fromString(split[0]);
		String[] radSplit = split[1].split(" ");
		double xRad = Double.parseDouble(radSplit[0]);
		double yRad = Double.parseDouble(radSplit[1]);
		double zRad = Double.parseDouble(radSplit[2]);
		return new SpheroidRegion(center, xRad, yRad, zRad);
	}
	
}
