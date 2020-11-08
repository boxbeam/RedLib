package redempt.redlib.commandmanager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.RedLib;
import redempt.redlib.commandmanager.exceptions.CommandParseException;
import redempt.redlib.commandmanager.exceptions.CommandHookException;
import redempt.redlib.misc.EventListener;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static redempt.redlib.commandmanager.Messages.msg;

/**
 * Represents a command which can be registered
 *
 * @author Redempt
 */
public class Command {
	
	private static List<ArgType<?>> types = new ArrayList<>();
	protected List<Command> children = new ArrayList<>();
	
	static {
		types.add(new ArgType<>("int", (Function<String, Integer>) Integer::parseInt));
		types.add(new ArgType<>("double", Double::parseDouble));
		types.add(new ArgType<>("float", Float::parseFloat));
		types.add(new ArgType<>("long", (Function<String, Long>) Long::parseLong));
		types.add(new ArgType<>("string", s -> s));
		types.add(new ArgType<>("boolean", s -> {
			switch (s.toLowerCase()) {
				case "true":
					return true;
				case "false":
					return false;
				default:
					return null;
			}
		}));
		types.add(new ArgType<>("player", Bukkit::getPlayer).tabStream(c -> Bukkit.getOnlinePlayers().stream().map(Player::getName)));
	}
	
	protected Plugin plugin;
	private CommandArgument[] args;
	private Flag[] flags;
	private ContextProvider<?>[] contextProviders;
	private ContextProvider<?>[] asserters;
	protected String[] names;
	private String permission;
	private SenderType type;
	protected String hook;
	private Method methodHook;
	protected String help;
	private Object listener;
	private boolean noTab = false;
	protected boolean topLevel = false;
	protected Command parent = null;
	private boolean hideSub = false;
	
	protected Command() {
	}
	
	protected Command(String[] names, CommandArgument[] args, Flag[] flags, ContextProvider<?>[] providers, ContextProvider<?>[] asserters, String help, String permission, SenderType type, String hook, List<Command> children, boolean hideSub, boolean noTab) {
		this.names = names;
		this.args = args;
		this.flags = flags;
		this.contextProviders = providers;
		this.asserters = asserters;
		this.permission = permission;
		this.type = type;
		this.hook = hook;
		this.help = help;
		this.children = children;
		this.hideSub = hideSub;
		this.noTab = noTab;
		for (Command command : children) {
			command.parent = this;
		}
	}
	
	/**
	 * Shows the help to a CommandSender
	 *
	 * @param sender The sender to show the help to
	 */
	public void showHelp(CommandSender sender) {
		String title = msg("helpTitle").replace("%cmdname%", names[0]);
		sender.sendMessage(title);
		sender.sendMessage(getHelpRecursive(sender, 0).trim());
	}
	
	protected String getHelpRecursive(CommandSender sender, int level) {
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
		name += flags.length > 0 ? String.join(" ", Arrays.stream(flags).map(Flag::toString).collect(Collectors.toList())) + " " : "";
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
	
	private static String[] splitArgs(String input) {
		List<String> args = new ArrayList<>();
		StringBuilder combine = new StringBuilder();
		boolean quotes = false;
		boolean argQuoted = false;
		char[] chars = input.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (c == '\\' && i + 1 < chars.length) {
				combine.append(chars[i + 1]);
				i++;
				continue;
			}
			if (c == '"') {
				argQuoted = true;
				quotes = !quotes;
				continue;
			}
			if (c == ' ' && !quotes) {
				combine.append(argQuoted ? '1' : '0');
				args.add(combine.toString());
				combine = new StringBuilder();
				argQuoted = false;
				continue;
			}
			combine.append(c);
		}
		if (combine.length() > 0) {
			combine.append(argQuoted ? '1' : '0');
			args.add(combine.toString());
		}
		return args.toArray(new String[args.size()]);
	}
	
	private Object[] processArgs(String[] argArray, CommandSender sender) {
		List<String> sargs = new ArrayList<>();
		Collections.addAll(sargs, argArray);
		Object[] output = new Object[args.length + flags.length + 1];
		//Flag checks
		for (Flag flag : flags) {
			if (flag.getType().getName().equals("boolean")) {
				output[flag.getPosition() + 1] = false;
				continue;
			}
			output[flag.getPosition() + 1] = flag.getDefaultValue(sender);
		}
		//Flag argument handling
		for (int i = 0; i < sargs.size(); i++) {
			String arg = sargs.get(i);
			boolean quoted = arg.charAt(arg.length() - 1) == '1';
			arg = arg.substring(0, arg.length() - 1);
			sargs.set(i, arg);
			if (!arg.startsWith("-") || quoted) {
				continue;
			}
			String farg = arg;
			Flag flag = Arrays.stream(flags).filter(f -> f.nameMatches(farg)).findFirst().orElse(null);
			if (flag == null) {
				continue;
			}
			if (flag.getType().getName().equals("boolean")) {
				output[flag.getPosition() + 1] = true;
				sargs.remove(i);
				i--;
				continue;
			}
			if (i == sargs.size() - 1) {
				return null;
			}
			String next = sargs.get(i + 1);
			next = next.substring(0, next.length() - 1);
			try {
				output[flag.getPosition() + 1] = Objects.requireNonNull(flag.getType().convert(sender, next));
			} catch (Exception e) {
				return null;
			}
			sargs.remove(i);
			sargs.remove(i);
			i--;
		}
		//Remove unused optional args
		List<CommandArgument> cmdArgs = Arrays.stream(args).collect(Collectors.toList());
		if (cmdArgs.size() > sargs.size()) {
			int diff = cmdArgs.size() - sargs.size();
			int optional = (int) cmdArgs.stream().filter(CommandArgument::isOptional).count();
			CommandArgument[] cargs = args.clone();
			for (int i = 0; i < cargs.length; i++) {
				if (cargs[i].isOptional()) {
					cargs[i] = null;
				}
			}
			List<CommandArgument> used = new ArrayList<>();
			if (optional >= diff) {
				for (int i = 0; i < sargs.size() && diff > 0; i++) {
					String argString = sargs.get(i);
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
					optionals.removeAll(used);
					if (optionals.size() > 1 && !optionals.stream().allMatch(arg -> arg.getType().getName().equals("string"))) {
						optionals.removeIf(arg -> arg.getType().getName().equals("string"));
					}
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
		if (cmdArgs.size() != sargs.size() &&
				(args.length == 0 || (!args[args.length - 1].consumes() && !args[args.length - 1].isVararg()))) {
			return null;
		}
		output[0] = sender;
		//Process remaining arguments
		for (CommandArgument arg : cmdArgs) {
			if (arg.consumes()) {
				StringBuilder builder = new StringBuilder();
				for (int x = cmdArgs.size() - 1; x < sargs.size(); x++) {
					builder.append(sargs.get(x)).append(" ");
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
			if (arg.isVararg()) {
				Class<?> clazz = methodHook.getParameterTypes()[arg.getPosition() + 1];
				if (!clazz.isArray()) {
					throw new IllegalStateException("Expected type parameter #" + (arg.getPosition() + 2) + " for method hook " + methodHook.getName() + " to be an array");
				}
				Class<?> arrType = clazz.getComponentType();
				Object arr = Array.newInstance(arrType, sargs.size() - cmdArgs.size() + 1);
				if (Array.getLength(arr) == 0 && !arg.isOptional()) {
					return null;
				}
				int pos = 0;
				for (int x = cmdArgs.size() - 1; x < sargs.size(); x++) {
					try {
						Array.set(arr, pos, arg.getType().convert(sender, sargs.get(x)));
						pos++;
					} catch (Exception e) {
						return null;
					}
				}
				output[arg.getPosition() + 1] = arr;
				return output;
			}
			try {
				output[arg.getPosition() + 1] = Objects.requireNonNull(arg.getType().convert(sender, sargs.get(cmdArgs.indexOf(arg))));
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
		if (!(sender instanceof Player)) {
			sender.sendMessage(Messages.msg("playerOnly"));
			return null;
		}
		Object[] output = new Object[contextProviders.length];
		for (int i = 0; i < output.length; i++) {
			Object obj = contextProviders[i].provide((Player) sender);
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
	
	private boolean assertAll(CommandSender sender) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Messages.msg("playerOnly"));
			return false;
		}
		for (ContextProvider<?> provider : asserters) {
			Object o = provider.provide((Player) sender);
			if (o == null) {
				String error = provider.getErrorMessage();
				if (error != null) {
					sender.sendMessage(error);
				} else {
					showHelp(sender);
				}
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Registers this command and its children
	 *
	 * @param prefix    The fallback prefix
	 * @param listeners The listener objects containing method hooks
	 */
	public void register(String prefix, Object... listeners) {
		Field field;
		try {
			field = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
			field.setAccessible(true);
			SimpleCommandMap map = (SimpleCommandMap) field.get(Bukkit.getPluginManager());
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
			Class<?> clazz = map.getClass();
			while (!clazz.getSimpleName().equals("SimpleCommandMap")) {
				clazz = clazz.getSuperclass();
			}
			Field mapField = clazz.getDeclaredField("knownCommands");
			mapField.setAccessible(true);
			Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) mapField.get(map);
			map.register(prefix, cmd);
			if (plugin == null) {
				plugin = JavaPlugin.getProvidingPlugin(Class.forName(new Exception().getStackTrace()[1].getClassName()));
			}
			new EventListener<>(RedLib.getInstance(), PluginDisableEvent.class, (l, e) -> {
				if (e.getPlugin().equals(plugin)) {
					try {
						l.unregister();
						Arrays.stream(names).forEach(knownCommands::remove);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			});
			registerHook(createHookMap(listeners));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected Map<String, MethodHook> createHookMap(Object... listeners) {
		Map<String, MethodHook> hooks = new HashMap<>();
		for (Object listener : listeners) {
			for (Method method : listener.getClass().getDeclaredMethods()) {
				CommandHook cmdHook = method.getAnnotation(CommandHook.class);
				if (cmdHook == null) {
					continue;
				}
				if (hooks.put(cmdHook.value(), new MethodHook(method, listener)) != null) {
					throw new CommandHookException("Duplicate method hook for name '" + cmdHook.value() + "'");
				}
			}
		}
		return hooks;
	}
	
	protected void registerHook(Map<String, MethodHook> hooks) {
		for (Command child : children) {
			child.registerHook(hooks);
		}
		if (hook == null) {
			return;
		}
		MethodHook mh = hooks.get(hook);
		if (hook != null && mh == null) {
			throw new CommandHookException("Command with hook name " + hook + " has no method hook");
		}
		methodHook = mh.getMethod();
		plugin = JavaPlugin.getProvidingPlugin(methodHook.getDeclaringClass());
		Class<?>[] params = methodHook.getParameterTypes();
		int expectedLength = args.length + contextProviders.length + flags.length + 1;
		if (params.length != expectedLength) {
			throw new IllegalStateException("Incorrect number of arguments for method hook! [" + methodHook.getDeclaringClass().getName() + "." + methodHook.getName() + "] "
					+ "Argument count should be " + expectedLength + ", got " + params.length);
		}
		this.listener = mh.getListener();
		if (!CommandSender.class.isAssignableFrom(params[0])) {
			throw new IllegalStateException("The first argument must be CommandSender or one of its subclasses! [" + methodHook.getDeclaringClass().getName() + "." + methodHook.getName() + "]");
		}
	}
	
	protected List<String> tab(CommandSender sender, String[] args) {
		List<String> completions = new ArrayList<>();
		//Handle child command completions
		if (args.length > 0) {
			for (Command child : children) {
				if (child.noTab) {
					continue;
				}
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
		//Add subcommands of this command as completions
		if (args.length == 1) {
			for (Command child : children) {
				if (child.noTab) {
					continue;
				}
				if (child.getPermission() != null && !sender.hasPermission(child.getPermission())) {
					continue;
				}
				if (child.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
					completions.add(child.getName());
				}
			}
		}
		//Handle flag completions and account for already-used flags
		int flagArgs = 1;
		Set<Flag> used = new HashSet<>();
		Flag flag = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (i == args.length - 1) {
				if (arg.startsWith("-")) {
					Arrays.stream(flags).filter(f -> !used.contains(f) && f.getName().startsWith(arg))
							.map(Flag::getNames).forEach(s -> Collections.addAll(completions, s));
				}
				if (flag != null && !flag.getType().getName().equals("boolean")) {
					flag.getType().tabComplete(sender)
							.stream().filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
							.map(s -> s.contains(" ") ? '"' + s + '"' : s)
							.forEach(completions::add);
				}
			}
			if (arg.startsWith("-")) {
				flag = Arrays.stream(flags).filter(f -> f.getName().equals(arg)).findFirst().orElse(null);
				if (flag != null) {
					used.add(flag);
					flagArgs++;
					continue;
				}
			}
			if (flag != null && !flag.getType().getName().equals("boolean")) {
				flagArgs++;
			}
			flag = null;
		}
		//Remaining completions for command arguments
		if (args.length - flagArgs < this.args.length && args.length - flagArgs >= 0) {
			String partial = args[args.length - 1].replaceAll("(^\")|(\"$)", "").toLowerCase();
			CommandArgument arg = this.args[args.length - flagArgs];
			List<String> argCompletions = arg.getType().tabComplete(sender);
			for (String completion : argCompletions) {
				if (completion.toLowerCase().startsWith(partial) && !partial.equals(completion)) {
					if (completion.contains(" ")) {
						completion = '"' + completion + '"';
					}
					completions.add(completion);
				}
			}
		} else if (this.args.length > 0 && this.args[this.args.length - 1].isVararg() && args.length > 0) {
			String partial = args[args.length - 1].replaceAll("(^\")|(\"$)", "").toLowerCase();
			this.args[this.args.length - 1].getType().tabComplete(sender).stream()
					.filter(s -> s.toLowerCase().startsWith(partial) && !s.equals(partial)).forEach(completions::add);
		}
		return completions;
	}
	
	protected boolean execute(CommandSender sender, String[] args) {
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
						sender.sendMessage(Messages.msg("consoleOnly"));
						return true;
					}
					break;
				case PLAYER:
					if (!(sender instanceof Player)) {
						sender.sendMessage(Messages.msg("playerOnly"));
						return true;
					}
					break;
			}
			Object[] objArgs = processArgs(splitArgs(String.join(" ", args)), sender);
			if (objArgs != null) {
				if (asserters.length > 0 && !assertAll(sender)) {
					return true;
				}
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
	
	protected static ArgType<?> getType(String name, ArgType<?>[] types) {
		for (ArgType<?> type : Command.types) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		for (ArgType<?> type : types) {
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
	 *
	 * @param stream The InputStream to load commands from
	 * @param types  Custom argument types
	 * @return The commands loaded from the stream
	 * @throws CommandParseException if the command file cannot be parsed
	 * @deprecated Outdated. Use {@link CommandParser#parse()}
	 */
	public static CommandCollection fromStream(InputStream stream, ArgType<?>... types) {
		return new CommandParser(stream).setArgTypes(types).parse();
	}
	
	public static enum SenderType {
		
		CONSOLE,
		PLAYER,
		EVERYONE;
		
	}
	
	protected static class MethodHook {
		
		private Method method;
		private Object listener;
		
		public MethodHook(Method method, Object listener) {
			this.method = method;
			this.listener = listener;
		}
		
		public Method getMethod() {
			return method;
		}
		
		public Object getListener() {
			return listener;
		}
		
	}
	
}
