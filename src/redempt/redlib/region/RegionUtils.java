package redempt.redlib.region;

import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for niche use cases of Regions
 *
 * @author Redempt
 */
public class RegionUtils {

    /**
     * Finds the spaces within a Region that can be considered "inside" - space that is surrounded
     * by other blocks
     *
     * @param region The region to find the interior space of
     * @return A MultiRegion representing all of the "inside" space within the given region
     */
    public static MultiRegion findInside(CuboidRegion region) {
        int[] dim = region.getBlockDimensions();
        MultiRegion multi = null;
        multi = addAll(multi, iter(region, dim, new int[]{0, 1, 2}));
        multi = addAll(multi, iter(region, dim, new int[]{0, 2, 1}));
        multi = addAll(multi, iter(region, dim, new int[]{2, 1, 0}));
        if (multi != null) {
            multi.recalculate();
        }
        return multi;
    }

    private static MultiRegion addAll(MultiRegion region, List<CuboidRegion> reg) {
        int count = 0;
        for (CuboidRegion r : reg) {
            count++;
            if (region == null) {
                region = new MultiRegion(r);
                continue;
            }
            region.add(r);
            if (count >= 10) {
                region.recalculate();
                count = 0;
            }
        }
        return region;
    }

    private static List<CuboidRegion> iter(CuboidRegion region, int[] dim, int[] order) {
        int[] pos = new int[3];
        List<CuboidRegion> regions = new ArrayList<>();
        for (pos[order[0]] = 0; pos[order[0]] < dim[order[0]]; pos[order[0]]++) {
            for (pos[order[1]] = 0; pos[order[1]] < dim[order[1]]; pos[order[1]]++) {
                Block block = null;
                for (pos[order[2]] = 0; pos[order[2]] < dim[order[2]]; pos[order[2]]++) {
                    Block b = region.getStart().getBlock().getRelative(pos[0], pos[1], pos[2]);
                    if (b.getType().isSolid()) {
                        block = b;
                        break;
                    }
                }
                if (block == null) {
                    continue;
                }
                for (pos[order[2]] = dim[order[2]] - 1; pos[order[2]] >= 0; pos[order[2]]--) {
                    Block b = region.getStart().getBlock().getRelative(pos[0], pos[1], pos[2]);
                    if (b.equals(block)) {
                        break;
                    }
                    if (b.getType().isSolid()) {
                        CuboidRegion reg = new CuboidRegion(block.getLocation(), b.getLocation());
                        reg.expand(1, 0, 1, 0, 1, 0);
                        regions.add(reg);
                        break;
                    }
                }
            }
        }
        return regions;
    }

}
