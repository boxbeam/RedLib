package redempt.redlib.enchants;

import org.bukkit.event.Event;

import java.util.function.BiConsumer;

class EnchantListener<T extends Event> {
	
	private BiConsumer<T, Integer> activate;
	private BiConsumer<T, Integer> deactivate;
	
	public EnchantListener(BiConsumer<T, Integer> activate, BiConsumer<T, Integer> deactivate) {
		this.activate = activate;
		this.deactivate = deactivate;
	}
	
	public void activate(Event event, int level) {
		activate.accept((T) event, level);
	}
	
	public void deactivate(Event event, int level) {
		deactivate.accept((T) event, level);
	}
	
}
