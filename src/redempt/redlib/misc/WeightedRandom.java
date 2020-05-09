package redempt.redlib.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Uses a map of outcomes to weights to get random values
 * @param <T> The type of the value
 * @author Redempt
 */
public class WeightedRandom<T> {
	
	private Map<T, Integer> weights;
	private int total;
	private List<T> list = new ArrayList<>();
	
	/**
	 * Creates a WeightedRandom using the map of weights
	 * @param weights The map of outcomes to weights
	 */
	public WeightedRandom(Map<T, Integer> weights) {
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
		int random = (int) Math.round(Math.random() * (total - 1));
		int pos = 0;
		int roll = 0;
		while (random >= (roll = weights.get(list.get(pos)))) {
			random -= roll;
			pos++;
		}
		return list.get(pos);
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
