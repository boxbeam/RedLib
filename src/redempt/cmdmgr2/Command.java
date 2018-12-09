package redempt.cmdmgr2;

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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;

/**
 * Represents a command which can be registered
 * @author Redempt
 */
public class Command {
	
	private static List<CommandArgumentType<?>> types = new ArrayList<>();
	protected List<Command> children = new ArrayList<>();
	
	static {
		types.add(new CommandArgumentType<Integer>("int", Integer::parseInt));
		types.add(new CommandArgumentType<Double>("double", Double::parseDouble));
		types.add(new CommandArgumentType<Float>("float", Float::parseFloat));
		types.add(new CommandArgumentType<Long>("long", Long::parseLong));
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
	private Command parent = null;
	private boolean hideSub = false;
	
	private Command(String[] names, CommandArgument[] args, String help, String permission, SenderType type, String hook, List<Command> children, boolean hideSub) {
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
		String title = ChatColor.translateAlternateColorCodes('&', CmdMgr.helpTitle).replace("%cmdname%", names[0]);
		sender.sendMessage(title);
		sender.sendMessage(getHelpRecursive(sender, 0).trim());
	}
	
	private String getHelpRecursive(CommandSender sender, int level) {
		if (permission != null && !sender.hasPermission(permission)) {
			return "";
		}
		String help = this.help == null ? "" : ChatColor.translateAlternateColorCodes('&', CmdMgr.helpEntry).replace("%cmdname%", getFullName()).replace("%help%", this.help) + "\n";
		if (hideSub && level != 0) {
			if (help.equals("")) {
				return ChatColor.translateAlternateColorCodes('&', CmdMgr.helpEntry).replace("%cmdname%", getFullName()).replace("%help%", "[Hidden subcommands]") + "\n";
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
			for (int i = 0; i < cmdArgs.size(); i++) {
				if (cmdArgs.get(i).isOptional()) {
					cmdArgs.remove(i);
					diff--;
					if (diff <= 0) {
						break;
					}
				}
			}
			if (diff > 0) {
				return null;
			}
		}
		if (cmdArgs.size() != args.length && (c.length == 0 || !c[c.length - 1].consumes())) {
			return null;
		}
		Object[] output = new Object[c.length + 1];
		output[0] = sender;
		for (int i = 1; i < output.length; i++) {
			if (!cmdArgs.contains(c[i - 1])) {
				output[i] = null;
				continue;
			}
			if (c[i - 1].consumes()) {
				if (i < c.length) {
					throw new IllegalArgumentException("Consuming argument must be the last argument!");
				}
				String combine = "";
				for (int x = i - 1; x < args.length; x++) {
					combine += args[x] + " ";
				}
				combine = combine.substring(0, combine.length() - 1);
				output[i] = c[i - 1].getType().convert(sender, combine);
				return output;
			}
			try {
				output[i] = c[i - 1].getType().convert(sender, args[i - 1]);
			} catch (Exception e) {
				return null;
			}
			if (output[i] == null) {
				return null;
			}
		}
		return output;
	}
	
	/**
	 * Registers this command and its children
	 * @param prefix The fallback prefix
	 * @param listener The listener object containing method hooks
	 */
	public void register(String prefix, Object listener) {
		this.listener = listener;
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
			for (Method method : listener.getClass().getDeclaredMethods()) {
				if (method.isAnnotationPresent(CommandHook.class)) {
					CommandHook cmdHook = method.getAnnotation(CommandHook.class);
					if (cmdHook.value().equals(hook)) {
						methodHook = method;
						Class<?>[] params = method.getParameterTypes();
						if (!CommandSender.class.isAssignableFrom(params[0])) {
							throw new IllegalStateException("The first argument must be CommandSender or one of its subclasses! [" + method.getName() + ", " + method.getClass().getName() + "]");
						}
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (Command child : children) {
			child.registerHook(listener);
		}
	}
	
	private void registerHook(Object listener) {
		this.listener = listener;
		for (Method method : listener.getClass().getDeclaredMethods()) {
			if (method.isAnnotationPresent(CommandHook.class)) {
				CommandHook cmdHook = method.getAnnotation(CommandHook.class);
				if (cmdHook.value().equals(hook)) {
					methodHook = method;
					Class<?>[] params = method.getParameterTypes();
					if (!CommandSender.class.isAssignableFrom(params[0])) {
						throw new IllegalStateException("The first argument must be CommandSender or one of its subclasses! [" + method.getName() + ", " + method.getClass().getName() + "]");
					}
					break;
				}
			}
		}
		for (Command child : children) {
			child.registerHook(listener);
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
			CommandArgument arg = this.args[args.length - 1];
			List<String> argCompletions = arg.getType().tabComplete(sender);
			for (String completion : argCompletions) {
				if (completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()) && !args[args.length - 1].equals(completion)) {
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
					String[] split = line.split(" ");
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
						if (name.endsWith("...")) {
							consumes = true;
							name = name.substring(0, name.length() - 3);
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
						args.add(new CommandArgument(argType, name, optional, hideType, consumes));
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
	
	private static class CommandArgument {
		
		private CommandArgumentType<?> type;
		private String name;
		private boolean optional;
		private boolean hideType;
		private boolean consume;
		
		public CommandArgument(CommandArgumentType<?> type, String name, boolean optional, boolean hideType, boolean consume) {
			this.name = name;
			this.type = type;
			this.optional = optional;
			this.hideType = hideType;
			this.consume = consume;
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
	
	private static enum SenderType {
		
		CONSOLE,
		PLAYER,
		EVERYONE;
		
	}
	
}
