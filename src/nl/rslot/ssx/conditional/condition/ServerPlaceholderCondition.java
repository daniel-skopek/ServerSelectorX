package nl.rslot.ssx.conditional.condition;

import java.util.Map;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import nl.rslot.ssx.Main;
import nl.rslot.ssx.ServerSelectorX;
import nl.rslot.ssx.placeholders.GlobalPlaceholder;
import nl.rslot.ssx.placeholders.Placeholder;
import nl.rslot.ssx.placeholders.PlayerPlaceholder;
import nl.rslot.ssx.placeholders.Server;

public class ServerPlaceholderCondition extends Condition {

	ServerPlaceholderCondition() {
		super("server-placeholder");
	}

	@Override
	public boolean isTrue(final Player player, final Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("server-name")) {
			throw new InvalidConfigurationException("Missing requried option: 'server-name'");
		}

		if (!options.containsKey("placeholder-name")) {
			throw new InvalidConfigurationException("Missing required option 'placeholder-name' (placeholder name, no %)");
		}

		if (!options.containsKey("placeholder-value")) {
			throw new InvalidConfigurationException("Missing required option 'placeholder-value' (expected placeholder value)");
		}

		if (!(options.get("placeholder-value") instanceof String)) {
			throw new InvalidConfigurationException("Invalid type for placeholder-value option. It has to be a string, but it is: " + options.get("placeholder-value").getClass().getSimpleName());
		}

		final String placeholderName = (String) options.get("placeholder-name");
		final String expectedPlaceholderValue = (String) options.get("placeholder-value");

		if (placeholderName.contains("%")) {
			throw new InvalidConfigurationException("Placeholder name must not contain percentage symbols");
		}

		final String serverName = (String) options.get("server-name");
		final Server server = ServerSelectorX.getServer(serverName);

		if (server == null) {
			Main.getPlugin().getLogger().warning(String.format(
					"Cannot obtain placeholder %s for server %s, the server is offline. Consider adding condition checking if the server is online.",
					placeholderName, serverName));
			return false;
		}

		final Placeholder placeholder = server.getPlaceholder(placeholderName);
		String actualPlaceholderValue;
		if (placeholder instanceof GlobalPlaceholder) {
			actualPlaceholderValue = ((GlobalPlaceholder) placeholder).getValue();
		} else if (placeholder instanceof PlayerPlaceholder) {
			actualPlaceholderValue = ((PlayerPlaceholder) placeholder).getValue(player);
		} else {
			throw new IllegalStateException();
		}

		actualPlaceholderValue = server.parsePlaceholders(player, actualPlaceholderValue);

		final String comparisonMode = (String) options.getOrDefault("placeholder-comparison", "equals");

		return comparisonMode.equals("equals") && expectedPlaceholderValue.equals(actualPlaceholderValue) ||
				comparisonMode.equals("less") && Double.parseDouble(expectedPlaceholderValue) > Double.parseDouble(actualPlaceholderValue) ||
				comparisonMode.equals("more") && Double.parseDouble(expectedPlaceholderValue) < Double.parseDouble(actualPlaceholderValue);
	}
}
