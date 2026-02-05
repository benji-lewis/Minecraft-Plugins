package uk.co.xfour.kimjongun3;

import org.bukkit.NamespacedKey;

public final class KimJongUnKeys {
    public final NamespacedKey mobKey;
    public final NamespacedKey itemKey;
    public final NamespacedKey launchpadKey;
    public final NamespacedKey missileKey;
    public final NamespacedKey radiationSuitKey;

    public KimJongUnKeys(KimJongUn3Plugin plugin) {
        this.mobKey = new NamespacedKey(plugin, "kimjongun3_mob");
        this.itemKey = new NamespacedKey(plugin, "kimjongun3_item");
        this.launchpadKey = new NamespacedKey(plugin, "kimjongun3_launchpad");
        this.missileKey = new NamespacedKey(plugin, "kimjongun3_missile");
        this.radiationSuitKey = new NamespacedKey(plugin, "kimjongun3_radiation_suit");
    }
}
