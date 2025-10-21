package xyz.derkades.serverselectorx.placeholders;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import xyz.derkades.serverselectorx.Main;
import xyz.derkades.serverselectorx.ServerSelectorX;

public class PapiExpansion extends PlaceholderExpansion {

	@Override
	public String onPlaceholderRequest(final Player player, String identifier) {
		if (identifier.startsWith("server_")) {
			identifier = identifier.substring(7); // Remove 'server_'
			final StringBuilder serverName = new StringBuilder();
			for (final char c : identifier.toCharArray()) {
				if (c == '_') {
					break;
				}
				serverName.append(c);
			}

			// Remove the server name and the underscore after it
			final String placeholderName = identifier.substring(serverName.length() + 1);

			final Server server = ServerSelectorX.getServer(serverName.toString());

			if (placeholderName.equals("status")) {
				final String status = server != null ? "online" : "offline";
				return Main.getConfigurationManager().getMiscConfiguration().getString("placeholders.status-" + status, status);
			} else if (server != null) {
				final Placeholder placeholder = server.getPlaceholder(placeholderName);
				if (placeholder instanceof PlayerPlaceholder) {
					return ((PlayerPlaceholder) placeholder).getValue(player);
				} else {
					return ((GlobalPlaceholder) placeholder).getValue();
				}
			} else {
				return "";
			}
		}

		return null;
	}

	@Override
	public @NotNull String getAuthor() {
		return String.join(", ", Main.getPlugin().getDescription().getAuthors());
	}

	@Override
	public @NotNull String getIdentifier() {
		return "ssx";
	}

	@Override
	public @NotNull String getVersion() {
		return Main.getPlugin().getDescription().getVersion();
	}

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

}
