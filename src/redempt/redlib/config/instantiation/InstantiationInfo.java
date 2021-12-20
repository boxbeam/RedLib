package redempt.redlib.config.instantiation;

import redempt.redlib.config.conversion.StringConverter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A class which holds info used for instantiation from config
 * @author Redempt
 */
public class InstantiationInfo {

	private Method postInit;
	private Field configPath;
	private StringConverter<?> configPathConverter;
	
	/**
	 * Creates an InstantiationInfo object
	 * @param postInit The post-init method to invoke, only used by {@link EmptyInstantiator}
	 * @param configPath The ConfigPath field, if one exists
	 * @param configPathConverter The converter for the ConfigPath field, if it exists
	 */
	public InstantiationInfo(Method postInit, Field configPath, StringConverter<?> configPathConverter) {
		this.postInit = postInit;
		this.configPath = configPath;
		this.configPathConverter = configPathConverter;
	}
	
	/**
	 * @return The post-init method, or null
	 */
	public Method getPostInit() {
		return postInit;
	}
	
	/**
	 *
	 * @return The config path field, or null
	 */
	public Field getConfigPath() {
		return configPath;
	}
	
	/**
	 * @return The config path type converter, or null
	 */
	public StringConverter<?> getConfigPathConverter() {
		return configPathConverter;
	}
	
}
