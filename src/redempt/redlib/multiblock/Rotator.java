package redempt.redlib.multiblock;

import static redempt.redlib.RedLib.midVersion;

/**
 * Used to rotate blocks and block sections when building or testing for the presence of a MultiBlockStructure
 * @author Redempt
 *
 */
public class Rotator {
	
	private static final String[] BLOCK_DIRECTIONS = {"north", "east", "south", "west"};
	
	private int rotation;
	private boolean mirrored;
	private double x = 0;
	private double z = 0;
	
	/**
	 * Constructs a new Rotator
	 * @param rotation The number of 90-degree clockwise rotations this Rotator applies
	 * @param mirrored Whether this Rotator should mirror over the X axis
	 */
	public Rotator(int rotation, boolean mirrored) {
		while (rotation < 0) {
			rotation += 4;
		}
		this.rotation = rotation % 4;
		this.mirrored = mirrored;
	}
	
	/**
	 * Rotates block data. NOTE: Only works for 1.13+
	 * @param data The block data to rotate
	 * @return The rotated block data
	 */
	public String rotate(String data) {
		rotate:
		if (midVersion >= 13) {
			if (data.contains("facing=")) {
				int start = data.indexOf("facing=") + 7;
				int end = data.indexOf(',', start);
				end = end == -1 ? data.indexOf(']', start) : end;
				String facing = data.substring(start, end);
				int num = -1;
				for (int i = 0; i < BLOCK_DIRECTIONS.length; i++) {
					if (facing.equals(BLOCK_DIRECTIONS[i])) {
						num = i;
						break;
					}
				}
				if (num == -1) {
					return data;
				}
				if (mirrored && (num == 1 || num == 3)) {
					num += 2;
				}
				num += rotation;
				num %= 4;
				facing = BLOCK_DIRECTIONS[num];
				data = data.substring(0, start) + facing + data.substring(end);
			}
			if (data.contains("axis=")) {
				int start = data.indexOf("axis=") + 5;
				int end = data.indexOf(',', start);
				end = end == -1 ? data.indexOf(']', start) : end;
				String axis = data.substring(start, end);
				if (rotation % 2 != 0) {
					if (axis.equals("x")) {
						axis = "z";
					} else if (axis.equals("z")) {
						axis = "x";
					}
				}
				data = data.substring(0, start) + axis + data.substring(end);
			}
			String[] directions = new String[BLOCK_DIRECTIONS.length];
			for (int i = 0; i < BLOCK_DIRECTIONS.length; i++) {
				int start = data.indexOf(BLOCK_DIRECTIONS[i] + "=");
				if (start == -1) {
					break rotate;
				}
				start += BLOCK_DIRECTIONS[i].length() + 1;
				int end = data.indexOf(',', start);
				end = end == -1 ? data.indexOf(']', start) : end;
				directions[i] = data.substring(start, end);
			}
			for (int i = 0; i < directions.length; i++) {
				int dir = ((i - (rotation % 4)) + 4) % 4;
				if (mirrored && (i == 0 || i == 2)) {
					dir += 2;
				}
				dir %= 4;
				int start = data.indexOf(BLOCK_DIRECTIONS[i] + "=") + BLOCK_DIRECTIONS[i].length() + 1;
				int end = data.indexOf(',', start);
				end = end == -1 ? data.indexOf(']', start) : end;
				data = data.substring(0, start) + directions[dir] + data.substring(end);
			}
		}
		return data;
	}
	
	/**
	 * Sets the relative coordinates this Rotator will rotate
	 * @param x The relative X coordinate
	 * @param z The relative Z coordinate
	 */
	public void setLocation(double x, double z) {
		this.x = mirrored ? -x : x;
		this.z = z;
	}
	
	/**
	 * @return The rotated relative block X
	 */
	public int getRotatedBlockX() {
		return (int) getRotatedX();
	}
	
	/**
	 * @return The rotated relative block Z
	 */
	public int getRotatedBlockZ() {
		return (int) getRotatedZ();
	}
	
	/**
	 * @return The rotated relative X
	 */
	public double getRotatedX() {
		switch (rotation) {
			case 0:
				return x;
			case 1:
				return -z;
			case 2:
				return -x;
			case 3:
				return z;
		}
		return 0;
	}
	
	/**
	 * @return The rotated relative Z
	 */
	public double getRotatedZ() {
		switch (rotation) {
			case 0:
				return z;
			case 1:
				return x;
			case 2:
				return -z;
			case 3:
				return -x;
		}
		return 0;
	}
	
	/**
	 * Gets a Rotator which will negate the operations of this Rotator
	 * @return The inverse Rotator
	 */
	public Rotator getInverse() {
		return new Rotator(-rotation, mirrored);
	}
	
	/**
	 * Gets a clone of this Rotator
	 * @return The clone of this Rotator
	 */
	public Rotator clone() {
		return new Rotator(rotation, mirrored);
	}
	
	/**
	 * Gets the rotation, in number of 90-degree clockwise rotations
	 * @return The rotation
	 */
	public int getRotation() {
		return rotation;
	}
	
	/**
	 * Sets the rotation
	 * @param rotation The rotation to set
	 */
	public void setRotation(int rotation) {
		this.rotation = rotation;
	}
	
	/**
	 * Sets whether this rotator mirrors over the X axis
	 * @param mirrored Whether this rotator mirrors over the X axis
	 */
	public void setMirrored(boolean mirrored) {
		this.mirrored = mirrored;
	}
	
	/**
	 * Gets whether this rotator mirrors over the X axis
	 * @return Whether this rotator mirrors over the X axis
	 */
	public boolean isMirrored() {
		return mirrored;
	}
	
}