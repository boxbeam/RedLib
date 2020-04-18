package redempt.redlib.commandmanager;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Used to provide context arguments to command method hooks
 * @author Redempt
 *
 * @param <T>
 */
public class ContextProvider<T> {
	
	/**
	 * Use "mainhand" in the command file. Assumes the sender is a player.
	 * Returns the item in the player's main hand, or errors if it is air.
	 */
	public static ContextProvider<ItemStack> mainHand = new ContextProvider<ItemStack>("mainhand",
			ChatColor.RED + "You must be holding an item to do this!",
			c -> {
				@SuppressWarnings("deprecation")
				ItemStack item = c.getItemInHand();
				if (item == null || item.getType() == Material.AIR) {
					return null;
				}
				return item.clone();
			});
	
	/**
	 * Creates a ContextProvider which returns true if the predicate's condition is met, and null otherwise, which will cause the command to fail
	 * @param name The name of the ContextProvider to be created
	 * @param error The error message to be shown to the user if the predicate returns false
	 * @param assertion The predicate which tests the assertion
	 * @return A ContextProvider which asserts that the given condition is met, and returns false otherwise
	 */
	public static ContextProvider<Boolean> assertProvider(String name, String error, Predicate<Player> assertion) {
		return new ContextProvider<Boolean>(name, error, c -> {
			return assertion.test(c) ? true : null;
		});
	}
	
	/**
	 * Creates a ContextProvider which returns true if the predicate's condition is met, and null otherwise, which will cause the command to fail
	 * @param name The name of the ContextProvider to be created
	 * @param assertion The predicate which tests the assertion
	 * @return A ContextProvider which asserts that the given condition is met, and returns false otherwise
	 */
	public static ContextProvider<Boolean> assertProvider(String name, Predicate<Player> assertion) {
		return assertProvider(name, null, assertion);
	}
	
	private String name;
	private String error = null;
	private Function<Player, T> provider;
	
	/**
	 * Constructs a ContextProvider.
	 * If this constructor is used, the sender will be shown the help menu if the provider returns null
	 * @param name The name of this ContextProvider
	 * @param provider The function to get the needed context for the given sender
	 */
	public ContextProvider(String name, Function<Player, T> provider) {
		if (name.contains(" ")) {
			throw new IllegalArgumentException("Context provider name cannot contain a space");
		}
		this.name = name;
		this.provider = provider;
	}
	
	/**
	 * Constructs a ContextProvider.
	 * If this constructor is used, the sender will be shown the given error message if the provider returns null
	 * @param name The name of this ContextProvider
	 * @param error The error message to be shown to the user if the provider returns null
	 * @param provider The function to get the needed context for the given sender
	 */
	public ContextProvider(String name, String error, Function<Player, T> provider) {
		this(name, provider);
		this.error = error;
	}
	
	/**
	 * @return The name of this sender
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return The error message shown to a user if a command using this ContextProvider is run and this ContextProvider returns null
	 */
	public String getErrorMessage() {
		return error;
	}
	
	/**
	 * Creates a new ContextProvider based on this one which converts from this type to another
	 * @param <K> The type of the resulting ContextProvider
	 * @param name The name of the ContextProvider being created
	 * @param error The error message to be shown to the user if the provider returns null
	 * @param func The function to convert from the type this ContextProvider returns to the type the new one will
	 * @return The resulting ContextProvider
	 */
	public <K> ContextProvider<K> map(String name, String error, Function<T, K> func) {
		return new ContextProvider<K>(name, error, c -> {
			T obj = provide(c);
			if (obj == null) {
				return null;
			}
			return func.apply(obj);
		});
	}
	
	/**
	 * Creates a new ContextProvider based on this one which converts from this type to another
	 * @param <K> The type of the resulting ContextProvider
	 * @param name The name of the ContextProvider being created
	 * @param error The error message to be shown to the user if the provider returns null
	 * @param func The function to convert from the type this ContextProvider returns to the type the new one will
	 * @return The resulting ContextProvider
	 */
	public <K> ContextProvider<K> map(String name, String error, BiFunction<Player, T, K> func) {
		return new ContextProvider<K>(name, error, c -> {
			T obj = provide(c);
			if (obj == null) {
				return null;
			}
			return func.apply(c, obj);
		});
	}
	
	/**
	 * Creates a new ContextProvider based on this one which converts from this type to another
	 * @param <K> The type of the resulting ContextProvider
	 * @param name The name of the ContextProvider being created
	 * @param func The function to convert from the type this ContextProvider returns to the type the new one will
	 * @return The resulting ContextProvider
	 */
	public <K> ContextProvider<K> map(String name, Function<T, K> func) {
		return map(name, null, func);
	}
	
	/**
	 * Creates a new ContextProvider based on this one which converts from this type to another
	 * @param <K> The type of the resulting ContextProvider
	 * @param name The name of the ContextProvider being created
	 * @param func The function to convert from the type this ContextProvider returns to the type the new one will
	 * @return The resulting ContextProvider
	 */
	public <K> ContextProvider<K> map(String name, BiFunction<Player, T, K> func) {
		return map(name, null, func);
	}
	
	protected T provide(Player sender) {
		return provider.apply(sender);
	}
	
}