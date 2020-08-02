package redempt.redlib.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uses a map of outcomes to weights to get random values
 * @param <T> The type of the value
 * @author Redempt
 */
public class WeightedRandom<T> {
	
	private Map<T, Double> weights;
	private double total;
	private List<T> list = new ArrayList<>();
	
	/**
	 * Create a new WeightedRandom from a map of outcomes to their weights
	 * @param map The map of outcomes to their weights
	 * @param <T> The type of the outcomes
	 * @return A WeightedRandom which can be used to roll for the given outcome
	 */
	public static <T> WeightedRandom<T> fromIntMap(Map<T, Integer> map) {
		HashMap<T, Double> dmap = new HashMap<>();
		map.forEach((k, v) -> {
			dmap.put(k, (double) v);
		});
		return new WeightedRandom<T>(dmap, false);
	}
	
	/**
	 * Create a new WeightedRandom from a map of outcomes to their weights
	 * @param map The map of outcomes to their weights
	 * @param <T> The type of the outcomes
	 * @return A WeightedRandom which can be used to roll for the given outcome
	 */
	public static <T> WeightedRandom<T> fromDoubleMap(Map<T, Double> map) {
		return new WeightedRandom<T>(map, false);
	}
	
	/**
	 * Creates a WeightedRandom using the map of weights
	 * @param weights The map of outcomes to weights
	 * @deprecated Use {@link WeightedRandom#fromIntMap(Map)}
	 */
	public WeightedRandom(Map<T, Integer> weights) {
		this.weights = new HashMap<>();
		weights.forEach((k, v) -> {
			this.weights.put(k, (double) v);
			total += v;
			list.add(k);
		});
	}
	
	private WeightedRandom(Map<T, Double> weights, boolean no) {
		this.weights = weights;
		weights.forEach((k, v) -> {
			total += v;
			list.add(k);
		});
	}
	
	/**
	 * Rolls and gets a weighted random outcome
	 * @return A weighted random outcome, or null if there are no possible outcomes
	 */
	public T roll() {
		if (list.size() == 0) {
			return null;
		}
		double random = Math.random() * (total);
		int pos = 0;
		double roll = 0;
		while (random > (roll = weights.get(list.get(pos)))) {
			random -= roll;
			pos++;
		}
		return list.get(pos);
	}
	
	/**
	 * Gets the chance each outcome has to occur in percentage (0-100)
	 * @return A map of each outcome to its percentage chance to occur when calling {@link WeightedRandom#roll()}
	 */
	public Map<T, Double> getPercentages() {
		Map<T, Double> percentages = new HashMap<>();
		weights.forEach((k, v) -> {
			percentages.put(k, (((double) v) / (double) total) * 100d);
		});
		return percentages;
	}
	
	/**
	 * Gets the map of weights for this WeightedRandom
	 * @return The weight map
	 */
	public Map<T, Double> getWeights() {
		return weights;
	}
	
	/**
	 * Sets another weight in this WeightedRandom
	 * @param outcome The outcome to set
	 * @param weight The weight to set
	 */
	public void set(T outcome, int weight) {
		set(outcome, (double) weight);
	}
	
	public void set(T outcome, double weight) {
		remove(outcome);
		weights.put(outcome, weight);
		list.add(outcome);
		total += weight;
	}
	
	/**
	 * Removes an outcome from this WeightedRandom
	 * @param outcome The outcome to remove
	 */
	public void remove(T outcome) {
		Double weight = weights.remove(outcome);
		if (weight == null) {
			return;
		}
		total -= weight;
		list.remove(outcome);
	}
	
	/**
	 * Creates a copy of this WeightedRandom
	 * @return An identical copy of this WeightedRandom
	 */
	public WeightedRandom<T> clone() {
		return new WeightedRandom<T>(new HashMap<>(weights), false);
	}
	
	/**
	 * Performs a single roll given a map of outcomes to weights. If you need to roll multiple times, instantiate a WeightedRandom and call roll on that each time instead.
	 * @param map The map of outcomes to weights
	 * @param <T> The type being returned
	 * @return A weighted random outcome
	 */
	public static <T> T roll(Map<T, Integer> map) {
		return new WeightedRandom<T>(map).roll();
	}
	
}
