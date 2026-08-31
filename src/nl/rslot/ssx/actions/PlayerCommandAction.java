package nl.rslot.ssx.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import nl.rslot.ssx.Main;
import nl.rslot.ssx.util.PlaceholderUtil;

public class PlayerCommandAction extends Action {

	public PlayerCommandAction() {
		super("playercommand", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		//Send command 2 ticks later to let the GUI close first (for commands that open a GUI)
		player.getScheduler().runDelayed(Main.getPlugin(),
				scheduledTask -> Bukkit.dispatchCommand(player, PlaceholderUtil.parsePapiPlaceholders(player, value)),
				null,
				2);
		return false;
	}

}
