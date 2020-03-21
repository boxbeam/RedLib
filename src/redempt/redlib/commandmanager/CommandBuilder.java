package redempt.redlib.commandmanager;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.command.CommandSender;

import redempt.redlib.commandmanager.Command.CommandArgument;
import redempt.redlib.commandmanager.Command.CommandArgumentType;
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
	private SenderType type = SenderType.EVERYONE;
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
	 * Sets the hook for this command
	 * @param hook The handler for when this command is run, takes the sender and all arguments after that as a single String
	 * @param argName The name of the argument, to be shown in the help menu
	 * @return The command builder
	 */
	public CommandBuilder hook(BiConsumer<CommandSender, String> hook, String argName) {
		listener = new Object() {
			
			@CommandHook("_")
			public void func(CommandSender sender, String arg) {
				hook.accept(sender, arg);
			}
			
		};
		args = new CommandArgument[] {new CommandArgument(new CommandArgumentType<String>("string", s -> s),
				0,
				argName,
				false,
				true,
				true)};
		return this;
	}
	
	/**
	 * Builds and registers this command
	 * @param prefix The command fallback prefix
	 * @return The registered command
	 */
	public Command build(String prefix) {
		Command command = new Command(names, args, new ContextProvider<?>[] {}, help, permission, type, listener == null ? null : "_", new ArrayList<>(), hideSub);
		command.register(prefix, listener == null ? new Object() : listener);
		return command;
	}
	
}
