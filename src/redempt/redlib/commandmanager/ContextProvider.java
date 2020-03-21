package redempt.redlib.commandmanager;

import java.util.function.Function;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
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
				ItemStack item = ((Player) c).getItemInHand();
				if (item == null || item.getType() == Material.AIR) {
					return null;
				}
				return item.clone();
			});
	
	private String name;
	private String error = null;
	private Function<CommandSender, T> provider;
	
	/**
	 * Constructs a ContextProvider.
	 * If this constructor is used, the sender will be shown the help menu if the provider returns null
	 * @param name The name of this ContextProvider
	 * @param provider The function to get the needed context for the given sender
	 */
	public ContextProvider(String name, Function<CommandSender, T> provider) {
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
	public ContextProvider(String name, String error, Function<CommandSender, T> provider) {
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
	
	protected T provide(CommandSender sender) {
		return provider.apply(sender);
	}
	
}