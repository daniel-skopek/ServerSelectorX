package nl.rslot.ssx.menu;

// package xyz.derkades.derkutils.bukkit.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class SlotClickEvent {

	private final Player player;
	private final int position;
	private final ClickType click;

	SlotClickEvent(final Player player,
					 final int position,
					 final ClickType click) {
		this.player = player;
		this.position = position;
		this.click = click;
	}

	public Player getPlayer() {
		return this.player;
	}

	/**
	 * @return Slot number of the item clicked
	 */
	public int getPosition() {
		return this.position;
	}

	public ClickType getClickType() {
		return this.click;
	}

}
