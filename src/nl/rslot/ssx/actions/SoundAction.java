package nl.rslot.ssx.actions;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundAction extends Action {

	public SoundAction() {
		super("sound", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final Sound sound = Registry.SOUNDS.get(NamespacedKey.fromString(value));
		try {
			player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
		} catch (final IllegalArgumentException e) {
			player.sendMessage("Invalid sound name");
			player.sendMessage("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Sounds");
		}

		return false;
	}

}
