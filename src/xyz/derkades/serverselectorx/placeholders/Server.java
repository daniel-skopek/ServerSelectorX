package xyz.derkades.serverselectorx.placeholders;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.Main;

public class Server {

	private final String name;
	private transient long lastInfoTime = 0;
	private Map<String, Placeholder> placeholders;

	public Server(final String name) {
		this.name = Objects.requireNonNull(name, "Server name is null");
	}

	public String getName() {
		return this.name;
	}

	public long getTimeSinceLastMessage() {
		return System.currentTimeMillis() - this.lastInfoTime;
	}

	public boolean isOnline() {
		final int timeout = Main.getConfigurationManager().getApiConfiguration().getInt("server-offline-timeout", 6000);
		return this.getTimeSinceLastMessage() < timeout;
	}

	public Collection<Placeholder> getPlaceholders() {
		return this.placeholders.values();
	}

	public boolean hasPlaceholder(final String name) {
		return this.placeholders != null && this.placeholders.containsKey(name);
	}

	public Placeholder getPlaceholder(final String name) {
		final FileConfiguration configMisc = Main.getConfigurationManager().getMiscConfiguration();

		if (!this.isOnline()) {
			return new GlobalPlaceholder(name, configMisc.getString("placeholders.offline", "-"));
		}

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

	public void updatePlaceholders(final Map<String, Placeholder> placeholders) {
		this.placeholders = placeholders;
		this.lastInfoTime = System.currentTimeMillis();
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
