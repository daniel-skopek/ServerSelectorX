package nl.rslot.ssx;

import java.util.List;
import java.util.Objects;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import nl.rslot.ssx.actions.Action;
import nl.rslot.ssx.conditional.ConditionalItem;
import nl.rslot.ssx.menu.IconMenu;
import nl.rslot.ssx.menu.MenuCloseEvent;
import nl.rslot.ssx.menu.OptionClickEvent;
import nl.rslot.ssx.menu.SlotClickEvent;
import nl.rslot.ssx.util.ColorUtil;
import nl.rslot.ssx.util.Cooldown;
import nl.rslot.ssx.util.PlaceholderUtil;

public class Menu extends IconMenu {

	private final FileConfiguration config;
	private final String configName;

	private boolean closed = false;

	private static String getTitle(final FileConfiguration config, final Player player) {
		if (config == null || !config.isString("title")) {
			return "[title missing]";
		}
		String titleString = config.getBoolean("title-minimessage")
				? Main.miniMessageToLegacy(config.getString("title"))
				: ColorUtil.parseColors(config.getString("title"));
		titleString = titleString.replace("{player}", player.getName());
		titleString = PlaceholderUtil.parsePapiPlaceholders(player, titleString);
		return titleString;
	}

	public Menu(final Player player, final @Nullable FileConfiguration config, final String configName) {
		super(Main.getPlugin(),
				getTitle(config, player),
				config == null ? 1 : config.getInt("rows", 6),
				player);

		this.config = config;
		this.configName = configName;

		if (this.config == null ||
				this.config.getConfigurationSection("menu") == null) {
			player.sendMessage("The configuration file failed to load, probably due to a syntax error.");
			player.sendMessage("Take a look at the console for any YAML errors, or paste your config in http://www.yamllint.com/");
			player.sendMessage("Check for identation and balanced quotes. If you want to use quotation marks in strings, they must be escaped properly by putting two quotation marks (for example \"\" or '').");
			player.sendMessage("Menu name: " + configName);
			return;
		}

		final int updateInterval = config.getInt("update-interval", 100);

		player.getScheduler().runAtFixedRate(Main.getPlugin(), scheduledTask -> {
			if (Menu.this.closed) { // menu has been closed
				scheduledTask.cancel();
				return;
			}

			Menu.this.addItems();

			if (config.getBoolean("disable-updates", false)) {
				scheduledTask.cancel();
			}
		}, null, 0, updateInterval);
	}

	private void addItems() {
		final OfflinePlayer potentiallyOffline = this.getPlayer();
		if (!(potentiallyOffline instanceof Player)) {
			Main.getPlugin().getLogger().warning("Player " + potentiallyOffline.getUniqueId() + " went offline?");
			return;
		}

		final Player player = (Player) potentiallyOffline;

		final ConfigurationSection menuSection = this.config.getConfigurationSection("menu");
		if (menuSection == null) {
			Main.getPlugin().getLogger().warning("Menu " + this.configName + " is missing a menu section. The menu will not contain any items.");
			return;
		}

		for (final String key : menuSection.getKeys(false)) {
			if (!this.config.isConfigurationSection("menu." + key)) {
				player.sendMessage("Invalid item " + key + ", check indentation.");
				continue;
			}

			final ConfigurationSection section = Objects.requireNonNull(menuSection.getConfigurationSection(key),
					"Null configuration section: menu." + key);

			final String cooldownId = player.getName() + this.configName + key;
			try {
				ConditionalItem.getItem(player, section, cooldownId,
						item -> this.addToMenu(menuSection, key, player, item));
			} catch (final InvalidConfigurationException e) {
				player.sendMessage("Invalid configuration: " + e.getMessage());
			}
		}
	}

	public void addToMenu(final ConfigurationSection menuSection,
						  final String key,
						  final Player player,
						  final ItemStack item) {
		if (key.equals("fill") || key.equals("-1")) { // -1 for backwards compatibility
			final List<Integer> skip = menuSection.getConfigurationSection(key).getIntegerList("fill-skip");
			// Fill all blank slots
			for (int i = 0; i < this.getInventory().getSize(); i++) {
				if (!this.hasItem(i) && !skip.contains(i)) {
					this.addItem(i, item);
				}
			}
		} else {
			for (final String split : key.split(",")) {
				int slot;
				try {
					slot = Integer.parseInt(split);
				} catch (final NumberFormatException e) {
					player.sendMessage("Invalid slot number " + split);
					return;
				}

				if (slot >= this.getInventory().getSize() || slot < 0) {
					player.sendMessage("You put an item in slot " + slot + ", which is higher than the maximum number of slots in your menu.");
					player.sendMessage("Use numbers 0 to " + (this.getInventory().getSize() - 1) + "or increase the number of rows in the config");
					return;
				}

				this.addItem(slot, item);
			}
		}
	}

	@Override
	public boolean onOptionClick(final OptionClickEvent event) {
		return ConditionalItem.runActions(event);
	}

	@Override
	public boolean onBlankClick(final SlotClickEvent event) {
		if (!this.config.contains("empty-slot-actions")) {
			return false;
		}

		final ConfigurationSection section = this.config.getConfigurationSection("empty-slot-actions");

		String matchedKey = null;

		for (final String key : section.getKeys(false)) {
			if (key.equals("other")) {
				continue;
			}

			for (final String split : key.split(",")) {
				final int slot = Integer.parseInt(split);
				if (event.getPosition() == slot) {
					matchedKey = key;
					break;
				}
			}
		}

		if (matchedKey == null && section.contains("other")) {
			matchedKey = "other";
		}

		if (matchedKey == null) {
			return false;
		}

		final List<String> actions = section.getStringList(matchedKey);
		return Action.runActions(event.getPlayer(), actions);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onClose(final MenuCloseEvent event) {
		this.closed = true;

		if (event.getOfflinePlayer() instanceof Player) {
			ServerSelectorX.getHotbarItemManager().updateSsxItems((Player) event.getOfflinePlayer());
		}

		if (this.config == null) {
			Main.getPlugin().getLogger().warning("Ignoring menu close event, config failed to load");
			return;
		}

		// this cooldown is checked in the item click event
		// 1.16 clients for some inexplicable reason interact with hotbar items when
		// a menu item is clicked. To avoid the menu re-opening right after it is closed,
		// I use this cooldown.
		Cooldown.addCooldown("ssxitemglobal" + this.getPlayer().getName(), 100);

		if (!this.config.contains("on-close")) {
			return;
		}

		final String reason;
		switch(event.getReason()) {
		case PLAYER_CLOSED:
			reason = "player";
			break;
		case ITEM_CLICK:
			reason = "item";
			break;
		case PLAYER_QUIT:
			reason = "quit";
			break;
		default:
			reason = null;
		}

		if (reason == null) {
			return;
		}

		this.config.getMapList("on-close").forEach(map -> {
			final List<String> reasons = (List<String>) map.get("reasons");
			if (reasons.contains(reason)) {
				final List<String> actions = (List<String>) map.get("actions");
				if (event.getOfflinePlayer() instanceof Player) {
					Action.runActions(event.getPlayer(), actions);
				} else {
					Action.runActions(null, actions);
				}
			}
		});
	}

}
