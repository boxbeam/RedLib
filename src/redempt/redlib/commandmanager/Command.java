package redempt.redlib.commandmanager;

import static redempt.redlib.commandmanager.Messages.msg;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;

import redempt.redlib.commandmanager.exceptions.CommandParseException;
import redempt.redlib.commandmanager.exceptions.MissingHookException;

/**
 * Represents a command which can be registered
 * @author Redempt
 */
public class Command {
	
	private static List<CommandArgumentType<?>> types = new ArrayList<>();
	protected List<Command> children = new ArrayList<>();
	
	static {
		types.add(new CommandArgumentType<>("int", (Function<String, Integer>) Integer::parseInt));
		types.add(new CommandArgumentType<>("double", Double::parseDouble));
		types.add(new CommandArgumentType<>("float", Float::parseFloat));
		types.add(new CommandArgumentType<>("long", (Function<String, Long>) Long::parseLong));
		types.add(new CommandArgumentType<>("string", s -> s));
		types.add(new CommandArgumentType<>("boolean", s -> {
			switch (s.toLowerCase()) {
				case "true":
					return true;
				case "false":
					return false;
				default:
					return null;
			}
		}));
	}
	
	private CommandArgument[] args;
	private ContextProvider<?>[] contextProviders;
	private String[] names;
	private String permission;
	private SenderType type;
	protected String hook;
	private Method methodHook;
	private String help;
	private Object listener;
	protected boolean topLevel = false;
	protected Command parent = null;
	private boolean hideSub = false;
	
	protected Command(String[] names, CommandArgument[] args, ContextProvider<?>[] providers, String help, String permission, SenderType type, String hook, List<Command> children, boolean hideSub) {
		this.names = names;
		this.args = args;
		this.contextProviders = providers;
		this.permission = permission;
		this.type = type;
		this.hook = hook;
		this.help = help;
		this.children = children;
		this.hideSub = hideSub;
		for (Command command : children) {
			command.parent = this;
		}
	}
	
	/**
	 * Shows the help to a CommandSender
	 * @param sender The sender to show the help to
	 */
	public void showHelp(CommandSender sender) {
		String title = msg("helpTitle").replace("%cmdname%", names[0]);
		sender.sendMessage(title);
		sender.sendMessage(getHelpRecursive(sender, 0).trim());
	}
	
	private String getHelpRecursive(CommandSender sender, int level) {
		if (permission != null && !sender.hasPermission(permission)) {
			return "";
		}
		StringBuilder help = new StringBuilder();
		help.append(this.help == null ? "" : msg("helpEntry").replace("%cmdname%", getFullName()).replace("%help%", this.help) + "\n");
		if (hideSub && level != 0) {
			if (help.length() == 0) {
				return msg("helpEntry").replace("%cmdname%", getFullName()).replace("%help%", "[Hidden subcommands]") + "\n";
			}
			return help.toString();
		}
		for (Command command : children) {
			help.append(command.getHelpRecursive(sender, level + 1));
		}
		return help.toString();
	}
	
	/**
	 * @return The expanded name of the command, plus arguments
	 */
	public String getFullName() {
		String name = getExpandedName() + " ";
		name += String.join(" ", Arrays.stream(args).map(CommandArgument::toString).collect(Collectors.toList()));
		return name;
	}
	
	/**
	 * @return The name of the command concatenated with its parents' names
	 */
	public String getExpandedName() {
		String name = names[0];
		if (parent != null) {
			name = parent.getExpandedName() + " " + name;
			return name;
		}
		return "/" + name;
	}
	
	private static String[] parseArgs(String input) {
		List<String> args = new ArrayList<>();
		StringBuilder combine = new StringBuilder();
		boolean quotes = false;
		char[] chars = input.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (c == '\\' && i + 1 < chars.length) {
				combine.append(chars[i + 1]);
				i++;
				continue;
			}
			if (c == '"') {
				quotes = !quotes;
				continue;
			}
			if (c == ' ' && !quotes) {
				args.add(combine.toString());
				combine = new StringBuilder();
				continue;
			}
			combine.append(c);
		}
		if (combine.length() > 0) {
			args.add(combine.toString());
		}
		return args.toArray(new String[args.size()]);
	}
	
	private Object[] processArgs(String[] sargs, CommandSender sender) {
		List<CommandArgument> cmdArgs = Arrays.stream(args).collect(Collectors.toList());
		if (cmdArgs.size() > sargs.length) {
			int diff = cmdArgs.size() - sargs.length;
			int optional = (int) cmdArgs.stream().filter(CommandArgument::isOptional).count();
			CommandArgument[] cargs = args.clone();
			for (int i = 0; i < cargs.length; i++) {
				if (cargs[i].isOptional()) {
					cargs[i] = null;
				}
			}
			List<CommandArgument> used = new ArrayList<>();
			if (optional >= diff) {
				for (int i = 0; i < sargs.length && diff > 0; i++) {
					String argString = sargs[i];
					List<CommandArgument> optionals = new ArrayList<>();
					if (!cmdArgs.get(i).isOptional()) {
						continue;
					}
					optionals.add(cmdArgs.get(i));
					for (int j = i - 1; j >= 0; j--) {
						CommandArgument arg = cmdArgs.get(j);
						if (arg.isOptional()) {
							optionals.add(arg);
						} else {
							break;
						}
					}
					for (int j = i + 1; j < cmdArgs.size(); j++) {
						CommandArgument arg = cmdArgs.get(j);
						if (arg.isOptional()) {
							optionals.add(arg);
						} else {
							break;
						}
					}
					optionals.removeIf(arg -> {
						try {
							return arg.getType().convert(sender, argString) == null;
						} catch (Exception e) {
							return true;
						}
					});
					if (optionals.size() > 1 && !optionals.stream().allMatch(arg -> arg.getType().getName().equals("string"))) {
						optionals.removeIf(arg -> arg.getType().getName().equals("string"));
					}
					optionals.removeAll(used);
					if (optionals.size() == 0) {
						continue;
					}
					CommandArgument chosen = optionals.get(0);
					used.add(chosen);
					cargs[i] = chosen;
					diff--;
				}
			}
			cmdArgs = Arrays.stream(cargs).filter(arg -> arg != null).collect(Collectors.toList());
		}
		if (cmdArgs.size() != sargs.length && (args.length == 0 || !args[args.length - 1].consumes())) {
			return null;
		}
		Object[] output = new Object[args.length + 1];
		output[0] = sender;
		for (CommandArgument arg : cmdArgs) {
			if (arg.consumes()) {
				if (arg.pos != args.length - 1) {
					throw new IllegalArgumentException("Consuming argument must be the last argument!");
				}
				StringBuilder builder = new StringBuilder();
				for (int x = cmdArgs.size() - 1; x < sargs.length; x++) {
					builder.append(sargs[x]).append(" ");
				}
				String combine = builder.toString();
				try {
					combine = combine.substring(0, combine.length() - 1);
					output[arg.getPosition() + 1] = arg.getType().convert(sender, combine);
				} catch (Exception e) {
					return null;
				}
				return output;
			}
			try {
				output[arg.getPosition() + 1] = Objects.requireNonNull(arg.getType().convert(sender, sargs[cmdArgs.indexOf(arg)]));
			} catch (Exception e) {
				return null;
			}
		}
		for (CommandArgument arg : args) {
			if (arg.isOptional() && output[arg.getPosition() + 1] == null) {
				output[arg.getPosition() + 1] = arg.getDefaultValue(sender);
			}
		}
		return output;
	}
	
	private Object[] getContext(CommandSender sender) {
		Object[] output = new Object[contextProviders.length];
		for (int i = 0; i < output.length; i++) {
			Object obj = null;
			if (sender instanceof Player) {
				obj = contextProviders[i].provide((Player) sender);
			}
			if (obj == null) {
				String error = contextProviders[i].getErrorMessage();
				if (error != null) {
					sender.sendMessage(error);
				} else {
					showHelp(sender);
				}
				return null;
			}
			output[i] = obj;
		}
		return output;
	}
	
	/**
	 * Registers this command and its children
	 * @param prefix The fallback prefix
	 * @param listeners The listener objects containing method hooks
	 */
	public void register(String prefix, Object... listeners) {
		Field field;
		try {
			field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			field.setAccessible(true);
			SimpleCommandMap map = (SimpleCommandMap) field.get(Bukkit.getServer());
			org.bukkit.command.Command cmd = new org.bukkit.command.Command(names[0], help == null ? "None" : help, "", Arrays.stream(names).skip(1).collect(Collectors.toList())) {

				@Override
				public boolean execute(CommandSender sender, String name, String[] args) {
					Command.this.execute(sender, args);
					return true;
				}
				
				@Override
				public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
					return tab(sender, args);
				}
				
			};
			map.register(prefix, cmd);
			loop:
			for (Object listener : listeners) {
				for (Method method : listener.getClass().getDeclaredMethods()) {
					CommandHook cmdHook = method.getAnnotation(CommandHook.class);
					if (cmdHook != null) {
						if (cmdHook.value().equals(hook)) {
							methodHook = method;
							this.listener = listener;
							Class<?>[] params = method.getParameterTypes();
							int expectedLength = args.length + contextProviders.length + 1;
							if (params.length != expectedLength) {
								throw new IllegalStateException("Incorrect number of arguments for method hook! [" + method.getDeclaringClass().getName() + "." + method.getName() + "] "
										+ "Argument count should be " + expectedLength + ", got " + params.length);
							}
							if (!CommandSender.class.isAssignableFrom(params[0])) {
								throw new IllegalStateException("The first argument must be CommandSender or one of its subclasses! [" + method.getDeclaringClass().getName() + "." + method.getName() + "]");
							}
							break loop;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (Command child : children) {
			child.registerHook(listeners);
		}
	}
	
	private void registerHook(Object... listeners) {
		loop:
		for (Object listener : listeners) {
			for (Method method : listener.getClass().getDeclaredMethods()) {
				if (method.isAnnotationPresent(CommandHook.class)) {
					CommandHook cmdHook = method.getAnnotation(CommandHook.class);
					if (cmdHook.value().equals(hook)) {
						methodHook = method;
						Class<?>[] params = method.getParameterTypes();
						int expectedLength = args.length + contextProviders.length + 1;
						if (params.length != expectedLength) {
							throw new IllegalStateException("Incorrect number of arguments for method hook! [" + method.getDeclaringClass().getName() + "." + method.getName() + "] "
									+ "Argument count should be " + expectedLength + ", got " + params.length);
						}
						this.listener = listener;
						if (!CommandSender.class.isAssignableFrom(params[0])) {
							throw new IllegalStateException("The first argument must be CommandSender or one of its subclasses! [" + method.getDeclaringClass().getName() + "." + method.getName() + "]");
						}
						break loop;
					}
				}
			}
		}
		if (hook != null && methodHook == null) {
			throw new MissingHookException("Command with hook name " + hook + " has no method hook");
		}
		for (Command child : children) {
			child.registerHook(listeners);
		}
	}
	
	private List<String> tab(CommandSender sender, String[] args) {
		List<String> completions = new ArrayList<>();
		if (args.length > 0) {
			for (Command child : children) {
				if (child.getPermission() != null && !sender.hasPermission(child.getPermission())) {
					continue;
				}
				for (String name : child.getAliases()) {
					if (name.equalsIgnoreCase(args[0])) {
						String[] copy = Arrays.copyOfRange(args, 1, args.length);
						completions.addAll(child.tab(sender, copy));
					}
				}
			}
		}
		if (args.length == 1) {
			for (Command child : children) {
				if (child.getPermission() != null && !sender.hasPermission(child.getPermission())) {
					continue;
				}
				if (child.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
					completions.add(child.getName());
				}
			}
		}
		if (args.length - 1 < this.args.length && args.length > 0) {
			String partial = args[args.length - 1].replaceAll("(^\")|(\"$)", "");
			CommandArgument arg = this.args[args.length - 1];
			List<String> argCompletions = arg.getType().tabComplete(sender);
			for (String completion : argCompletions) {
				if (completion.toLowerCase().startsWith(partial.toLowerCase()) && !partial.equals(completion)) {
					if (completion.contains(" ")) {
						completion = '"' + completion + '"';
					}
					completions.add(completion);
				}
			}
		}
		return completions;
	}
	
	private boolean execute(CommandSender sender, String[] args) {
		if (permission != null && !sender.hasPermission(permission)) {
			sender.sendMessage(msg("noPermission").replace("%permission%", permission));
			return true;
		}
		if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
			showHelp(sender);
			return true;
		}
		if (methodHook != null) {
			type = type == null ? SenderType.EVERYONE : type;
			switch (type) {
				case EVERYONE:
					break;
				case CONSOLE:
					if (sender instanceof Player) {
						sender.sendMessage(ChatColor.RED + "This command can only be executed from console!");
						return true;
					}
					break;
				case PLAYER:
					if (!(sender instanceof Player)) {
						sender.sendMessage(ChatColor.RED + "This command can only be executed as a player!");
						return true;
					}
					break;
			}
			Object[] objArgs = processArgs(parseArgs(String.join(" ", args)), sender);
			if (objArgs != null) {
				int size = objArgs.length + contextProviders.length;
				Object[] arr = new Object[size];
				System.arraycopy(objArgs, 0, arr, 0, objArgs.length);
				if (contextProviders.length > 0) {
					Object[] context = getContext(sender);
					if (context == null) {
						return true;
					}
					System.arraycopy(context, 0, arr, objArgs.length, context.length);
				}
				try {
					methodHook.invoke(listener, arr);
					return true;
				} catch (IllegalAccessException | InvocationTargetException e) {
					e.printStackTrace();
					sender.sendMessage(ChatColor.RED + "An error was encountered in running this command. Please notify an admin.");
					return true;
				} catch (IllegalArgumentException e) {
					if (topLevel) {
						showHelp(sender);
						return true;
					}
				}
			}
		}
		if (args.length == 0) {
			if (topLevel) {
				showHelp(sender);
				return true;
			}
			return false;
		}
		String[] truncArgs = Arrays.copyOfRange(args, 1, args.length);
		for (Command command : children) {
			for (String alias : command.getAliases()) {
				if (alias.equals(args[0])) {
					if (command.execute(sender, truncArgs)) {
						return true;
					}
				}
			}
		}
		if (parent != null) {
			for (Command command : parent.children) {
				if (!command.equals(this) && command.getName().equals(this.getName())) {
					return false;
				}
			}
		}
		showHelp(sender);
		return true;
	}
	
	protected static CommandArgumentType<?> getType(String name, CommandArgumentType<?>[] types) {
		for (CommandArgumentType<?> type : Command.types) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		for (CommandArgumentType<?> type : types) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		return null;
	}
	
	/**
	 * @return The command's primary name/first alias
	 */
	public String getName() {
		return names[0];
	}
	
	/**
	 * @return All of the command's names/aliases
	 */
	public String[] getAliases() {
		return names;
	}
	
	/**
	 * @return The command's help message
	 */
	public String getHelp() {
		return help;
	}
	
	/**
	 * @return Nullable. The permission required to run the command
	 */
	public String getPermission() {
		return permission;
	}
	
	/**
	 * Loads commands from a command file in stream form. Use {@link org.bukkit.plugin.java.JavaPlugin#getResource} for this
	 * @param stream The InputStream to load commands from
	 * @param types Custom argument types
	 * @return The commands loaded from the stream
	 * @throws CommandParseException if the command file cannot be parsed
	 * @deprecated Outdated. Use {@link CommandParser#parse()}
	 */
	public static CommandCollection fromStream(InputStream stream, CommandArgumentType<?>... types) {
		return new CommandParser(stream).setArgTypes(types).parse();
	}
	
	/**
	 * A command argument type, which converts a String argument to another type
	 * @author Redempt
	 * @param <T> The type this CommandArgumentType converts to
	 */
	public static class CommandArgumentType<T> {
		
		/**
		 * The CommandArgumentType for a Player
		 */
		public static CommandArgumentType<Player> playerType = new CommandArgumentType<Player>("player", s -> Bukkit.getPlayer(s))
				.tabStream(c -> Bukkit.getOnlinePlayers().stream().map(Player::getName));
		
		/**
		 * Creates a CommandArgumentType for an enum, which will accept all of the enum's values as arguments and offer all enum values as tab completions
		 * @param <T> The enum type
		 * @param name The name of the CommandArgumentType
		 * @param clazz The enum class to make a CommandArgumentType from
		 * @return A CommandArgumentType for the given enum
		 */
		public static <T extends Enum> CommandArgumentType<T> of(String name, Class<T> clazz) {
			if (!clazz.isEnum()) {
				throw new IllegalArgumentException("Class must be an enum type!");
			}
			try {
				Method getValues = clazz.getDeclaredMethod("values");
				Object[] values = (Object[]) getValues.invoke(null);
				List<String> strings = Arrays.stream(values).map(Object::toString).collect(Collectors.toList());
				return new CommandArgumentType<T>(name, s -> {
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
		 * Creates a CommandArgumentType for a set of possible string inputs
		 * @param name The name of the CommandArgumentType
		 * @param values The list of possible inputs
		 * @return A CommandArgumentType for the given inputs, which will offer tab completion and accept any of the supplied strings, or return null if the given argument does not match any of them
		 */
		public static CommandArgumentType<String> of(String name, String... values) {
			List<String> list = Arrays.stream(values).collect(Collectors.toList());
			return new CommandArgumentType<>(name, s -> list.contains(s) ? s : null)
					.tab(c -> list);
		}
		
		private Function<String, T> func = null;
		private BiFunction<CommandSender, String, T> bifunc = null;
		private String name;
		private Function<CommandSender, List<String>> tab = null;
		
		/**
		 * Had to make this into a single constructor that takes an Object for Maven reasons
		 * @param name The name of this command argument type, to be used in the command file
		 * @param convert The {@link Function} to convert from a String to whatever type this converts to
		 */
		public CommandArgumentType(String name, Function<String, T> convert) {
			if (name.contains(" ")) {
				throw new IllegalArgumentException("Command argument type name cannot contain a space");
			}
			func = convert;
			this.name = name;
		}
		
		/**
		 * Had to make this into a single constructor that takes an Object for Maven reasons
		 * @param name The name of this command argument type, to be used in the command file
		 * @param convert The {@link BiFunction} to convert from a String to whatever type this converts to
		 */
		public CommandArgumentType(String name, BiFunction<CommandSender, String, T> convert) {
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
		public CommandArgumentType<T> tab(Function<CommandSender, List<String>> tab) {
			this.tab = tab;
			return this;
		}
		
		/**
		 * Sets the tab completer for this type, can be used instead of tab
		 * @param tab The function returning a Stream of all completions for this sender
		 * @return itself
		 */
		public CommandArgumentType<T> tabStream(Function<CommandSender, Stream<String>> tab) {
			this.tab = c -> tab.apply(c).collect(Collectors.toList());
			return this;
		}
		
		private List<String> tabComplete(CommandSender sender) {
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
		 * Creates a new CommandArgumentType based on this one which converts from this type to another
		 * @param <K> The type of the resulting CommandArgumentType
		 * @param name The name of the CommandArgumentType being created
		 * @param func The function to convert from the type this CommandArgumentType returns to the type the new one will
		 * @return The resulting CommandArgumentType
		 */
		public <K> CommandArgumentType<K> map(String name, Function<T, K> func) {
			return new CommandArgumentType<>(name, (c, s) -> {
				T obj = convert(c, s);
				if (obj == null) {
					return null;
				}
				return func.apply(obj);
			});
		}
		
		/**
		 * Creates a new CommandArgumentType based on this one which converts from this type to another
		 * @param <K> The type of the resulting CommandArgumentType
		 * @param name The name of the CommandArgumentType being created
		 * @param func The function to convert from the type this CommandArgumentType returns to the type the new one will
		 * @return The resulting CommandArgumentType
		 */
		public <K> CommandArgumentType<K> map(String name, BiFunction<CommandSender, T, K> func) {
			return new CommandArgumentType<>(name, (c, s) -> {
				T obj = convert(c, s);
				if (obj == null) {
					return null;
				}
				return func.apply(c, obj);
			});
		}
		
	}
	
	public static enum SenderType {
		
		CONSOLE,
		PLAYER,
		EVERYONE;
		
	}
	
}
