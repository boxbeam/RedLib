package redempt.redlib.blockdata;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

/**
 * Represents a world and chunk X and Z
 * @author Redempt
 */
public class ChunkPosition {

	private int x;
	private int z;
	private String world;
	
	/**
	 * Creates a ChunkPosition from a chunk
	 * @param chunk The chunk to create a position for
	 */
	public ChunkPosition(Chunk chunk) {
		this(chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
	}
	
	/**
	 * Creates a ChunkPosition from a Block
	 * @param block The Block to create a position for
	 */
	public ChunkPosition(Block block) {
		this(new BlockPosition(block), block.getWorld().getName());
	}
	
	/**
	 * Creates a ChunkPosition from chunk coordinates and a world name
	 * @param x The chunk X
	 * @param z The chunk Z
	 * @param world The world name
	 */
	public ChunkPosition(int x, int z, String world) {
		this.x = x;
		this.z = z;
		this.world = world;
	}
	
	public ChunkPosition(BlockPosition bPos, String world) {
		this(bPos.getX() >> 4, bPos.getZ() >> 4, world);
	}
	
	/**
	 * @return The chunk X
	 */
	public int getX() {
		return x;
	}
	
	/**
	 * @return The chunk Z
	 */
	public int getZ() {
		return z;
	}
	
	/**
	 * @return The world this ChunkPosition is in
	 */
	public World getWorld() {
		return Bukkit.getWorld(world);
	}
	
	/**
	 * @return The name of the world this ChunkPosition is in
	 */
	public String getWorldName() {
		return world;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(x, z, world);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ChunkPosition)) {
			return false;
		}
		ChunkPosition pos = (ChunkPosition) o;
		return pos.x == x && pos.z == z && world.equals(pos.world);
	}
	
}
