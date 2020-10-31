package redempt.redlib.commandmanager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A command argument type, which converts a String argument to another type
 * @author Redempt
 * @param <T> The type this ArgType converts to
 */
public class ArgType<T> {
	
	/**
	 * The ArgType for a Player
	 * @deprecated Now included by default, there is no need to add it yourself
	 */
	public static ArgType<Player> playerType = new ArgType<Player>("player", s -> Bukkit.getPlayer(s))
			.tabStream(c -> Bukkit.getOnlinePlayers().stream().map(Player::getName));
	
	/**
	 * Creates a ArgType for an enum, which will accept all of the enum's values as arguments and offer all enum values as tab completions
	 * @param <T> The enum type
	 * @param name The name of the ArgType
	 * @param clazz The enum class to make a ArgType from
	 * @return A ArgType for the given enum
	 */
	public static <T extends Enum> ArgType<T> of(String name, Class<T> clazz) {
		if (!clazz.isEnum()) {
			throw new IllegalArgumentException("Class must be an enum type!");
		}
		try {
			Method getValues = clazz.getDeclaredMethod("values");
			Object[] values = (Object[]) getValues.invoke(null);
			List<String> strings = Arrays.stream(values).map(Object::toString).collect(Collectors.toList());
			return new ArgType<T>(name, s -> {
				try {
					return (T) Enum.valueOf(clazz, s);
				} catch (Exception e) {
					return null;
				}
			}).tab(c -> strings);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Creates a ArgType for a set of possible string inputs
	 * @param name The name of the ArgType
	 * @param values The list of possible inputs
	 * @return A ArgType for the given inputs, which will offer tab completion and accept any of the supplied strings, or return null if the given argument does not match any of them
	 */
	public static ArgType<String> of(String name, String... values) {
		List<String> list = Arrays.stream(values).collect(Collectors.toList());
		return new ArgType<>(name, s -> list.contains(s) ? s : null)
				.tab(c -> list);
	}
	
	/**
	 * Creates an ArgType for a map of a String to another type
	 * @param name The name of the ArgType
	 * @param map The map from String to the type this ArgType will provide
	 * @param <T> The type this ArgType will provide
	 * @return The constructed ArgType
	 */
	public static <T> ArgType<T> of(String name, Map<String, T> map) {
		return new ArgType<>(name, map::get).tabStream(c -> map.keySet().stream());
	}
	
	private Function<String, T> func = null;
	private BiFunction<CommandSender, String, T> bifunc = null;
	private String name;
	private Function<CommandSender, List<String>> tab = null;
	
	/**
	 * Create a ArgType from a name and converter
	 * @param name The name of this command argument type, to be used in the command file
	 * @param convert The {@link Function} to convert from a String to whatever type this converts to
	 */
	public ArgType(String name, Function<String, T> convert) {
		if (name.contains(" ")) {
			throw new IllegalArgumentException("Command argument type name cannot contain a space");
		}
		func = convert;
		this.name = name;
	}
	
	/**
	 * Create a ArgType from a name and converter
	 * @param name The name of this command argument type, to be used in the command file
	 * @param convert The {@link BiFunction} to convert from a String to whatever type this converts to
	 */
	public ArgType(String name, BiFunction<CommandSender, String, T> convert) {
		if (name.contains(" ")) {
			throw new IllegalArgumentException("Command argument type name cannot contain a space");
		}
		bifunc = convert;
		this.name = name;
	}
	
	/**
	 * Sets the tab completer for this type
	 * @param tab The function returning a List of all completions for this sender
	 * @return itself
	 */
	public ArgType<T> tab(Function<CommandSender, List<String>> tab) {
		this.tab = tab;
		return this;
	}
	
	/**
	 * Sets the tab completer for this type, can be used instead of tab
	 * @param tab The function returning a Stream of all completions for this sender
	 * @return itself
	 */
	public ArgType<T> tabStream(Function<CommandSender, Stream<String>> tab) {
		this.tab = c -> tab.apply(c).collect(Collectors.toList());
		return this;
	}
	
	protected List<String> tabComplete(CommandSender sender) {
		if (tab == null) {
			return new ArrayList<>();
		}
		List<String> values = tab.apply(sender);
		if (values == null) {
			return new ArrayList<>();
		}
		return values;
	}
	
	/**
	 * @return The name of this argument type
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Converts an argument to another type
	 * @param sender The sender of the command
	 * @param argument The argument to be converted
	 * @return The converted argument for use in a method hook
	 */
	public T convert(CommandSender sender, String argument) {
		return func == null ? bifunc.apply(sender, argument) : func.apply(argument);
	}
	
	/**
	 * Creates a new ArgType based on this one which converts from this type to another
	 * @param <K> The type of the resulting ArgType
	 * @param name The name of the ArgType being created
	 * @param func The function to convert from the type this ArgType returns to the type the new one will
	 * @return The resulting ArgType
	 */
	public <K> ArgType<K> map(String name, Function<T, K> func) {
		return new ArgType<>(name, (c, s) -> {
			T obj = convert(c, s);
			if (obj == null) {
				return null;
			}
			return func.apply(obj);
		});
	}
	
	/**
	 * Creates a new ArgType based on this one which converts from this type to another
	 * @param <K> The type of the resulting ArgType
	 * @param name The name of the ArgType being created
	 * @param func The function to convert from the type this ArgType returns to the type the new one will
	 * @return The resulting ArgType
	 */
	public <K> ArgType<K> map(String name, BiFunction<CommandSender, T, K> func) {
		return new ArgType<>(name, (c, s) -> {
			T obj = convert(c, s);
			if (obj == null) {
				return null;
			}
			return func.apply(c, obj);
		});
	}
	
}