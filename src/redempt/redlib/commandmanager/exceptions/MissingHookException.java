package redempt.redlib.commandmanager.exceptions;

/**
 * Thrown when a command with a hook name specified does not find a method hook
 */
public class MissingHookException extends IllegalStateException {
	
	public MissingHookException(String s) {
		super(s);
	}
	
}
