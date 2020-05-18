# RedLib
RedLib is a Spigot plugin development library, designed to make your life easier and soothe the pain points of plugin development. Below, find instructions for the various components of RedLib.

# Installation

To build the RedLib jar, it is recommended to use Jitpack with Gradle or Maven if possible. If you don't want to do that, you can build the jar yourself manually. For Windows, use Git Bash. For Linux or OSX, just ensure you have Git installed. Navigate to the directory where you want to clone the repository, and run:

```
git clone https://github.com/Redempt/RedLib
cd RedLib
./gradlew jar
```
After running these commands, the jar will be at `build/libs/RedLib.jar`. RedLib is a standalone plugin, so you will need to install it on any servers that have plugins which depend on it, and specify it as a dependency in your plugin.yml like this:

`depend: [RedLib]`
You may also need to add the jar to your classpath. After that, you should be good to go!

# Usage

## Command Manager
Let's admit it: Writing commands for Spigot plugins is a pain. Before I had this tool, I often found myself getting lazy and doing the least amount of work, not creating fleshed-out or good commands.

Well, the command manager aspect of RedLib is designed to allow you to do very little work and handles virtually everything for you. Rather than registering commands like you normally would with Spigot, you can instead write a command file which specifies the basic info of the command, and the command manager takes it from there. It will generate help pages, verify permissions, cast arguments, and hook directly into methods in your code.

The command file is the core piece of information that the command manager needs to create commands for you. It's meant to be very easy and intuitive to write. Here, I will give an example of how to use it. For more specific and complete tutorials, check out [this](https://github.com/Redempt/RedLib/blob/master/example/ExampleListener.java) and [this](https://github.com/Redempt/RedLib/blob/master/example/examplecmd.txt).

To define a command, this is all you need:

```
commandname {
	hook hookName
}
```
This is about as basic as a command will get. It defines the name of the command, and the hook name. It takes no arguments. When this command is run, though, it needs a method to call. Here's how you would do that.

First, you will need to load and register the command file. Assuming the file is called "command.txt" and is in the root directory of your plugin, you could load it like this in your plugin's onEnable:

```
@Override
public void onEnable() {
	new CommandParser(this.getResource("command.txt")).parse().register("prefix", listener);
}
```
Now, "prefix" will be the fallback prefix of the command. The listener argument is an object (or objects) that contain method hooks for the command that are called when the command is run. It must be annotated with @CommandHook(arg), with the argument of the annotation being the name of the hook that you specified in the command file.

```
@CommandHook("hookName")
public void doSomething(CommandSender sender) {
	sender.sendMessage("Hi, you just ran this command!");
}
```
Congratulations! You've just made your first command with the command manager. But there's a lot more you can do with this. Let's try adding some arguments and make a real command:

```
smite string:player {
	permission smite.use
	user player
	help Smites the specified player
	hook smite
}
```
Now, there are a lot of new flags here, and they're fairly self-explanatory, but I'll go over them anyways.
- permission: The permission needed to run the command. This is inherited by child commands (we'll get there soon) and automatically checked.
- help: The message shown in the help page and given to the user if they use the command improperly.
- user: What type of sender this command can be executed by. Should be one of `player`, `console`, or `everyone`.
And there's also another new thing: The argument. `string:player` specifies an argument. It will be passed as a String, and in the help screen, the name of the argument will be shown as `string:player`.

So let's look at what this command's hook might look like:

```
@CommandHook("smite")
public void smite(Player sender, String target) {
	Player victim = Bukkit.getPlayer(target);
	if (victim == null) {
		sender.sendMessage(ChatColor.RED + "Invalid target!");
		return;
	}
	victim.getWorld().strikeLightning(victim.getLocation());
	sender.sendMessage(ChatColor.GREEN + "You smited " + victim.getName() + "!");
}
```
All hook methods must take the sender as the first argument. Since this method is limited to being called by players, it's okay to use `Player` as the first argument's type rather than `CommandSender` because console will be disallowed from using it.

But this is still a bit clunky. Rather than converting the String argument to a `Player` in the command hook method, there's something much easier we can do. Let's go back and register our command a little bit differently:

```
@Override
public void onEnable() {
	CommandArgumentType<Player> playerType = new CommandArgumentType<Player>("player", Bukkit::getPlayer)
	.tabStream(sender -> Bukkit.getOnlinePlayers().map(Player::getName));
	new CommandParser(this.getResource("command.txt")).setArgTypes(playerType).register("smite", listener);
}
```
You can give the command manager a `CommandArgumentType`, which tells it how to convert it from a String argument from a command to whatever type it represents. In this case, `Bukkit::getPlayer` is a `Function<String, Player>` which will convert the `String` argument to a `Player` for any command hook methods. We then pass it to the method which loads the command info from the file. Optionally, you can add a tab provider to the `CommandArgumentType`, which is a lambda that takes the `CommandSender` tab completing and returns a list of possible completions. The ones which don't match the partial argument the sender has already typed are automatically removed by the command manager.

Default types you can use are: `int`, `double`, `string`, `float`, and `long`.

We also specify a name for this `CommandArgumentType`, `"player"` which we can now use in the command file:

```
smite player:target* {
	permission smite.use
	user player
	help Smites the specified player
	hook smite
}
```
The `CommandArgumentType` for `Player` is not included by default, but you don't need to define the type yourself. Since it's so common, it can be found at `CommandArgumentType.playerType`.

Note the `*` at the end of the argument. This isn't required, but it means that, in the help screen, the command will be shown as `smite <target>` instead of `smite <player:target>`. Anyways, now that we have specified `player` as the argument type and registered it in the command file, so we can now use it in the listener:

```
@CommandHook("smite")
public void smite(Player sender, Player target) {
	target.getWorld().strikeLightning(target.getLocation());
	sender.sendMessage(ChatColor.GREEN + "You smited " + target.getName() + "!");
}
```
Much nicer! If the sender provides an invalid player (if the Function we gave it to convert a String to a Player returns null), the sender will be shown the help screen.

Now, what if you have a command that takes an arbitrary number of arguments? No problem! You can specify varargs in the command file, which consume all arguments following them and will be passed to the `Function<String, T>` of the respective `CommandArgumentType`. All you have to do is put `...` at the end of the argument. Obviously, they have to be the last argument in the command:

```
broadcast string:message*... {
	permission broadcast.use
	help Makes an announcement
	hook broadcast
}
```

```
@CommandHook("broadcast")
public void broadcastMessage(CommandSender sender, String message) {
	Bukkit.broadcastMessage(message);
}
```
Easy!

Let's say we're sadists and we want to make a smite command which, if a target is not specified, smites the sender. You can create optional arguments like this:

```
smite player:player*? {
	permission smite.use
	user player
	help Smites the specified player
	hook smite
}
```
The ? specifies that the argument is optional.

```
@CommandHook("smite")
public void smite(Player sender, Player target) {
	target = (target == null) ? sender : target;
	target.getWorld().strikeLightning(target.getLocation());
	sender.sendMessage(ChatColor.GREEN + "You smited " + target.getName() + "!");
}
```
If an optional argument isn't provided, the method hook will be `null`. That means you have to use wrapper classes of primitive types in your hook method's arguments: `Integer` instead of `int`, since `int` is not nullable.

However, a default value can be specified:
```
givelava int:num?(1) {
	hook givelava
	help Gives you the specified amount of lava
	permission givelava.use
	user player
}
```
By putting a parenthetical expression at the end of the argument, you can tell the command manager to use a default value if the optional argument is not provided. Note that this is evaluated immediately, and the CommandArgumentType will be passed `null` in place of the CommandSender it usually takes. Since the argument now has a default value, it will never be `null`, so it's safe to use a primitive type like `int` again.

If you need a type that's not static, and depends on the sender, you can put `context` before the value.

```smite player:target?(context self) {
	hook smite
	help Smites a player, or yourself if no target is specified
	permission smite.use
	user player
}
```
This will use the context provider called `self`, which must return a Player, to supply the default value when the command is run without that argument.

Child commands can be created by simply nesting command bodies inside each other.

```
base {
	//Commands don't need hooks! You can create commands that exist only to contain child commands.
	//The child will inherit the permission requirement of the parent unless otherwise specified
	child {
		hook childCommand
		help This is a child command
	}
}
```
Pretty simple. Running `/base child` will call the child command's method hook. The method hook for the base command will not be called even if it is defined. You can nest child commands in child commands, as deep as you want.

There are some cases where you have to get a lot of context on the player for a command, and you have to do a lot of null checks. This is normally fine, but when you have many commands which require similar context, you often end up having to copy-paste the context. It usually looks like this:

```
@CommandHook("leavefaction")
public void leaveFaction(Player sender) {
	Faction faction = Faction.getFaction(player);
	if (faction == null) {
		player.sendMessage(ChatColor.RED + "You do not belong to a faction!");
		return;
	}
	faction.removePlayer(player);
	player.sendMessage(ChatColor.GREEN + "You successfully left your faction!");
}
```
You can use context arguments in your command file like this:

```
faction {
	leave {
		help Leaves the faction you are in
		hook leavefaction
		user player
		context faction
	}
}
```
Now all you need to do is register the context provider:

```
new CommandParser(this.getResource("command.txt")).setContextProviders(
	new ContextProvider<Faction>("faction", ChatColor.RED + "You do not belong to a faction!", c -> Faction.getFaction((Player) c))).parse()...
```
And with that registered, you can take a Faction argument at the end of your arguments list for your method:

```
@CommandHook("leavefaction")
public void leaveFaction(Player player, Faction faction) {
	//Null check on faction has already been done, player will be shown error if it is null and this method will not be called
	faction.removePlayer(player);
	player.sendMessage("You successfully left your faction!");
}
```
You can register as many context providers as you want, as long as you take them as arguments in the same order they're listed in the command file at the end of your method's argument list.

For more info on the command manager in the form of examples, check out [this](https://github.com/Redempt/RedLib/blob/master/example/ExampleListener.java) and [this](https://github.com/Redempt/RedLib/blob/master/example/examplecmd.txt).

## Config Manager
Another big pain in Spigot is boilerplate code to load and save config values. ConfigManager is a simple yet powerful tool which allows you to load and save values directly from variables in your code. It's so easy to use, anyone could figure it out!

In your main plugin class, you can put the following code:

```
private static ConfigManager config;

@Override
public void onEnable() {
	config = new ConfigManager(this).register(this).saveDefault().load();
}
```
On its own, this really won't do anything. But it's important to note what all of these method calls mean, because they'll become relevant the moment we add a config hook. Instantiating the ConfigManager with a plugin as an argument just instantiates it for the default config, `config.yml` in your plugin's data folder. Calling `register(this)` registers your plugin's config hooks, meaning that's where the data will be loaded to and saved from. Calling `saveDefaults()` means that any values initialized in the hooked variables will be saved into the config. `load()` then loads all the data from config into the variables.

"What variables?" I hear you say. With ConfigManager, all you have to do is create a variable annotated with `@ConfigHook`. It's this simple:

```
@ConfigHook("delay")
int delay = 5;
```

When `register` is called, it will find this variable. The `"delay"` in the `ConfigHook` annotation specifies that the path to the value in the config is `delay`. Notice that it has an initialized value of 5, meaning that when `saveDefaults()` is called, if there isn't a value with the path `delay` in the config, it will be set to `5` and saved. When `load()` is called, it will load whatever value is in the config at the path `delay` into your variable. It works for all types supported by Spigot's YAML parser, including string lists and ItemStacks!

If you call `save()` on the ConfigManager, it will save all the values currently in your variables to config.

But sometimes you need to load configuration sections, too. ConfigManager's still got you covered! All you have to do is write a class with a variable for each value you want to load from each entry in the config section. One example would be if you want to make a grouping system:

```
public class Group {
	
	@ConfigHook("owner)
	private UUID owner;
	@ConfigHook("members")
	private List<UUID> members = ConfigManager.list(UUID.class);
	
	//A constructor with no arguments is required
	protected Group() {}
	
	public Group(UUID leader) {
		owner = leader;
		members = new ArrayList<>();
	}
	
}
```
And that's all we need, there's just one last step. In your main file again, you can add:
```
@ConfigHook("groups.*")
Map<String, Group> groups = ConfigManager.map(Group.class);

@Override
public void onEnable() {
	new ConfigManager(this).addConverter(UUID.class, UUID::fromString, UUID::toString)
		.register(this).saveDefaults().load();
}
```
Putting `.*` at the end is required, as it specifies that this will use the whole config section. When you call `load()` on your ConfigManager, it will load all the entries from the `group` section into the Map. The section would look like this:
```
groups:
	a:
		owner: UUID-here
		members:
		- member1
		- member2
```
You may also notice that I added a converter for UUID. This is because the Group class stores the owner's and members' UUIDs, which isn't a data type YAML lets you store directly. All I have to do to make it work, though, is call `addConverter` and pass the class I'm trying to convert, a method to convert it from a string, and a method to convert it back to a string. Neat!

For more info and docs for ConfigManager, check out [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/configmanager/ConfigManager.java).

## Item Utilities
One of the most consistently annoying parts of Spigot is making items. We all know it. This part of RedLib is very simple, so I'm going to keep the section it short and sweet.

```
ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
ItemMeta meta = item.getItemMeta();
meta.setDisplayName(ChatColor.RED + "Cool sword");
List<String> lore = new ArrayList<>();
lore.add(ChatColor.GREEN + "This sword is very cool.");
meta.setLore(lore);
item.setItemMeta(meta);
```
Nobody likes doing this. Here's a way to simplify it with `ItemUtils`:

```
ItemStack item = ItemUtils.setLore(ItemUtils.rename(new ItemStack(Material.DIAMOND_SWORD), ChatColor.RED + "Cool sword"), ChatColor.GREEN + "This sword is very cool.");
```
However, this is still a bit clunky. We can do even better with `ItemBuilder`:

```
ItemStack item = new ItemBuilder(Material.DIAMOND_SWORD)
	.setName(ChatColor.RED + "Cool sword")
	.setLore(ChatColor.GREEN + "This sword is very cool.");
```
There are plenty more methods to both `ItemUtils` and `ItemBuilder`, like for adding enchantments or attributes, or setting durability. Check out [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/itemutils/ItemBuilder.java) and [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/itemutils/ItemUtils.java) for more info.

## Inventory GUI
Another thing that's always annoying to do manually is create GUIs in Spigot. RedLib has an InventoryGUI class and ItemButton class that will make this a bit easier. To create an InventoryGUI, simply call its constructor with a pre-existing Inventory. Then you can add buttons to it at a given position:

```
InventoryGUI gui = new InventoryGUI(Bukkit.createInventory(null, 27));
ItemBuilder icon = new ItemBuilder(Material.DIAMOND_SWORD)
	.setName(ChatColor.RED + "Die.")
	.setLore(ChatColor.GREEN + "Click this to die.");
ItemButton button = ItemButton.create(icon, e -> e.getWhoClicked().damage(1000));
gui.addButton(button, 1, 2);
```

You can also open slots within the inventory, allowing players to place items into it. This is handy for GUIs that need to allow the player to interact with the items in them.

```
InventoryGUI gui = new InventoryGUI(Bukkit.createInventory(null, 27));
gui.openSlot(11);
```

Opening the slot means that the player can place and remove items from the slot. You can set a listener callback for when the player interacts with open slots using the `setOnClickOpenSlot` method.

If an `InventoryGUI` has open slots, the items in open slots will be automatically returned to the player when the inventory is closed. The player who receives the item will be the last player who closed the inventory. To disable this behavior, you can call `setReturnsItems(false);`.

By default, an `InventoryGUI` will be destroyed once all viewers have closed the inventory. This is because most GUIs are transient: created new each time a player runs a command or clicks some element in the world, and never used again once closed. This behavior can be disabled by calling`setDestroyOnClose(false);`.

And that's really all there is to be said about the InventoryGUI. It's simple but powerful. For more info, check out [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/inventorygui/InventoryGUI.java) and [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/inventorygui/ItemButton.java).

## Multi-Block Structures
This is probably a bit niche, but creating large multi-block structures can be annoying, and without a helper class, it's nearly impossible. RedLib has a very powerful and easy-to-use multi-block structure library built in to help with this.

To get started, you're first going to want to go into RedLib's `config.yml` and set the flag `devMode: true` to enable the dev tools. Once in-game, build your multi-block structure. Use the multi-block structure tool to select two corners of it, and run `/struct create [name]`. The name doesn't matter much here.

Once the structure is created, you can left-click the wand on any block and it will give you info about it. If it's not part of the structure you defined, it will simply tell you that. If it _is_ part of the structure you defined, it will tell you the name of the structure, the rotation of the structure, and whether it is flipped or not (because the utility class automatically checks for rotations of your structure). It will also tell you the relative coordinates of the block you clicked in the structure. The same block in the structure will always have the same relative coordinates, regardless of rotation, mirroring, and location. It's good to note these down if you want certain blocks to do certain things when the structure is clicked.

If you want to register it as a structure programmatically, you should run `/struct print` and copy the string it gives you. Once you have that, you can use `MultiBlockStructure.create()` and pass it that string along with a name to create the multi-block structure handler.

Once you have a `MultiBlockStructure` instance, you can use it to check for the existence of that structure anywhere in the world. By running `getAt(Location)`, you can get an instance of `Structure`, or `null` if it doesn't exist there. Using the `Structure` instance, you can get relative blocks within the structure and access more in-depth info about it.

With a `Structure` instance, you can get relative blocks within the structure (using the relative coordinates you may or may not have noted down earlier), check its rotation/mirroring, and get all of the blocks in it (by type if you want).

For more info, check out [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/multiblock/MultiBlockStructure.java) and [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/multiblock/Structure.java).

## Regions
Also maybe a bit niche, but the Region utilities let you easily define regions of blocks in the world. You can create a `SelectionTool` with a custom item which allows players to select a region, and then get the region selected as a `Region` object. With the `Region` object, you can iterate all the blocks with `forEachBlock`. This will potentially be expanded in the future to include methods to move and resize the region.

You can also protect the region from changes from certain sources. To do this, all you have to do is call `Region#protect(ProtectionType... types)`. The `ProtectionType` enum comes with some pre-defined categories that make it simple: `ProtectionType.DIRECT_PLAYER`, for example, is for any changes made to blocks directly by players. `ProtectionType.NATURAL` represents natural changes, like crop growth.

For more info, check out [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/region/SelectionTool.java), [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/region/Region.java), and [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/region/MultiRegion.java).

## Miscellaneous

ChatPrompt - Allows you to send a prompt to a player in chat and get their response in a callback.

Path - Allows you to get all of the blocks along a certain path, either specifying a start and end location, a start location and vector, or just a start location with a direction. Useful for creating lines of particle effects.

EventListener - Allows you to define a listener for an event with a lambda rather than having to create a class for it or invoke the anonymous class definition

Hologram - Lets you build and manipulate holograms with armor stands easily

LocationUtils - Lets you check whether a certain location is safe, and find the closest safe location to another location
