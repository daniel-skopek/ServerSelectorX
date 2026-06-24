package nl.rslot.ssx;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import nl.rslot.ssx.placeholders.Server;

public class ServerSelectorXCommandCompleter implements TabCompleter {

	@Nullable
	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
		if (!sender.hasPermission("ssx.admin")) {
			return null;
		}

		if (args.length == 1) {
			return Arrays.stream(
					new String[]{
							"reload", "rl",
							"reloadcommands", "rlcmd",
							"status",
							"placeholders",
							"items",
							"menus",
							"commands",
							"itemdebug",
							"sync",
							"synclist",
							"openmenu"
			}).filter(c -> c.contains(args[0])).collect(Collectors.toList());
		}

		if (args.length == 2 &&
				args[0].equals("placeholders")) {
			return ServerSelectorX.getServers().stream()
					.map(Server::getName)
					.filter(name -> name.contains(args[1]))
					.collect(Collectors.toList());
		}

		if (args.length == 2 &&
				args[0].equals("openmenu")) {
			return Main.getConfigurationManager().listMenuConfigurations().stream()
					.filter(name -> name.contains(args[1]))
					.collect(Collectors.toList());
		}

		if (args.length == 3 &&
				args[0].equals("openmenu")) {
			return Bukkit.getOnlinePlayers().stream()
					.map(Player::getName)
					.filter(name -> name.contains(args[2]))
					.collect(Collectors.toList());
		}

		return null;
	}

}
