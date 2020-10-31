package redempt.redlib.commandmanager;

import org.bukkit.command.CommandSender;

import java.util.function.Function;

class Flag {

	private ArgType<?> type;
	private String name;
	private int pos;
	private Function<CommandSender, Object> defaultValue = null;
	
	public Flag(ArgType<?> type, String name, int pos, Function<CommandSender, Object> defaultValue) {
		this.type = type;
		this.name = name;
		this.pos = pos;
		this.defaultValue = defaultValue;
	}
	
	public Object getDefaultValue(CommandSender sender) {
		return defaultValue == null ? null : defaultValue.apply(sender);
	}
	
	public int getPosition() {
		return pos;
	}
	
	public ArgType<?> getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return "[" + name + (type.getName().equals("boolean") ? "]" : " " + type.getName() + "]");
	}
	
}
