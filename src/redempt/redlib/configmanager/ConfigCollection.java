package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.Iterator;

public class ConfigCollection<T> implements Collection<T>, ConfigStorage {

	private Collection<T> wrapped;
	private Class<T> clazz;
	private ConfigObjectMapper<T> mapper;
	private ConfigurationSection section;
	private ConfigManager manager;
	private ConversionType type;
	
	ConfigCollection(Class<T> clazz, ConversionType type, Collection<T> wrapped) {
		this.clazz = clazz;
		this.type = type;
		this.wrapped = wrapped;
	}
	
	/**
	 * @return The wrapped collection of this ConfigCollection
	 */
	public Collection<T> getWrapped() {
		return wrapped;
	}
	
	@Override
	public void init(ConfigManager manager) {
		if (this.manager != null) {
			return;
		}
		mapper = new ConfigObjectMapper<>(clazz, type, manager);
		this.manager = manager;
	}
	
	@Override
	public void save(ConfigurationSection section) {
		int[] count = {0};
		forEach(i -> {
			mapper.save(section, count[0] + "", i);
			count[0]++;
		});
	}
	
	@Override
	public void load(ConfigurationSection section) {
		clear();
		section.getKeys(false).forEach(k -> {
			add(mapper.load(section, k));
		});
	}
	
	@Override
	public int size() {
		return wrapped.size();
	}
	
	@Override
	public boolean isEmpty() {
		return wrapped.isEmpty();
	}
	
	@Override
	public boolean contains(Object o) {
		return wrapped.contains(o);
	}
	
	@Override
	public Iterator<T> iterator() {
		return wrapped.iterator();
	}
	
	@Override
	public Object[] toArray() {
		return wrapped.toArray();
	}
	
	@Override
	public <T1> T1[] toArray(T1[] a) {
		return wrapped.toArray(a);
	}
	
	@Override
	public boolean add(T t) {
		return wrapped.add(t);
	}
	
	@Override
	public boolean remove(Object o) {
		return wrapped.remove(o);
	}
	
	@Override
	public boolean containsAll(Collection<?> c) {
		return wrapped.containsAll(c);
	}
	
	@Override
	public boolean addAll(Collection<? extends T> c) {
		return wrapped.addAll(c);
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		return wrapped.removeAll(c);
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		return wrapped.retainAll(c);
	}
	
	@Override
	public void clear() {
		wrapped.clear();
	}
	
}
