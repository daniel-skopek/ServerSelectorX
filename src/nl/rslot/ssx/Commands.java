package nl.rslot.ssx;

import java.util.List;
import java.util.Objects;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import nl.rslot.ssx.actions.Action;
import nl.rslot.ssx.util.ColorUtil;
import nl.rslot.ssx.util.CommandMapUtil;
import nl.rslot.ssx.util.Cooldown;

public class Commands {

	static void registerCustomCommands() {
		final CommandMap commandMap = CommandMapUtil.getCommandMap();

		for (final String commandName : Main.getConfigurationManager().listCommandConfigurations()) {
			final FileConfiguration config = Main.getConfigurationManager().getCommandConfiguration(commandName);
			final String description = Objects.requireNonNull(config.getString("description", "Opens menu"));
			final String usage = Objects.requireNonNull(config.getString("usage", "/<command>"));
			final List<String> aliases = config.getStringList("aliases");
			final List<String> actions = config.getStringList("actions");

			if (commandMap.getCommand(commandName) != null) {
				Main.getPlugin().getLogger().warning("Skipped registering command /" + commandName + ", it already exists.");
				continue;
			}

			commandMap.register("ssx-custom", new Command(commandName, description, usage, aliases) {

				@Override
				public boolean execute(final CommandSender sender, final String label, final String[] args) {
					if (sender instanceof Player){
						final Player player = (Player) sender;

						final FileConfiguration configMisc = Main.getConfigurationManager().getMiscConfiguration();

						if (config.isInt("cooldown")) {
							final long timeLeft = Cooldown.getCooldown("ssxcommand" + commandName);
							if (timeLeft > 0) {
								final String cooldownMessage = configMisc.getString("cooldown-message");
								if (cooldownMessage != null) {
									player.sendMessage(ColorUtil.parseColors(String.format(cooldownMessage, timeLeft / 1000.0)));
								}
								return true;
							}

							Cooldown.addCooldown("ssxcommand" + commandName, config.getInt("cooldown"));
						}

						if (config.getBoolean("permission", false) && !player.hasPermission("ssx.command." + commandName)) {
							final String permissionMessage = configMisc.getString("no-permission");
							if (permissionMessage != null) {
								player.sendMessage(ColorUtil.parseColors(permissionMessage));
							}
							return true;
						}

						Action.runActions(player, actions);
					}
					return true;
				}
			});
		}

	}

}
