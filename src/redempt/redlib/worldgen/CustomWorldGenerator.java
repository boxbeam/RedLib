package redempt.redlib.worldgen;

import org.bukkit.Bukkit;
import org.bukkit.World;
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
	
	public CustomWorldGenerator(long seed, CustomBiome... biomes) {
		for (CustomBiome biome : biomes) {
			for (int i = biome.getElevationMin(); i < biome.getElevationMax(); i++) {
				List<CustomBiome> list = this.biomes.get(i);
				list = list == null ? new ArrayList<>() : list;
				list.add(biome);
				this.biomes.set(i, list);
			}
		}
		elevation = new NoiseGenerator(seed);
		elevation.setScale(1 / 1000d);
		temperature.setScale(1 / 1000d);
		elevation.setWeight(127.5);
		temperature.setWeight(50);
	}
	
	public void setBiomeSize(double size) {
		elevation.setScale(1 / size);
		temperature.setScale(1 / size);
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
				int elevation = (int) (this.elevation.noise(x + ix, z + iz) + 127.5);
				double temperature = this.temperature.noise(x + ix, z + iz) + 50;
				BiomeBlend blend = getBiomes(elevation, temperature);
				if (blend == null) {
					continue;
				}
				if (blend.getRatio() == 0) {
					blend.getFirst().generateColumn(data, x, z, ix, iz, blend.getFirst().getBlockHeight(x + ix, z + iz));
					continue;
				}
				int fheight = blend.getFirst().getBlockHeight(x + ix, z + iz);
				int sheight = blend.getSecond().getBlockHeight(x + ix, z + iz);
				int height = (int) ((sheight - fheight) * blend.getRatio() + fheight);
				blend.getPriority().generateColumn(data, x, z, ix, iz, height);
			}
		}
		return data;
	}
	
	private BiomeBlend getBiomes(int elevation, double temperature) {
		List<CustomBiome> biomes = this.biomes.get(elevation);
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
		int bottom = Math.min(second.getElevationMin(), first.getElevationMin());
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
