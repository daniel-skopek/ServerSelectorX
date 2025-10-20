package xyz.derkades.serverselectorx.conditional.condition;

import java.util.Map;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import xyz.derkades.serverselectorx.ServerSelectorX;
import xyz.derkades.serverselectorx.placeholders.Server;

public class ServerOnlineCondition extends Condition {

	ServerOnlineCondition() {
		super("server-online");
	}

	@Override
	public boolean isTrue(final Player player, final Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("server-name")) {
			throw new InvalidConfigurationException("Option 'server-name' missing, this is required to use the 'server-online' condition");
		}

		final String serverName = (String) options.get("server-name");
		final Server server = ServerSelectorX.getServer(serverName);

		return server.isOnline();
	}
}
