package redempt.redlib.commandmanager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.RedLib;
import redempt.redlib.commandmanager.exceptions.CommandParseException;
import redempt.redlib.commandmanager.exceptions.CommandHookException;
import redempt.redlib.misc.EventListener;
import redempt.redlib.nms.NMSObject;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static redempt.redlib.commandmanager.Messages.msg;

/**
 * Represents a command which can be registered
 *
 * @author Redempt
 */
public class Command {
	
	private static List<ArgType<?>> types = new ArrayList<>();
	private static Map<String, org.bukkit.command.Command> knownCommands;
	private static SimpleCommandMap commandMap;
	
	static {
		try {
			Field field = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
			field.setAccessible(true);
			commandMap = (SimpleCommandMap) field.get(Bukkit.getPluginManager());
			Class<?> clazz = commandMap.getClass();
			while (!clazz.getSimpleName().equals("SimpleCommandMap")) {
				clazz = clazz.getSuperclass();
			}
			Field mapField = clazz.getDeclaredField("knownCommands");
			mapField.setAccessible(true);
			knownCommands = (Map<String, org.bukkit.command.Command>) mapField.get(commandMap);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
	
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
		}).tabStream(c -> Stream.of("true", "false")));
		types.add(new ArgType<Player>("player", (Function<String, Player>) Bukkit::getPlayerExact).tabStream(c -> Bukkit.getOnlinePlayers().stream().map(Player::getName)));
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
	private boolean noHelp = false;
	
	protected Command() {}
	
	protected Command(String[] names, CommandArgument[] args, Flag[] flags, ContextProvider<?>[] providers,
	                  ContextProvider<?>[] asserters, String help, String permission, SenderType type, String hook,
	                  List<Command> children, boolean hideSub, boolean noTab, boolean noHelp) {
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
		this.noHelp = noHelp;
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
	
	private static Result<String[], Boolean[]> splitArgs(String input) {
		List<String> args = new ArrayList<>();
		List<Boolean> quoted = new ArrayList<>();
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
				if (combine.length() > 0) {
					args.add(combine.toString());
					quoted.add(!quotes);
					combine = new StringBuilder();
				}
				continue;
			}
			if (c == ' ' && !quotes) {
				if (combine.length() > 0) {
					args.add(combine.toString());
					quoted.add(false);
					combine = new StringBuilder();
				}
				continue;
			}
			combine.append(c);
		}
		if (combine.length() > 0) {
			args.add(combine.toString());
			quoted.add(false);
		}
		return new Result<>(null, args.toArray(new String[args.size()]), quoted.toArray(new Boolean[quoted.size()]));
	}
	
	private Result<Object[], String> processArgs(String[] argArray, Boolean[] quoted, CommandSender sender) {
		Object[] output = new Object[args.length + flags.length + 1];
		output[0] = sender;
		List<String> args = new ArrayList<>();
		List<Boolean> quotedList = new ArrayList<>();
		Collections.addAll(args, argArray);
		Collections.addAll(quotedList, quoted);
		String err;
		
		err = processFlags(args, output, quotedList, sender);
		if (err != null) {
			return new Result<>(this, null, err);
		}
		
		List<CommandArgument> commandArgs = new ArrayList<>();
		Collections.addAll(commandArgs, this.args);
		err = convertArgs(commandArgs, args, quotedList, output, sender);
		if (err != null) {
			return new Result<>(this, null, err);
		}
		return new Result<>(this, output, null);
	}
	
	private String getWrongArgumentCountMessage(int args, int optionals) {
		if (optionals == 0) {
			return Messages.msg("wrongArgumentCount")
					.replace("%args%", this.args.length + "")
					.replace("%count%", args + "");
		} else {
			return Messages.msg("wrongArgumentCount")
					.replace("%args%", (this.args.length - optionals) + "-" + this.args.length)
					.replace("%count%", args + "");
		}
	}
	
	private String convertArgs(List<CommandArgument> commandArgs, List<String> args, List<Boolean> quoted, Object[] output, CommandSender sender) {
		if (commandArgs.size() == 0) {
			if (args.size() > 0) {
				return getWrongArgumentCountMessage(args.size(), 0);
			}
			return null;
		}
		int diff = commandArgs.size() - args.size();
		int optionals = (int) commandArgs.stream().filter(CommandArgument::isOptional).count();
		boolean lastTakesAll = commandArgs.size() > 0 && commandArgs.get(commandArgs.size() - 1).takesAll();
		if (optionals < diff || (args.size() > commandArgs.size() && !lastTakesAll)) {
			return getWrongArgumentCountMessage(args.size(), optionals);
		}
		int argPos = 0;
		for (int i = 0; i < args.size(); i++) {
			CommandArgument carg = commandArgs.get(argPos);
			String arg = args.get(i);
			if (carg.takesAll()) {
				Result<Object, String> result = processTakeAllArg(carg, args, quoted, i, output, sender);
				if (result.getMessage() != null) {
					return result.getMessage();
				}
				output[carg.getPosition() + 1] = result.getValue();
				return null;
			}
			if (!carg.isOptional() || diff == 0) {
				Result<Object, String> convertResult = convertArg(carg, arg, output, sender);
				if (convertResult.getMessage() != null) {
					return convertResult.getMessage();
				}
				output[carg.getPosition() + 1] = convertResult.getValue();
				argPos++;
				continue;
			}
			Result<Object, String> convertResult = convertArg(carg, arg, output, sender);
			if (convertResult.getValue() == null) {
				if (carg.isContextDefault() && !(sender instanceof Player)) {
					return Messages.msg("contextDefaultFromConsole").replace("%arg%", carg.getName());
				}
				diff--;
				output[carg.getPosition() + 1] = carg.getDefaultValue(sender);
				commandArgs.remove(argPos);
				i--;
				continue;
			}
			output[carg.getPosition() + 1] = convertResult.getValue();
			argPos++;
		}
		for (int i = argPos; i < commandArgs.size(); i++) {
			CommandArgument carg = commandArgs.get(i);
			if (carg.isContextDefault() && !(sender instanceof Player)) {
				return Messages.msg("contextDefaultFromConsole").replace("%arg%", carg.getName());
			}
			output[carg.getPosition() + 1] = carg.getDefaultValue(sender);
			diff--;
		}
		if (diff != 0) {
			return Messages.msg("ambiguousOptional");
		}
		return null;
	}
	
	private Result<Object, String> processTakeAllArg(CommandArgument arg, List<String> args, List<Boolean> quoted, int start, Object[] output, CommandSender sender) {
		if (start >= args.size()) {
			if (!arg.isOptional()) {
				return new Result<>(this, null, Messages.msg("needArgument").replace("%arg%", arg.getName()));
			}
			if (arg.isContextDefault() && !(sender instanceof Player)) {
				return new Result<>(this, null, Messages.msg("contextDefaultFromConsole").replace("%arg%", arg.getName()));
			}
		}
		if (arg.consumes()) {
			if (start >= args.size()) {
				return new Result<>(this, arg.getDefaultValue(sender), null);
			}
			StringBuilder builder = new StringBuilder();
			for (int i = start; i < args.size(); i++) {
				if (quoted.get(i)) {
					builder.append('"').append(args.get(i)).append('"');
				} else {
					builder.append(args.get(i));
				}
				if (i != args.size() - 1) {
					builder.append(' ');
				}
			}
			return convertArg(arg, builder.toString(), output, sender);
		}
		Class<?> clazz = methodHook.getParameterTypes()[arg.getPosition() + 1];
		if (!clazz.isArray()) {
			throw new IllegalStateException("Expected type parameter #" + (arg.getPosition() + 2) + " for method hook " + methodHook.getName() + " to be an array");
		}
		clazz = clazz.getComponentType();
		if (start >= args.size()) {
			Object arr = Array.newInstance(clazz, 1);
			Array.set(arr, 0, arg.getDefaultValue(sender));
			return new Result<>(this, arr, null);
		}
		Object arr = Array.newInstance(clazz, args.size() - start);
		for (int i = start; i < args.size(); i++) {
			Result<Object, String> convert = convertArg(arg, args.get(i), output, sender);
			if (convert.getMessage() != null) {
				return convert;
			}
			Array.set(arr, i, convert.getValue());
		}
		return new Result<>(this, arr, null);
	}
	
	private Result<Object, String> convertArg(CommandArgument carg, String arg, Object[] output, CommandSender sender) {
		ArgType<?> type = carg.getType();
		Object prev = null;
		if (type.getParent() != null && carg.getPosition() > 0) {
			prev = output[carg.getPosition()];
		}
		try {
			return new Result<>(this, Objects.requireNonNull(carg.getType().convert(sender, prev, arg)), null);
		} catch (Exception e) {
			return new Result<>(this, null, Messages.msg("invalidArgument").replace("%arg%", carg.getName()).replace("%value%", arg));
		}
	}
	
	private String processFlags(List<String> args, Object[] output, List<Boolean> quoted, CommandSender sender) {
		for (int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			if (!arg.startsWith("-") || quoted.get(i)) {
				continue;
			}
			Flag flag = Arrays.stream(flags).filter(f -> f.nameMatches(arg)).findFirst().orElse(null);
			if (flag == null) {
				continue;
			}
			if (flag.getType().getName().equals("boolean")) {
				output[flag.getPosition() + 1] = true;
				args.remove(i);
				quoted.remove(i);
				i--;
				continue;
			}
			if (i == args.size() - 1) {
				return Messages.msg("needFlagValue").replace("%flag%", flag.getName());
			}
			String next = args.get(i + 1);
			try {
				output[flag.getPosition() + 1] = Objects.requireNonNull(flag.getType().convert(sender, null, next));
			} catch (Exception ex) {
				return Messages.msg("invalidArgument").replace("%arg%", flag.getName()).replace("%value%", next);
			}
			args.subList(i, i + 2).clear();
			quoted.subList(i, i + 1).clear();
			i--;
		}
		for (Flag flag : flags) {
			if (output[flag.getPosition() + 1] != null) {
				continue;
			}
			if (flag.getType().getName().equals("boolean")) {
				output[flag.getPosition() + 1] = false;
				continue;
			}
			if (flag.isContextDefault() && !(sender instanceof Player)) {
				return Messages.msg("contextDefaultFlagFromConsole").replace("%flag%", flag.getName());
			}
			output[flag.getPosition() + 1] = flag.getDefaultValue(sender);
		}
		return null;
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
		commandMap.register(prefix, cmd);
		if (plugin == null) {
			plugin = RedLib.getCallingPlugin();
		}
		new EventListener<>(RedLib.getInstance(), PluginDisableEvent.class, (l, e) -> {
			if (e.getPlugin().equals(plugin)) {
				try {
					l.unregister();
					unregister();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		registerHook(createHookMap(listeners));
	}
	
	private void unregister() {
		Arrays.stream(names).forEach(knownCommands::remove);
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
					flag.getType().tabComplete(sender, null, args)
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
		args = splitArgsForTab(args);
		//Remaining completions for command arguments
		if (args.length - flagArgs < this.args.length && args.length - flagArgs >= 0) {
			String partial = args[args.length - 1].replaceAll("(^\")|(\"$)", "").toLowerCase();
			CommandArgument arg = this.args[args.length - flagArgs];
			Object previous = getPrevious(args, args.length - 1, args.length - flagArgs, sender);
			List<String> argCompletions = arg.getType().tabComplete(sender, args, previous);
			for (String completion : argCompletions) {
				if (completion == null) {
					continue;
				}
				if (completion.toLowerCase().startsWith(partial) && !partial.equals(completion)) {
					if (completion.contains(" ")) {
						completion = '"' + completion + '"';
					}
					completions.add(completion);
				}
			}
		} else if (this.args.length > 0 && this.args[this.args.length - 1].takesAll() && args.length > 0) {
			String partial = args[args.length - 1].replaceAll("(^\")|(\"$)", "").toLowerCase();
			Object previous = getPrevious(args, args.length - 1, this.args.length - 1, sender);
			this.args[this.args.length - 1].getType().tabComplete(sender, args, previous).stream()
					.filter(s -> s.toLowerCase().startsWith(partial) && !s.equals(partial)).forEach(completions::add);
		}
		return completions;
	}
	
	private Object getPrevious(String[] args, int pos, int argNum, CommandSender sender) {
		if (argNum < 1 || pos < 1) {
			return null;
		}
		CommandArgument arg = this.args[argNum];
		CommandArgument prevArg = this.args[argNum - 1];
		Object previous = null;
		if (arg.getType().getParent() != null) {
			previous = getPrevious(args, pos - 1, argNum - 1, sender);
		}
		return prevArg.getType().convert(sender, previous, args[argNum - 1]);
	}
	
	private String[] splitArgsForTab(String[] args) {
		List<String> argList = new ArrayList<>();
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.length() > 0 && arg.charAt(0) == '"' && i + 1 < args.length) {
				String next = args[i + 1];
				if (next.length() > 0 && next.charAt(next.length() - 1) == '"') {
					argList.add(arg + " " + next);
					i++;
					continue;
				}
			}
			argList.add(args[i]);
		}
		return argList.toArray(new String[0]);
	}
	
	protected Result<Boolean, String> execute(CommandSender sender, String[] args) {
		if (permission != null && !sender.hasPermission(permission)) {
			sender.sendMessage(msg("noPermission").replace("%permission%", permission));
			return new Result<>(this, true, null);
		}
		if (args.length > 0 && args[0].equalsIgnoreCase("help") && !noHelp) {
			showHelp(sender);
			return new Result<>(this, true, null);
		}
		List<Result<Boolean, String>> results = new ArrayList<>();
		if (methodHook != null) {
			type = type == null ? SenderType.EVERYONE : type;
			switch (type) {
				case EVERYONE:
					break;
				case CONSOLE:
					if (sender instanceof Player) {
						sender.sendMessage(Messages.msg("consoleOnly"));
						return new Result<>(this, true, null);
					}
					break;
				case PLAYER:
					if (!(sender instanceof Player)) {
						sender.sendMessage(Messages.msg("playerOnly"));
						return new Result<>(this, true, null);
					}
					break;
			}
			Result<String[], Boolean[]> split = splitArgs(String.join(" ", args));
			Result<Object[], String> result = processArgs(split.getValue(), split.getMessage(), sender);
			Object[] objArgs = result.getValue();
			if (objArgs != null) {
				if (asserters.length > 0 && !assertAll(sender)) {
					return new Result<>(this, true, null);
				}
				int size = objArgs.length + contextProviders.length;
				Object[] arr = new Object[size];
				System.arraycopy(objArgs, 0, arr, 0, objArgs.length);
				if (contextProviders.length > 0) {
					Object[] context = getContext(sender);
					if (context == null) {
						return new Result<>(this, true, null);
					}
					System.arraycopy(context, 0, arr, objArgs.length, context.length);
				}
				try {
					methodHook.invoke(listener, arr);
					return new Result<>(this, true, null);
				} catch (IllegalAccessException | InvocationTargetException e) {
					e.printStackTrace();
					sender.sendMessage(ChatColor.RED + "An error was encountered in running this command. Please notify an admin.");
					return new Result<>(this, true, null);
				} catch (IllegalArgumentException e) {
					if (topLevel) {
						showHelp(sender);
						return new Result<>(this, true, null);
					}
				}
			}
			results.add(new Result<>(this, false, result.getMessage()));
		}
		if (args.length == 0) {
			if (topLevel) {
				showHelp(sender);
				return new Result<>(this, true, null);
			}
			return new Result<>(this, false, results.stream().findFirst().map(Result::getMessage).orElse(null));
		}
		String[] truncArgs = Arrays.copyOfRange(args, 1, args.length);
		for (Command command : children) {
			for (String alias : command.getAliases()) {
				if (alias.equals(args[0])) {
					Result<Boolean, String> result = command.execute(sender, truncArgs);
					if (result.getValue()) {
						return new Result<>(this, true, null);
					} else {
						results.add(result);
					}
				}
			}
		}
		Result<Boolean, String> deepest = results.stream().max(Comparator.comparingInt(r -> r.getCommand().getDepth())).orElse(
				new Result<>(this, false, Messages.msg("invalidSubcommand").replace("%value%", args[0]))
		);
		if (!topLevel) {
			return deepest;
		}
		if (deepest.getMessage() != null) {
			sender.sendMessage(deepest.getMessage());
		}
		deepest.getCommand().showHelp(sender);
		return null;
	}
	
	private int getDepth() {
		int depth = 0;
		Command c = this;
		while (c.parent != null) {
			c = c.parent;
			depth++;
		}
		return depth;
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
