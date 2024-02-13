package redempt.redlib.multiblock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import redempt.redlib.RedLib;

/**
 * Cross-version wrapper for block data - {@link BlockData} in 1.13+, {@link MaterialData} otherwise
 *
 * @author Redempt
 */
public class StructureData {

    private BlockData data;
    private MaterialData mdata;

    protected StructureData(String string) {
        if (RedLib.MID_VERSION >= 13) {
            data = Bukkit.createBlockData(string);
        } else {
            String[] split = string.split(":");
            Material type = Material.valueOf(split[0]);
            byte data = 0;
            if (split.length == 2) {
                data = Byte.parseByte(split[1]);
            }
            mdata = new MaterialData(type, data);
        }
    }

    public StructureData(Material type) {
        if (RedLib.MID_VERSION >= 13) {
            data = type.createBlockData();
        } else {
            mdata = new MaterialData(type, (byte) 0);
        }
    }

    /**
     * Creates a StructureData from a BlockData, for 1.13+
     *
     * @param data The BlockData
     */
    public StructureData(BlockData data) {
        this.data = data;
    }

    public StructureData getRotated(Rotator rotator) {
        if (RedLib.MID_VERSION >= 13) {
            return new StructureData(rotator.rotate(data));
        } else {
            return this;
        }
    }

    /**
     * Creates a StructureData from a Material and byte data, for 1.12 and below
     *
     * @param type The block type
     * @param data The data byte
     */
    public StructureData(Material type, byte data) {
        mdata = new MaterialData(type, data);
    }

    /**
     * Sets this StructureData at the given location
     *
     * @param block The block to set
     */
    public void setBlock(Block block) {
        if (RedLib.MID_VERSION >= 13) {
            block.setBlockData(data, false);
        } else {
            BlockState state = block.getState();
            state.setType(mdata.getItemType());
            state.setRawData(mdata.getData());
            state.update(true, false);
        }
    }

    /**
     * Gets the BlockState to set for a given block
     *
     * @param block The Block to get the BlockState at
     * @return The BlockState that would be set
     */
    public BlockState getState(Block block) {
        if (RedLib.MID_VERSION >= 13) {
            BlockState state = block.getState();
            state.setBlockData(data);
            return state;
        } else {
            BlockState state = block.getState();
            state.setType(mdata.getItemType());
            state.setRawData(mdata.getData());
            return state;
        }
    }

    /**
     * Sends a fake block change to a Player
     *
     * @param player The Player to send the fake block change to
     * @param loc    The Location of the fake block
     */
    public void sendBlock(Player player, Location loc) {
        if (RedLib.MID_VERSION >= 13) {
            player.sendBlockChange(loc, data);
        } else {
            player.sendBlockChange(loc, mdata.getItemType(), mdata.getData());
        }
    }

    /**
     * @return Whether the Material is air - for 1.15+, AIR, CAVE_AIR, or VOID_AIR
     */
    public boolean isAir() {
        return RedLib.MID_VERSION >= 15 ? getType().isAir() : getType() == Material.AIR;
    }

    /**
     * @return The type of this StructureData
     */
    public Material getType() {
        return RedLib.MID_VERSION >= 13 ? data.getMaterial() : mdata.getItemType();
    }

    /**
     * Compares this StructureData to a Block
     *
     * @param block     The Block to compare with
     * @param strict    Whether to compare strictly
     * @param ignoreAir Whether to return true automatically if this StructureData is air
     * @return Whether the block matches this StructureData within the given parameters
     */
    public boolean compare(Block block, boolean strict, boolean ignoreAir) {
        if (ignoreAir && isAir()) {
            return true;
        }
        if (!strict) {
            return block.getType() == getType();
        }
        if (RedLib.MID_VERSION >= 13) {
            return block.getBlockData().matches(data);
        } else {
            return block.getData() == mdata.getData() && block.getType() == mdata.getItemType();
        }
    }

}
