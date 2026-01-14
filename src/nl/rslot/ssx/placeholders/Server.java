package nl.rslot.ssx.placeholders;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import nl.rslot.ssx.Main;

public class Server {

	private final String name;
	private final Map<String, Placeholder> placeholders;

	public Server(final String name, final Map<String, Placeholder> placeholders) {
		this.name = Objects.requireNonNull(name);
		this.placeholders = placeholders;
	}

	public String getName() {
		return this.name;
	}

	public Collection<Placeholder> getPlaceholders() {
		return this.placeholders.values();
	}

	public boolean hasPlaceholder(final String name) {
		return this.placeholders != null && this.placeholders.containsKey(name);
	}

	public Placeholder getPlaceholder(final String name) {
		final FileConfiguration configMisc = Main.getConfigurationManager().getMiscConfiguration();

		if (this.hasPlaceholder(name)) {
			return this.placeholders.get(name);
		}

		Main.getPlugin().getLogger().warning("Placeholder " + name + " was requested but not received from the server (" + this.getName() + ").");
		return new GlobalPlaceholder(name, configMisc.getString("placeholders.missing", "?"));
	}

	public int getOnlinePlayers() {
		return this.hasPlaceholder("online") ? Integer.parseInt(((GlobalPlaceholder) this.getPlaceholder("online")).getValue()) : 0;
	}

	public int getMaximumPlayers() {
		return this.hasPlaceholder("max") ? Integer.parseInt(((GlobalPlaceholder) this.getPlaceholder("max")).getValue()) : 0;
	}

	public String parsePlaceholders(final Player player, String string) {
		if (this.placeholders == null) {
			return string;
		}

		for (final Placeholder placeholder : this.getPlaceholders()) {
			String value;
			if (placeholder instanceof GlobalPlaceholder) {
				value = ((GlobalPlaceholder) placeholder).getValue();
			} else if (placeholder instanceof PlayerPlaceholder && player != null) {
				value = ((PlayerPlaceholder) placeholder).getValue(player);
			} else {
				continue;
			}
			string = string.replace("{" + placeholder.getKey() + "}", value);
		}
		return string;
	}

}
