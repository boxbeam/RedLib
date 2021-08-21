package redempt.redlib.region;

import org.bukkit.Bukkit;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import redempt.redlib.RedLib;
import redempt.redlib.misc.EventListener;
import redempt.redlib.region.events.RegionEnterEvent;
import redempt.redlib.region.events.RegionEnterEvent.EnterCause;
import redempt.redlib.region.events.RegionExitEvent;
import redempt.redlib.region.events.RegionExitEvent.ExitCause;

/**
 * @author Redempt
 */
public class RegionEnterExitListener {
	
	private static RegionMap<Region> regionMap = new RegionMap<>();
	
	static {
		register();
	}
	
	private static void register() {
		new EventListener<>(RedLib.getInstance(), PlayerMoveEvent.class, e -> {
			regionMap.get(e.getFrom()).forEach(r -> {
				if (r.contains(e.getFrom()) && !r.contains(e.getTo())) {
					Bukkit.getPluginManager().callEvent(new RegionExitEvent(e.getPlayer(), r, ExitCause.MOVE, e));
				}
			});
			regionMap.get(e.getTo()).forEach(r -> {
				if (!r.contains(e.getFrom()) && r.contains(e.getTo())) {
					Bukkit.getPluginManager().callEvent(new RegionEnterEvent(e.getPlayer(), r, EnterCause.MOVE, e));
				}
			});
		});
		new EventListener<>(RedLib.getInstance(), PlayerTeleportEvent.class, e -> {
			regionMap.get(e.getFrom()).forEach(r -> {
				if (r.contains(e.getFrom()) && !r.contains(e.getTo())) {
					Bukkit.getPluginManager().callEvent(new RegionExitEvent(e.getPlayer(), r, ExitCause.TELEPORT, e));
				}
			});
			regionMap.get(e.getTo()).forEach(r -> {
				if (!r.contains(e.getFrom()) && r.contains(e.getTo())) {
					Bukkit.getPluginManager().callEvent(new RegionEnterEvent(e.getPlayer(), r, EnterCause.TELEPORT, e));
				}
			});
		});
		new EventListener<>(RedLib.getInstance(), PlayerQuitEvent.class, e -> {
			regionMap.get(e.getPlayer().getLocation()).forEach(r -> {
				if (r.contains(e.getPlayer().getLocation())) {
					Bukkit.getPluginManager().callEvent(new RegionExitEvent(e.getPlayer(), r, ExitCause.QUIT, null));
				}
			});
		});
		new EventListener<>(RedLib.getInstance(), PlayerJoinEvent.class, e -> {
			regionMap.get(e.getPlayer().getLocation()).forEach(r -> {
				if (r.contains(e.getPlayer().getLocation())) {
					Bukkit.getPluginManager().callEvent(new RegionEnterEvent(e.getPlayer(), r, EnterCause.JOIN, null));
				}
			});
		});
		new EventListener<>(RedLib.getInstance(), PlayerDeathEvent.class, e -> {
			regionMap.get(e.getEntity().getLocation()).forEach(r -> {
				if (r.contains(e.getEntity().getLocation())) {
					Bukkit.getPluginManager().callEvent(new RegionExitEvent(e.getEntity(), r, ExitCause.DEATH, null));
				}
			});
		});
		new EventListener<>(RedLib.getInstance(), PlayerRespawnEvent.class, e -> {
			regionMap.get(e.getPlayer().getLocation()).forEach(r -> {
				if (r.contains(e.getPlayer().getLocation())) {
					Bukkit.getPluginManager().callEvent(new RegionEnterEvent(e.getPlayer(), r, EnterCause.RESPAWN, null));
				}
			});
		});
	}
	
	protected static RegionMap<Region> getRegionMap() {
		return regionMap;
	}
	
}
