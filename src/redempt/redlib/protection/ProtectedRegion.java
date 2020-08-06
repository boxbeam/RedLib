package redempt.redlib.protection;

import org.bukkit.plugin.Plugin;
import redempt.redlib.protection.ProtectionPolicy.ProtectionType;
import redempt.redlib.region.Region;

/**
 * Represents a Region which has been protected using a ProtectionPolicy
 * @author Redempt
 *
 */
public class ProtectedRegion {
	
	private Region region;
	private ProtectionPolicy policy;
	
	public ProtectedRegion(Plugin plugin, Region region, ProtectionType... types) {
		this.region = region;
		this.policy = new ProtectionPolicy(plugin, region, b -> region.contains(b.getLocation()), types);
	}
	
	public ProtectedRegion(Region region, ProtectionType... types) {
		this.region = region;
		this.policy = new ProtectionPolicy(region, b -> region.contains(b.getLocation()), types);
	}
	
	/**
	 * @return The region being protected
	 */
	public Region getRegion() {
		return region;
	}
	
	/**
	 * @return The {@link ProtectionPolicy} protecting the region
	 */
	public ProtectionPolicy getPolicy() {
		return policy;
	}
	
	/**
	 * Disables all protections for this region
	 */
	public void unprotect() {
		policy.disable();
	}
	
}