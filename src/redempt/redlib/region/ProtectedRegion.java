package redempt.redlib.region;

import redempt.redlib.region.ProtectionPolicy.ProtectionType;

/**
 * Represents a Region which has been protected using a ProtectionPolicy
 * @author Redempt
 *
 */
public class ProtectedRegion {
	
	private Region region;
	private ProtectionPolicy policy;
	
	protected ProtectedRegion(Region region, ProtectionType... types) {
		this.region = region;
		this.policy = new ProtectionPolicy(b -> region.isInside(b.getLocation()), types);
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