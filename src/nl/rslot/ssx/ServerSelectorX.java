package nl.rslot.ssx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import nl.rslot.ssx.actions.Action;
import nl.rslot.ssx.placeholders.Server;
import nl.rslot.ssx.util.Cooldown;

public class ServerSelectorX {

	public static void registerAction(final Action action) {
		Action.ACTIONS.add(action);
	}

	public static boolean runAction(final Player player, final String actionString) {
		return Action.runAction(player, actionString);
	}

	public static boolean runActions(final Player player, final List<String> actionStrings) {
		return Action.runActions(player, actionStrings);
	}

	public static @Nullable Server getServer(final String name) {
		return Main.placeholderReceiver().getServer(name);
	}

	public static Collection<Server> getServers() {
		return Collections.unmodifiableCollection(Main.placeholderReceiver().getServers().values());
	}

	public static int getGlobalPlayerCount() {
		return getServers().stream().mapToInt(Server::getOnlinePlayers).sum();
	}

	public static void teleportPlayerToServer(final Player player, final String server){
		if (Cooldown.getCooldown("servertp" + player.getName() + server) > 0) {
			return;
		}

		Cooldown.addCooldown("servertp" + player.getName() + server, 1000);

		// Send message to BungeeCord
		try (
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos)
			){

	        dos.writeUTF("Connect");
	        dos.writeUTF(server);
	        player.sendPluginMessage(Main.getPlugin(), "BungeeCord", baos.toByteArray());
		} catch (final IOException e){
			throw new RuntimeException(e);
		}
	}

	public static HotbarItemManager getHotbarItemManager() {
		return Main.getPlugin().getHotbarItemManager();
	}

	public static @Nullable String getItemConfigName(final ItemStack item) {
		final ItemMeta meta = item.getItemMeta();
		final PersistentDataContainer data = meta.getPersistentDataContainer();
		return data.get(Main.getPlugin().KEY_CONFIG_NAME, PersistentDataType.STRING);
	}

	public static boolean isSSXItem(final ItemStack item) {
		return getItemConfigName(item) != null;
	}

	public static void setItemConfigName(final ItemStack item, final @Nullable String value) {
		final ItemMeta meta = item.getItemMeta();
		final PersistentDataContainer data = meta.getPersistentDataContainer();
		data.set(Main.getPlugin().KEY_CONFIG_NAME, PersistentDataType.STRING, value);
		item.setItemMeta(meta);
	}

}
