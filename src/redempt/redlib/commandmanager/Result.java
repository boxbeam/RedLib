package redempt.redlib.commandmanager;

class Result<T, V> {
	
	private T value;
	private V message;
	private Command cmd;
	
	public Result(Command cmd, T value, V message) {
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
	
	public V getMessage() {
		return message;
	}
	
}
