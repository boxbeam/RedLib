package redempt.redlib.commandmanager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redempt.redlib.commandmanager.Command.CommandArgumentType;
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
	
	private CommandArgumentType<?>[] argTypes = {};
	private ContextProvider<?>[] contextProviders = {};
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
	public CommandParser setArgTypes(CommandArgumentType<?>... types) {
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
						String[] argSplit = split[i].split(":");
						if (argSplit.length != 2) {
							throw new CommandParseException("Invalid command argument syntax" + split[i] + ", line " + pos);
						}
						CommandArgumentType<?> argType = Command.getType(argSplit[0], argTypes);
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
						CommandArgument arg = new CommandArgument(argType, i - 1, name, optional, hideType, consumes);
						if (arg.isOptional()) {
							arg.setDefaultValue(defaultValue);
						}
						args.add(arg);
					}
				} else if (depth == 2) {
					children.addAll(fromLines(lines, pos).getCommands());
				}
			}
			if (depth == 1) {
				if (line.startsWith("help ")) {
					if (help != null) {
						help += "\n" + line.replaceFirst("^help ", "");
					} else {
						help = line.replaceFirst("^help ", "");
					}
				}
				if (line.startsWith("permission ")) {
					permission = line.replaceFirst("^permission ", "");
				}
				if (line.startsWith("user")) {
					switch (line.replaceFirst("^users? ", "")) {
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
				if (line.startsWith("context ")) {
					contextProviders.clear();
					String rest = line.substring(8);
					String[] split = rest.split(" ");
					int fpos = pos;
					for (String name : split) {
						ContextProvider<?> provider = Arrays.stream(this.contextProviders).filter(c -> c.getName().equals(name)).findFirst()
							.orElseThrow(() -> new CommandParseException("Missing context provider " + name + ", line " + fpos));
						contextProviders.add(provider);
					}
				}
				if (line.startsWith("assert ")) {
					asserters.clear();
					String rest = line.substring(7);
					String[] split = rest.split(" ");
					int fpos = pos;
					for (String name : split) {
						ContextProvider<?> provider = Arrays.stream(this.contextProviders).filter(c -> c.getName().equals(name)).findFirst()
								.orElseThrow(() -> new CommandParseException("Missing context provider " + name + ", line " + fpos));
						asserters.add(provider);
					}
				}
				if (line.equalsIgnoreCase("hidesub")) {
					hideSub = true;
				}
				if (line.equalsIgnoreCase("notab")) {
					noTab = true;
				}
				if (line.startsWith("hook ")) {
					hook = line.replaceAll("^hook ", "");
				}
			}
			if (line.equals("}")) {
				depth--;
				if (depth == 0) {
					commands.add(new Command(names, args.toArray(new CommandArgument[args.size()]),
							contextProviders.toArray(new ContextProvider<?>[contextProviders.size()]),
							asserters.toArray(new ContextProvider<?>[asserters.size()]),
							help, permission, type, hook, children, hideSub, noTab));
					children = new ArrayList<>();
					names = null;
					args = new ArrayList<>();
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
