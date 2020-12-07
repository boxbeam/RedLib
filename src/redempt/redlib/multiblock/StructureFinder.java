package redempt.redlib.multiblock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;

import java.util.*;

import static redempt.redlib.RedLib.midVersion;

class StructureFinder {
	
	private MultiBlockStructure type;
	private boolean ignoreAir;
	private boolean strictMode;
	private Map<Material, List<int[]>> materialMap = new HashMap<>();
	private List<Material> materials = new ArrayList<>();
	
	public StructureFinder(MultiBlockStructure type, String[][][] data, int dimX, int dimY, int dimZ, boolean strictMode, boolean ignoreAir) {
		this.type = type;
		this.ignoreAir = ignoreAir;
		this.strictMode = strictMode;
	}
	
	private Material getType(int x, int y, int z) {
		String data = type.data[x][y][z];
		int ind = data.indexOf(midVersion >= 13 ? '[' : ':');
		ind = ind == -1 ? data.length() : ind;
		return Material.valueOf(data.substring(0, ind).toUpperCase());
	}
	
	private void initializeMap() {
		for (int x = 0; x < type.dimX; x++) {
			for (int y = 0; y < type.dimY; y++) {
				for (int z = 0; z < type.dimZ; z++) {
					Material type = getType(x, y, z);
					List<int[]> list = materialMap.get(type);
					if (list == null) {
						list = new ArrayList<>();
						materialMap.put(type, list);
						materials.add(type);
					}
					list.add(new int[] {x, y, z});
				}
			}
		}
		boolean air = materials.remove(Material.AIR);
		materials.sort(Comparator.comparingInt(m -> materialMap.get(m).size()));
		if (air) {
			materials.add(Material.AIR);
		}
	}
	
	public Structure getAt(Block block) {
		if (materialMap.size() == 0) {
			initializeMap();
		}
		List<int[]> locations = materialMap.get(block.getType());
		if (locations == null) {
			if (!ignoreAir) {
				return null;
			}
			locations = materialMap.get(Material.AIR);
			if (locations == null) {
				return null;
			}
		}
		List<Integer> rotations = new ArrayList<>();
		Collections.addAll(rotations, 0, 1, 2, 3);
		Rotator rotator = new Rotator(1, false);
		for (int rot = 0; rot < 4; rot++) {
			for (int mirror = 0; mirror < 2; mirror++) {
				rotator.setRotation(rot);
				rotator.setMirrored(mirror == 1);
				Structure struct = getAt(block, rotator);
				if (struct != null) {
					return struct;
				}
			}
		}
		return null;
	}
	
	private Structure getAt(Block block, Rotator rotator) {
		int maxX = type.dimX;
		int maxY = type.dimY;
		int maxZ = type.dimZ;
		Structure struct;
		boolean foundY;
		for (int x = 0; x < maxX; x++) {
			for (int y = 0; y < maxY; y++) {
				foundY = false;
				for (int z = 0; z < maxZ; z++) {
					rotator.setLocation(x, z);
					Block b = block.getRelative(-rotator.getRotatedBlockX(), -y, -rotator.getRotatedBlockZ());
					if (!ignoreAir && !materialMap.containsKey(b.getType())) {
						if (foundY) {
							maxZ = z;
						}
						break;
					}
					foundY = true;
					struct = getExact(b, rotator);
					if (struct != null) {
						return struct;
					}
				}
				if (!foundY) {
					maxY = y;
				}
			}
		}
		return null;
	}
	
	private Structure getExact(Block block, Rotator rotator) {
		for (Material mat : materials) {
			List<int[]> instances = materialMap.get(mat);
			for (int[] pos : instances) {
				String data = type.data[pos[0]][pos[1]][pos[2]];
				rotator.setLocation(pos[0], pos[2]);
				Block b = block.getRelative(rotator.getRotatedBlockX(), pos[1], rotator.getRotatedBlockZ());
				if (!type.compare(data, b, rotator)) {
					return null;
				}
			}
		}
		return new Structure(type, block.getLocation(), rotator.clone());
	}
	
}
