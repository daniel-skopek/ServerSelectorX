package nl.rslot.ssx.conditional.condition;

import java.util.Map;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import nl.rslot.ssx.InvisibilityToggle;

public class HasHiddenOthersCondition extends Condition {

    HasHiddenOthersCondition() {
        super("has-hidden-others");
    }

    @Override
    public boolean isTrue(final Player player, final Map<String, Object> options) throws InvalidConfigurationException {
        return InvisibilityToggle.hasHiddenOthers(player);
    }

}
