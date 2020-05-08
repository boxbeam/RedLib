package redempt.redlib.commandmanager;

import org.bukkit.command.CommandSender;
import redempt.redlib.commandmanager.Command.CommandArgumentType;

import java.util.function.Function;

class CommandArgument {
	
	private CommandArgumentType<?> type;
	private String name;
	private boolean optional;
	private boolean hideType;
	private boolean consume;
	private Function<CommandSender, Object> defaultValue = null;
	public int pos;
	
	public CommandArgument(CommandArgumentType<?> type, int pos, String name, boolean optional, boolean hideType, boolean consume) {
		this.name = name;
		this.type = type;
		this.pos = pos;
		this.optional = optional;
		this.hideType = hideType;
		this.consume = consume;
	}
	
	public void setDefaultValue(Function<CommandSender, Object> value) {
		this.defaultValue = value;
	}
	
	public Object getDefaultValue(CommandSender sender) {
		return defaultValue == null ? null : defaultValue.apply(sender);
	}
	
	public int getPosition() {
		return pos;
	}
	
	public CommandArgumentType<?> getType() {
		return type;
	}
	
	public boolean isOptional() {
		return optional;
	}
	
	public boolean consumes() {
		return consume;
	}
	
	@Override
	public String toString() {
		String name = hideType ? this.name : type.getName() + ":" + this.name;
		if (optional) {
			name = "[" + name + "]";
		} else {
			name = "<" + name + ">";
		}
		return name;
	}
	
}