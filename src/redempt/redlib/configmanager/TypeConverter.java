package redempt.redlib.configmanager;

import java.util.function.Function;

class TypeConverter<T> {
	
	private Function<String, T> load;
	private Function<T, String> save;
	
	public TypeConverter(Function<String, T> load, Function<T, String> save) {
		this.load = load;
		this.save = save;
	}
	
	public T load(String string) {
		return load.apply(string);
	}
	
	public String save(Object obj) {
		return save.apply((T) obj);
	}
	
}
