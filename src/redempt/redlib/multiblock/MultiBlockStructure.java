package redempt.redlib.multiblock;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

import redempt.redlib.RedLib;

/**
 * A utility class to create interactive multi-block structures
 * @author Redempt
 *
 */
@SuppressWarnings("deprecation")
public class MultiBlockStructure {
	
	/**
	 * Use this to get the info to build a multi-block structure.
	 * Should be hard-coded.
	 * @param start One bounding corner of the region
	 * @param end The other bounding corner of the region
	 * @return A string representing all of the block data for the region
	 */
	public static String stringify(Location start, Location end) {
		if (!start.getWorld().equals(end.getWorld())) {
			throw new IllegalArgumentException("Locations must be in the same  world");
		}
		int minX = Math.min(start.getBlockX(), end.getBlockX());
		int minY = Math.min(start.getBlockY(), end.getBlockY());
		int minZ = Math.min(start.getBlockZ(), end.getBlockZ());
		
		int maxX = Math.max(start.getBlockX(), end.getBlockX());
		int maxY = Math.max(start.getBlockY(), end.getBlockY());
		int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());
		
		int midVersion = Integer.parseInt(RedLib.getServerVersion().split("\\.")[1]);
		
		String output = (maxX - minX + 1) + "x" + (maxY - minY + 1) + "x" + (maxZ - minZ + 1) + ";";
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = new Location(start.getWorld(), x, y, z).getBlock();
					block.getType();
					if (midVersion >= 13) {
						output += block.getBlockData().getAsString() + ";";
					} else {
						output += block.getType() + ":" + block.getData();
					}
				}
			}
		}
		return output;
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an info string
	 * @param info The info string. Get this from {@link MultiBlockStructure#stringify(Location, Location)}
	 * @param name The name of this multi-block structure
	 * @param symmetry What kind of symmetry this multi-block structure has - 
	 * used to determine whether the structure must be rotated when testing if it exists at a given location
	 * @return
	 */
	public static MultiBlockStructure create(String info, String name, Symmetry symmetry) {
		return new MultiBlockStructure(info, name, symmetry);
	}
	
	private String[][][] data;
	private String dataString;
	private String name;
	private int dimX;
	private int dimY;
	private int dimZ;
	private Symmetry symmetry;
	
	private MultiBlockStructure(String info, String name, Symmetry symmetry) {
		this.dataString = info;
		this.name = name;
		String[] split = info.split(";");
		String[] dimSplit = split[0].split("x");
		dimX = Integer.parseInt(dimSplit[0]);
		dimY = Integer.parseInt(dimSplit[1]);
		dimZ = Integer.parseInt(dimSplit[2]);
		
		data = new String[dimX][dimY][dimZ];
		
		int pos = 1;
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					data[x][y][z] = split[pos];
					pos++;
				}
			}
		}
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 */
	public void build(Location loc, int rotation) {
		Rotator rotator = new Rotator(rotation);
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					rotator.setLocation(x, z);
					Location l = loc.clone().add(rotator.getRotatedX(), y, rotator.getRotatedZ());
					setBlock(l, data[x][y][z]);
				}
			}
		}
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 */
	public void build(Location loc) {
		build(loc, 0);
	}
	
	/**
	 * Gets this multi-block structure's name. May be faster to compare this than to use .equals().
	 * @return The name of this multi-block structure
	 */
	public String getName() {
		return name;
	}
	
	public boolean existsAt(Location loc) {
		Block block = loc.getBlock();
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					if (compare(data[x][y][z], block) && test(loc, x, y, z, symmetry.getRotationsNeeded())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private boolean test(Location loc, int x, int y, int z, int[] rotations) {
		for (int rot : rotations) {
			if (test(loc, x, y, z, rot)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean test(Location loc, int xPos, int yPos, int zPos, int rotation) {
//		Rotator rotator = new Rotator(0);
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					int xp = x - xPos;
					int yp = y - yPos;
					int zp = z - zPos;
					Block block = loc.clone().add(xp, yp, zp).getBlock();
					if (!compare(data[x][y][z], block)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private boolean compare(String data, Block block) {
		int midVersion = Integer.parseInt(RedLib.getServerVersion().split("\\.")[1]);
		if (midVersion >= 13) {
			return block.getBlockData().getAsString().equals(data);
		} else {
			String[] split = data.split(":");
			return block.getType() == Material.valueOf(split[0]) && block.getData() == Byte.parseByte(split[1]);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MultiBlockStructure) {
			MultiBlockStructure structure = (MultiBlockStructure) o;
			return structure.dataString.equals(dataString);
		}
		return super.equals(o);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(dataString, name);
	}
	
	@Override
	public String toString() {
		return dataString;
	}
	
	private void setBlock(Location loc, String data) {
		int midVersion = Integer.parseInt(RedLib.getServerVersion().split("\\.")[1]);
		if (midVersion >= 13) {
			loc.getBlock().setBlockData(Bukkit.createBlockData(data));
		} else {
			String[] split = data.split(":");
			Material type = Material.valueOf(split[0]);
			byte dataValue = Byte.parseByte(split[1]);
			BlockState state = loc.getBlock().getState();
			state.setData(new MaterialData(type, dataValue));
			state.update();
		}
	}
	
	public static enum Symmetry {
		
		SINGLE_AXIS_SYMMETRY(new int[] {0, 2}),
		DOUBLE_AXIS_SYMMETRY(new int[] {0}),
		NO_SYMMETRY(new int[] {0, 1, 2, 3});
		
		private int[] rotationsNeeded;
		
		private Symmetry(int[] rotationsNeeded) {
			this.rotationsNeeded = rotationsNeeded;
		}
		
		public int[] getRotationsNeeded() {
			return rotationsNeeded.clone();
		}
		
	}
	
	private static class Rotator {
		
		private static String[] rotations = {"x,z", "-z,x", "-x,-z", "z,-x"};
		
		private int rotation;
		private int x;
		private int z;
		
		public Rotator(int rotation) {
			this.rotation = rotation % 4;
		}
		
		public void setLocation(int x, int z) {
			this.x = x;
			this.z = z;
		}
		
		public int getRotatedX() {
			String rotationString = rotations[rotation];
			String xString = rotationString.split(",")[0];
			int val = 0;
			switch (xString.charAt(xString.length() - 1)) {
				case 'x':
					val = x;
					break;
				case 'z':
					val = z;
					break;
			}
			return xString.startsWith("-") ? -val : val;
		}
		
		public int getRotatedZ() {
			String rotationString = rotations[rotation];
			String xString = rotationString.split(",")[1];
			int val = 0;
			switch (xString.charAt(xString.length() - 1)) {
				case 'x':
					val = x;
					break;
				case 'z':
					val = z;
					break;
			}
			return xString.startsWith("-") ? -val : val;
		}
		
	}
	
}
