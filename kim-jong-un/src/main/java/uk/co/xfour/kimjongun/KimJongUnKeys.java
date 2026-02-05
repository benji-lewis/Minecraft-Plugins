package uk.co.xfour.kimjongun;

import org.bukkit.NamespacedKey;

public final class KimJongUnKeys {
    public final NamespacedKey mobKey;
    public final NamespacedKey itemKey;
    public final NamespacedKey launchpadKey;
    public final NamespacedKey missileKey;
    public final NamespacedKey radiationSuitKey;

    public KimJongUnKeys(KimJongUnPlugin plugin) {
        this.mobKey = new NamespacedKey(plugin, "kimjongun_mob");
        this.itemKey = new NamespacedKey(plugin, "kimjongun_item");
        this.launchpadKey = new NamespacedKey(plugin, "kimjongun_launchpad");
        this.missileKey = new NamespacedKey(plugin, "kimjongun_missile");
        this.radiationSuitKey = new NamespacedKey(plugin, "kimjongun_radiation_suit");
    }
}
