package redempt.redlib.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import redempt.redlib.RedLib;

/**
 * Represents a number of floating armor stands intended to display information
 * @author Redempt
 *
 */
public class Hologram {
	
	private static Objective objective;
	
	static {
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		objective = scoreboard.getObjective("hologramID");
		if (objective == null) {
			objective = scoreboard.registerNewObjective("hologramID", "dummy");
		}
	}
	
	/**
	 * Creates a Hologram
	 * @param loc The location to create the Hologram at
	 * @param lines The lines of text for this Hologram
	 * @return The Hologram that was created
	 */
	public static Hologram create(Location loc, String... lines) {
		Random random = new Random();
		Hologram hologram = new Hologram(random.nextInt(Integer.MAX_VALUE) + 1, loc.clone());
		for (String line : lines) {
			hologram.append(line);
		}
		return hologram;
	}
	
	/**
	 * Attempts to get a Hologram at a specified location
	 * @param loc The location to check at
	 * @return The Hologram found at the location, or null if none was found
	 */
	public static Hologram getAt(Location loc) {
		List<ArmorStand> stands = new ArrayList<>();
		loc.getChunk().load();
		Arrays.stream(loc.getChunk().getEntities())
			.filter(e -> e instanceof ArmorStand)
			.map(e -> (ArmorStand) e)
			.filter(s -> getId(s) != 0)
			.forEach(stands::add);
		if (stands.size() == 0) {
			return null;
		}
		double dist = Double.MAX_VALUE;
		ArmorStand closest = null;
		for (ArmorStand stand : stands) {
			double distance = stand.getLocation().distanceSquared(loc);
			if (distance < dist) {
				dist = distance;
				closest = stand;
			}
		}
		if (dist > 1) {
			return null;
		}
		int id = getId(closest);
		stands.removeIf(s -> getId(s) != id);
		stands.sort((a, b) -> (int) Math.signum(b.getLocation().getY() - a.getLocation().getY()));
		Hologram hologram = new Hologram(id, stands.get(0).getLocation(), stands);
		return hologram;
	}
	
	private int id;
	private List<EntityPersistor<ArmorStand>> stands = new ArrayList<>();
	private Location start;
	private double lineSpacing = 0.35;
	private int task = -1;
	private int iter = -1;
	
	private static int getId(ArmorStand stand) {
		return objective.getScore(stand.getUniqueId().toString()).getScore();
	}
	
	private static void setId(ArmorStand stand, int id) {
		objective.getScore(stand.getUniqueId().toString()).setScore(id);
	}
	
	private Hologram(int id, Location start) {
		this.id = id;
		this.start = start;
	}
	
	private Hologram(int id, Location start, List<ArmorStand> stands) {
		this.id = id;
		this.stands.clear();
		stands.stream().map(EntityPersistor::wrap).forEach(this.stands::add);
		this.start = start;
	}
	
	private void fixStands(int start) {
		if (stands.size() == 0) {
			return;
		}
		Location loc = this.start.clone();
		List<String> lines = new ArrayList<>(this.getStands().size());
		for (ArmorStand stand : getStands()) {
			lines.add(stand.getName());
		}
		clear();
		for (int i = 0; i < lines.size(); i++) {
			append(lines.get(i));
		}
	}
	
	/**
	 * @param line The index of the line
	 * @return The line of text at the given index
	 */
	public String getLine(int line) {
		return stands.get(line).get().getCustomName();
	}
	
	/**
	 * Moves this Hologram
	 * @param loc The location to move this Hologram to
	 */
	public void move(Location loc) {
		start = loc.clone();
		fixStands(1);
	}
	
	/**
	 * @return The location of the top of this Hologram
	 */
	public Location getLocation() {
		return start;
	}
	
	/**
	 * @return All the ArmorStands in this Hologram
	 */
	public List<ArmorStand> getStands() {
		return stands.stream().map(EntityPersistor::get).collect(Collectors.toList());
	}
	
	/**
	 * Sets the text for a line of this Hologram
	 * @param line The index of the line to set
	 * @param text The text to set the line to
	 */
	public void setLine(int line, String text) {
		stands.get(line).get().setCustomName(text);
	}
	
	/**
	 * Removes a line from this Hologram
	 * @param line The line number to remove
	 */
	public void remove(int line) {
		ArmorStand stand = stands.remove(line).get();
		stand.remove();
		fixStands(0);
	}
	
	/**
	 * Clears this Hologram
	 */
	public void clear() {
		stands.forEach(s -> s.get().remove());
		stands.clear();
	}
	
	/**
	 * @return The vertical distance between each line in this Hologram
	 */
	public double getLineSpacing() {
		return lineSpacing;
	}
	
	/**
	 * Sets the vertical distance between each line in this Hologram
	 * @param lineSpacing The line spacing to set
	 */
	public void setLineSpacing(double lineSpacing) {
		this.lineSpacing = lineSpacing;
		fixStands(0);
	}
	
	/**
	 * Adds a line at the bottom of this Hologram
	 * @param text The text to append
	 */
	public void append(String text) {
		ArmorStand stand = spawn(stands.size(), text);
		stands.add(EntityPersistor.wrap(stand));
	}
	
	/**
	 * Adds a line at the top of this Hologram
	 * @param text The text to add
	 */
	public void prepend(String text) {
		insert(0, text);
	}
	
	/**
	 * Inserts a line in this Hologram
	 * @param line The position to insert at
	 * @param text The text to insert
	 */
	public void insert(int line, String text) {
		ArmorStand stand = spawn(line, text);
		stands.add(line, EntityPersistor.wrap(stand));
		fixStands(line - 1);
	}
	
	/**
	 * @return The number of lines in this Hologram
	 */
	public int size() {
		return stands.size();
	}
	
	private ArmorStand spawn(int line, String text) {
		Location loc;
		if (stands.size() > 0) {
			loc = stands.get(0).get().getLocation();
			loc.subtract(0, lineSpacing * (double) line, 0);
		} else {
			loc = start;
		}
		ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		initiate(stand, text);
		return stand;
	}
	
	private void initiate(ArmorStand stand, String text) {
		stand.setCustomName(text);
		stand.setCustomNameVisible(true);
		stand.setVisible(false);
		stand.setGravity(false);
		setId(stand, id);
	}
	
}
