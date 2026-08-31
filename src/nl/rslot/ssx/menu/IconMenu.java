package nl.rslot.ssx.menu;


import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class IconMenu implements Listener {

	private String name;
	private final int size;
	private final UUID uuid;
	private final Inventory inventory;
	private @Nullable InventoryView view;
	private boolean closeEventCalled = false;
	private @Nullable CloseReason closeReason = null;

	/**
	 * Creates a new menu instance. To add items, use the {@link IconMenu#addItem(int, ItemStack)} method</li>
	 * @param plugin Bukkit plugin instance
	 * @param name Name of the menu
	 * @param rows Menu size in rows.
	 * @param player Player that this menu will be opened for
	 */
	public IconMenu(final Plugin plugin,
					final String name,
					final int rows,
					final Player player) {
		Objects.requireNonNull(name, "Name is null");
		Objects.requireNonNull(player, "Player is null");

		this.size = rows * 9;
		this.name = name;
		this.uuid = player.getUniqueId();

		this.inventory = Bukkit.createInventory(player, this.size, this.name);
		final InventoryView view = player.openInventory(this.inventory);
		if (view == null) {
			System.err.println("IconMenu: Failed to open inventory for " + player.getName() + ". Did a plugin cancel the event?");
			return;
		}
		this.view = view;

		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

		player.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
			// Unregister listeners for the menu if the player has opened a different
			// menu, which means that this menu must be closed.
			final Player online = Bukkit.getPlayer(IconMenu.this.uuid);
			if (online != null && online.getOpenInventory().getTopInventory().equals(IconMenu.this.inventory)) {
				// menu is still open
				return;
			}

			if (!IconMenu.this.closeEventCalled) {
				IconMenu.this.closeEventCalled = true;
				if (online == null) {
					IconMenu.this.onClose(new MenuCloseEvent(Bukkit.getOfflinePlayer(IconMenu.this.uuid), CloseReason.PLAYER_QUIT));
				} else {
					if (IconMenu.this.closeReason == null) {
						IconMenu.this.closeReason = CloseReason.PLAYER_CLOSED;
					}
					IconMenu.this.onClose(new MenuCloseEvent(online, IconMenu.this.closeReason));
				}
			}

			HandlerList.unregisterAll(IconMenu.this);
			scheduledTask.cancel();
		}, () -> {
			// Retired: the player went offline (or the plugin was disabled).
			if (!plugin.isEnabled() || IconMenu.this.closeEventCalled) {
				return;
			}
			IconMenu.this.closeEventCalled = true;
			HandlerList.unregisterAll(IconMenu.this);
			IconMenu.this.onClose(new MenuCloseEvent(Bukkit.getOfflinePlayer(IconMenu.this.uuid), CloseReason.PLAYER_QUIT));
		}, 1, 1);
	}

	/**
	 * Called when a player clicks on an item in the menu
	 * @param event
	 * @return Whether the menu should be closed
	 */
	public abstract boolean onOptionClick(OptionClickEvent event);

	/**
	 * Called when a player clicks on a blank slot in the menu
	 * @param event
	 * @return Whether the menu should be closed
	 */
	public boolean onBlankClick(final SlotClickEvent event) {
		return false;
	}

	/**
	 * Called when the menu closes
	 * @param event
	 */
	public void onClose(final MenuCloseEvent event) {}

	/**
	 * Sets the name of the menu. Has no effect after the menu has been opened.
	 * @param name
	 */
	public void setName(final String name) {
		this.name = name;
	}

	public int getSize() {
		return this.size;
	}

	public OfflinePlayer getPlayer() {
		final OfflinePlayer player = Bukkit.getPlayer(this.uuid);
		return player != null ? player : Bukkit.getOfflinePlayer(this.uuid);
	}

	/**
	 * Calls {@link #onClose(MenuCloseEvent)} with {@link CloseReason#FORCE_CLOSE} and closes the inventory.
	 */
	public void close() {
		this.closeReason = CloseReason.FORCE_CLOSE;
		this.view.close();
	}

	public void addItem(final int slot, final ItemStack item) {
		this.inventory.setItem(slot, item);
	}

	public boolean hasItem(final int slot) {
		final ItemStack item = this.inventory.getItem(slot);
		return item != null &&
				item.getType() != Material.AIR;
	}

	public void clear() {
		this.inventory.clear();
	}

	public Inventory getInventory() {
		return this.inventory;
	}

	public InventoryView getInventoryView() {
		return this.view;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(final InventoryClickEvent event) {
		if (!event.getView().getTopInventory().equals(this.inventory)) {
			return;
		}

		event.setCancelled(true);

		final int slot = event.getRawSlot();

		final Player clicker = (Player) event.getWhoClicked();

		if (slot >= 0 && slot < this.size) {
			final boolean close;
			if (this.hasItem(slot)) {
				close = this.onOptionClick(new OptionClickEvent(clicker, slot, event.getClick(), this.inventory.getItem(slot)));
			} else {
				close = this.onBlankClick(new SlotClickEvent(clicker, slot, event.getClick()));
			}

			if (close) {
				this.closeReason = CloseReason.ITEM_CLICK;
				this.view.close();
			}
		}
	}

}
