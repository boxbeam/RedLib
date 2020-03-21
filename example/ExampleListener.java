package example;

import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Entity;

import redempt.redlib.commandmanager.Command;
import redempt.redlib.commandmanager.Command.CommandArgumentType;
import redempt.redlib.commandmanager.CommandCollection;
import redempt.redlib.commandmanager.CommandHook;
import redempt.redlib.commandmanager.CommandFactory;
import redempt.redlib.commandmanager.ContextProvider;


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
		CommandCollection cmds = new CommandFactory(plugin.getResource("examplecmd.txt"))
				.setArgTypes(CommandArgumentType.playerType)
				//Registers a context provider which adds context that will be passed as an argument
				//To commands which specify that they need it using the 'context' flag
				//If they return null, they will show the error message - the second argument
				//The error message is optional, and the player will be shown the help menu instead if none is provided
				//This is useful if you have a lot of copy-pasted null checks between many commands for contextual information relating to the player
				.setContextProviders(new ContextProvider<Entity>("mount", ChatColor.RED + "You must be riding a mount to do this!", c -> ((Player) c).getVehicle()))
				.parse();
		
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
	
	//You can take the optional argument as an int here, because it will never be null. It will be 1 by default.
	@CommandHook("damage")
	public void damage(CommandSender sender, Player target, int damage) {
		target.damage(damage);
	}
	
	@CommandHook("broadcast")
	//This command's last argument consumes
	//That means that it will ignore spaces once it reaches that argument
	//Since there is only one argument, that means all arguments will be passed as a single string here
	public void broadcast(CommandSender sender, String message) {
		Bukkit.broadcastMessage(ChatColor.GREEN + "Broadcast: " + message);
	}
	
	@CommandHook("healmount")
	//You must take the context as an additional argument after the sender argument and all command arguments
	public void healMount(Player sender, Entity mount) {
		mount.remove();
		sender.sendMessage(ChatColor.GREEN + "Killed your mount! :)");
	}
	
}
