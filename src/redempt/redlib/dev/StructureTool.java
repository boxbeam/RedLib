package redempt.redlib.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import redempt.redlib.RedLib;
import redempt.redlib.commandmanager.CommandHook;
import redempt.redlib.itemutils.ItemBuilder;
import redempt.redlib.multiblock.MultiBlockStructure;
import redempt.redlib.multiblock.Structure;
import redempt.redlib.multiblock.Structure.StructureBlock;

public class StructureTool implements Listener {
	
	public static ItemStack getTool() {
		return new ItemBuilder(Material.BLAZE_ROD)
				.setName(ChatColor.GREEN + "Multi-Block Structure Tool")
				.addLore(ChatColor.BLUE + "Right-click two corners of a structure")
				.addLore(ChatColor.BLUE + "and type '/struct create' to create a test")
				.addLore(ChatColor.BLUE + "structure. Type '/struct export' to get the")
				.addLore(ChatColor.BLUE + "data string for the structure.");
	}
	
	public static StructureTool enable() {
		StructureTool tool = new StructureTool();
		Bukkit.getPluginManager().registerEvents(tool, RedLib.plugin);
		return tool;
	}
	
	private Map<UUID, Location[]> locations = new HashMap<>();
	private Map<UUID, MultiBlockStructure> structures = new HashMap<>();
	
	private StructureTool() {}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (!getTool().equals(e.getItem())) {
			return;
		}
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Location[] locs = locations.get(e.getPlayer().getUniqueId());
			if (locs == null) {
				locs = new Location[2];
			}
			if (locs[0] == null) {
				locs[0] = e.getClickedBlock().getLocation();
				locations.put(e.getPlayer().getUniqueId(), locs);
				e.getPlayer().sendMessage(ChatColor.GREEN + "First location set!");
			} else if (locs[1] == null) {
				locs[1] = e.getClickedBlock().getLocation();
				locations.put(e.getPlayer().getUniqueId(), locs);
				e.getPlayer().sendMessage(ChatColor.GREEN + "Second location set!");
				e.getPlayer().sendMessage(ChatColor.GREEN + "Use '/structure create' to register this structure!");
			} else {
				locs[0] = e.getClickedBlock().getLocation();
				locs[1] = null;
				locations.put(e.getPlayer().getUniqueId(), locs);
				e.getPlayer().sendMessage(ChatColor.GREEN + "First location set!");
			}
		}
		if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
			e.setCancelled(true);
			MultiBlockStructure structure = structures.get(e.getPlayer().getUniqueId());
			if (structure == null) {
				return;
			}
			Structure struct = structure.getAt(e.getClickedBlock().getLocation());
			if (struct == null) {
				e.getPlayer().sendMessage(ChatColor.RED + "Your registered structure does not exist at this block.");
				return;
			}
			e.getPlayer().sendMessage(ChatColor.GREEN + "-----");
			e.getPlayer().sendMessage(ChatColor.GREEN + "Structure name: " + ChatColor.WHITE + struct.getType().getName());
			e.getPlayer().sendMessage(ChatColor.GREEN + "Structure rotation: " + struct.getRotator().getRotation());
			e.getPlayer().sendMessage(ChatColor.GREEN + "Structure mirrored: " + struct.getRotator().isMirrored());
			StructureBlock block = struct.getBlock(e.getClickedBlock());
			e.getPlayer().sendMessage(ChatColor.GREEN + "Relative block X: " + block.getRelativeX());
			e.getPlayer().sendMessage(ChatColor.GREEN + "Relative block Y: " + block.getRelativeY());
			e.getPlayer().sendMessage(ChatColor.GREEN + "Relative block Z: " + block.getRelativeZ());
		}
	}
	
	@CommandHook("wand")
	public void giveWand(Player player) {
		player.getInventory().addItem(getTool());
	}
	
	@CommandHook("create")
	public void createStructure(Player player, String name) {
		Location[] locs = locations.get(player.getUniqueId());
		if (locs[0] == null || locs[1] == null) {
			player.sendMessage(ChatColor.RED + "You must set 2 locations with the structure wand (/struct wand) first!");
			return;
		}
		if (!locs[0].getWorld().equals(locs[1].getWorld())) {
			player.sendMessage(ChatColor.RED + "Locations must be in the same world!");
			return;
		}
		MultiBlockStructure mbs = MultiBlockStructure.create(MultiBlockStructure.stringify(locs[0], locs[1]), name, false, true);
		structures.put(player.getUniqueId(), mbs);
		player.sendMessage(ChatColor.GREEN + "Structure registered! Left click it with your wand to get debug info.");
	}
	
	@SuppressWarnings("deprecation")
	@CommandHook("export")
	public void export(Player player) {
		Location[] locs = locations.get(player.getUniqueId());
		if (locs[0] == null || locs[1] == null) {
			player.sendMessage(ChatColor.RED + "You must set 2 locations with the structure wand (/struct wand) first!");
			return;
		}
		if (!locs[0].getWorld().equals(locs[1].getWorld())) {
			player.sendMessage(ChatColor.RED + "Locations must be in the same world!");
			return;
		}
		player.sendMessage(ChatColor.GREEN + "Scanning blocks...");
		Bukkit.getScheduler().scheduleAsyncDelayedTask(RedLib.plugin, () -> {
			String mbs = MultiBlockStructure.stringify(locs[0], locs[1]);
			player.sendMessage(ChatColor.GREEN + "The multi-block structure string has been exported to plugins/RedLib/structure.dat.");
			try {
				Path path = Paths.get("plugins/RedLib/structure.dat");
				Files.write(path, mbs.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
}
