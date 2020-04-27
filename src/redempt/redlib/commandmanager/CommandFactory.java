package redempt.redlib.commandmanager;

import java.io.InputStream;

/**
 * @deprecated Kept for legacy purposes. Class has been refactored to {@link CommandParser}
 *
 */
public class CommandFactory extends CommandParser {

	public CommandFactory(InputStream stream) {
		super(stream);
	}
	
}
