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
	
	protected List<Command> children = new ArrayList<>();
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
	private boolean postArg = false;
	private boolean hasPostArgChild = false;
	
	protected Command() {}
	
	protected Command(String[] names, CommandArgument[] args, Flag[] flags, ContextProvider<?>[] providers,
	                  ContextProvider<?>[] asserters, String help, String permission, SenderType type, String hook,
	                  List<Command> children, boolean hideSub, boolean noTab, boolean noHelp, boolean postArg) {
		this.names = names;
		this.args = args;
		this.flags = flags;
		this.contextProviders = providers;
		this.asserters = asserters;
		this.permission = permission;
		this.type = type;
		this.hook = hook;
		this.help = help;
		this.hideSub = hideSub;
		this.noTab = noTab;
		this.noHelp = noHelp;
		this.postArg = postArg;
		this.children = children;
		for (Command command : children) {
			command.parent = this;
			if (command.args.length == 0 || args.length == 0) {
				continue;
			}
			CommandArgument first = command.args[0];
			if (first.getType().getParent() == null) {
				continue;
			}
			String firstName = args[args.length - 1].getType().getName();
			String secondName = first.getType().getParent().getName();
			if (!firstName.equals(secondName)) {
				throw new IllegalStateException("Argument " + first.getName() + " for command " + command.getName()
						+ " does not have an argument of type " + first.getType().getParent().getName() + " before it");
			}
		}
		hasPostArgChild = children.stream().anyMatch(c -> c.postArg);
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
		String name;
		if (postArg) {
			name = parent.getFullName() + " " + names[0] + " ";
		} else {
			 name = getExpandedName() + " ";
		}
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
	
	private Result<Object[], String> processArgs(String[] argArray, Boolean[] quoted, Object[] prepend, CommandSender sender) {
		Object[] output = new Object[args.length + flags.length + 1 + Math.max(0, prepend.length - 1)];
		if (prepend.length != 0) {
			System.arraycopy(prepend, 1, output, 1, prepend.length - 1);
		}
		int offset = 1 + Math.max(0, prepend.length - 1);
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
		err = convertArgs(commandArgs, args, quotedList, output, offset, sender);
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
	
	private String convertArgs(List<CommandArgument> commandArgs, List<String> args, List<Boolean> quoted, Object[] output, int offset, CommandSender sender) {
		if (commandArgs.size() == 0) {
			if (args.size() > 0) {
				return getWrongArgumentCountMessage(args.size(), 0);
			}
			return null;
		}
		int diff = commandArgs.size() - args.size();
		int optionals = (int) commandArgs.stream().filter(CommandArgument::isOptional).count();
		if (optionals < diff || (args.size() > commandArgs.size() && !lastArgTakesAll())) {
			return getWrongArgumentCountMessage(args.size(), optionals);
		}
		int argPos = 0;
		for (int i = 0; i < args.size(); i++) {
			CommandArgument carg = commandArgs.get(argPos);
			String arg = args.get(i);
			if (carg.takesAll()) {
				Result<Object, String> result = processTakeAllArg(carg, args, quoted, i, output, offset, sender);
				if (result.getMessage() != null) {
					return result.getMessage();
				}
				output[carg.getPosition() + offset] = result.getValue();
				return null;
			}
			if (!carg.isOptional() || diff == 0) {
				Result<Object, String> convertResult = convertArg(carg, arg, output, offset, sender);
				if (convertResult.getMessage() != null) {
					return convertResult.getMessage();
				}
				output[carg.getPosition() + offset] = convertResult.getValue();
				argPos++;
				continue;
			}
			Result<Object, String> convertResult = convertArg(carg, arg, output, offset, sender);
			if (convertResult.getValue() == null) {
				if (carg.isContextDefault() && !(sender instanceof Player)) {
					return Messages.msg("contextDefaultFromConsole").replace("%arg%", carg.getName());
				}
				diff--;
				output[carg.getPosition() + offset] = carg.getDefaultValue(sender);
				commandArgs.remove(argPos);
				i--;
				continue;
			}
			output[carg.getPosition() + offset] = convertResult.getValue();
			argPos++;
		}
		for (int i = argPos; i < commandArgs.size(); i++) {
			CommandArgument carg = commandArgs.get(i);
			if (carg.isContextDefault() && !(sender instanceof Player)) {
				return Messages.msg("contextDefaultFromConsole").replace("%arg%", carg.getName());
			}
			output[carg.getPosition() + offset] = carg.getDefaultValue(sender);
			diff--;
		}
		if (diff != 0) {
			return Messages.msg("ambiguousOptional");
		}
		return null;
	}
	
	private Result<Object, String> processTakeAllArg(CommandArgument arg, List<String> args, List<Boolean> quoted, int start, Object[] output, int offset, CommandSender sender) {
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
			return convertArg(arg, builder.toString(), output, offset, sender);
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
			Result<Object, String> convert = convertArg(arg, args.get(i), output, offset, sender);
			if (convert.getMessage() != null) {
				return convert;
			}
			Array.set(arr, i, convert.getValue());
		}
		return new Result<>(this, arr, null);
	}
	
	private Result<Object, String> convertArg(CommandArgument carg, String arg, Object[] output, int offset, CommandSender sender) {
		ArgType<?> type = carg.getType();
		Object prev = null;
		int pos = carg.getPosition() + (offset - 1);
		if (type.getParent() != null && pos > 0) {
			prev = output[pos];
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
				Command.this.execute(sender, args, new Object[0]);
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
		if (postArg) {
			expectedLength += parent.args.length + parent.contextProviders.length + parent.flags.length;
		}
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
		List<String> argList = splitArgsForTab(args);
		args = argList.toArray(new String[0]);
		List<String> completions = tab(sender, argList, args);
		String last = args.length > 0 ? args[args.length - 1].toLowerCase(Locale.ROOT) : "";
		return completions.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(last)).map(s -> s.contains(" ") ? "\"" + s + "\"" : s).collect(Collectors.toList());
	}
	
	private List<String> tab(CommandSender sender, List<String> args, String[] completeArgs) {
		List<String> completions = new ArrayList<>();
		boolean childrenTabbed = false;
		for (Command child : children) {
			if (child.noTab || child.postArg || (child.getPermission() != null && !sender.hasPermission(child.getPermission()))) {
				continue;
			}
			if (args.size() > 0 && child.nameMatches(args.get(0))) {
				completions.addAll(child.tab(sender, args.stream().skip(1).collect(Collectors.toList()), completeArgs));
				childrenTabbed = true;
			}
			if (args.size() == 1) {
				completions.add(child.getName());
			}
		}
		if (childrenTabbed) {
			return completions;
		}
		Result<Boolean, List<String>> flagResults = tabCompleteFlags(args, sender);
		if (flagResults.getValue()) {
			return flagResults.getMessage();
		} else {
			completions.addAll(flagResults.getMessage());
		}
		if (this.args.length > 0) {
			CommandArgument last = this.args[Math.max(0, Math.min(args.size() - 1, this.args.length - 1))];
			if (last.isVararg() || args.size() <= this.args.length) {
				completions.addAll(tabCompleteArgument(last, completeArgs, sender));
			}
		}
		if (args.size() > this.args.length && hasPostArgChild) {
			int next = this.args.length;
			String name = args.get(next);
			List<String> toComplete = args.stream().skip(next + 1).collect(Collectors.toList());
			for (Command child : children) {
				if (!child.isPostArg() || (child.getPermission() != null && !sender.hasPermission(child.getPermission()))) {
					continue;
				}
				if (next + 1 == args.size()) {
					completions.add(child.getName());
					continue;
				}
				if (child.nameMatches(name)) {
					completions.addAll(child.tab(sender, toComplete, completeArgs));
				}
			}
		}
		return completions;
	}
	
	private Result<Boolean, List<String>> tabCompleteFlags(List<String> args, CommandSender sender) {
		if (args.size() == 0) {
			return new Result<>(this, false, new ArrayList<>());
		}
		Set<Flag> used = new HashSet<>();
		for (int i = 0; i < args.size() - 1; i++) {
			String arg = args.get(i);
			if (!arg.startsWith("-")) {
				continue;
			}
			Flag flag = Arrays.stream(flags).filter(f -> f.nameMatches(arg)).findFirst().orElse(null);
			if (flag == null) {
				continue;
			}
			if (flag.getType().getName().equals("boolean")) {
				used.add(flag);
				args.remove(i);
				i--;
				continue;
			}
			if (i < args.size() - 2) {
				args.subList(i, i + 1).clear();
				i--;
			}
		}
		if (args.size() == 0) {
			return new Result<>(this, false, new ArrayList<>());
		}
		List<String> completions = new ArrayList<>();
		String lastArg = args.get(args.size() - 1);
		if (lastArg.startsWith("-")) {
			Arrays.stream(flags).filter(f -> !used.contains(f)).forEach(f -> Collections.addAll(completions, f.getNames()));
			return new Result<>(this, true, completions);
		}
		if (args.size() <= 1) {
			return new Result<>(this, false, completions);
		}
		String nextToLast = args.get(args.size() - 2);
		Flag flag = Arrays.stream(flags).filter(f -> f.nameMatches(nextToLast)).findFirst().orElse(null);
		if (flag == null) {
			return new Result<>(this, false, completions);
		}
		completions.addAll(flag.getType().tabComplete(sender, args.toArray(new String[0]), null));
		return new Result<>(this, true, completions);
	}
	
	private List<String> tabCompleteArgument(CommandArgument arg, String[] str, CommandSender sender) {
		return arg.getType().tabComplete(sender, str, getPrevious(str, str.length - 1, arg.getPosition(), sender));
	}
	
	private Object getPrevious(String[] args, int pos, int argNum, CommandSender sender) {
		if ((argNum < 1 || pos < 1) && !postArg) {
			return null;
		}
		if (argNum < 0 || pos < 0 || argNum >= this.args.length || pos >= args.length) {
			return null;
		}
		if (postArg && argNum == 0) {
			pos--;
		}
		CommandArgument arg = this.args[argNum];
		CommandArgument prevArg = argNum - 1 >= 0 ? this.args[argNum - 1] : parent.args[parent.args.length - 1];
		Object previous = null;
		if (arg.getType().getParent() != null) {
			previous = getPrevious(args, pos - 1, argNum - 1, sender);
		}
		return prevArg.getType().convert(sender, previous, args[pos - 1]);
	}
	
	private List<String> splitArgsForTab(String[] args) {
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
		return argList;
	}
	
	private Object[] combine(Object[] first, Object[] second) {
		Object[] newArr = new Object[first.length + second.length];
		System.arraycopy(first, 0, newArr, 0, first.length);
		System.arraycopy(second, 0, newArr, first.length, second.length);
		return newArr;
	}
	
	protected Result<Boolean, String> execute(CommandSender sender, String[] args, Object[] parentArgs) {
		if (permission != null && !sender.hasPermission(permission)) {
			sender.sendMessage(msg("noPermission").replace("%permission%", permission));
			return new Result<>(this, true, null);
		}
		if (args.length > 0 && args[0].equalsIgnoreCase("help") && !noHelp) {
			showHelp(sender);
			return new Result<>(this, true, null);
		}
		List<Result<Boolean, String>> results = new ArrayList<>();
		runHook:
		if (methodHook != null || hasPostArgChild) {
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
			String[] toProcess = split.getValue();
			if (hasPostArgChild) {
				String[] next = new String[Math.min(toProcess.length, this.args.length)];
				System.arraycopy(toProcess, 0, next, 0, next.length);
				toProcess = next;
			}
			Boolean[] quoted = split.getMessage();
			Result<Object[], String> result = processArgs(toProcess, quoted, parentArgs, sender);
			Object[] objArgs = result.getValue();
			if (objArgs == null) {
				results.add(new Result<>(this, false, result.getMessage()));
				break runHook;
			}
			if (asserters.length > 0 && !assertAll(sender)) {
				return new Result<>(this, true, null);
			}
			if (contextProviders.length > 0) {
				Object[] context = getContext(sender);
				if (context == null) {
					return new Result<>(this, true, null);
				}
				objArgs = combine(objArgs, context);
			}
			if (hasPostArgChild && split.getValue().length > this.args.length && !quoted[toProcess.length]) {
				int spaces = (int) Arrays.stream(toProcess).filter(s -> s.contains(" ")).count();
				int start = this.args.length + spaces;
				String[] truncArgs = Arrays.copyOfRange(args, start + 1, args.length);
				Object[] combined = combine(parentArgs, objArgs);
				for (Command command : children) {
					if (!command.isPostArg() || !command.nameMatches(args[start])) {
						continue;
					}
					Result<Boolean, String> execResult = command.execute(sender, truncArgs, combined);
					if (execResult.getValue()) {
						return new Result<>(this, true, null);
					}
					results.add(execResult);
				}
				break runHook;
			}
			if (methodHook == null) {
				break runHook;
			}
			try {
				methodHook.invoke(listener, objArgs);
				return new Result<>(this, true, null);
			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
				sender.sendMessage(msg("commandError"));
				return new Result<>(this, true, null);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.out.println(objArgs.length);
				if (topLevel) {
					showHelp(sender);
					return new Result<>(this, true, null);
				}
			}
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
			if (command.isPostArg() || !command.nameMatches(args[0])) {
				continue;
			}
			Result<Boolean, String> result = command.execute(sender, truncArgs, parentArgs);
			if (result.getValue()) {
				return new Result<>(this, true, null);
			}
			results.add(result);
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
	
	/**
	 * Check if a name matches any of this command's aliases
	 * @param name The name to check
	 * @return Whether the name matches any of this command's aliases
	 */
	public boolean nameMatches(String name) {
		return Arrays.stream(names).anyMatch(s -> s.equals(name));
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
	 * @return Whether the last argument in this command is consuming or vararg
	 */
	public boolean lastArgTakesAll() {
		return args.length > 0 && args[args.length - 1].takesAll();
	}
	
	/**
	 * @return All of the command's names/aliases
	 */
	public String[] getAliases() {
		return names;
	}
	
	/**
	 * @return Whether this subcommand comes after the arguments of its parent
	 */
	public boolean isPostArg() {
		return postArg;
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
