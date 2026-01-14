package nl.rslot.ssx.conditional.condition;

import java.util.Map;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import nl.rslot.ssx.Main;

public class CurrentServerCondition extends Condition {

	CurrentServerCondition() {
		super("current-server");
	}

	@Override
	public boolean isTrue(final Player player, final Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("server-name")) {
			throw new InvalidConfigurationException("Missing required option: 'server-name'");
		}

		final FileConfiguration configMisc = Main.getConfigurationManager().getMiscConfiguration();

		if (!configMisc.contains("server-name")) {
			throw new InvalidConfigurationException("To use the connected section, specify server-name in misc.yml");
		}

		final String serverName = (String) options.get("server-name");
		return configMisc.getString("server-name").equals(serverName);
	}
}
