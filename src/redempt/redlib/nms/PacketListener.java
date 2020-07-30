package redempt.redlib.nms;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import redempt.redlib.RedLib;
import redempt.redlib.misc.EventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers a packet listener for each player and intercepts packets, passing them to {@link PacketHandler}s
 */
public class PacketListener {
	
	private static boolean enabled = false;
	private static EventListener<?> joinListener = null;
	private static Map<String, List<PacketHandler>> handlers = new HashMap<>();
	
	/**
	 * Enables the PacketListener. Does nothing if already enabled. Otherwise, registers a packet listener for
	 * each player currently online, and registers an event listener to add packet listeners to any new
	 * players who join
	 */
	public static void enable() {
		if (enabled) {
			return;
		}
		enabled = true;
		Bukkit.getOnlinePlayers().forEach(PacketListener::addListener);
		joinListener = new EventListener<>(RedLib.getInstance(), PlayerJoinEvent.class, e -> {
			addListener(e.getPlayer());
		});
	}
	
	/**
	 * Disables the PacketListener. Does nothing if already disabled.
	 */
	public static void disable() {
		if (!enabled) {
			return;
		}
		Bukkit.getOnlinePlayers().forEach(PacketListener::removeListener);
		joinListener.unregister();
		joinListener = null;
		handlers.clear();
	}
	
	/**
	 * Adds a packet handler
	 * @param packetName The name of the packet, i.e. "PacketPlayOutChat"
	 * @param handler The handler for this packet
	 */
	public static void addPacketHandler(String packetName, PacketHandler handler) {
		List<PacketHandler> handlerList = handlers.get(packetName);
		if (handlerList == null) {
			handlerList = new ArrayList<>();
		}
		handlerList.add(handler);
		handlers.put(packetName, handlerList);
	}
	
	private static void addListener(Player player) {
		NMSObject play = new NMSObject(player).callMethod("getHandle");
		NMSObject networkManager = play.getField("playerConnection").getField("networkManager");
		ChannelDuplexHandler listener = new ChannelDuplexHandler() {
			
			@Override
			public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {
				NMSObject packet = new NMSObject(obj);
				String name = packet.getTypeName();
				List<PacketHandler> handlerList = handlers.get(name);
				if (handlerList == null) {
					super.channelRead(ctx, obj);
					return;
				}
				boolean cancelled = false;
				for (PacketHandler handler : handlerList) {
					if (!handler.handle(player, packet)) {
						cancelled = true;
					}
				}
				if (!cancelled) {
					super.channelRead(ctx, obj);
				}
			}
			
			@Override
			public void write(ChannelHandlerContext ctx, Object obj, ChannelPromise promise) throws Exception {
				NMSObject packet = new NMSObject(obj);
				String name = packet.getTypeName();
				List<PacketHandler> handlerList = handlers.get(name);
				if (handlerList == null) {
					super.write(ctx, obj, promise);
					return;
				}
				boolean cancelled = false;
				for (PacketHandler handler : handlerList) {
					if (!handler.handle(player, packet)) {
						cancelled = true;
					}
				}
				if (!cancelled) {
					super.write(ctx, obj, promise);
				}
			}
			
		};
		networkManager.getField("channel").callMethod("pipeline").callMethod("addBefore", "packet_handler", "redlib_packet_handler", listener);
		
	}
	
	private static void removeListener(Player player) {
		NMSObject play = new NMSObject(player).callMethod("getHandle");
		NMSObject networkManager = play.getField("playerConnection").getField("networkManager");
		networkManager.getField("channel").callMethod("pipeline").callMethod("remove", "redlib_packet_handler");
	}

}
