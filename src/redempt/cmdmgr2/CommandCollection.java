package redempt.cmdmgr2;

import java.util.List;

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
	
}
