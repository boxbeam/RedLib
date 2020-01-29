# RedLib
RedLib is a Spigot plugin development library, designed to make your life easier and soothe the pain points of plugin development. Below, find instructions for the various components of RedLib.

## Command manager
Let's admit it: Writing commands for Spigot plugins is a pain. Before I had this tool, I often found myself getting lazy and doing the least amount of work, not creating fleshed-out or good commands.

Well, the command manager aspect of RedLib is designed to allow you to do very little work and handles virtually everything for you. Rather than registering commands like you normally would with Spigot, you can instead write a command file which specifies the basic info of the command, and the command manager takes it from there. It will generate help pages, verify permissions, cast arguments, and hook directly into methods in your code.

The command file is the core piece of information that the command manager needs to create commands for you. It's meant to be very easy and intuitive to write. Here, I will give an example of how to use it. For more specific and complete tutorials, check out [this](https://github.com/Redempt/RedLib/blob/master/src/example/ExampleListener.java) and [this](https://github.com/Redempt/RedLib/blob/master/src/example/examplecmd.txt).

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
	Command.fromStream(this.getResource("command.txt")).register("prefix", listener);
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
And there's also another new thing: The argument. `string:player` specifies an argument. It will be passed as a String, and in the help screen, the name of the argument will be `string:player`.

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
All hook methods must take the sender as the first argument. Since this method is limited to being called by players, it's okay to use `Player` as the first argument's type rather than `CommandSender`.

This is still a bit clunky. Rather than converting the String argument to a `Player` in the command hook method, there's something much easier we can do. Let's go back and register our command a little bit differently:

```
@Override
public void onEnable() {
	CommandArgumentType<Player> playerType = new CommandArgumentType<Player>("player", Bukkit::getPlayer)
	.tabStream(sender -> Bukkit.getOnlinePlayers().map(Player::getName));
	Command.fromStream(this.getResource("command.txt"), playerType).register("smite", listener);
}
```
You can give the command manager a `CommandArgumentType`, which tells it how to convert it from a String argument from a command to whatever type it represents. We then pass it to the method which loads the command info from the file. Optionally, you can add a tab provider, which takes the `CommandSender` tab completing and returns a list of possible completions. The ones which don't match the partial argument the sender has already typed are automatically removed by the command manager.

Default types you can use are: `int`, `double`, `string`, `float`, and `long`.

We also specify a name for this `CommandArgumentType`, which we can now use in the command file:

```
smite player:player* {
	permission smite.use
	user player
	help Smites the specified player
	hook smite
}
```
Note the `*` at the end of the argument. This isn't required, but it means that, in the help screen, the command will be shown as `smite <player>` instead of `smite <player:player>`. Anyways, now that we have specified `player` as the argument type and registered it in the command file, so we can now use it in the listener:

```
@CommandHook("smite")
public void smite(Player sender, Player target) {
	target.getWorld().strikeLightning(target.getLocation());
	sender.sendMessage(ChatColor.GREEN + "You smited " + target.getName() + "!");
}
```
Much nicer! If the sender provides an invalid player (if the Function we gave it to convert a String to a Player returns null), the sender will be shown the help screen.

Now, what if you have a command that takes an arbitrary number of arguments? No problem! You can specify consuming arguments in the command file, which consume all arguments following them. Obviously, they have to be the last argument in the command:

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
If an optional argument isn't provided, you'll get `null`.

Now that I've touched on most of the basics, child commands:

```
base {
	//Commands don't need hooks! You can create commands that exist only to contain child commands.
	help This is a base command
	permission base.use
	//The child will inherit the permission requirement of the parent unless otherwise specified
	child {
		hook childCommand
		help This is a child command
	}
}
```
Pretty simple. Running `/base child` will call the child command.

Since this is getting a bit long, for more info on the command manager, check out [this](https://github.com/Redempt/RedLib/blob/master/src/example/ExampleListener.java) and [this](https://github.com/Redempt/RedLib/blob/master/src/example/examplecmd.txt).

## Item utilities
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
And that's really all there is to be said about the InventoryGUI. It's simple but powerful. For more info, check out [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/inventorygui/InventoryGUI.java) and [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/inventorygui/ItemButton.java).

## Multi-block structures
This is probably a bit niche, but creating large multi-block structures can be annoying, and without a helper class, it's nearly impossible. RedLib has a very powerful and easy-to-use multi-block structure library built in to help with this.

To get started, you're first going to want to go into RedLib's `config.yml` and set the flag `devMode: true` to enable the dev tools. Once in-game, build your multi-block structure. Use the multi-block structure tool to select two corners of it, and run `/struct create [name]`. The name doesn't matter much here.

Once the structure is created, you can left-click the wand on any block and it will give you info about it. If it's not part of the structure you defined, it will simply tell you that. If it _is_ part of the structure you defined, it will tell you the name of the structure, the rotation of the structure, and whether it is flipped or not (because the utility class automatically checks for rotations of your structure). It will also tell you the relative coordinates of the block you clicked in the structure. The same block in the structure will always have the same relative coordinates, regardless of rotation, mirroring, and location. It's good to note these down if you want certain blocks to do certain things when the structure is clicked.

If you want to register it as a structure programmatically, you should run `/struct print` and copy the string it gives you. Once you have that, you can use `MultiBlockStructure.create()` and pass it that string along with a name to create the multi-block structure handler.

Once you have a `MultiBlockStructure` instance, you can use it to check for the existence of that structure anywhere in the world. By running `existsAt(Location)`, you can check if it exists at that location (this can be any block in the structure, and the structure can be rotated or mirrored horizontally). By running `getAt(Location)`, you can get an instance of `Structure`, which will be more useful than simply checking for the existence of the structure.

With a `Structure` instance, you can get relative blocks within the structure (using the relative coordinates you may or may not have noted down earlier), check its rotation/mirroring, and get all of the blocks in it (by type if you want).

For more info, check out [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/multiblock/MultiBlockStructure.java) and [this](https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/multiblock/Structure.java).
