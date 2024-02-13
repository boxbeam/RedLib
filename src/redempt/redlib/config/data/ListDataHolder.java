package redempt.redlib.config.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ListDataHolder implements DataHolder {

    private List<Object> list;

    public ListDataHolder(List<?> list) {
        this.list = (List<Object>) list;
    }

    public ListDataHolder() {
        this(new ArrayList<>());
    }

    @Override
    public Object get(String path) {
        int index = Integer.parseInt(path);
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    @Override
    public void set(String path, Object obj) {
        int index = Integer.parseInt(path);
        obj = DataHolder.unwrap(obj);
        if (index >= list.size()) {
            list.add(obj);
        } else {
            list.set(index, obj);
        }
    }

    @Override
    public DataHolder getSubsection(String path) {
        Object obj = get(path);
        return obj instanceof Map ? new MapDataHolder((Map<String, Object>) obj) : null;
    }

    @Override
    public DataHolder createSubsection(String path) {
        int index = Integer.parseInt(path);
        MapDataHolder holder = new MapDataHolder();
        if (index >= list.size()) {
            list.add(holder.unwrap());
        } else {
            list.set(index, holder.unwrap());
        }
        return holder;
    }

    @Override
    public Set<String> getKeys() {
        return IntStream.range(0, list.size()).mapToObj(String::valueOf).collect(Collectors.toSet());
    }

    @Override
    public boolean isSet(String path) {
        int index = Integer.parseInt(path);
        return index > 0 && index < list.size();
    }

    @Override
    public String getString(String path) {
        Object val = get(path);
        return val == null ? null : String.valueOf(val);
    }

    @Override
    public DataHolder getList(String path) {
        Object obj = get(path);
        return obj instanceof List ? new ListDataHolder((List<?>) obj) : null;
    }

    @Override
    public void remove(String path) {
        list.remove(Integer.parseInt(path));
    }

    @Override
    public Object unwrap() {
        return list;
    }

}
