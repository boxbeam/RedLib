package redempt.redlib.commandmanager;

import java.util.stream.Collectors;

/**
 * An ArgType which is dependent on another type appearing before it and uses it for conversions and tab completion
 * @param <T> The type this ArgType converts to
 * @param <K> The type this ArgType is dependent on
 */
public class ArgSubtype<T, K> extends ArgType<T> {
	
	protected ArgSubtype(String name, ArgType<?> parent, ArgConverter<T, ?> convert) {
		super(name, parent, convert);
	}
	
	/**
	 * Sets the tab completer for this type
	 * @param tab The function returning a List of all completions for this sender and previous argument
	 * @return itself
	 */
	public ArgSubtype<T, K> tab(TabCompleter<K> tab) {
		super.setTab(tab);
		return this;
	}
	
	/**
	 * Sets the tab completer for this type, can be used instead of tab
	 * @param tab The function returning a Stream of all completions for this sender and previous argument
	 * @return itself
	 */
	public ArgSubtype<T, K> tabStream(TabStreamCompleter<K> tab) {
		super.setTab((c, p, s) -> tab.tabComplete(c, (K) s, p).collect(Collectors.toList()));
		return this;
	}
	
}
