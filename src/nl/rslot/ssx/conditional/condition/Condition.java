package nl.rslot.ssx.conditional.condition;

import java.util.Map;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

public abstract class Condition {

	private final String type;

	Condition(final String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	public abstract boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException;

}
