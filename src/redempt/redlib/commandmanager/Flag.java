package redempt.redlib.commandmanager;

import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.function.Function;

class Flag {

	private ArgType<?> type;
	private String name;
	private String[] names;
	private int pos;
	private Function<CommandSender, Object> defaultValue = null;
	private boolean contextDefault;
	
	public Flag(ArgType<?> type, String name, int pos, Function<CommandSender, Object> defaultValue, boolean contextDefault) {
		this.type = type;
		this.name = name;
		this.names = name.split(",");
		this.pos = pos;
		this.defaultValue = defaultValue;
		this.contextDefault = contextDefault;
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
	
	public boolean isContextDefault() {
		return contextDefault;
	}
	
	public boolean nameMatches(String name) {
		return Arrays.stream(names).anyMatch(name::equals);
	}
	
	public String getName() {
		return name;
	}
	
	public String[] getNames() {
		return names;
	}
	
	@Override
	public String toString() {
		return "[" + name + (type.getName().equals("boolean") ? "]" : " " + type.getName() + "]");
	}
	
}
