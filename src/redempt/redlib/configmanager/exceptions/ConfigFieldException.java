package redempt.redlib.configmanager.exceptions;

/**
 * Thrown when a field with a CommandHook is final
 */
public class ConfigFieldException extends IllegalStateException {
	
	public ConfigFieldException(String s) {
		super(s);
	}
	
}
