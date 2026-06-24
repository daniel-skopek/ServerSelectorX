package nl.rslot.ssx;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import nl.rslot.ssx.conditional.ConditionalItem;

public class HotbarItemManager {

	private final Main plugin;

	HotbarItemManager(final Main plugin) {
		this.plugin = plugin;
	}

	void enable() {
		Bukkit.getPluginManager().registerEvents(new BukkitEventListener(), this.plugin);
	}

	private void debug(final String message) {
		if (Main.ITEM_DEBUG) {
			Main.getPlugin().getLogger().info("[Item debug] " + message);
		}
	}

	private boolean shouldHaveItem(final Player player, final String configName) {
		final Configuration config = Main.getConfigurationManager().getItemConfiguration(configName);

		if (config == null) {
			this.debug("Item was for a config file that is now removed");
			return false;
		}

		if (config.getBoolean("give.permission")) {
			this.debug("Permissions are enabled, checking permission");
			final String permission = "ssx.item." + configName;
			if (!player.hasPermission(permission)) {
				this.debug("Player does not have permission");
				return false;
			}
		}

		if (config.isList("worlds")) {
			this.debug("World whitelisting is enabled");
			// World whitelisting option is present
			if (!config.getStringList("worlds").contains(player.getWorld().getName())) {
				this.debug("Player is in a world that is not whitelisted (" + player.getWorld().getName() + ")");
				return false;
			}
		}

		return true;
	}

	private void giveItem(final Player player, final String configName, final int currentSlot) {
		final Configuration config = Main.getConfigurationManager().getItemConfiguration(configName);

		if (!config.isConfigurationSection("item")) {
			Main.getPlugin().getLogger().warning("Item '" + configName + "' has no item section, it has been ignored.");
			return;
		}

		final String cooldownId = player.getUniqueId() + configName;
		final PlayerInventory inv = player.getInventory();
		try {
			ConditionalItem.getItem(player, config.getConfigurationSection("item"), cooldownId, item -> {
				ServerSelectorX.setItemConfigName(item, configName);

				final int configuredSlot = config.getInt("give.inv-slot", 0);

				if (configuredSlot < 0) { // First available slot
					// Update item in current slot if specified, otherwise add new item
					if (currentSlot < 0) {
						inv.addItem(item);
					} else {
						inv.setItem(currentSlot, item);
					}
				} else {
					if (currentSlot < 0 || currentSlot == configuredSlot) {
						// No existing item or item in correct slot, overwrite item in configured slot
						inv.setItem(configuredSlot, item);
					} else {
						// Item is not in the slot that it is supposed to be in.
						inv.setItem(currentSlot, null);
						inv.setItem(configuredSlot, item);
					}
				}
			});
		} catch (final InvalidConfigurationException e) {
			player.sendMessage(String.format("Invalid item config (in %s.yaml): %s",
					configName, e.getMessage()));
		}
	}

	/**
	 * Update SSX items for all online players
	 */
	public void updateSsxItems() {
		for (final Player player : Bukkit.getOnlinePlayers()) {
			this.updateSsxItems(player);
		}
	}

	public void updateSsxItems(final Player player) {
		this.debug("Updating items for: " + player.getName());
		final ItemStack[] contents = player.getInventory().getContents();

		// First remove any items

		final Set<String> itemConfigNames = Main.getConfigurationManager().listItemConfigurations();

		final Set<String> presentItems = new HashSet<>();

		for (int slot = 0; slot < contents.length; slot++) {
			final ItemStack item = contents[slot];
			if (item == null) {
				continue;
			}

			final String configName = ServerSelectorX.getItemConfigName(item);
			if (configName == null) {
				// Not an SSX item
				continue;
			}

			if (this.shouldHaveItem(player, configName)) {
				this.debug("Player is allowed to keep item in slot " + slot);
			} else {
				this.debug("Player is not allowed to keep item in slot " + slot + ", removing it");
				player.getInventory().setItem(slot, null);
				continue;
			}

			this.debug("Update item " + slot + " in case the configuration has changed");

			this.giveItem(player, configName, slot);
			presentItems.add(configName);
		}

		for (final String configName : itemConfigNames) {
			if (presentItems.contains(configName)) {
				continue;
			}

			this.debug("Player does not have item " + configName);

			if (!this.shouldHaveItem(player, configName)) {
				this.debug("Player should not have the item, not giving it.");
				continue;
			}

			this.debug("Player should have this item, giving it now");

			this.giveItem(player, configName, -1);
		}
	}

	private class BukkitEventListener implements Listener {

		@EventHandler(priority = EventPriority.HIGH)
		public void onJoin(final PlayerJoinEvent event) {
			final Player player = event.getPlayer();
			final FileConfiguration config = Main.getConfigurationManager().getInventoryConfiguration();

			if (config.getBoolean("clear-inv", false) && !player.hasPermission("ssx.clearinvbypass")) {
				HotbarItemManager.this.debug("Clearing inventory for " + player.getName());
				final PlayerInventory inv = player.getInventory();
				inv.setStorageContents(new ItemStack[inv.getStorageContents().length]);
			}

			HotbarItemManager.this.updateSsxItems(player);
		}

		@EventHandler(priority = EventPriority.HIGH)
		public void onWorldChange(final PlayerChangedWorldEvent event) {
			HotbarItemManager.this.updateSsxItems(event.getPlayer());
		}

		@EventHandler(priority = EventPriority.HIGH)
		public void onRespawn(final PlayerRespawnEvent event) {
			HotbarItemManager.this.updateSsxItems(event.getPlayer());
		}

		@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
		public void onClear(final PlayerCommandPreprocessEvent event) {
			if (event.getMessage().equals("/clear") && event.getPlayer().hasPermission("minecraft.command.clear")) {
				Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> HotbarItemManager.this.updateSsxItems(event.getPlayer()), 1);
			}
		}

		@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
		public void onPickUpItem(final EntityPickupItemEvent event) {
			if (event.getEntity().getType() != EntityType.PLAYER) {
				return;
			}

			final ItemStack pickedUpItem = event.getItem().getItemStack();
			final String item1name = ServerSelectorX.getItemConfigName(pickedUpItem);
			if (item1name == null) {
				return; // not an SSX item
			}

			// Scan player inventory for an item with the same config name
			// If found, the item should not be picked up

			final Player player = (Player) event.getEntity();
			for (final ItemStack item2 : player.getInventory().getStorageContents()) {
				if (item2 == null || item2.getType() == Material.AIR) {
					continue;
				}
				final String item2name = ServerSelectorX.getItemConfigName(item2);
				if (item2name != null && item1name.equals(item2name)) {
					Main.getPlugin().getLogger().info("Deleted duplicate item picked up by " + event.getEntity().getName());
					event.setCancelled(true);
					event.getItem().remove();
					return;
				}
			}
		}
	}

}
