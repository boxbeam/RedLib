package redempt.redlib.multiblock;

import static redempt.redlib.RedLib.midVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

/**
 * A utility class to create interactive multi-block structures
 * @author Redempt
 *
 */
@SuppressWarnings("deprecation")
public class MultiBlockStructure {
	
	/**
	 * Use this to get the info to construct a multi-block structure.
	 * Should be hard-coded.
	 * You can use the multi-block structure tool (/struct wand) as long as devMode is true
	 * @param start One bounding corner of the region
	 * @param end The other bounding corner of the region
	 * @return A string representing all of the block data for the region
	 * @throws IllegalArgumentException if the specified locations are not in the same world
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
		
		String output = (maxX - minX + 1) + "x" + (maxY - minY + 1) + "x" + (maxZ - minZ + 1) + ";";
		StringBuilder builder = new StringBuilder();
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = start.getWorld().getBlockAt(x, y, z);
					if (midVersion >= 13) {
						builder.append(block.getBlockData().getAsString()).append(';');
					} else {
						output += block.getType() + ":" + block.getData() + ";";
					}
				}
			}
		}
		output += builder.toString();
		output = output.substring(0, output.length() - 1);
		output = minify(output);
		return output;
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an info string
	 * @param info The info string. Get this from {@link MultiBlockStructure#stringify(Location, Location)}
	 * @param name The name of the multi-block structure
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(String info, String name) {
		return new MultiBlockStructure(info, name, true, false);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an info string
	 * @param info The info string. Get this from {@link MultiBlockStructure#stringify(Location, Location)}
	 * @param name The name of the multi-block structure
	 * @param strictMode Whether block data is taken into account. Only checks material if false. Defaults to true.
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(String info, String name, boolean strictMode) {
		return new MultiBlockStructure(info, name, strictMode, false);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an info string
	 * @param info The info string. Get this from {@link MultiBlockStructure#stringify(Location, Location)}
	 * @param name The name of the multi-block structure
	 * @param strictMode Whether block data is taken into account. Only checks material if false. Defaults to true.
	 * @param ignoreAir If true, air in the original structure is skipped when checking blocks. Defaults to false.
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(String info, String name, boolean strictMode, boolean ignoreAir) {
		return new MultiBlockStructure(info, name, strictMode, ignoreAir);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an input stream containing the info string
	 * @param stream The input stream. Get this from {@link org.bukkit.plugin.java.JavaPlugin#getResource(String)}
	 * @param name The name of the multi-block structure
	 * @param strictMode Whether block data is taken into account. Only checks material if false. Defaults to true.
	 * @param ignoreAir If true, air in the original structure is skipped when checking blocks. Defaults to false.
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(InputStream stream, String name, boolean strictMode, boolean ignoreAir) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			String combine = "";
			while ((line = reader.readLine()) != null) {
				combine += line;
			}
			return create(combine, name, strictMode, ignoreAir);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an input stream containing the info string
	 * @param stream The input stream. Get this from {@link org.bukkit.plugin.java.JavaPlugin#getResource(String)}
	 * @param name The name of the multi-block structure
	 * @param strictMode Whether block data is taken into account. Only checks material if false. Defaults to true.
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(InputStream stream, String name, boolean strictMode) {
		return create(stream, name, strictMode, false);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an input stream containing the info string
	 * @param stream The input stream. Get this from {@link org.bukkit.plugin.java.JavaPlugin#getResource(String)}
	 * @param name The name of the multi-block structure
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(InputStream stream, String name) {
		return create(stream, name, true, false);
	}
	
	private static String minify(String data) {
		data = data.replace("minecraft:", "");
		String[] split = data.split(";");
		int same = 0;
		String output = split[0] + ";";
		for (int i = 1; i < split.length - 1; i++) {
			if (split[i].equals(split[i + 1])) {
				same += same == 0 ? 2 : 1;
				continue;
			} else if (same > 0) {
				output += split[i - 1] + "*" + same + ";";
				same = 0;
				continue;
			}
			output += split[i] + ";";
		}
		if (same > 0) {
			output += split[split.length - 1] + "*" + same + ";";
		} else {
			output += split[split.length - 1];
		}
		Map<String, Integer> count = new HashMap<>();
		split = output.split(";");
		for (int i = 1; i < split.length; i++) {
			String str = split[i];
			if (str.contains("*")) {
				str = str.substring(0, str.indexOf('*'));
			}
			if (!count.containsKey(str)) {
				count.put(str, 1);
				continue;
			}
			count.put(str, count.get(str) + 1);
		}
		List<String> replace = new ArrayList<>();
		for (Entry<String, Integer> entry : count.entrySet()) {
			if (entry.getValue() >= 2) {
				replace.add(entry.getKey());
			}
		}
		replace.sort((a, b) -> b.length() - a.length());
		String prepend = "";
		for (int i = 0; i < replace.size(); i++) {
			String str = replace.get(i);
			prepend += str + ";";
			output = output.replaceAll("(?<=;|^)" + Pattern.quote(str) + "(?=[^a-z]|$)", i + "");
		}
		if (replace.size() > 0) {
			output = "(" + prepend.substring(0, prepend.length() - 1) + ")" + output + ";";
		}
		return output;
	}
	
	private static String expand(String data) {
		String[] replace = null;
		if (data.startsWith("(")) {
			String list = data.substring(1, data.indexOf(')'));
			replace = list.split(";");
			data = data.substring(data.indexOf(')') + 1);
		}
		StringBuilder builder = new StringBuilder();
		for (String str : data.split(";")) {
			String[] split = str.split("\\*");
			String val = "";
			try {
				int index = Integer.parseInt(split[0]);
				val = replace[index];
			} catch (NumberFormatException e) {
				val = split[0];
			}
			if (split.length > 1) {
				int times = Integer.parseInt(split[1]);
				for (int i = 0; i < times; i++) {
					builder.append(val).append(';');
				}
				continue;
			}
			builder.append(val).append(';');
		}
		return builder.toString() + ";";
	}
	
	private String[][][] data;
	private String dataString;
	private String name;
	private int dimX;
	private int dimY;
	private int dimZ;
	private boolean strictMode = true;
	private boolean ignoreAir = false;
	
	private MultiBlockStructure(String info, String name, boolean strictMode, boolean ignoreAir) {
		info = expand(info);
		this.dataString = info;
		this.name = name;
		this.strictMode = strictMode;
		this.ignoreAir = ignoreAir;
		String[] split = info.split(";");
		String[] dimSplit = split[0].split("x");
		dimX = Integer.parseInt(dimSplit[0]);
		dimY = Integer.parseInt(dimSplit[1]);
		dimZ = Integer.parseInt(dimSplit[2]);
		data = parse(info, dimX, dimY, dimZ);
	}
	
	private static String[][][] parse(String info, int dimX, int dimY, int dimZ) {
		String[] split = info.split(";");
		String[][][] data = new String[dimX][dimY][dimZ];
		
		int pos = 1;
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					data[x][y][z] = split[pos];
					pos++;
				}
			}
		}
		return data;
	}
	
	private void forEachData(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror, BiConsumer<Location, String> callback) {
		Rotator rotator = new Rotator(rotation, mirror);
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					rotator.setLocation(x, z);
					Location l = loc.clone().add(rotator.getRotatedX(), y, rotator.getRotatedZ());
					rotator.setLocation(relX, relZ);
					l.subtract(rotator.getRotatedX(), relY, rotator.getRotatedZ());
					callback.accept(l, rotator.rotate(data[x][y][z]));
				}
			}
		}
	}
	
	/**
	 * Iterates each block which would be set if this structure is built
	 * @param loc The location the structure would be built at
	 * @param relX The relative X in the structure to center at
	 * @param relY The relative Y in the structure to center at
	 * @param relZ The relative Z in the structure to center at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @param callback The callback to be called, passed the {@link BlockState} which would be set if the structure is built here
	 */
	public void forEachBlock(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror, Consumer<BlockState> callback) {
		forEachData(loc, relX, relY, relZ, rotation, mirror, (l, s) -> {
			callback.accept(getStateToSet(l, s));
		});
	}
	
	/**
	 * 
	 * @param relX The relative X of the block within this multi-block structure
	 * @param relY The relative Y of the block within this multi-block structure
	 * @param relZ The relative Z of the block within this multi-block structure
	 * @return A BlockState, with the Location (0, 0, 0) in the default world, with the data at the specified relative location within this multi-block structure.
	 * This is done for compatibility reasons. For 1.8, MaterialData would make the most sense, while for 1.13+, BlockData would. BlockState can be converted to either.
	 * @throws ArrayIndexOutOfBoundsException if the relative coordinates do not exist within this structure
	 */
	public BlockState getData(int relX, int relY, int relZ) {
		Location loc = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
		return this.getStateToSet(loc, data[relX][relY][relZ]);
	}
	
	/**
	 * Iterates each block which would be set if this structure is built
	 * @param loc The location the structure would be built at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @param callback The callback to be called, passed the {@link BlockState} which would be set if the structure is built here
	 */
	public void forEachBlock(Location loc, int rotation, boolean mirror, Consumer<BlockState> callback) {
		forEachBlock(loc, 0, 0, 0, rotation, mirror, callback);
	}
	
	/**
	 * Iterates each block which would be set if this structure is built
	 * @param loc The location the structure would be built at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param callback The callback to be called, passed the {@link BlockState} which would be set if the structure is built here
	 */
	public void forEachBlock(Location loc, int rotation, Consumer<BlockState> callback) {
		forEachBlock(loc, 0, 0, 0, rotation, false, callback);
	}
	
	/**
	 * Iterates each block which would be set if this structure is built
	 * @param loc The location the structure would be built at
	 * @param callback The callback to be called, passed the {@link BlockState} which would be set if the structure is built here
	 */
	public void forEachBlock(Location loc, Consumer<BlockState> callback) {
		forEachBlock(loc, 0, 0, 0, 0, false, callback);
	}
	
	/**
	 * Uses a Predicate to test each block where this structure would be built
	 * @param loc The location to test the conditions at
	 * @param relX The relative X in the structure to test centered at
	 * @param relY The relative Y in the structure to test centered at
	 * @param relZ The relative Z in the structure to test centered at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @param filter The predicate to check each location
	 * @return Whether every location passed the check
	 */
	public boolean canBuild(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror, Predicate<Location> filter) {
		boolean[] canBuild = {true};
		forEachData(loc, relX, relY, relZ, rotation, mirror, (l, d) -> {
			if (!filter.test(l)) {
				canBuild[0] = false;
			}
		});
		return canBuild[0];
	}
	
	/**
	 * Uses a Predicate to test each block where this structure would be built
	 * @param loc The location to test the conditions at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @param filter The predicate to check each location
	 * @return Whether every location passed the check
	 */
	public boolean canBuild(Location loc, int rotation, boolean mirror, Predicate<Location> filter) {
		return canBuild(loc, 0, 0, 0, rotation, mirror, filter);
	}
	
	/**
	 * Uses a Predicate to test each block where this structure would be built
	 * @param loc The location to test the conditions at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param filter The predicate to check each location
	 * @return Whether every location passed the check
	 */
	public boolean canBuild(Location loc, int rotation, Predicate<Location> filter) {
		return canBuild(loc, 0, 0, 0, rotation, false, filter);
	}
	
	/**
	 * Uses a Predicate to test each block where this structure would be built
	 * @param loc The location to test the conditions at
	 * @param filter The predicate to check each location
	 * @return Whether every location passed the check
	 */
	public boolean canBuild(Location loc, Predicate<Location> filter) {
		return canBuild(loc, 0, 0, 0, 0, false, filter);
	}
	
	/**
	 * Sends ghost blocks of this multi-block structure to the given player at the given location
	 * @param loc The location to visualize the structure at
	 * @param relX The relative X in the structure to visualize centered at
	 * @param relY The relative Y in the structure to visualize centered at
	 * @param relZ The relative Z in the structure to visualize centered at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 */
	public void visualize(Player player, Location loc, int relX, int relY, int relZ, int rotation, boolean mirror) {
		forEachData(loc, relX, relY, relZ, rotation, mirror, (l, d) -> {
			sendBlock(player, l, d);
		});
	}
	
	/**
	 * Sends ghost blocks of this multi-block structure to the given player at the given location
	 * @param loc The location to visualize the structure at
	 * @param relX The relative X in the structure to visualize centered at
	 * @param relY The relative Y in the structure to visualize centered at
	 * @param relZ The relative Z in the structure to visualize centered at
	 */
	public void visualize(Player player, Location loc, int relX, int relY, int relZ) {
		visualize(player, loc, relX, relY, relZ, 0, false);
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @param relX The relative X in the structure to build centered at
	 * @param relY The relative Y in the structure to build centered at
	 * @param relZ The relative Z in the structure to build centered at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @return The Structure instance that was created
	 */
	public Structure build(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror) {
		forEachData(loc, relX, relY, relZ, rotation, mirror, (l, d) -> {
			BlockState state = getStateToSet(l, d);
			if (state != null) {
				state.update(true, false);
			}
		});
		return getAt(loc, relX, relY, relZ, rotation, mirror);
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @param relX The relative X in the structure to build centered at
	 * @param relY The relative Y in the structure to build centered at
	 * @param relZ The relative Z in the structure to build centered at
	 * @return The Structure instance that was created
	 */
	public Structure build(Location loc, int relX, int relY, int relZ) {
		return build(loc, relX, relY, relZ, 0, false);
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @param relX The relative X in the structure to build centered at
	 * @param relY The relative Y in the structure to build centered at
	 * @param relZ The relative Z in the structure to build centered at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @return The Structure instance that was created
	 */
	public Structure build(Location loc, int relX, int relY, int relZ, int rotation) {
		return build(loc, relX, relY, relZ, rotation, false);
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @return The Structure instance that was created
	 */
	public Structure build(Location loc) {
		return build(loc, 0, 0, 0, 0, false);
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @return The Structure instance that was created
	 */
	public Structure build(Location loc, int rotation) {
		return build(loc, 0, 0, 0, rotation, false);
	}
	
	/**
	 * Gets this multi-block structure's name. May be faster to compare this than to use .equals().
	 * @return The name of this multi-block structure
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the dimensions of this multi-block structure. [x, y, z]
	 * @return The dimensions of this multi-block structure
	 */
	public int[] getDimensions() {
		return new int[] {dimX, dimY, dimZ};
	}
	
	/**
	 * @return Whether this structure ignores air in the data when building and checking for presence
	 */
	public boolean ignoresAir() {
		return ignoreAir;
	}
	
	/**
	 * @return Whether this structure ignores data other than block type when checking for presence
	 */
	public boolean isStrictMode() {
		return strictMode;
	}
	
	/**
	 * Gets the Structure at the given block, if it exists.
	 * The given location can be any part of the multi-block structure.
	 * @param loc The location to check at
	 * @return The structure at this block, or null if it does not exist
	 */
	public Structure getAt(Location loc) {
		Block block = loc.getBlock();
		for (int rot = 0; rot < 4; rot++) {
			Rotator rotator = new Rotator(rot, false);
			for (int x = 0; x < dimX; x++) {
				for (int y = 0; y < dimY; y++) {
					for (int z = 0; z < dimZ; z++) {
						Structure s;
						if (compare(data[x][y][z], block, rotator) && (s = test(loc, x, y, z, rotator)) != null) {
							return s;
						}
					}
				}
			}
		}
		for (int rot = 0; rot < 4; rot++) {
			Rotator rotator = new Rotator(rot, true);
			for (int x = 0; x < dimX; x++) {
				for (int y = 0; y < dimY; y++) {
					for (int z = 0; z < dimZ; z++) {
						Structure s;
						if (compare(data[x][y][z], block, rotator) && (s = test(loc, x, y, z, rotator)) != null) {
							return s;
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Gets the Structure at the given block, if it exists. All parameters must be known.
	 * Significantly faster than {@link MultiBlockStructure#getAt(Location)}
	 * @param loc The location to check at
	 * @param relX The relative X in the structure of the location
	 * @param relY The relative Y in the structure of the location
	 * @param relZ The relative Z in the structure of the location
	 * @param rotation The rotation of the structure
	 * @param mirror Whether the structure is mirrored
	 * @return The structure at this block, or null if it does not exist
	 */
	public Structure getAt(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror) {
		Structure s;
		Rotator rotator = new Rotator(rotation, mirror);
		if (compare(data[relX][relY][relZ], loc.getBlock(), rotator) && (s = test(loc, relX, relY, relZ, rotator)) != null) {
			return s;
		}
		return null;
	}
	
	private Structure test(Location loc, int xPos, int yPos, int zPos, Rotator rotator) {
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					rotator.setLocation(x - xPos, z - zPos);
					int xp = rotator.getRotatedX();
					int yp = y - yPos;
					int zp = rotator.getRotatedZ();
					Block block = loc.clone().add(xp, yp, zp).getBlock();
					if (!compare(data[x][y][z], block, rotator)) {
						return null;
					}
				}
			}
		}
		rotator.setLocation(xPos, zPos);
		loc = loc.subtract(rotator.getRotatedX(), yPos, rotator.getRotatedZ());
		return new Structure(this, loc, rotator);
	}
	
	private boolean compare(String data, Block block, Rotator rotator) {
		if (midVersion >= 13) {
			data = rotator.rotate(data);
			data = data.startsWith("minecraft:") ? data : "minecraft:" + data;
			if (ignoreAir && Bukkit.createBlockData(data).getMaterial() == Material.AIR) {
				return true;
			}
			if (!strictMode) {
				return block.getType() == Bukkit.createBlockData(data).getMaterial();
			}
			return block.getBlockData().getAsString().equals(data);
		} else {
			String[] split = data.split(":");
			if (ignoreAir && split[0].equals("AIR")) {
				return true;
			}
			if (!strictMode) {
				return block.getType() == Material.valueOf(split[0]);
			}
			return block.getType() == Material.valueOf(split[0]) && block.getData() == Byte.parseByte(split[1]);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MultiBlockStructure) {
			MultiBlockStructure structure = (MultiBlockStructure) o;
			return structure.dataString.equals(dataString) && structure.name.equals(name);
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
	
	private BlockState getStateToSet(Location loc, String data) {
		if (midVersion >= 13) {
			BlockData blockData = Bukkit.createBlockData(data);
			if (ignoreAir && blockData.getMaterial() == Material.AIR) {
				return null;
			}
			BlockState state = loc.getBlock().getState();
			state.setBlockData(blockData);
			return state;
		} else {
			String[] split = data.split(":");
			Material type = Material.valueOf(split[0]);
			if (ignoreAir && type == Material.AIR) {
				return null;
			}
			byte dataValue = Byte.parseByte(split[1]);
			BlockState state = loc.getBlock().getState();
			state.setData(new MaterialData(type, dataValue));
			return state;
		}
	}
	
	private void sendBlock(Player player, Location loc, String data) {
		if (midVersion >= 13) {
			BlockData blockData = Bukkit.createBlockData(data);
			if (ignoreAir && blockData.getMaterial() == Material.AIR) {
				return;
			}
			player.sendBlockChange(loc, blockData);
		} else {
			String[] split = data.split(":");
			Material type = Material.valueOf(split[0]);
			if (ignoreAir && type == Material.AIR) {
				return;
			}
			byte dataValue = Byte.parseByte(split[1]);
			player.sendBlockChange(loc, type, dataValue);
		}
	}
	
	/**
	 * Used to rotate blocks and block sections when building or testing for the presence of a MultiBlockStructure
	 * @author Redempt
	 *
	 */
	public static class Rotator {
		
		private static String[] rotations = {"x,z", "-z,x", "-x,-z", "z,-x"};
		private static String[] blockDirections = {"north", "east", "south", "west"};
		
		private int rotation;
		private boolean mirrored;
		private int x = 0;
		private int z = 0;
		
		/**
		 * Constructs a new Rotator
		 * @param rotation The number of 90-degree clockwise rotations this Rotator applies
		 * @param mirrored Whether this Rotator should mirror over the X axis
		 */
		public Rotator(int rotation, boolean mirrored) {
			while (rotation < 0) {
				rotation += 4;
			}
			this.rotation = rotation % 4;
			this.mirrored = mirrored;
		}
		
		/**
		 * Rotates block data. NOTE: Only works for 1.13+
		 * @param data The block data to rotate
		 * @return The rotated block data
		 */
		public String rotate(String data) {
			rotate:
			if (midVersion >= 13) {
				if (data.contains("facing=")) {
					int start = data.indexOf("facing=") + 7;
					int end = data.indexOf(',', start);
					end = end == -1 ? data.indexOf(']', start) : end;
					String facing = data.substring(start, end);
					int num = -1;
					for (int i = 0; i < blockDirections.length; i++) {
						if (facing.equals(blockDirections[i])) {
							num = i;
							break;
						}
					}
					if (num == -1) {
						return data;
					}
					if (mirrored && (num == 1 || num == 3)) {
						num += 2;
					}
					num += rotation;
					num %= 4;
					facing = blockDirections[num];
					data = data.substring(0, start) + facing + data.substring(end);
				}
				if (data.contains("axis=")) {
					int start = data.indexOf("axis=") + 5;
					int end = data.indexOf(',', start);
					end = end == -1 ? data.indexOf(']', start) : end;
					String axis = data.substring(start, end);
					if (rotation % 2 != 0) {
						if (axis.equals("x")) {
							axis = "z";
						} else if (axis.equals("z")) {
							axis = "x";
						}
					}
					data = data.substring(0, start) + axis + data.substring(end);
				}
				boolean[] directions = new boolean[blockDirections.length];
				for (int i = 0; i < blockDirections.length; i++) {
					int start = data.indexOf(blockDirections[i] + "=");
					if (start == -1) {
						break rotate;
					}
					start += blockDirections[i].length() + 1;
					int end = data.indexOf(',', start);
					end = end == -1 ? data.indexOf(']', start) : end;
					directions[i] = data.substring(start, end).equals("true");
				}
				for (int i = 0; i < directions.length; i++) {
					int dir = ((i - (rotation % 4)) + 4) % 4;
					if (mirrored && (i == 0 || i == 2)) {
						dir += 2;
					}
					dir %= 4;
					int start = data.indexOf(blockDirections[i] + "=") + blockDirections[i].length() + 1;
					int end = data.indexOf(',', start);
					end = end == -1 ? data.indexOf(']', start) : end;
					data = data.substring(0, start) + directions[dir] + data.substring(end);
				}
			}
			return data;
		}
		
		/**
		 * Sets the relative coordinates this Rotator will rotate
		 * @param x The relative X coordinate
		 * @param z The relative Z coordinate
		 */
		public void setLocation(int x, int z) {
			this.x = mirrored ? -x : x;
			this.z = z;
		}
		
		/**
		 * Gets the rotated relative X
		 * @return The rotated relative X
		 */
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
		
		/**
		 * Gets the rotated relative Z
		 * @return The rotated relative Z
		 */
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
		
		/**
		 * Gets a Rotator which will negate the operations of this Rotator
		 * @return The inverse Rotator
		 */
		public Rotator getInverse() {
			return new Rotator(-rotation, mirrored);
		}
		
		/**
		 * Gets a clone of this Rotator
		 * @return The clone of this Rotator
		 */
		public Rotator clone() {
			return new Rotator(rotation, mirrored);
		}
		
		/**
		 * Gets the rotation, in number of 90-degree clockwise rotations
		 * @return The rotation
		 */
		public int getRotation() {
			return rotation;
		}
		
		/**
		 * Gets whether this rotator mirrors over the X axis
		 * @return Whether this rotator mirrors over the X axis
		 */
		public boolean isMirrored() {
			return mirrored;
		}
		
	}
	
}
