package nl.rslot.ssx.conditional.condition;

import java.util.Map;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class Condition {

	private final @NotNull String type;

	Condition(final @NotNull String type) {
		this.type = type;
	}

	public @NotNull String getType() {
		return this.type;
	}

	public abstract boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException;

}
