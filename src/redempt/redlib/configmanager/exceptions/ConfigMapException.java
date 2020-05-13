package redempt.redlib.configmanager.exceptions;

/**
 * Thrown when there is an error mapping a ConfigurationSection to an object
 */
public class ConfigMapException extends IllegalStateException {
	
	public ConfigMapException(String s) {
		super(s);
	}
	
}
