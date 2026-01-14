package nl.rslot.ssx.actions;

import org.bukkit.entity.Player;

import nl.rslot.ssx.ServerSelectorX;
import nl.rslot.ssx.placeholders.Server;

public class FirstAvailableServerAction extends Action {

	public FirstAvailableServerAction() {
		super("firstavailableserver", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final String[] serverNames = value.split(":");

		for (final String serverName : serverNames) {
			final Server server = ServerSelectorX.getServer(serverName);

			if (server == null) {
				continue;
			}

			if (server.getOnlinePlayers() >= server.getMaximumPlayers()) {
				continue;
			}

			ServerSelectorX.teleportPlayerToServer(player, serverName);
			return false;
		}

		return false;
	}

}
