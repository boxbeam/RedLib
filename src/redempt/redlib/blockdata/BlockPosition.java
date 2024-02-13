package redempt.redlib.blockdata;

import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

class BlockPosition {

    private int x;
    private int y;
    private int z;

    public BlockPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockPosition(Block block) {
        this(block.getX(), block.getY(), block.getZ());
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Block getBlock(World world) {
        return world.getBlockAt(x, y, z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockPosition)) {
            return false;
        }
        BlockPosition pos = (BlockPosition) o;
        return pos.x == x && pos.y == y && pos.z == z;
    }

    @Override
    public String toString() {
        return x + " " + y + " " + z;
    }

}
