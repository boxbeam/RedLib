package redempt.redlib.itemutils;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import redempt.redlib.json.JSONList;
import redempt.redlib.json.JSONMap;
import redempt.redlib.nms.NMSHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

class ItemSerializer {
	
	private static Map<Class<?>, Function<Map<String, Object>, ?>> deserializers = new HashMap<>();
	
	private static interface EFunction<A, B> {
		
		public static <A, B> Function<A, B> wrap(EFunction<A, B> func) {
			return a -> {
				try {
					return func.apply(a);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			};
		}
		
		B apply(A a) throws Exception;
		
	}
	
	private static Object invokeDeserialize(Class<?> clazz, Map<String, Object> data) {
		find:
		if (!deserializers.containsKey(clazz)) {
			DelegateDeserialization annotation = clazz.getAnnotation(DelegateDeserialization.class);
			Class<?> target = annotation == null ? clazz : annotation.value();
			boolean found = false;
			try {
				Method method = target.getDeclaredMethod("deserialize", Map.class);
				deserializers.put(clazz, EFunction.wrap(m -> method.invoke(null, m)));
				found = true;
			} catch (NoSuchMethodException e) {}
			if (found) {
				break find;
			}
			try {
				Constructor<?> con = target.getDeclaredConstructor(Map.class);
				deserializers.put(clazz, EFunction.wrap(con::newInstance));
				found = true;
			} catch (NoSuchMethodException e) {}
			if (!found) {
				throw new IllegalStateException("No suitable deserialization method found for " + clazz);
			}
		}
		return deserializers.get(clazz).apply(data);
	}
	
	
	private static Object deserializeObject(JSONMap map) {
		try {
			Class<?> clazz = Class.forName(map.getString("==").replace("%version%", NMSHelper.getNMSVersion()));
			return invokeDeserialize(clazz, map);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Object recursiveDeserialize(Object obj) {
		if (obj instanceof JSONMap) {
			JSONMap map = (JSONMap) obj;
			map.keySet().forEach(k -> {
				map.put(k, recursiveDeserialize(map.get(k)));
			});
			if (map.containsKey("==")) {
				return deserializeObject(map);
			}
		}
		if (obj instanceof JSONList) {
			JSONList list = (JSONList) obj;
			for (int i = 0; i < list.size(); i++) {
				list.set(i, recursiveDeserialize(list.get(i)));
			}
		}
		return obj;
	}
	
	public static JSONMap toJSON(ConfigurationSerializable s, Class<?> clazz) {
		Map<String, Object> map = s.serialize();
		JSONMap json = new JSONMap();
		json.put("==", clazz.getName().replace(NMSHelper.getNMSVersion(), "%version%"));
		map.forEach((k, v) -> {
			json.put(k, serialize(v));
		});
		return json;
	}
	
	public static Object serialize(Object o) {
		if (o instanceof ConfigurationSerializable) {
			Class<?> clazz = o.getClass();
			return toJSON((ConfigurationSerializable) o, clazz);
		} else if (o instanceof Map) {
			Map map = (Map) o;
			JSONMap json = new JSONMap();
			map.forEach((k, v) -> {
				json.put(k.toString(), serialize(v));
			});
			return json;
		} else if (o instanceof List) {
			List list = (List) o;
			JSONList json = new JSONList();
			list.stream().map(ItemSerializer::serialize).forEach(json::add);
			return json;
		}
		return o;
	}
	
}
