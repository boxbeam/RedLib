package redempt.redlib.commandmanager;

import org.bukkit.command.CommandSender;
import redempt.redlib.commandmanager.Command.CommandArgumentType;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The name for the CommandArgumentType class is rather long. This class exists only
 * as an alias for CommandArgumentType to save you a little bit of time.
 * @param <T> The type this ArgType represents
 */
public class ArgType<T> extends CommandArgumentType<T> {
	
	/**
	 * Create a CommandArgumentType from a name and converter
	 * @param name The name of this command argument type, to be used in the command file
	 * @param convert The {@link Function} to convert from a String to whatever type this converts to
	 */
	public ArgType(String name, Function<String, T> convert) {
		super(name, convert);
	}
	
	/**
	 * Create a CommandArgumentType from a name and converter
	 * @param name The name of this command argument type, to be used in the command file
	 * @param convert The {@link BiFunction} to convert from a String to whatever type this converts to
	 */
	public ArgType(String name, BiFunction<CommandSender, String, T> convert) {
		super(name, convert);
	}
	
}
