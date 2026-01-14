package nl.rslot.ssx.actions;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import nl.rslot.ssx.ServerSelectorX;
import nl.rslot.ssx.placeholders.Server;

public class RoundRobinServerAction extends Action {

	private static final int MAX_ATTEMPTS = 20;

	private static final Map<String, Integer> LAST_USED_SERVER = new HashMap<>();
	private static final Map<String, Integer> ATTEMPTS = new HashMap<>();

	public RoundRobinServerAction() {
		super("roundrobinserver", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final String[] serverNames = value.split(":");

		if (!LAST_USED_SERVER.containsKey(value)) {
			// This action has never been used before, use first server
			this.teleportIfPossible(player, serverNames[0], value);
			LAST_USED_SERVER.put(value, 0);
		} else {
			int index = LAST_USED_SERVER.get(value) + 1;

			if (serverNames.length >= index) {
				index = 0; // Start from the beginning!
			}

			this.teleportIfPossible(player, serverNames[index], value);
			LAST_USED_SERVER.put(value, index);
		}

		return false;
	}

	private void teleportIfPossible(final Player player, final String serverName, final String value) {
		// Prevent a stack overflow error when all servers are offline or full
		if (ATTEMPTS.containsKey(value)) {
			final int attempts = ATTEMPTS.get(value);
			if (attempts >= MAX_ATTEMPTS) {
				return;
			}

			ATTEMPTS.put(value, attempts + 1);
		} else {
			ATTEMPTS.put(value, 1);
		}

		final Server server = ServerSelectorX.getServer(serverName);
		if (server == null) {
			this.apply(player, value);
		}

		if (server.getOnlinePlayers() >= server.getMaximumPlayers()) {
			this.apply(player, value);
		}

		ServerSelectorX.teleportPlayerToServer(player, serverName);
	}

}
