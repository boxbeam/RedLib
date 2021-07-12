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
	 */
	public static ArgType<Player> playerType = new ArgType<Player>("player", s -> Bukkit.getPlayerExact(s))
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
			}).setTab(c -> strings);
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
				.setTab(c -> list);
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
	
	private static <T, K> T convertCast(ArgConverter<T, K> convert, CommandSender sender, Object previous, String str) {
		return convert.convert(sender, (K) previous, str);
	}
	
	private static <T> List<String> tabCast(TabCompleter<T> tab, CommandSender sender, String[] args, Object prev) {
		return tab.tabComplete(sender, args, (T) prev);
	}
	
	private ArgConverter<T, ?> convert;
	private ArgType<?> parent;
	private TabCompleter<?> tab = null;
	private String name;
	
	protected ArgType(String name, ArgType<?> parent, ArgConverter<T, ?> convert) {
		if (name.contains(" ")) {
			throw new IllegalArgumentException("Command argument type name cannot contain a space");
		}
		this.convert = convert;
		this.name = name;
		this.parent = parent;
	}
	
	/**
	 * @return The parent type of this ArgType, or null
	 */
	public ArgType<?> getParent() {
		return parent;
	}
	
	/**
	 * Create a ArgType from a name and converter
	 * @param name The name of this command argument type, to be used in the command file
	 * @param convert The {@link Function} to convert from a String to whatever type this converts to
	 */
	public ArgType(String name, Function<String, T> convert) {
		this(name, (c, s) -> convert.apply(s));
	}
	
	/**
	 * Create a ArgType from a name and converter
	 * @param name The name of this command argument type, to be used in the command file
	 * @param convert The {@link BiFunction} to convert from a String to whatever type this converts to
	 */
	public ArgType(String name, BiFunction<CommandSender, String, T> convert) {
		this(name, null, (c, p, s) -> convert.apply(c, s));
	}
	
	/**
	 * Sets the tab completer for this type
	 * @param tab The function returning a List of all completions for this sender
	 * @return itself
	 */
	public ArgType<T> setTab(Function<CommandSender, List<String>> tab) {
		this.tab = (c, s, o) -> tab.apply(c);
		return this;
	}
	
	/**
	 * Sets the tab completer for this type
	 * @param tab The function returning a List of all completions for this sender
	 * @return itself
	 */
	public ArgType<T> setTab(BiFunction<CommandSender, String[], List<String>> tab) {
		this.tab = (c, s, o) -> tab.apply(c, s);
		return this;
	}
	
	protected ArgType<T> setTab(TabCompleter<?> tab) {
		this.tab = tab;
		return this;
	}
	
	/**
	 * Sets the tab completer for this type, can be used instead of tab
	 * @param tab The function returning a Stream of all completions for this sender
	 * @return itself
	 */
	public ArgType<T> tabStream(Function<CommandSender, Stream<String>> tab) {
		this.tab = (c, s, o) -> tab.apply(c).collect(Collectors.toList());
		return this;
	}
	
	/**
	 * Sets the tab completer for this type, can be used instead of tab
	 * @param tab The function returning a Stream of all completions for this sender
	 * @return itself
	 */
	public ArgType<T> tabStream(BiFunction<CommandSender, String[], Stream<String>> tab) {
		this.tab = (c, s, o) -> tab.apply(c, s).collect(Collectors.toList());
		return this;
	}
	
	protected List<String> tabComplete(CommandSender sender, String[] args, Object prev) {
		if (tab == null || prev == null && parent != null) {
			return new ArrayList<>();
		}
		List<String> values = tabCast(tab, sender, args, prev);
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
	 * @param previous The value of the previous argument
	 * @param argument The argument to be converted
	 * @return The converted argument for use in a method hook
	 */
	public T convert(CommandSender sender, Object previous, String argument) {
		return convertCast(convert, sender, previous, argument);
	}
	
	/**
	 * Creates a new ArgType based on this one which converts from this type to another
	 * @param <K> The type of the resulting ArgType
	 * @param name The name of the ArgType being created
	 * @param func The function to convert from the type this ArgType returns to the type the new one will
	 * @return The resulting ArgType
	 */
	public <K> ArgType<K> map(String name, Function<T, K> func) {
		return new ArgType<>(name, parent, (c, p, s) -> {
			T obj = convert(c, p, s);
			if (obj == null) {
				return null;
			}
			return func.apply(obj);
		}).setTab(tab);
	}
	
	/**
	 * Creates a new ArgType based on this one which converts from this type to another
	 * @param <K> The type of the resulting ArgType
	 * @param name The name of the ArgType being created
	 * @param func The function to convert from the type this ArgType returns to the type the new one will
	 * @return The resulting ArgType
	 */
	public <K> ArgType<K> map(String name, BiFunction<CommandSender, T, K> func) {
		return new ArgType<>(name, parent, (c, p, s) -> {
			T obj = convert(c, p, s);
			if (obj == null) {
				return null;
			}
			return func.apply(c, obj);
		}).setTab(tab);
	}
	
	/**
	 * Creates a new ArgSubtype with this ArgType as its parent. ArgSubtypes are argument types
	 * which must follow another argument type, and use info from the previous argument to determine
	 * their values for conversion and tab completion.
	 * @param name The name of the new ArgSubtype
	 * @param convert The function to convert using the previous argument value
	 * @param <K> The type the new ArgSubtype will convert to
	 * @return The created ArgSubtype
	 */
	public <K> ArgSubtype<K, T> subType(String name, BiFunction<String, T, K> convert) {
		return new ArgSubtype<>(name, this, (c, p, s) -> convert.apply(s, (T) p));
	}
	
	/**
	 * Creates a new ArgSubtype with this ArgType as its parent. ArgSubtypes are argument types
	 * which must follow another argument type, and use info from the previous argument to determine
	 * their values for conversion and tab completion.
	 * @param name The name of the new ArgSubtype
	 * @param convert The function to convert using the previous argument value
	 * @param <K> The type the new ArgSubtype will convert to
	 * @return The created ArgSubtype
	 */
	public <K> ArgSubtype<K, T> subType(String name, ArgConverter<K, T> convert) {
		return new ArgSubtype<>(name, this, convert);
	}
	
	public static interface ArgConverter<T, K> {
		
		public T convert(CommandSender sender, K previous, String str);
		
	}
	
	public static interface TabCompleter<T> {
		
		public List<String> tabComplete(CommandSender sender, String[] prev, T prevArg);
		
	}
	
	public static interface TabStreamCompleter<T> {
		
		public Stream<String> tabComplete(CommandSender sender, T prevArg, String[] prev);
		
	}
	
}