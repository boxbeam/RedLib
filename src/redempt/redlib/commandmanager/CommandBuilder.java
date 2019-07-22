package redempt.redlib.commandmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;

import redempt.redlib.commandmanager.Command.CommandArgument;
import redempt.redlib.commandmanager.Command.SenderType;

/**
 * A very basic command builder. It is recommended to use a command file where possible.
 * @author Redempt
 *
 */
public class CommandBuilder {
	
	private String[] names;
	private String permission;
	private CommandArgument[] args;
	private String help;
	private SenderType type;
	private List<CommandBuilder> children = new ArrayList<>();
	private boolean hideSub = false;
	private Object listener;
	
	/**
	 * Creates a CommandBuilder and sets the names
	 * @param names
	 */
	public CommandBuilder(String... names) {
		this.names = names;
	}
	
	/**
	 * Sets the permission for the command
	 * @param permission The permission to set
	 * @return The command builder
	 */
	public CommandBuilder permission(String permission) {
		this.permission = permission;
		return this;
	}
	
	/**
	 * Sets the help message for the command
	 * @param help The help to set
	 * @return The command builder
	 */
	public CommandBuilder help(String help) {
		this.help = help;
		return this;
	}
	
	/**
	 * Sets the senders who can use this command
	 * @param type The sender type which can use this command
	 * @return The command builder
	 */
	public CommandBuilder type(SenderType type) {
		this.type = type;
		return this;
	}
	
	/**
	 * Sets the hook for this command
	 * @param hook The handler for when this command is run, cannot take any arguments but the sender
	 * @return The command builder
	 */
	public CommandBuilder hook(Consumer<CommandSender> hook) {
		listener = new Object() {
			
			@CommandHook("_")
			public void func(CommandSender sender) {
				hook.accept(sender);
			}
			
		};
		args = new CommandArgument[0];
		return this;
	}
	
	/**
	 * Adds a child to this command
	 * @param command The child command builder
	 * @return The command builder
	 */
	public CommandBuilder addChild(CommandBuilder command) {
		this.children.add(command);
		return this;
	}
	
	private List<Command> buildChildren(String prefix) {
		return children.stream().map(c -> {
			Command cmd = new Command(c.names, c.args, c.help, c.permission, c.type, "_", c.buildChildren(prefix), c.hideSub);
			System.out.println(c.names[0]);
			cmd.register(prefix, c.listener);
			return cmd;
		}).collect(Collectors.toList());
	}
	
	/**
	 * Builds and registers this command and its children. Only call this on the root command.
	 * @param prefix The command fallback prefix
	 * @return The registered command
	 */
	public Command build(String prefix) {
		Command command = new Command(names, args, help, permission, type, listener == null ? null : "_", buildChildren(prefix), hideSub);
		command.register(prefix, listener == null ? new Object() : listener);
		return command;
	}
	
}
