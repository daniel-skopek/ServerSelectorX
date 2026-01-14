package nl.rslot.ssx.actions;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.rslot.ssx.Main;

public class AdvMessageAction extends Action {

	public AdvMessageAction() {
		super("advmessage", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final Component message = MiniMessage.miniMessage().deserialize(value);
		Main.adventure().player(player).sendMessage(message);
		return false;
	}

}
