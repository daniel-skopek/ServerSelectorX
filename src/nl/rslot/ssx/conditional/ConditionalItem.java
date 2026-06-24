package nl.rslot.ssx.conditional;

import static org.bukkit.event.block.Action.LEFT_CLICK_AIR;
import static org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import nl.rslot.ssx.Main;
import nl.rslot.ssx.ServerSelectorX;
import nl.rslot.ssx.actions.Action;
import nl.rslot.ssx.conditional.condition.Condition;
import nl.rslot.ssx.conditional.condition.Conditions;
import nl.rslot.ssx.menu.OptionClickEvent;
import nl.rslot.ssx.placeholders.Server;
import nl.rslot.ssx.util.ColorUtil;
import nl.rslot.ssx.util.Cooldown;
import nl.rslot.ssx.util.PlaceholderUtil;

public class ConditionalItem {

	@SuppressWarnings("unchecked")
	private static Map<String, Object> matchSection(final Player player, final ConfigurationSection globalSection) throws InvalidConfigurationException {
		if (!globalSection.contains("conditional")) {
			return sectionToMap(globalSection);
		}

		final List<Map<?, ?>> conditionalsList = globalSection.getMapList("conditional");

		for (final Map<?, ?> genericMap : conditionalsList) {
			final Map<String, Object> map = (Map<String, Object>) genericMap;

			// Add options from global section to this map
			for (final String key : globalSection.getKeys(false)) {
				map.putIfAbsent(key, globalSection.get(key));
			}

			if (!map.containsKey("type")) {
				throw new InvalidConfigurationException("Missing 'type' option for a conditional");
			}

			final String type = (String) map.get("type");
			final boolean invert = (boolean) map.getOrDefault("invert-condition", false);

			final Condition condition = Conditions.getConditionByType(type);
			if (condition == null) {
				throw new InvalidConfigurationException("Unknown condition type: " + type);
			}
			if (condition.isTrue(player, map) != invert) {
				return map;
			}
		}

		return sectionToMap(globalSection);
	}

	private static Map<String, Object> sectionToMap(final ConfigurationSection section) {
		final Map<String, Object> map = new HashMap<>();
		for (final String key : section.getKeys(false)) {
			map.put(key, section.get(key));
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	public static void getItem(final Player player, final ConfigurationSection section,
							   final String cooldownId, final Consumer<ItemStack> consumer)
			throws InvalidConfigurationException {

		final Map<String, Object> matchedSection = matchSection(player, section);

		final String materialString = (String) matchedSection.getOrDefault("material", null);

		if (materialString == null) {
			throw new InvalidConfigurationException("Material is missing from config or null");
		}

		Main.getItemFromMaterialString(player, materialString, item -> {
			final ItemMeta meta = item.getItemMeta();

			final boolean useMiniMessage = (boolean) matchedSection.getOrDefault("minimessage", false);
			final String title = (String) matchedSection.getOrDefault("title", " ");
			final List<String> lore = (List<String>) matchedSection.getOrDefault("lore", Collections.emptyList());
			final boolean enchanted = (boolean) matchedSection.getOrDefault("enchanted", false);
			final boolean hideFlags = (boolean) matchedSection.getOrDefault("hide-flags", true);
			final boolean amountOnline = (boolean) matchedSection.getOrDefault("amount-online", false);
			int amount = (int) matchedSection.getOrDefault("amount", 1);
			final int durability = (int) matchedSection.getOrDefault("durability", -1);
			final List<String> actions = (List<String>) matchedSection.getOrDefault("actions", Collections.emptyList());
			final List<String> leftClickActions = (List<String>) matchedSection.getOrDefault("left-click-actions", Collections.emptyList());
			final List<String> rightClickActions = (List<String>) matchedSection.getOrDefault("right-click-actions", Collections.emptyList());
			final int cooldownTime = (int) matchedSection.getOrDefault("cooldown", 0);
			final List<String> cooldownActions = (List<String>) matchedSection.getOrDefault("cooldown-actions", Collections.emptyList());
			final @Nullable String serverName = (String) matchedSection.get("server-name");
			final @Nullable String color = (String) matchedSection.get("color");

			final @Nullable Server server = serverName != null ? Main.placeholderReceiver().getServer(serverName) : null;

			if (server != null && amountOnline) {
				final int online = server.getOnlinePlayers();
				amount = online >= 1 && online <= 64 ? online : 1;
			}

			final Function<String, String> stringConverter = string -> {
				string = PlaceholderUtil.parsePapiPlaceholders(player, string);
				if (server != null) {
					string = server.parsePlaceholders(player, string);
				}
				string = string.replace("{player}", player.getName());
				string = string.replace("{globalOnline", String.valueOf(ServerSelectorX.getGlobalPlayerCount()));
				if (useMiniMessage) {
					string = Main.miniMessageToLegacy(string);
				} else {
					string = "&r&f" + string;
				}
				string = ColorUtil.parseColors(string);
				return string;
			};

			meta.setDisplayName(stringConverter.apply(title));

			if (!lore.isEmpty()) {
				final List<String> parsedLore = new ArrayList<>(lore.size());
				for (final String line : lore) {
					parsedLore.add(stringConverter.apply(line));
				}
				meta.setLore(parsedLore);
			}

			if (enchanted) {
				meta.setEnchantmentGlintOverride(true);
			}

			if (hideFlags) {
				meta.addItemFlags(ItemFlag.values());
			}

			item.setAmount(amount);

			if (durability >= 0) {
				((Damageable) meta).setDamage(durability);
			}

			if (color != null) {
				if (color.length() != 7 || color.charAt(0) != '#') {
					player.sendMessage("Invalid color '" + color + "', should be a # followed by 6 hex digits");
					return;
				}

				((LeatherArmorMeta) meta).setColor(ColorUtil.hexToColor(materialString));
			}

			final PersistentDataContainer data = meta.getPersistentDataContainer();
			data.set(Main.getPlugin().KEY_ACTIONS, PersistentDataType.LIST.strings(), actions);
			data.set(Main.getPlugin().KEY_ACTIONS_LEFT, PersistentDataType.LIST.strings(), leftClickActions);
			data.set(Main.getPlugin().KEY_ACTIONS_RIGHT, PersistentDataType.LIST.strings(), rightClickActions);

			if (cooldownTime > 0) {
				data.set(Main.getPlugin().KEY_COOLDOWN_TIME, PersistentDataType.INTEGER, cooldownTime);
				data.set(Main.getPlugin().KEY_COOLDOWN_ID, PersistentDataType.STRING, cooldownId);
				data.set(Main.getPlugin().KEY_COOLDOWN_ACTIONS, PersistentDataType.LIST.strings(), cooldownActions);

			}

			item.setItemMeta(meta);
			consumer.accept(item);
		});
	}

	public static boolean runActions(final OptionClickEvent event) {
		final ClickType click = event.getClickType();
		final boolean leftClick = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;
		final boolean rightClick = click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT;
		return runActions(event.getPlayer(), event.getItemStack(), leftClick, rightClick);
	}

	public static boolean runActions(final PlayerInteractEvent event) {
		final boolean leftClick = event.getAction() == LEFT_CLICK_AIR || event.getAction() == LEFT_CLICK_BLOCK;
		final boolean rightClick = event.getAction() == RIGHT_CLICK_AIR || event.getAction() == RIGHT_CLICK_BLOCK;
		return runActions(event.getPlayer(), event.getItem(), leftClick, rightClick);
	}

	private static boolean runActions(final Player player, final ItemStack item, final boolean isLeftClick, final boolean isRightClick) {
		if (item == null) {
			Main.getPlugin().getLogger().warning("Received click event for null item. This is a bug.");
			return false;
		}

		final PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();

		final List<String> actions = data.get(Main.getPlugin().KEY_ACTIONS, PersistentDataType.LIST.strings());
		final List<String> leftActions = data.get(Main.getPlugin().KEY_ACTIONS_LEFT, PersistentDataType.LIST.strings());
		final List<String> rightActions = data.get(Main.getPlugin().KEY_ACTIONS_RIGHT, PersistentDataType.LIST.strings());
		final int cooldownTime = data.getOrDefault(Main.getPlugin().KEY_COOLDOWN_TIME, PersistentDataType.INTEGER, 0);

		if (cooldownTime > 0 &&
				( // Only apply cooldown if an action is about to be performed
						!actions.isEmpty() ||
								isRightClick && !rightActions.isEmpty() ||
								isLeftClick && !leftActions.isEmpty()
				)
		) {
			final String cooldownId = data.get(Main.getPlugin().KEY_COOLDOWN_ID, PersistentDataType.STRING);
			if (Cooldown.getCooldown(cooldownId) > 0) {
				final List<String> cooldownActions = data.get(Main.getPlugin().KEY_COOLDOWN_ACTIONS, PersistentDataType.LIST.strings());
				return Action.runActions(player, cooldownActions);
			} else {
				Cooldown.addCooldown(cooldownId, cooldownTime);
			}
		}

		boolean close = false;
		if (actions != null) {
			close = Action.runActions(player, actions);
		}
		if (isRightClick && rightActions != null) {
			close |= Action.runActions(player, rightActions);
		} else if (isLeftClick && leftActions != null) {
			close |= Action.runActions(player, leftActions);
		}
		return close;

	}

}
