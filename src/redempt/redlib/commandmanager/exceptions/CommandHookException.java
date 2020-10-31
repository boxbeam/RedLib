package redempt.redlib.commandmanager.exceptions;

/**
 * Thrown when a command with a hook name specified does not find a method hook
 */
public class CommandHookException extends IllegalStateException {
	
	public CommandHookException(String s) {
		super(s);
	}
	
}
