package redempt.redlib.commandmanager;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.bukkit.event.Listener;

import redempt.redlib.RedLib;

/**
 * Represents a command which can be registered
 * @author Redempt
 */
public class Command implements Listener {
	
	private static List<CommandArgumentType<?>> types = new ArrayList<>();
	protected List<Command> children = new ArrayList<>();
	
	static {
		types.add(new CommandArgumentType<Integer>("int", (Function<String, Integer>) Integer::parseInt));
		types.add(new CommandArgumentType<Double>("double", Double::parseDouble));
		types.add(new CommandArgumentType<Float>("float", Float::parseFloat));
		types.add(new CommandArgumentType<Long>("long", (Function<String, Long>) Long::parseLong));
		types.add(new CommandArgumentType<String>("string", s -> s));
	}
	
	private CommandArgument[] args;
	private String[] names;
	private String permission;
	private SenderType type;
	protected String hook;
	private Method methodHook;
	private String help;
	private Object listener;
	private boolean topLevel = false;
	protected Command parent = null;
	private boolean hideSub = false;
	
	protected Command(String[] names, CommandArgument[] args, String help, String permission, SenderType type, String hook, List<Command> children, boolean hideSub) {
		this.names = names;
		this.args = args;
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
		String title = ChatColor.translateAlternateColorCodes('&', RedLib.helpTitle).replace("%cmdname%", names[0]);
		sender.sendMessage(title);
		sender.sendMessage(getHelpRecursive(sender, 0).trim());
	}
	
	private String getHelpRecursive(CommandSender sender, int level) {
		if (permission != null && !sender.hasPermission(permission)) {
			return "";
		}
		String help = this.help == null ? "" : ChatColor.translateAlternateColorCodes('&', RedLib.helpEntry).replace("%cmdname%", getFullName()).replace("%help%", this.help) + "\n";
		if (hideSub && level != 0) {
			if (help.equals("")) {
				return ChatColor.translateAlternateColorCodes('&', RedLib.helpEntry).replace("%cmdname%", getFullName()).replace("%help%", "[Hidden subcommands]") + "\n";
			}
			return help;
		}
		for (Command command : children) {
			help += command.getHelpRecursive(sender, level + 1);
		}
		return help;
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
		}
		return name;
	}
	
	private static String[] parseArgs(String input) {
		List<String> args = new ArrayList<>();
		String combine = "";
		boolean quotes = false;
		char[] chars = input.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (c == '\\' && i + 1 < chars.length) {
				combine += chars[i + 1];
				i++;
				continue;
			}
			if (c == '"') {
				quotes = !quotes;
				continue;
			}
			if (c == ' ' && !quotes) {
				args.add(combine);
				combine = "";
				continue;
			}
			combine += c;
		}
		if (!combine.equals("")) {
			args.add(combine);
		}
		return args.toArray(new String[args.size()]);
	}
	
	private static Object[] processArgs(String[] args, CommandArgument[] c, CommandSender sender) {
		List<CommandArgument> cmdArgs = Arrays.stream(c).collect(Collectors.toList());
		if (cmdArgs.size() > args.length) {
			int diff = cmdArgs.size() - args.length;
			int optional = (int) cmdArgs.stream().filter(CommandArgument::isOptional).count();
			CommandArgument[] cargs = c.clone();
			for (int i = 0; i < cargs.length; i++) {
				if (cargs[i].isOptional()) {
					cargs[i] = null;
				}
			}
			List<CommandArgument> used = new ArrayList<>();
			if (optional >= diff) {
				for (int i = 0; i < args.length && diff > 0; i++) {
					String argString = args[i];
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
		if (cmdArgs.size() != args.length && (c.length == 0 || !c[c.length - 1].consumes())) {
			return null;
		}
		Object[] output = new Object[c.length + 1];
		output[0] = sender;
		for (CommandArgument arg : cmdArgs) {
			if (arg.consumes()) {
				if (arg.pos != c.length - 1) {
					throw new IllegalArgumentException("Consuming argument must be the last argument!");
				}
				String combine = "";
				for (int x = cmdArgs.size() - 1; x < args.length; x++) {
					combine += args[x] + " ";
				}
				try {
					combine = combine.substring(0, combine.length() - 1);
					output[arg.getPosition() + 1] = arg.getType().convert(sender, combine);
				} catch (Exception e) {
					return null;
				}
				return output;
			}
			try {
				output[arg.getPosition() + 1] = Objects.requireNonNull(arg.getType().convert(sender, args[cmdArgs.indexOf(arg)]));
			} catch (Exception e) {
				return null;
			}
		}
		for (CommandArgument arg : c) {
			if (arg.isOptional() && output[arg.getPosition() + 1] == null) {
				output[arg.getPosition() + 1] = arg.getDefaultValue();
			}
		}
		return output;
	}
	
	/**
	 * Registers this command and its children
	 * @param prefix The fallback prefix
	 * @param listener The listener objects containing method hooks
	 */
	public void register(String prefix, Object... listeners) {
		Field field;
		try {
			field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			field.setAccessible(true);
			SimpleCommandMap map = (SimpleCommandMap) field.get(Bukkit.getServer());
			org.bukkit.command.Command cmd = new org.bukkit.command.Command(names[0], help == null ? "None" : help, "", Arrays.stream(names).filter(s -> !s.equals(names[0])).collect(Collectors.toList())) {

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
					if (method.isAnnotationPresent(CommandHook.class)) {
						CommandHook cmdHook = method.getAnnotation(CommandHook.class);
						if (cmdHook.value().equals(hook)) {
							methodHook = method;
							this.listener = listener;
							Class<?>[] params = method.getParameterTypes();
							if (!CommandSender.class.isAssignableFrom(params[0])) {
								throw new IllegalStateException("The first argument must be CommandSender or one of its subclasses! [" + method.getName() + ", " + method.getClass().getName() + "]");
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
	
	protected void registerHook(Object... listeners) {
		loop:
		for (Object listener : listeners) {
			for (Method method : listener.getClass().getDeclaredMethods()) {
				if (method.isAnnotationPresent(CommandHook.class)) {
					CommandHook cmdHook = method.getAnnotation(CommandHook.class);
					if (cmdHook.value().equals(hook)) {
						methodHook = method;
						Class<?>[] params = method.getParameterTypes();
						this.listener = listener;
						if (!CommandSender.class.isAssignableFrom(params[0])) {
							throw new IllegalStateException("The first argument must be CommandSender or one of its subclasses! [" + method.getName() + ", " + method.getClass().getName() + "]");
						}
						break loop;
					}
				}
			}
		}
		for (Command child : children) {
			child.registerHook(listeners);
		}
	}
	
	private List<String> tab(CommandSender sender, String[] args) {
		List<String> completions = new ArrayList<>();
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
			sender.sendMessage(ChatColor.RED + "You do not have permission to run this command! (" + permission + ")");
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
			Object[] objArgs = processArgs(parseArgs(String.join(" ", args)), this.args, sender);
			if (objArgs != null) {
				try {
					methodHook.invoke(listener, objArgs);
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
	
	/**
	 * Loads commands from a command file in stream form. Use Plugin#getResource for this
	 * @param stream The InputStream to load commands from
	 * @param types Custom argument types
	 * @return The commands loaded from the stream
	 */
	public static CommandCollection fromStream(InputStream stream, CommandArgumentType<?>... types) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String line = "";
		List<String> lines = new ArrayList<>();
		try {
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		} catch (EOFException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fromLines(lines, 0, types);
	}
	
	private static String[] splitArgs(String args) {
		List<String> split = new ArrayList<>();
		String combine = "";
		int depth = 0;
		for (char c : args.toCharArray()) {
			switch (c) {
				case '(':
					depth++;
					break;
				case ')':
					depth--;
					break;
				case ' ':
					if (depth == 0) {
						split.add(combine);
						combine = "";
					} else {
						combine += c;
					}
					continue;
			}
			combine += c;
		}
		if (combine.length() > 0) {
			split.add(combine);
		}
		return split.toArray(new String[split.size()]);
	}
	
	private static CommandCollection fromLines(List<String> lines, int lineNumber, CommandArgumentType<?>... types) {
		int depth = 0;
		String help = null;
		String[] names = null;
		List<CommandArgument> args = new ArrayList<>();
		String permission = null;
		String hook = null;
		SenderType type = SenderType.EVERYONE;
		List<Command> commands = new ArrayList<>();
		List<Command> children = new ArrayList<>();
		boolean hideSub = false;
		for (int pos = lineNumber; pos < lines.size(); pos++) {
			String line = lines.get(pos).trim();
			if (line.endsWith("{")) {
				depth++;
				if (depth == 1) {
					line = line.replaceAll("\\{$", "").trim();
					String[] split = splitArgs(line);
					names = split[0].split(",");
					for (int i = 1; i < split.length; i++) {
						String[] argSplit = split[i].split(":");
						if (argSplit.length != 2) {
							throw new IllegalStateException("Invalid argument " + split[i] + ", line " + pos);
						}
						CommandArgumentType<?> argType = getType(argSplit[0], types);
						if (argType == null) {
							throw new IllegalStateException("Missing command argument type " + argSplit[0] + ", line " + pos);
						}
						String name = argSplit[1];
						boolean hideType = false;
						boolean optional = false;
						boolean consumes = false;
						Object defaultValue = null;
						if (name.endsWith("...")) {
							consumes = true;
							name = name.substring(0, name.length() - 3);
						}
						int startIndex = -1;
						if ((startIndex = name.indexOf('(')) != -1) {
							int pdepth = 0;
							int length = 0;
							for (int j = startIndex; j < name.length(); j++) {
								char c = name.charAt(j);
								length++;
								if (c == '(') {
									pdepth++;
								}
								if (c == ')') {
									pdepth--;
									if (pdepth == 0) {
										break;
									}
								}
							}
							if (pdepth != 0) {
								throw new IllegalStateException("Unbalanced parenthesis in argument: " + name + ", line " + pos);
							}
							if (startIndex + length < name.length()) {
								throw new IllegalStateException("Invalid format for argument " + name + ": Cannot define any info after default value (parenthesis), line " + pos);
							}
							String value = name.substring(startIndex + 1, startIndex + length - 1);
							name = name.substring(0, startIndex);
							try {
								defaultValue = argType.convert(null, value);
							} catch (Exception e) {
								e.printStackTrace();
								throw new IllegalArgumentException("Invalid default argument value " + value + ", line " + pos + ". Note that default values are evaluated immediately, so the CommandSender passed to the CommandArgumentType will ne null.");
							}
						}
						if (name.endsWith("*?") || name.endsWith("?*")) {
							hideType = true;
							optional = true;
							name = name.substring(0, name.length() - 2);
						}
						if (name.endsWith("*")) {
							hideType = true;
							name = name.substring(0, name.length() - 1);
						}
						if (name.endsWith("?")) {
							optional = true;
							name = name.substring(0, name.length() - 1);
						}
						if (name.equals(argType.getName())) {
							hideType = true;
						}
						CommandArgument arg = new CommandArgument(argType, i - 1, name, optional, hideType, consumes);
						if (arg.isOptional()) {
							arg.setDefaultValue(defaultValue);
						}
						args.add(arg);
					}
				} else if (depth == 2) {
					children.addAll(fromLines(lines, pos, types).getCommands());
				}
			}
			if (depth == 1) {
				if (line.startsWith("help ")) {
					if (help != null) {
						help += "\n" + line.replaceAll("^help ", "");
					} else {
						help = line.replaceAll("^help ", "");
					}
				}
				if (line.startsWith("permission ")) {
					permission = line.replaceAll("^permission ", "");
				}
				if (line.startsWith("user")) {
					switch (line.replaceAll("^users? ", "")) {
						case "player":
						case "players":
							type = SenderType.PLAYER;
							break;
						case "console":
						case "server":
							type = SenderType.CONSOLE;
							break;
						default:
							type = SenderType.EVERYONE;
							break;
					}
				}
				if (line.equalsIgnoreCase("hidesub")) {
					hideSub = true;
				}
				if (line.startsWith("hook ")) {
					hook = line.replaceAll("^hook ", "");
				}
			}
			if (line.equals("}")) {
				depth--;
				if (depth == 0) {
					commands.add(new Command(names, args.toArray(new CommandArgument[args.size()]), help, permission, type, hook, children, hideSub));
					children = new ArrayList<>();
					names = null;
					args = new ArrayList<>();
					help = null;
					permission = null;
					type = null;
					hook = null;
					hideSub = false;
					if (lineNumber != 0) {
						return new CommandCollection(commands);
					}
				}
			}
		}
		if (lineNumber == 0) {
			commands.stream().forEach(c -> c.topLevel = true);
		}
		return new CommandCollection(commands);
	}
	
	private static CommandArgumentType<?> getType(String name, CommandArgumentType<?>[] types) {
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
	
	protected static class CommandArgument {
		
		private CommandArgumentType<?> type;
		private String name;
		private boolean optional;
		private boolean hideType;
		private boolean consume;
		private Object defaultValue = null;
		private int pos;
		
		public CommandArgument(CommandArgumentType<?> type, int pos, String name, boolean optional, boolean hideType, boolean consume) {
			this.name = name;
			this.type = type;
			this.pos = pos;
			this.optional = optional;
			this.hideType = hideType;
			this.consume = consume;
		}
		
		public void setDefaultValue(Object value) {
			this.defaultValue = value;
		}
		
		public Object getDefaultValue() {
			return defaultValue;
		}
		
		public int getPosition() {
			return pos;
		}
		
		public CommandArgumentType<?> getType() {
			return type;
		}
		
		public boolean isOptional() {
			return optional;
		}
		
		public boolean consumes() {
			return consume;
		}
		
		@Override
		public String toString() {
			String name = hideType ? this.name : type.getName() + ":" + this.name;
			if (optional) {
				name = "[" + name + "]";
			} else {
				name = "<" + name + ">";
			}
			return name;
		}
		
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
		public static CommandArgumentType<Player> playerType = new CommandArgumentType<Player>("player", name -> Bukkit.getPlayer(name))
				.tabStream(c -> Bukkit.getOnlinePlayers().stream().map(Player::getName));
		
		private Function<String, T> func = null;
		private BiFunction<CommandSender, String, T> bifunc = null;
		private String name;
		private Function<CommandSender, List<String>> tab = null;
		
		/**
		 * Had to make this into a single constructor that takes an Object for Maven reasons
		 * @param name The name of this command argument type, to be used in the command file
		 * @param convert The Function<String, T> to convert from a String to whatever type this converts to
		 */
		public CommandArgumentType(String name, Function<String, T> convert) {
			func = convert;
			this.name = name;
		}
		
		/**
		 * Had to make this into a single constructor that takes an Object for Maven reasons
		 * @param name The name of this command argument type, to be used in the command file
		 * @param convert The BiFunction<CommandSender, String, T> to convert from a String to whatever type this converts to
		 */
		public CommandArgumentType(String name, BiFunction<CommandSender, String, T> convert) {
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
			this.tab = c -> {
				return tab.apply(c).collect(Collectors.toList());
			};
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
		 * @param sender The sender of the comand
		 * @param argument The argument to be converted
		 * @return The converted argument for use in a method hook
		 */
		public T convert(CommandSender sender, String argument) {
			return func == null ? bifunc.apply(sender, argument) : func.apply(argument);
		}
		
	}
	
	public static enum SenderType {
		
		CONSOLE,
		PLAYER,
		EVERYONE;
		
	}
	
}
