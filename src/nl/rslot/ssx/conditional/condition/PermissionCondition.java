package nl.rslot.ssx.conditional.condition;

import java.util.Map;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

public class PermissionCondition extends Condition {

	PermissionCondition() {
		super("permission");
	}

	@Override
	public boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("permission")) {
			throw new InvalidConfigurationException("Missing required option: 'permission' (the permission node to check)");
		}

		String permissionNode = (String) options.get("permission");
		return player.hasPermission(permissionNode);
	}
}
