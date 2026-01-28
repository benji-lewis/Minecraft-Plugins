package uk.co.xfour.kimjongun2;

import org.bukkit.NamespacedKey;

public final class KimJongUnKeys {
    public final NamespacedKey mobKey;
    public final NamespacedKey itemKey;
    public final NamespacedKey launchpadKey;
    public final NamespacedKey missileKey;

    public KimJongUnKeys(KimJongUn2Plugin plugin) {
        this.mobKey = new NamespacedKey(plugin, "kimjongun2_mob");
        this.itemKey = new NamespacedKey(plugin, "kimjongun2_item");
        this.launchpadKey = new NamespacedKey(plugin, "kimjongun2_launchpad");
        this.missileKey = new NamespacedKey(plugin, "kimjongun2_missile");
    }
}
