package nl.rslot.ssx.conditional.condition;

import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import nl.rslot.ssx.Main;

public class EffectCondition extends Condition {

	EffectCondition() {
		super("has-effect");
	}

	@Override
	public boolean isTrue(final Player player, final Map<String, Object> options) throws InvalidConfigurationException {
		if (!options.containsKey("effect-type")) {
			throw new InvalidConfigurationException("Missing required option for conditional: 'effect-type'");
		}

		final String effectTypeStr = (String) options.get("effect-type");
		final PotionEffectType effect = Registry.EFFECT.get(NamespacedKey.fromString(effectTypeStr));
		if (effect == null) {
			Main.getPlugin().getLogger().warning(
					String.format("Skipped effect condition for %s, effect type %s is invalid", player.getName(), effectTypeStr));
			return false;
		}

		final PotionEffect activeEffect = player.getPotionEffect(effect);

		if (activeEffect == null) {
			return false;
		}

		if (!options.containsKey("effect-minimum-strength")) {
			return true;
		}

		final int minimumStrength = (int) options.get("effect-minimum-strength");
		// Bukkit amplifier is 0 when displayed strength is 1
		return activeEffect.getAmplifier() + 1 >= minimumStrength;
	}
}
