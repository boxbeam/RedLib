package redempt.redlib.worldgen;

import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator.ChunkData;

/**
 * @deprecated Implementation in progress.
 */
public abstract class CustomBiome {
	
	private String name;
	private int temperature;
	private int elevationMin;
	private int elevationMax;
	private Biome vanillaBiome;
	
	public CustomBiome(String name, int temperature, int elevationMin, int elevationMax, Biome vanillaBiome) {
		this.name = name;
		this.temperature = temperature;
		this.elevationMin = elevationMin;
		this.elevationMax = elevationMax;
		this.vanillaBiome = vanillaBiome;
	}
	
	public abstract void generateColumn(ChunkData data, int cx, int cz, int bx, int bz, int height);
	public abstract int getBlockHeight(int x, int z);
	public abstract int getCaveHeight(int x, int z, int blockHeight);
	
	public String getName() {
		return name;
	}
	public int getTemperature() {
		return temperature;
	}
	
	public int getElevationMin() {
		return elevationMin;
	}
	
	public int getElevationMax() {
		return elevationMax;
	}
	
	public Biome getVanillaBiome() {
		return vanillaBiome;
	}

	public void prefill(int cx, int cz) {}
	
}
