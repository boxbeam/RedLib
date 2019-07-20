package redempt.cmdmgr2;

import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import redempt.cmdmgr2.Command.CommandArgumentType;

/**
 * @author Redempt
 * @deprecated For example purposes only
 */
@Deprecated
public class ExampleListener {
	
	public ExampleListener(String prefix, Plugin plugin) {
		//Create a custom argument type for Player
		//The name of this custom argument to be used in the command file is player
		//This will be used to convert a String to a Player, so this one only works for online players
		//You can make your own argument types for anything you might need
		//The argument will be run through this to cast it to another type to be passed to the hook method
		//If the CommandSender is required for context, you can pass a BiFunction<CommandSender, String, T>
		//If your Function returns null, the player will be shown the command help
		CommandArgumentType<Player> playerType = new CommandArgumentType<Player>("player", (Function<String, Player>) Bukkit::getPlayer);
		
		//Tab completer for the player argument type
		//This returns a stream of all online Players for possible tab completions
		//Values which do not match the partial string are automatically excluded
		playerType.tabStream(c -> Bukkit.getOnlinePlayers().stream().map(Player::getName));
		
		//Takes a stream of the command file
		//Easiest way to use this is Plugin#getResource
		//You can then pass as many custom argument types as you need
		//The CommandCollection represents all commands loaded from the command file
		//Multiple commands can be loaded from a single command file
		CommandCollection cmds = Command.fromStream(plugin.getResource("examplecmd.txt"), playerType);
		
		//Prefix is the fallback prefix, like /minecraft:kill or /bukkit:plugins
		//The second argument is a listener Object which contains methods hooks
		cmds.register(prefix, this);
	}
	
	//The CommandHook annotation specifies that this is a method hook
	//The String in the annotation should be the same as the hook specified in the command file
	@CommandHook("test")
	//The first argument always must be a CommandSender
	//It is also acceptable to have the first argument as a Player if the command file specifies that only players may run the command
	//Permissions are checked prior to this method being called. If execution reaches this method, the sender has permission to run this command
	public void test(CommandSender sender) {
		sender.sendMessage("This is a test");
	}
	
	@CommandHook("add")
	//You can use types like int and Integer since int is specified as the argument type
	//Note that the third argument is Integer rather than int because the argument is optional as specified by the command file
	//Integer is nullable while int is not
	public void add(Player player, int num1, Integer num2) {
		if (num2 != null) {
			player.sendMessage("Sum: " + String.valueOf(num1 + num2.intValue()));
			return;
		}
		player.sendMessage(num1 + "");
	}
	
	@CommandHook("kill")
	public void kill(Player sender, Player target) {
		if (target != null) {
			target.damage(1000);
			return;
		}
		sender.damage(1000);
	}
	
	@CommandHook("broadcast")
	//This is a consuming command
	//That means that all arguments are passed as a single String
	//Spaces are included in this String
	//Arguments may contain spaces if they are surrounded by ""
	//Quotes can be escaped with \
	public void broadcast(CommandSender sender, String message) {
		Bukkit.broadcastMessage(ChatColor.GREEN + "Broadcast: " + message);
	}
	
}
