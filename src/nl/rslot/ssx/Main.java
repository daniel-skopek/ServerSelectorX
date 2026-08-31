package nl.rslot.ssx;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import nl.rslot.ssx.configuration.ConfigurationManager;
import nl.rslot.ssx.placeholders.PapiExpansionRegistrar;
import nl.rslot.ssx.placeholders.PlaceholderReceiver;
import nl.rslot.ssx.util.PlaceholderUtil;

public class Main extends JavaPlugin {

	// When set to true, debug information related to giving items on join is printed to the
	// console. This boolean is enabled using /ssx lagdebug
	static boolean ITEM_DEBUG = false;

	private static ConfigurationManager configurationManager;

	private static Main plugin;

	private static BukkitAudiences adventure;

	private final HotbarItemManager hotbarItemManager = new HotbarItemManager(this);
	public HotbarItemManager getHotbarItemManager() { return this.hotbarItemManager; }

	private Heads heads;

	private static PlaceholderReceiver placeholderReceiver;

	public final NamespacedKey KEY_CONFIG_NAME = new NamespacedKey(this, "config_name");
	public final NamespacedKey KEY_ACTIONS = new NamespacedKey(this, "actions");
	public final NamespacedKey KEY_ACTIONS_LEFT = new NamespacedKey(this, "actions_left");
	public final NamespacedKey KEY_ACTIONS_RIGHT = new NamespacedKey(this, "actions_right");
	public final NamespacedKey KEY_COOLDOWN_TIME = new NamespacedKey(this, "cooldown_time");
	public final NamespacedKey KEY_COOLDOWN_ID = new NamespacedKey(this, "cooldown_id");
	public final NamespacedKey KEY_COOLDOWN_ACTIONS = new NamespacedKey(this, "cooldown_actions");

	@SuppressWarnings("null")
	@NotNull
	public static Main getPlugin(){
		return plugin;
	}

	@Override
	public void onEnable() {
		plugin = this;

		configurationManager = new ConfigurationManager();
		try {
			configurationManager.reload();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		this.heads = new Heads(this);

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		final PluginCommand command = Objects.requireNonNull(this.getCommand("serverselectorx"),
				"Command missing from plugin.yml");
		command.setExecutor(new ServerSelectorXCommand());
		command.setTabCompleter(new ServerSelectorXCommandCompleter());

		// Register custom selector commands
		Commands.registerCustomCommands();

		adventure = BukkitAudiences.create(this);

		new ItemMoveDropCancelListener();
		this.hotbarItemManager.enable();

		Bukkit.getPluginManager().registerEvents(new ItemClickListener(), this);
		Bukkit.getPluginManager().registerEvents(new ActionsOnJoinListener(), this);

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			PapiExpansionRegistrar.register();
		}

		placeholderReceiver = new PlaceholderReceiver();
		placeholderReceiver.loadConfiguration();
	}

	@Override
	public void onDisable() {
		if (adventure != null) {
			adventure.close();
			adventure = null;
		}
	}

	public static ConfigurationManager getConfigurationManager() {
		return configurationManager;
	}

	public static BukkitAudiences adventure() {
		return adventure;
	}

	public static PlaceholderReceiver placeholderReceiver() {
		return placeholderReceiver;
	}

    public static void getItemFromMaterialString(final Player player, @Nullable String materialString, final Consumer<ItemStack> itemConsumer) throws InvalidConfigurationException {
		if (materialString == null || materialString.isEmpty()) {
			return;
		}

		if (materialString.charAt(0) == '!') {
			materialString = PlaceholderUtil.parsePapiPlaceholders(player, materialString.substring(1));
		}

		if (materialString.startsWith("head:")) {
			final String headValue = materialString.substring(5);
			plugin.heads.getHeadItem(player, headValue, itemConsumer);
			return;
		}

		final ItemType item;
		try {
			// convert to lowercase for best-effort Material compatibility
			item = Registry.ITEM.get(NamespacedKey.fromString(materialString.toLowerCase()));
			Objects.nonNull(item); // NamespacedKey.fromString also raises NPE
		} catch (final NullPointerException e) {
			player.sendMessage("Invalid item name '" + materialString + "'");
			player.sendMessage("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Item-names");
			return;
		}

		itemConsumer.accept(item.createItemStack());
	}

	private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
			.character(ChatColor.COLOR_CHAR)
			.hexColors()
			.useUnusualXRepeatedCharacterHexFormat()
			.build();

	public static String miniMessageToLegacy(final String miniMessage) {
		return LEGACY_COMPONENT_SERIALIZER.serialize(MiniMessage.miniMessage().deserialize(miniMessage));
	}

}
