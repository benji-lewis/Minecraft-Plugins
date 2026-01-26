package uk.co.xfour.kimjongunmob;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.utils.NpcEquipmentSlot;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.persistence.PersistentDataContainer;

public class KimJongUnMobPlugin extends JavaPlugin implements Listener {
    private static final double SPAWN_CHANCE_PER_PLAYER = 0.12;
    private static final int SPAWN_INTERVAL_TICKS = 20 * 60 * 5;
    private static final int MISSILE_FUSE_TICKS = 60;
    private static final double MISSILE_LAUNCH_SPEED = 1.4;
    private static final String KIM_SKIN_URL = "https://minotar.net/skin/KimJongUn";

    private final Random random = new Random();
    private NamespacedKey itemTypeKey;
    private BukkitTask spawnTask;
    private boolean fancyNpcsAvailable;

    @Override
    public void onEnable() {
        itemTypeKey = new NamespacedKey(this, "kim_jong_un_item_type");
        fancyNpcsAvailable = isFancyNpcsAvailable();
        if (!fancyNpcsAvailable) {
            getLogger().severe("FancyNpcs is required for Kim Jong Un NPCs. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        registerRecipes();
        scheduleSpawns();
    }

    @Override
    public void onDisable() {
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("spawnkim")) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        spawnKimNpc(player.getLocation(), player.getUniqueId());
        player.sendMessage(Component.text("A Kim Jong Un has appeared.", NamedTextColor.RED));
        return true;
    }

    @EventHandler
    public void onMissileActivate(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!isMissileItem(item)) {
            return;
        }
        Location blockLocation = Objects.requireNonNull(event.getClickedBlock()).getLocation();
        if (!blockLocation.getBlock().isBlockPowered()) {
            event.getPlayer().sendMessage(Component.text("The missile needs redstone power to activate.", NamedTextColor.YELLOW));
            return;
        }
        consumeMissileItem(event.getPlayer(), event.getHand());
        launchMissile(blockLocation.add(0.5, 1.0, 0.5));
        event.setCancelled(true);
    }

    private void scheduleSpawns() {
        spawnTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (random.nextDouble() <= SPAWN_CHANCE_PER_PLAYER) {
                    Location spawnLocation = player.getLocation().clone().add(randomOffset(), 0, randomOffset());
                    spawnKimNpc(spawnLocation, player.getUniqueId());
                    player.sendMessage(Component.text("Kim Jong Un lurks nearby.", NamedTextColor.DARK_RED));
                }
            }
        }, SPAWN_INTERVAL_TICKS, SPAWN_INTERVAL_TICKS);
    }

    private boolean isFancyNpcsAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("FancyNPCs");
        if (plugin == null) {
            plugin = Bukkit.getPluginManager().getPlugin("FancyNpcs");
        }
        if (plugin == null) {
            return false;
        }
        if (!plugin.isEnabled()) {
            getLogger().warning("FancyNpcs is installed but not enabled yet.");
        }
        return plugin.isEnabled();
    }

    private double randomOffset() {
        return random.nextInt(16) - 8;
    }

    private void spawnKimNpc(Location location, UUID creator) {
        if (!fancyNpcsAvailable) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        String npcName = "kim-jong-un-" + UUID.randomUUID();
        NpcData data = new NpcData(npcName, creator, location);
        data.setDisplayName(ChatColor.RED + "Kim Jong Un");
        data.setSkin(KIM_SKIN_URL);
        data.setType(EntityType.PLAYER);
        data.setTurnToPlayer(true);
        data.setEquipment(createKimEquipment());
        Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
        npc.setSaveToFile(false);
        data.setOnClick(player -> {
            Location dropLocation = npc.getData().getLocation();
            if (dropLocation == null) {
                dropLocation = player.getLocation();
            }
            World dropWorld = dropLocation.getWorld();
            if (dropWorld != null) {
                dropWorld.dropItemNaturally(dropLocation, createRandomMissilePart());
            }
            npc.removeForAll();
            FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
        });
        npc.create();
        FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
        npc.spawnForAll();
    }

    private Map<NpcEquipmentSlot, ItemStack> createKimEquipment() {
        Map<NpcEquipmentSlot, ItemStack> equipment = new EnumMap<>(NpcEquipmentSlot.class);
        equipment.put(NpcEquipmentSlot.CHEST, createBlackArmorPiece(Material.LEATHER_CHESTPLATE));
        equipment.put(NpcEquipmentSlot.LEGS, createBlackArmorPiece(Material.LEATHER_LEGGINGS));
        equipment.put(NpcEquipmentSlot.FEET, createBlackArmorPiece(Material.LEATHER_BOOTS));
        equipment.put(NpcEquipmentSlot.MAINHAND, new ItemStack(Material.IRON_SWORD));
        return equipment;
    }

    private ItemStack createBlackArmorPiece(Material material) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(Color.fromRGB(12, 12, 12));
        meta.displayName(Component.text("Charcoal Suit", NamedTextColor.GRAY));
        item.setItemMeta(meta);
        return item;
    }

    private void registerRecipes() {
        ItemStack missile = createMissileItem();
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "redstone_missile"), missile);
        recipe.shape(" N ", "BWB", " F ");
        recipe.setIngredient('N', new RecipeChoice.ExactChoice(createNoseCone()));
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(createMissileBody()));
        recipe.setIngredient('W', new RecipeChoice.ExactChoice(createWarheadCore()));
        recipe.setIngredient('F', new RecipeChoice.ExactChoice(createMissileFins()));
        Bukkit.addRecipe(recipe);
    }

    private ItemStack createNoseCone() {
        return createPart(Material.IRON_INGOT, "Thermonuclear Warhead", "Aerodynamic guidance tip.", "Recovered from a leader's cache.", "item_nose");
    }

    private ItemStack createMissileBody() {
        return createPart(Material.QUARTZ, "Thermonuclear Missile Body", "Reinforced casing section.", "item_body");
    }

    private ItemStack createMissileFins() {
        return createPart(Material.PHANTOM_MEMBRANE, "Thermonuclear Missile Fins", "Stabilizing fins with scorch marks.", "item_fins");
    }

    private ItemStack createWarheadCore() {
        return createPart(Material.NETHER_STAR, "Thermonuclear Warhead Core", "Unstable energy core.", "item_warhead");
    }

    private ItemStack createRandomMissilePart() {
        List<ItemStack> parts = List.of(
                createNoseCone(),
                createMissileBody(),
                createMissileFins(),
                createWarheadCore()
        );
        return parts.get(random.nextInt(parts.size()));
    }

    private ItemStack createMissileItem() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Thermonuclear missile", NamedTextColor.RED));
        meta.lore(List.of(
                Component.text("Place on a powered block to launch.", NamedTextColor.GRAY),
                Component.text("Detonates shortly after lift-off.", NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, "missile");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPart(Material material, String name, String loreLine, String typeId) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD));
        meta.lore(List.of(Component.text(loreLine, NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, typeId);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPart(Material material, String name, String loreLine1, String loreLine2, String typeId) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text(loreLine1, NamedTextColor.GRAY),
                Component.text(loreLine2, NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, typeId);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isMissileItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String type = container.get(itemTypeKey, PersistentDataType.STRING);
        return "missile".equalsIgnoreCase(type);
    }

    private void consumeMissileItem(Player player, EquipmentSlot hand) {
        if (player.getGameMode().name().toLowerCase(Locale.ROOT).contains("creative")) {
            return;
        }
        ItemStack item = player.getInventory().getItem(hand);
        if (item == null) {
            return;
        }
        if (item.getAmount() <= 1) {
            player.getInventory().setItem(hand, null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void launchMissile(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.SMOKE_LARGE, location, 20, 0.3, 0.5, 0.3, 0.02);
        TNTPrimed tnt = (TNTPrimed) world.spawnEntity(location, EntityType.PRIMED_TNT);
        tnt.setFuseTicks(MISSILE_FUSE_TICKS);
        tnt.setVelocity(new Vector(0, MISSILE_LAUNCH_SPEED, 0));
    }
}
