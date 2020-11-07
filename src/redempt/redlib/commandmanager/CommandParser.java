package redempt.redlib.commandmanager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redempt.redlib.commandmanager.Command.SenderType;
import redempt.redlib.commandmanager.exceptions.CommandParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Used to get ContextProviders and CommandArgumentTypes for a command file, then parse it
 * @author Redempt
 *
 */
public class CommandParser {
	
	private ArgType<?>[] argTypes = {};
	private ContextProvider<?>[] contextProviders = {ContextProvider.self};
	private InputStream stream;
	
	/**
	 * Constructs a CommandParser to parse input from the given stream. Use {@link org.bukkit.plugin.java.JavaPlugin#getResource} for this
	 * @param stream The stream to read the command info from
	 */
	public CommandParser(InputStream stream) {
		this.stream = stream;
	}
	
	/**
	 * Sets the CommandArgumentTypes to be used when building this command.
	 * @param types The CommandArgumentTypes to be used
	 * @return This CommandParser
	 */
	public CommandParser setArgTypes(ArgType<?>... types) {
		if (Arrays.stream(types).anyMatch(t -> t == null)) {
			throw new IllegalArgumentException("Command argument types cannot be null!");
		}
		this.argTypes = types;
		return this;
	}
	
	/**
	 * Sets the ContextProviders to be used when building this command.
	 * @param providers The ContextProviders to be used
	 * @return This CommandParser
	 */
	public CommandParser setContextProviders(ContextProvider<?>... providers) {
		if (Arrays.stream(providers).anyMatch(t -> t == null)) {
			throw new IllegalArgumentException("Context providers cannot be null!");
		}
		this.contextProviders = providers;
		return this;
	}
	
	/**
	 * Parses the command info from the stream
	 * @return A CommandCollection representing all the commands which were parsed. Use {@link CommandCollection#register(String, Object...)} to register all commands in it at once.
	 */
	public CommandCollection parse() {
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
			return null;
		}
		return fromLines(lines, 0);
	}
	
	private CommandCollection fromLines(List<String> lines, int lineNumber) {
		int depth = 0;
		String help = null;
		String[] names = null;
		List<CommandArgument> args = new ArrayList<>();
		List<Flag> flags = new ArrayList<>();
		List<ContextProvider<?>> contextProviders = new ArrayList<>();
		List<ContextProvider<?>> asserters = new ArrayList<>();
		String permission = null;
		String hook = null;
		SenderType type = SenderType.EVERYONE;
		List<Command> commands = new ArrayList<>();
		List<Command> children = new ArrayList<>();
		boolean hideSub = false;
		boolean noTab = false;
		for (int pos = lineNumber; pos < lines.size(); pos++) {
			String line = lines.get(pos).trim();
			if (line.endsWith("{")) {
				depth++;
				if (depth == 1) {
					line = line.substring(0, line.length() - 1).trim();
					String[] split = splitArgs(line);
					names = split[0].split(",");
					for (int i = 1; i < split.length; i++) {
						if (split[i].startsWith("-") && !split[i].contains(":")) {
							split[i] = "boolean:" + split[i];
						}
						CommandArgument arg = parseArg(split[i], i, pos);
						if (arg.getName().startsWith("-")) {
							if (arg.isOptional()) {
								throw new CommandParseException("Flags cannot be marked as optional, they are optional by definition, line " + pos);
							}
							if (arg.consumes()) {
								throw new CommandParseException("Flags cannot be vararg, line " + pos);
							}
							Flag flag = new Flag(arg.getType(), arg.getName(), arg.getPosition(), arg.getDefaultValue());
							for (String name : flag.getNames()) {
								if (!name.startsWith("-")) {
									throw new CommandParseException("All flag names and aliases must start with a dash, line " + pos);
								}
							}
							flags.add(flag);
							continue;
						}
						args.add(arg);
					}
				} else if (depth == 2) {
					children.addAll(fromLines(lines, pos).getCommands());
				}
			}
			if (depth == 1) {
				String[] tag = getTag(line);
				try {
					switch (tag[0]) {
						case "help":
							if (help == null) {
								help = tag[1];
							} else {
								help += "\n" + tag[1];
							}
							break;
						case "permission":
							permission = tag[1];
							break;
						case "user":
						case "users":
							switch (tag[1]) {
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
							}
							break;
						case "context":
							contextProviders.clear();
							String[] split = tag[1].split(" ");
							int fpos = pos;
							for (String name : split) {
								ContextProvider<?> provider = Arrays.stream(this.contextProviders).filter(c -> c.getName().equals(name)).findFirst()
										.orElseThrow(() -> new CommandParseException("Missing context provider " + name + ", line " + fpos));
								contextProviders.add(provider);
							}
							break;
						case "assert":
							asserters.clear();
							split = tag[1].split(" ");
							fpos = pos;
							for (String name : split) {
								ContextProvider<?> provider = Arrays.stream(this.contextProviders).filter(c -> c.getName().equals(name)).findFirst()
										.orElseThrow(() -> new CommandParseException("Missing context provider " + name + ", line " + fpos));
								asserters.add(provider);
							}
							break;
						case "hidesub":
							hideSub = true;
							break;
						case "notab":
							noTab = true;
							break;
						case "hook":
							hook = tag[1];
							break;
					}
				} catch (ArrayIndexOutOfBoundsException ex) {
					throw new CommandParseException("Missing tag data for tag " + tag[0] + ", line " + lineNumber);
				}
			}
			if (line.equals("}")) {
				depth--;
				if (depth == 0) {
					commands.add(new Command(names, args.toArray(new CommandArgument[args.size()]),
							flags.toArray(new Flag[flags.size()]),
							contextProviders.toArray(new ContextProvider<?>[contextProviders.size()]),
							asserters.toArray(new ContextProvider<?>[asserters.size()]),
							help, permission, type, hook, children, hideSub, noTab));
					children = new ArrayList<>();
					names = null;
					args = new ArrayList<>();
					flags = new ArrayList<>();
					contextProviders = new ArrayList<>();
					asserters = new ArrayList<>();
					help = null;
					permission = null;
					type = null;
					hook = null;
					hideSub = false;
					noTab = false;
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
	
	private CommandArgument parseArg(String arg, int argPos, int pos) {
		String[] argSplit = arg.split(":");
		if (argSplit.length != 2) {
			throw new CommandParseException("Invalid command argument syntax" + arg + ", line " + pos);
		}
		ArgType<?> argType = Command.getType(argSplit[0], argTypes);
		if (argType == null) {
			throw new CommandParseException("Missing command argument type " + argSplit[0] + ", line " + pos);
		}
		String name = argSplit[1];
		boolean hideType = false;
		boolean optional = false;
		boolean consumes = false;
		Function<CommandSender, Object> defaultValue = c -> null;
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
				throw new CommandParseException("Unbalanced parenthesis in argument: " + name + ", line " + pos);
			}
			if (startIndex + length < name.length()) {
				throw new CommandParseException("Invalid format for argument " + name + ": Cannot define any argument info after default value (parenthesis), line " + pos);
			}
			String value = name.substring(startIndex + 1, startIndex + length - 1);
			name = name.substring(0, startIndex);
			if (value.startsWith("context ")) {
				String pname = value.substring(8);
				int fpos = pos;
				ContextProvider<?> provider = Arrays.stream(this.contextProviders).filter(c -> c.getName().equals(pname)).findFirst()
						.orElseThrow(() -> new CommandParseException("Missing context provider " + pname + ", line " + fpos));
				defaultValue = c -> provider.provide((Player) c);
			} else {
				defaultValue = c -> argType.convert(c, value.startsWith("\\") ? value.substring(1) : value);
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
		CommandArgument carg = new CommandArgument(argType, argPos - 1, name, optional, hideType, consumes);
		if (carg.isOptional() || name.startsWith("-")) {
			carg.setDefaultValue(defaultValue);
		}
		return carg;
	}
	
	private static String[] getTag(String line) {
		int index = line.indexOf(' ');
		if (index == -1) {
			return new String[] {line};
		}
		return new String[] {line.substring(0, index), line.substring(index + 1)};
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
	
}
