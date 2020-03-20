package redempt.redlib.commandmanager.exceptions;

/**
 * Thrown when there is a problem found while parsing a command file
 * @author Redempt
 *
 */
public class CommandParseException extends IllegalStateException {

	private static final long serialVersionUID = 329877560896L;
	
	public CommandParseException(String s) {
		super(s);
	}
	
}
