package redempt.redlib.worldgen;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @deprecated Implementation in progress.
 */
public class CustomWorldGenerator extends ChunkGenerator {
	
	private List<List<CustomBiome>> biomes = new ArrayList<>(255);
	
	private NoiseGenerator elevation;
	private NoiseGenerator temperature;
	private int elevationMin = -1;
	private int elevationMax = -1;
	
	public CustomWorldGenerator(long seed, CustomBiome... biomes) {
		for (int i = 0; i < 255; i++) {
			this.biomes.add(null);
		}
		for (CustomBiome biome : biomes) {
			for (int i = biome.getElevationMin(); i < biome.getElevationMax(); i++) {
				List<CustomBiome> list = this.biomes.get(i);
				list = list == null ? new ArrayList<>() : list;
				list.add(biome);
				this.biomes.set(i, list);
				if (elevationMin == -1 || biome.getElevationMin() < elevationMin) {
					elevationMin = biome.getElevationMin();
				}
				if (elevationMax == -1 || biome.getElevationMax() > elevationMax) {
					elevationMax = biome.getElevationMax();
				}
			}
		}
		elevation = new NoiseGenerator(seed);
		elevation.setScale(1 / 1000d);
		temperature = new NoiseGenerator(seed / 2);
		temperature.setScale(1 / 1000d);
		elevation.setWeight((elevationMax - elevationMin));
		temperature.setWeight(50);
	}
	
	public void setBiomeSize(double size) {
		elevation.setScale(1 / size);
		temperature.setScale(1 / size);
	}
	
	public void setSeed(long seed) {
		elevation.setSeed(seed);
		temperature.setSeed(seed / 2);
	}
	
	@Override
	public boolean isParallelCapable() {
		return true;
	}
	
	@Override
	public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
		ChunkData data = createChunkData(world);
		for (int ix = 0; ix < 16; ix++) {
			for (int iz = 0; iz < 16; iz++) {
				double elevation = (int) (this.elevation.noise(16 * x + ix, 16 * z + iz) + (elevationMax - elevationMin));
				elevation /= 2;
				elevation += elevationMin;
				double temperature = this.temperature.noise(16 * x + ix, 16 * z + iz) + 50;
				BiomeBlend blend = getBiomes(elevation, temperature);
				if (blend == null) {
					continue;
				}
				if (blend.getRatio() == 0) {
					blend.getFirst().generateColumn(data, x, z, ix, iz, blend.getFirst().getBlockHeight(16 * x + ix, 16 * z + iz));
					continue;
				}
				int fheight = blend.getFirst().getBlockHeight(16 * x + ix, 16 * z + iz);
				int sheight = blend.getSecond().getBlockHeight(16 * x + ix, 16 * z + iz);
				int height = (int) NoiseGenerator.smoothstep(fheight, sheight, blend.getRatio());
				data.setBlock(ix, 150, iz, Material.COBBLESTONE);
				blend.getPriority().generateColumn(data, x, z, ix, iz, height);
				Biome b = blend.getPriority().getVanillaBiome();
				if (b != null) {
					biome.setBiome(16 * x + ix, 16 * z + iz, b);
				}
			}
		}
		return data;
	}
	
	private BiomeBlend getBiomes(double elevation, double temperature) {
		List<CustomBiome> biomes = this.biomes.get((int) elevation);
		if (biomes == null || biomes.size() == 0) {
			return null;
		}
		if (biomes.size() == 1) {
			return new BiomeBlend(biomes.get(0), null, 0);
		}
		CustomBiome first = null;
		double dist = 0;
		for (CustomBiome biome : biomes) {
			double cdist = Math.abs(temperature - biome.getTemperature());
			if (first == null || cdist < dist) {
				dist = cdist;
				first = biome;
			}
		}
		CustomBiome second = null;
		for (CustomBiome biome : biomes) {
			if (biome.equals(first)) {
				continue;
			}
			double cdist = Math.abs(temperature - biome.getTemperature());
			if (second == null || cdist < dist) {
				dist = cdist;
				second = biome;
			}
		}
		int top = Math.min(first.getElevationMax(), second.getElevationMax());
		int bottom = Math.max(second.getElevationMin(), first.getElevationMin());
		CustomBiome least = (first.getElevationMax() + first.getElevationMin()) / 2 <
				(second.getElevationMax() + second.getElevationMin()) / 2 ? first : second;
		CustomBiome greatest = first == least ? second : first;
		double ratio = (elevation - bottom) / (double) (top - bottom);
		return new BiomeBlend(least, greatest, ratio);
	}
	
	private static class BiomeBlend {
		
		private CustomBiome first;
		private CustomBiome second;
		private double ratio;
		
		public BiomeBlend(CustomBiome first, CustomBiome second, double ratio) {
			this.first = first;
			this.second = second;
			this.ratio = ratio;
		}
		
		public CustomBiome getFirst() {
			return first;
		}
		
		public CustomBiome getSecond() {
			return second;
		}
		
		public double getRatio() {
			return ratio;
		}
		
		public CustomBiome getPriority() {
			return ratio < 0.5 ? first : second;
		}
		
	}
	
}
