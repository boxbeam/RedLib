package redempt.redlib.worldgen;

import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator.ChunkData;

/**
 * @deprecated Implementation in progress.
 */
public abstract class CustomBiome {
	
	public abstract ChunkData generateColumn(ChunkData data, int cx, int cz, int bx, int bz, int height);
	public abstract int getBlockHeight(int x, int z);
	public abstract int getCaveHeight(int x, int z);
	public abstract String getName();
	public abstract int getTemperature();
	public abstract int getElevationMin();
	public abstract int getElevationMax();
	public abstract Biome getVanillaBiome();

}
