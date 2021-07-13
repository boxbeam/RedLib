package redempt.redlib.commandmanager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import redempt.redlib.RedLib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a collection of commands which can be mass-registered. Can contain any amount of commands, including 0
 * @author Redempt
 *
 */
public class CommandCollection {
	
	private List<Command> commands;
	
	public CommandCollection(List<Command> commands) {
		this.commands = commands;
	}

	/**
	 * Register all commands in this CommandCollection
	 * @param plugin the plugin that owns the commands
	 * @param prefix The fallback prefix of the commands
	 * @param listeners The list of listener objects which contain hooks for the commands in this collection
	 */
	public void register(Plugin plugin, String prefix, Object... listeners) {
		mergeBaseCommands();
		commands.forEach(c -> {
			c.plugin = plugin;
			c.register(prefix, listeners);
		});
	}
	
	/**
	 * Register all commands in this CommandCollection
	 * @param prefix The fallback prefix of the commands
	 * @param listeners The list of listener objects which contain hooks for the commands in this collection
	 */
	public void register(String prefix, Object... listeners) {
		register(RedLib.getCallingPlugin(), prefix, listeners);
	}
	
	private void mergeBaseCommands() {
		Map<String, List<Command>> names = new HashMap<>();
		for (Command command : commands) {
			String name = String.join(", ", command.getAliases());
			List<Command> cmds = names.getOrDefault(name, new ArrayList<>());
			cmds.add(command);
			names.put(name, cmds);
		}
		names.forEach((k, v) -> {
			if (v.size() > 1) {
				commands.removeAll(v);
				commands.add(new MergedBaseCommand(v));
			}
		});
	}
	
	/**
	 * 
	 * @return The commands in this CommandCollection
	 */
	public List<Command> getCommands() {
		return commands;
	}
	
	/**
	 * Recursively searches this CommandCollection for a command by a given hook, then shows the help to the given sender
	 * @param hookName The hook name of the command
	 * @param sender The sender to be shown the help menu for this command
	 * @throws IllegalArgumentException if no command by that hook name was found
	 */
	public void showHelp(String hookName, CommandSender sender) {
		Command result = getByHookName(hookName);
		if (result == null) {
			throw new IllegalArgumentException("No command by that hook name exists!");
		}
		result.showHelp(sender);
	}
	
	
	/**
	 * Recurseively searches this CommandCollection for a command with a given hook name
	 * @param hookName The hook name of the command
	 * @return The command in this CommandCollection by that hook name, or null if none found
	 */
	public Command getByHookName(String hookName) {
		for (Command command : commands) {
			Command result = getByHookName(hookName, command);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
	
	private Command getByHookName(String hookName, Command base) {
		if (hookName.equals(base.hook)) {
			return base;
		}
		for (Command child : base.children) {
			Command result = getByHookName(hookName, child);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
	
	private static class MergedBaseCommand extends Command {
		
		public MergedBaseCommand(List<Command> commands) {
			this.children = commands;
			this.help = commands.get(0).help;
			this.names = commands.get(0).names;
			for (Command command : children) {
				command.topLevel = false;
				command.parent = this;
			}
		}
		
		@Override
		public Result<Boolean, String> execute(CommandSender sender, String[] args, Object[] prevArgs) {
			List<Result<Boolean, String>> results = new ArrayList<>();
			for (Command cmd : children) {
				results.add(cmd.execute(sender, args, prevArgs));
			}
			if (results.stream().anyMatch(Result::getValue)) {
				return null;
			}
			sender.sendMessage(Messages.msg("helpTitle").replace("%cmdname%", children.get(0).getName()));
			sender.sendMessage(getHelpRecursive(sender, 0));
			return null;
		}
		
		@Override
		public String getHelpRecursive(CommandSender sender, int level) {
			return children.stream().map(c -> c.getHelpRecursive(sender, 1)).collect(Collectors.joining("\n"));
		}
		
		@Override
		public List<String> tab(CommandSender sender, String[] args) {
			List<String> completions = new ArrayList<>();
			children.forEach(c -> completions.addAll(c.tab(sender, args)));
			return completions;
		}
		
		@Override
		public void register(String prefix, Object... listeners) {
			super.register(prefix, listeners);
			Map<String, MethodHook> hooks = createHookMap(listeners);
			for (Command command : children) {
				command.registerHook(hooks, this.plugin);
			}
		}
		
	}
	
}
