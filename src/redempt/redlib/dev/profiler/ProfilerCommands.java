package redempt.redlib.dev.profiler;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redempt.redlib.commandmanager.CommandHook;
import redempt.redlib.dev.profiler.SampleSummary.SampleMethod;
import redempt.redlib.misc.FormatUtils;

import java.util.*;
import java.util.function.Predicate;

public class ProfilerCommands {

	private static Profiler profiler = new PassiveProfiler();
	private static SampleSummary summary;
	private static boolean verbose = false;
	private static boolean showPercent = true;
	private static Set<SampleMethod> showChildren = new HashSet<>();
	private static SampleMethod selected = null;
	private static List<SampleSummary> reports = null;
	private static int childLimit = 30;
	
	public static Profiler getProfiler() {
		return profiler;
	}
	
	private TextComponent[] toMessage(SampleMethod method) {
		String text = ChatColor.GREEN + (verbose ? method.getName() : method.getShortName())
				+ "[" + method.getDepth() + "]: " + ChatColor.YELLOW +
				(showPercent ? FormatUtils.truncateDouble(method.getPrevalence()) + "%" : method.getCount() + "ms");
		TextComponent main = new TextComponent(text);
		HoverEvent hover = new HoverEvent(Action.SHOW_TEXT, new BaseComponent[] {new TextComponent(ChatColor.GREEN + "Click to select")});
		main.setHoverEvent(hover);
		ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/redlib:profiler select " + getSelector(method));
		main.setClickEvent(click);
		TextComponent expand;
		if (!showChildren.contains(method)) {
			expand = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "+ ");
			hover = new HoverEvent(Action.SHOW_TEXT, new BaseComponent[] {new TextComponent(ChatColor.GREEN + "Click to expand")});
			expand.setHoverEvent(hover);
		} else {
			expand = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "- ");
			hover = new HoverEvent(Action.SHOW_TEXT, new BaseComponent[] {new TextComponent(ChatColor.GREEN + "Click to collapse")});
			expand.setHoverEvent(hover);
		}
		click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/redlib:profiler toggleexpand " + getSelector(method));
		expand.setClickEvent(click);
		return new TextComponent[] {expand, main};
	}
	
	private String getSelector(SampleMethod method) {
		List<SampleMethod> list = summary.getMethodsByName().get(method.getName());
		return method.getName() + "=" + list.indexOf(method);
	}
	
	private SampleMethod getSampleMethod(String selector) {
		int index = selector.indexOf('=');
		if (index == -1) {
			return null;
		}
		String name = selector.substring(0, index);
		List<SampleMethod> list = summary.getMethodsByName().get(name);
		return list.get(Integer.parseInt(selector.substring(index + 1)));
	}
	
	@CommandHook("start")
	public void start(CommandSender sender) {
		profiler.start();
		sender.sendMessage(ChatColor.YELLOW + "RedLib profiler started.");
	}
	
	@CommandHook("stop")
	public void stop(CommandSender sender) {
		profiler.stop();
		sender.sendMessage(ChatColor.YELLOW + "RedLib profiler stopped.");
	}
	
	@CommandHook("summary")
	public void summary(Player sender) {
		summary = profiler.getSummary();
		selected = summary.getRoot();
		showChildren.clear();
		showSelection(sender);
	}
	
	@CommandHook("verbose")
	public void verbose(Player sender) {
		verbose = !verbose;
		showSelection(sender);
	}
	
	@CommandHook("timeformat")
	public void timeFormat(Player sender) {
		showPercent = !showPercent;
		showSelection(sender);
	}
	
	@CommandHook("root")
	public void root(Player player) {
		showChildren.clear();
		selected = summary.getRoot();
		showSelection(player);
	}
	
	@CommandHook("select")
	public void select(Player player, String selector) {
		SampleMethod method = getSampleMethod(selector);
		if (method == null) {
			player.sendMessage(ChatColor.RED + "Invalid selector!");
			return;
		}
		selected = method;
		showSelection(player);
	}
	
	@CommandHook("up")
	public void up(Player sender, int count) {
		SampleMethod method = selected;
		if (method == null) {
			sender.sendMessage(ChatColor.RED + "No profiler selection.");
			return;
		}
		for (int i = 0; i < count; i++) {
			if (method.getParent() != null) {
				method = method.getParent();
			}
		}
		selected = method;
		showSelection(sender);
	}
	
	@CommandHook("collapse")
	public void collapse(Player player) {
		showChildren.clear();
		showSelection(player);
	}
	
	@CommandHook("toggleexpand")
	public void toggleExpand(Player player, String selector) {
		SampleMethod method = getSampleMethod(selector);
		if (!showChildren.add(method)) {
			showChildren.remove(method);
		}
		showSelection(player);
	}
	
	@CommandHook("search")
	public void search(Player player, int depth, double overPercent, int overMillis, String term) {
		if (summary == null) {
			player.sendMessage(ChatColor.RED + "No selection.");
			return;
		}
		showChildren.clear();
		List<SampleMethod> results = search(selected, m -> m.getDepth() >= depth
				&& m.getPrevalence() >= overPercent && m.getCount() >= overMillis && m.getName().contains(term));
		player.sendMessage(ChatColor.YELLOW + "Search results (" + results.size() + ")");
		for (SampleMethod method : results) {
			player.spigot().sendMessage(toMessage(method)[1]);
		}
	}
	
	private List<SampleMethod> search(SampleMethod root, Predicate<SampleMethod> filter) {
		List<SampleMethod> methods = new ArrayList<>();
		Deque<SampleMethod> queue = new ArrayDeque<>();
		queue.add(root);
		while (queue.size() > 0) {
			SampleMethod method = queue.poll();
			if (filter.test(method)) {
				methods.add(method);
				continue;
			}
			queue.addAll(method.getChildren());
		}
		methods.sort(Comparator.comparingDouble(SampleMethod::getPrevalence).reversed());
		return methods;
	}
	
	@CommandHook("setminimum")
	public void setMinimum(CommandSender sender, int ticks) {
		TickMonitorProfiler.setTickMinimum(ticks);
		sender.sendMessage(ChatColor.YELLOW + "Tick monitor minimum duration set.");
	}
	
	@CommandHook("clear")
	public void clear(CommandSender sender) {
		TickMonitorProfiler.clear();
		sender.sendMessage(ChatColor.YELLOW + "Tick monitor reports cleared.");
	}
	
	@CommandHook("startmonitor")
	public void startMonitor(CommandSender sender, Integer minimum) {
		if (minimum != null) {
			TickMonitorProfiler.setTickMinimum(minimum);
		}
		TickMonitorProfiler.start();
		sender.sendMessage(ChatColor.YELLOW + "Tick monitor profiler started.");
	}
	
	@CommandHook("reports")
	public void showReports(Player player) {
		reports = new ArrayList<>();
		reports.addAll(TickMonitorProfiler.getReports());
		player.sendMessage(ChatColor.YELLOW + "Tick Monitor Reports (" + reports.size() + ")");
		for (int i = 0; i < reports.size(); i++) {
			SampleSummary report = reports.get(i);
			TextComponent component = new TextComponent(ChatColor.RED + "" + report.getDuration() + "ms tick " + ChatColor.YELLOW
					+ FormatUtils.formatTimeOffset(System.currentTimeMillis() - report.getEnd()) + " ago");
			HoverEvent hover = new HoverEvent(Action.SHOW_TEXT, new BaseComponent[] {new TextComponent(ChatColor.GREEN + "Click to select")});
			component.setHoverEvent(hover);
			ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/redlib:profiler monitor select " + i);
			component.setClickEvent(click);
			player.spigot().sendMessage(component);
		}
	}
	
	@CommandHook("selectreport")
	public void selectReport(Player player, int report) {
		summary = reports.get(report);
		selected = summary.getRoot();
		showChildren.clear();
		showSelection(player);
	}
	
	@CommandHook("limit")
	public void setLimit(Player player, int limit) {
		childLimit = limit;
		showSelection(player);
	}
	
	private void showSelection(Player player) {
		if (selected == null) {
			player.sendMessage(ChatColor.RED + "No profiler selection.");
			return;
		}
		TextComponent component = new TextComponent(ChatColor.RED + "===========================");
		HoverEvent hover = new HoverEvent(Action.SHOW_TEXT, new BaseComponent[] {new TextComponent(ChatColor.GREEN + "Click to go up")});
		component.setHoverEvent(hover);
		ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/redlib:profiler up");
		component.setClickEvent(click);
		player.spigot().sendMessage(component);
		showSelection(player, selected, selected.getDepth());
	}
	
	private String repeat(int times) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < times; i++) {
			builder.append(' ');
		}
		return builder.toString();
	}
	
	private void showSelection(Player player, SampleMethod method, int depth) {
		TextComponent[] components = toMessage(method);
		components[0].setText(repeat(method.getDepth() - depth) + components[0].getText());
		player.spigot().sendMessage(components);
		if (showChildren.contains(method)) {
			method.getChildren().stream().sorted(Comparator.comparingDouble(SampleMethod::getPrevalence).reversed())
					.limit(childLimit).forEach(m -> showSelection(player, m, depth));
		}
	}
	
}
