package redempt.redlib.commandmanager;

import java.util.List;

import org.bukkit.command.CommandSender;

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
	 * @param prefix The fallback prefix of the commands
	 * @param listener The listener object containing method hooks
	 */
	public void register(String prefix, Object listener) {
		commands.stream().forEach(c -> c.register(prefix, listener));
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
	
}
