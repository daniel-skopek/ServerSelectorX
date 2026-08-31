package nl.rslot.ssx.actions;

import org.bukkit.entity.Player;

import nl.rslot.ssx.Main;

public class DelayAction extends Action {

	public DelayAction() {
		super("delay", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final String timeString = value.split(":")[0];
		final String actionString = value.substring(timeString.length() + 1);

		int delay;
		try {
			delay = Integer.parseInt(timeString);
		} catch (final NumberFormatException e) {
			player.sendMessage("Invalid number " + timeString);
			return false;
		}

		player.getScheduler().runDelayed(Main.getPlugin(), scheduledTask -> Action.runAction(player, actionString), null, delay);
		return false;
	}

}
