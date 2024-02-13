package redempt.redlib.region;

import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

class MultiRegionMeta {

    private List<TreeSet<Double>> points = new ArrayList<>();

    public MultiRegionMeta(List<Region> regions) {
        for (int i = 0; i < 6; i++) {
            TreeSet<Double> direction = new TreeSet<>();
            for (Region region : regions) {
                direction.add(getCoordinate(region, i));
            }
            points.add(direction);
        }
    }

    public double getNextStep(BlockFace face, double current) {
        int index = getBlockFaceIndex(face);
        TreeSet<Double> list = points.get(index);
        Double next = index >= 3 ? list.lower(current) : list.higher(current);
        return next == null ? current : next;
    }

    public double getCurrentStep(Region region, BlockFace face) {
        return getCoordinate(region, getBlockFaceIndex(face));
    }

    private int getBlockFaceIndex(BlockFace face) {
        switch (face) {
            case EAST:
                return 0;
            case UP:
                return 1;
            case SOUTH:
                return 2;
            case WEST:
                return 3;
            case DOWN:
                return 4;
            case NORTH:
                return 5;
        }
        return -1;
    }

    private double getCoordinate(Region region, int coord) {
        switch (coord) {
            case 0:
                return region.getEnd().getX();
            case 1:
                return region.getEnd().getY();
            case 2:
                return region.getEnd().getZ();
            case 3:
                return region.getStart().getX();
            case 4:
                return region.getStart().getY();
            case 5:
                return region.getStart().getZ();
        }
        return 0;
    }

}
