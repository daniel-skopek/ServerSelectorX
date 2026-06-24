package nl.rslot.ssx.actions;

import org.bukkit.entity.Player;

import nl.rslot.ssx.util.ColorUtil;
import nl.rslot.ssx.util.PlaceholderUtil;

public class MessageAction extends Action {

	public MessageAction() {
		super("message", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		player.sendMessage(PlaceholderUtil.parsePapiPlaceholders(player, ColorUtil.parseColors(value)));
		return false;
	}

}
