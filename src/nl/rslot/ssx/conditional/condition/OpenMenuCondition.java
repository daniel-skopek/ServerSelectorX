package nl.rslot.ssx.conditional.condition;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

public class OpenMenuCondition extends Condition {

    OpenMenuCondition() {
        super("open-menu");
    }

    @Override
    public boolean isTrue(Player player, Map<String, Object> options) {
        // When a player does not have any open menu, in creative it returns "creative" and in survival it returns "crafting"
        return player.getOpenInventory().getType() != InventoryType.CRAFTING &&
                player.getOpenInventory().getType() != InventoryType.CREATIVE;
    }

}
