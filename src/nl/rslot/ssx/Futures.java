package nl.rslot.ssx;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.bukkit.entity.Player;

public class Futures {

	// From PlaceholderAPI, licensed under GPLv3.
	// Find the original source code and full license text here:
	// https://github.com/PlaceholderAPI/PlaceholderAPI
	public static <T> void whenCompleteOnMainThread(final Player player, final CompletableFuture<T> future, final BiConsumer<T, Throwable> consumer) {
		future.whenComplete((value, exception) ->
				player.getScheduler().run(Main.getPlugin(), scheduledTask -> consumer.accept(value, exception), null));
	}

}
