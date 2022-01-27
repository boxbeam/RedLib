package redempt.redlib.config.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An arbitrary data structure which can map keys to values
 * @author Redempt
 */
public interface DataHolder {
	
	/**
	 * Unwraps the object a DataHolder wraps, if it is one
	 * @param obj The object
	 * @return The unwrapped object, or the original object if it was not a DataHolder
	 */
	public static Object unwrap(Object obj) {
		if (obj instanceof DataHolder) {
			return ((DataHolder) obj).unwrap();
		}
		return obj;
	}
	
	/**
	 * Gets the object mapped to the given path
	 * @param path The path
	 * @return The object mapped to the path
	 */
	public Object get(String path);
	
	/**
	 * Sets the object at a given path
	 * @param path The path to the object
	 * @param obj The object to set
	 */
	public void set(String path, Object obj);
	
	/**
	 * Gets an existing subsection of this DataHolder
	 * @param path The path to the data
	 * @return The subsection, or null
	 */
	public DataHolder getSubsection(String path);
	
	/**
	 * Creates a subsection of this DataHolder
	 * @param path The path of the subsection to create
	 * @return The created subsection
	 */
	public DataHolder createSubsection(String path);
	
	/**
	 * @return All valid keys
	 */
	public Set<String> getKeys();
	
	/**
	 * Checks whether a given path has a value associated
	 * @param path The path to check
	 * @return Whether the path has an associated value
	 */
	public boolean isSet(String path);
	
	/**
	 * Gets a string value from a path
	 * @param path The path to the string
	 * @return The string
	 */
	public String getString(String path);
	
	/**
	 * Gets a list subsection
	 * @param path The path to the subsection
	 * @return The list subsection, or null
	 */
	public DataHolder getList(String path);
	
	/**
	 * Removes a mapping
	 * @param path The path of the data to remove
	 */
	public void remove(String path);
	
	/**
	 * Unwraps the object this DataHolder wraps
	 * @return The wrapped storage
	 */
	public Object unwrap();
	
	/**
	 * Sets comments on the given path, if it is supported
	 * @param path The path to apply comments to
	 * @param comments The comments to apply
	 */
	public default void setComments(String path, List<String> comments, Map<String, List<String>> allComments) {}
	
}
