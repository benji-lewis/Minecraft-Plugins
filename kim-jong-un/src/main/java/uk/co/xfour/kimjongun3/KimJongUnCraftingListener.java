package uk.co.xfour.kimjongun3;

import java.util.Optional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;

/**
 * Validates Kim Jong Un custom crafting recipes using persistent item IDs.
 */
public class KimJongUnCraftingListener implements Listener {
    private final KimJongUnItems items;

    /**
     * Creates a crafting listener bound to the item registry helper.
     *
     * @param items item helper for custom item identification
     */
    public KimJongUnCraftingListener(KimJongUnItems items) {
        this.items = items;
    }

    /**
     * Replaces crafting outputs with plugin items when valid ingredient IDs are present.
     *
     * @param event crafting preparation event
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        Optional<KimJongUnItems.KimJongUnItem> crafted = items.resolveCustomCraft(inventory.getMatrix());
        inventory.setResult(crafted.map(items::createItem).orElse(null));
    }
}
