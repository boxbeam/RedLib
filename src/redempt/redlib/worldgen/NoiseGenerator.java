package redempt.redlib.worldgen;

import java.util.Random;

/**
 * A Perlin Noise generator which can be faster than {@link org.bukkit.util.noise.PerlinNoiseGenerator}
 *
 * @author Redempt
 */
public class NoiseGenerator {

    private static SVec[] vecs = {new SVec(1, 0, 1), new SVec(1, 0, -1), new SVec(-1, 0, 1), new SVec(-1, 0, -1),
            new SVec(1, 0, 0), new SVec(0, 0, 1), new SVec(-1, 0, 0), new SVec(0, 0, -1)};

    private static SVec[] vecs3 = {new SVec(1, 1, 0), new SVec(-1, 1, 0), new SVec(1, -1, 0), new SVec(-1, -1, 0),
            new SVec(1, 0, 1), new SVec(1, 0, -1), new SVec(-1, 0, 1), new SVec(-1, 0, -1),
            new SVec(0, 1, 1), new SVec(0, 1, -1), new SVec(0, -1, 1), new SVec(0, -1, -1)};

    private static long hash(long... nums) {
        long hash = 23;
        for (long num : nums) {
            if (num == 0) {
                hash += 143;
                continue;
            }
            hash *= num;
            hash += 37;
        }
        return hash;
    }

    private static SVec generateVector(long hash, Random random) {
        random.setSeed(hash);
        return vecs[random.nextInt(8)];
    }

    private static SVec generateVector3(long hash, Random random) {
        random.setSeed(hash);
        return vecs3[random.nextInt(12)];
    }

    /**
     * Smoothly interpolates between two values
     *
     * @param first  The first value
     * @param second The second value
     * @param w      The ratio
     * @return The smoothly interpolated value
     */
    public static double smoothstep(double first, double second, double w) {
        double value = w * w * w * (w * (w * 6 - 15) + 10);
        return first + (value * (second - first));
    }

    private long seed;
    private double scale = 1;
    private double weight = 1;
    private Random random;
    private SVec[][][] prefill = null;
    private int[] prefillPos = {0, 0, 0};

    /**
     * Creates a noise generator with a seed
     *
     * @param seed The seed to use
     */
    public NoiseGenerator(long seed) {
        this.seed = seed;
        random = new Random();
    }

    /**
     * Creates a noise generator with a random seed
     */
    public NoiseGenerator() {
        this(new Random().nextLong());
    }

    /**
     * Sets the scale of this noise generator. All inputs will be multiplied by the scale. Default value is 1.
     *
     * @param scale The scale to set
     */
    public void setScale(double scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("Scale cannot be negative");
        }
        this.scale = scale;
    }

    /**
     * Sets the weight of this noise generator. All outputs will be multiplied by the weight. Default value is 1.
     *
     * @param weight The weight to set
     */
    public void setWeight(double weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }
        this.weight = weight;
    }

    /**
     * @return The scale of this noise generator
     */
    public double getScale() {
        return scale;
    }

    /**
     * @return The weight of this noise generator
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Set the seed of this noise generator
     *
     * @param seed The seed to set
     */
    public void setSeed(long seed) {
        this.seed = seed;
    }

    /**
     * Pre-fills the gradient vectors in the specified area of this noise generator. Call this method if you know
     * what region you are going to be repeatedly requesting noise values from, as it significantly improves
     * performance. Inputs for this method are scaled according to the scale of this noise generator.
     *
     * @param x      The X coordinate to generate starting at
     * @param y      The Y coordinate to generate starting at
     * @param z      The Z coordiante to generate starting at
     * @param width  The width along the X-axis to fill
     * @param height The height along the Y-axis to fill
     * @param length The length along the Z-axis to fill
     */
    public void prefill(int x, int y, int z, int width, int height, int length) {
        x = (int) Math.floor((double) x * scale);
        y = (int) Math.floor((double) y * scale);
        z = (int) Math.floor((double) z * scale);
        width = (int) Math.ceil((double) width * scale) + 1;
        height = (int) Math.ceil((double) height * scale) + 1;
        length = (int) Math.ceil((double) length * scale) + 1;
        prefillPos[0] = x;
        prefillPos[1] = y;
        prefillPos[2] = z;
        prefill = new SVec[width][height][length];
        for (int xp = 0; xp < width; xp++) {
            for (int yp = 0; yp < height; yp++) {
                for (int zp = 0; zp < length; zp++) {
                    prefill[xp][yp][zp] = generateVector3(hash(seed, x + xp, y + yp, z + zp), random);
                }
            }
        }
    }

    /**
     * Pre-fills the gradient vectors in the specified area of this noise generator. Call this method if you know
     * what region you are going to be repeatedly requesting noise values from, as it significantly improves
     * performance. Inputs for this method are scaled according to the scale of this noise generator.
     *
     * @param x      The X coordinate to generate starting at
     * @param z      The Z coordiante to generate starting at
     * @param width  The width along the X-axis to fill
     * @param length The length along the Z-axis to fill
     */
    public void prefill(int x, int z, int width, int length) {
        x = (int) Math.floor((double) x * scale);
        z = (int) Math.floor((double) z * scale);
        width = (int) Math.ceil((double) width * scale) + 1;
        length = (int) Math.ceil((double) length * scale) + 1;
        prefillPos[0] = x;
        prefillPos[1] = 0;
        prefillPos[2] = z;
        prefill = new SVec[width][1][length];
        for (int xp = 0; xp < width; xp++) {
            for (int zp = 0; zp < length; zp++) {
                prefill[xp][0][zp] = generateVector(hash(seed, x + xp, z + zp), random);
            }
        }
    }

    private SVec getVector(int x, int y, int z) {
        if (prefill != null && x >= prefillPos[0] && x < prefillPos[0] + prefill.length
                && y >= prefillPos[1] && y < prefillPos[1] + prefill[0].length
                && z >= prefillPos[2] && z < prefillPos[2] + prefill[0][0].length) {
            return prefill[x - prefillPos[0]][y - prefillPos[1]][z - prefillPos[2]];
        }
        return generateVector3(hash(seed, x, y, z), random);
    }

    private SVec getVector(int x, int z) {
        if (prefill != null && x >= prefillPos[0] && x < prefillPos[0] + prefill.length
                && z >= prefillPos[2] && z < prefillPos[2] + prefill[0][0].length) {
            return prefill[x - prefillPos[0]][0][z - prefillPos[2]];
        }
        return generateVector(hash(seed, x, z), random);
    }

    /**
     * Gets a noise value at a specific 2D point
     *
     * @param x The X coordinate to get the noise at
     * @param z The Z coordinate to get the noise at
     * @return A noise value between the weight and the negative weight
     */
    public double noise(double x, double z) {
        x *= scale;
        z *= scale;
        int minX = (int) Math.floor(x);
        int minZ = (int) Math.floor(z);
        x -= minX;
        z -= minZ;
        double[] dots = new double[4];
        dots[0] = getVector(minX, minZ).dot(x, z);
        dots[1] = getVector(minX, minZ + 1).dot(x, z - 1);
        dots[2] = getVector(minX + 1, minZ).dot(x - 1, z);
        dots[3] = getVector(minX + 1, minZ + 1).dot(x - 1, z - 1);

        dots[0] = smoothstep(dots[0], dots[1], z);
        dots[1] = smoothstep(dots[2], dots[3], z);

        dots[0] = smoothstep(dots[0], dots[1], x);
        return dots[0] * weight;
    }


    /**
     * Gets a noise value at a specific 3D point
     *
     * @param x The X coordinate to get the noise at
     * @param y The Y coordinate to get the noise at
     * @param z The Z coordinate to get the noise at
     * @return A noise value between the weight and the negative weight
     */
    public double noise(double x, double y, double z) {
        x *= scale;
        y *= scale;
        z *= scale;
        int minX = (int) Math.floor(x);
        int minY = (int) Math.floor(y);
        int minZ = (int) Math.floor(z);
        x -= minX;
        y -= minY;
        z -= minZ;
        double[] dots = new double[8];
        dots[0] = getVector(minX, minY, minZ).dot(x, y, z);
        dots[1] = getVector(minX, minY, minZ + 1).dot(x, y, z - 1);
        dots[2] = getVector(minX, minY + 1, minZ).dot(x, y - 1, z);
        dots[3] = getVector(minX, minY + 1, minZ + 1).dot(x, y - 1, z - 1);
        dots[4] = getVector(minX + 1, minY, minZ).dot(x - 1, y, z);
        dots[5] = getVector(minX + 1, minY, minZ + 1).dot(x - 1, y, z - 1);
        dots[6] = getVector(minX + 1, minY + 1, minZ).dot(x - 1, y - 1, z);
        dots[7] = getVector(minX + 1, minY + 1, minZ + 1).dot(x - 1, y - 1, z - 1);

        dots[0] = smoothstep(dots[0], dots[1], z);
        dots[1] = smoothstep(dots[2], dots[3], z);
        dots[2] = smoothstep(dots[4], dots[5], z);
        dots[3] = smoothstep(dots[6], dots[7], z);

        dots[0] = smoothstep(dots[0], dots[1], y);
        dots[1] = smoothstep(dots[2], dots[3], y);

        dots[0] = smoothstep(dots[0], dots[1], x);
        return dots[0] * weight;
    }

    private static class SVec {

        private double x;
        private double y;
        private double z;

        public SVec(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double length() {
            return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        }

        public SVec normalize() {
            double length = length();
            x /= length;
            y /= length;
            z /= length;
            return this;
        }

        public double dot(SVec v) {
            return (x * v.x) + (y * v.y) + (z * v.z);
        }

        public double dot(double x, double z) {
            return (x * this.x) + (z * this.z);
        }

        public double dot(double x, double y, double z) {
            return (x * this.x) + (y * this.y) + (z * this.z);
        }

        public SVec clone() {
            return new SVec(x, y, z);
        }

        public String toString() {
            return x + ", " + y + ", " + z;
        }

    }

}
