package redempt.redlib.misc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import redempt.redlib.RedLib;

public class ChatPrompt implements Listener {
	
	private static Map<Player, Prompt> prompts = new HashMap<>();
	
	static {
		new ChatPrompt();
	}
	
	/**
	 * Prompts a player with callbacks for player response and cancelling
	 * @param player The player to prompt
	 * @param prompt The prompt to send to the player
	 * @param onResponse The callback for when the player responds
	 * @param onCancel The callback for when the prompt is cancelled
	 */
	public static void prompt(Player player, String prompt, Consumer<String> onResponse, Consumer<CancelReason> onCancel) {
		Prompt removed = prompts.remove(player);
		if (removed != null) {
			removed.cancel(CancelReason.PROMPT_OVERRIDDEN);
		}
		prompts.put(player, new Prompt(onResponse, onCancel));
		player.sendMessage(prompt);
		player.sendMessage(RedLib.getMessage("cancelPromptMessage").replace("%cancel%", RedLib.getMessage("cancelText")));
	}
	
	/**
	 * Prompts a player with callbacks for player response and cancelling
	 * @param player The player to prompt
	 * @param prompt The prompt to send to the player
	 * @param onResponse The callback for when the player responds
	 */
	public static void prompt(Player player, String prompt, Consumer<String> onResponse) {
		prompt(player, prompt, onResponse, c -> {});
	}
	
	private ChatPrompt() {
		Bukkit.getPluginManager().registerEvents(this, RedLib.plugin);
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		Prompt p = prompts.remove(e.getPlayer());
		if (p == null) {
			return;
		}
		e.setCancelled(true);
		if (e.getMessage().equalsIgnoreCase(RedLib.getMessage("cancelText"))) {
			p.cancel(CancelReason.PLAYER_CANCELLED);
			return;
		}
		p.respond(e.getMessage());
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Prompt p = prompts.remove(e.getPlayer());
		if (p != null) {
			p.cancel(CancelReason.PLAYER_LEFT);
		}
	}
	
	private static class Prompt {
		
		private Consumer<String> onResponse;
		private Consumer<CancelReason> onCancel;
		
		public Prompt(Consumer<String> onResponse, Consumer<CancelReason> onCancel) {
			this.onResponse = onResponse;
			this.onCancel = onCancel;
		}
		
		public void respond(String response) {
			onResponse.accept(response);
		}
		
		public void cancel(CancelReason reason) {
			onCancel.accept(reason);
		}
		
	}
	
	public enum CancelReason {
		/**
		 * Passed when the player was given another prompt. This prompt is removed and cancelled.
		 */
		PROMPT_OVERRIDDEN,
		/**
		 * Passed when the prompt was cancelled because the player typed 'cancel'.
		 */
		PLAYER_CANCELLED,
		/**
		 * Passed when the prompt was cancelled because the player left the server.
		 */
		PLAYER_LEFT
	}
	
}