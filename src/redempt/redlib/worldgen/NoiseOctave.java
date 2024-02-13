package redempt.redlib.worldgen;

import java.util.Random;

/**
 * Used to generate noise values for octaves of Perlin Noise
 *
 * @author Redempt
 */
public class NoiseOctave {

    private NoiseGenerator[] noise;
    private double weight = 0;

    /**
     * Creates a noise octave with the given seed and size. All noise generators within this octave
     * will be initialized with the seed plus their position inside this octave.
     *
     * @param seed The seed to use
     * @param size The number of noise generators to use
     */
    public NoiseOctave(long seed, int size) {
        noise = new NoiseGenerator[size];
        for (int i = 0; i < size; i++) {
            noise[i] = new NoiseGenerator(seed + i);
        }
        weight = size;
    }

    /**
     * Sets the seed of this noise octave.
     *
     * @param seed The seed to set
     */
    public void setSeed(long seed) {
        for (int i = 0; i < noise.length; i++) {
            noise[i].setSeed(seed + i);
        }
    }

    /**
     * Creates a noise octave with a random seed
     *
     * @param size The number of noise generators to use
     */
    public NoiseOctave(int size) {
        this(new Random().nextLong(), size);
    }

    /**
     * @return The size of this noise octave
     */
    public int size() {
        return noise.length;
    }

    /**
     * Sets the scale of every noise generator in this octave
     *
     * @param scales A vararg of scales to set. The length of this vararg must be the same as the size of this
     *               noise octave.
     */
    public void setScales(double... scales) {
        if (scales.length != noise.length) {
            throw new IllegalArgumentException("Number of scales passed is not equal to size of NoiseOctave");
        }
        for (int i = 0; i < noise.length; i++) {
            noise[i].setScale(scales[i]);
        }
    }

    /**
     * Sets the weight of every noise generator in this octave
     *
     * @param weights A vararg of weights to set. The length of this vararg must be the same as the size of this
     *                noise octave.
     */
    public void setWeights(double... weights) {
        if (weights.length != noise.length) {
            throw new IllegalArgumentException("Number of weights passed is not equal to size of NoiseOctave");
        }
        double total = 0;
        for (int i = 0; i < noise.length; i++) {
            noise[i].setWeight(weights[i]);
            total += weights[i];
        }
        weight = total;
    }

    /**
     * Pre-fills gradient vectors in the noise generators in this octave for the given region. Call this method
     * whenever you know a region you will be repeatedly requesting noise values from, as it significantly
     * increases performance.
     *
     * @param x      The X coordinate to generate starting at
     * @param z      The Z coordinate to generate starting at
     * @param width  The width along the X-axis to fill
     * @param length The length along the Z-axis to fill
     */
    public void prefill(int x, int z, int width, int length) {
        for (NoiseGenerator gen : noise) {
            gen.prefill(x, z, width, length);
        }
    }

    /**
     * Pre-fills gradient vectors in the noise generators in this octave for the given region. Call this method
     * whenever you know a region you will be repeatedly requesting noise values from, as it significantly
     * increases performance.
     *
     * @param x      The X coordinate to generate starting at
     * @param y      The Y coordinate to generate starting at
     * @param z      The Z coordinate to generate starting at
     * @param width  The width along the X-axis to fill
     * @param height The height along the Y-axis to fill
     * @param length The length along the Z-axis to fill
     */
    public void prefill(int x, int y, int z, int width, int height, int length) {
        for (NoiseGenerator gen : noise) {
            gen.prefill(x, y, z, width, height, length);
        }
    }

    /**
     * Gets the noise value at the given point
     *
     * @param x The X coordinate to get noise at
     * @param z The Z coordinate to get noise at
     * @return A noise value between -1 and 1
     */
    public double noise(double x, double z) {
        double sum = 0;
        for (NoiseGenerator gen : noise) {
            sum += gen.noise(x, z);
        }
        return sum / weight;
    }

    /**
     * Gets the noise value at the given point
     *
     * @param x The X coordinate to get noise at
     * @param y The Y coordinate to get noise at
     * @param z The Z coordinate to get noise at
     * @return A noise value between -1 and 1
     */
    public double noise(double x, double y, double z) {
        double sum = 0;
        for (NoiseGenerator gen : noise) {
            sum += gen.noise(x, y, z);
        }
        return sum / weight;
    }

}
