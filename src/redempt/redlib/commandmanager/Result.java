package redempt.redlib.commandmanager;

class Result<T> {
	
	private T value;
	private String message;
	private Command cmd;
	
	public Result(Command cmd, T value, String message) {
		this.value = value;
		this.message = message;
		this.cmd = cmd;
	}
	
	public Command getCommand() {
		return cmd;
	}
	
	public T getValue() {
		return value;
	}
	
	public String getMessage() {
		return message;
	}
	
}
