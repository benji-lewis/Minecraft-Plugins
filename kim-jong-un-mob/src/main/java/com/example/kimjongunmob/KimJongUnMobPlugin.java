package com.example.kimjongunmob;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class KimJongUnMobPlugin extends JavaPlugin implements Listener {
    private static final double SPAWN_CHANCE_PER_PLAYER = 0.12;
    private static final int SPAWN_INTERVAL_TICKS = 20 * 60 * 5;
    private static final int MISSILE_FUSE_TICKS = 60;
    private static final double MISSILE_LAUNCH_SPEED = 1.4;
    private static final String KIM_SKIN_URL = "https://minotar.net/skin/KimJongUn";

    private final Random random = new Random();
    private NamespacedKey mobKey;
    private NamespacedKey itemTypeKey;
    private BukkitTask spawnTask;

    @Override
    public void onEnable() {
        mobKey = new NamespacedKey(this, "kim_jong_un_mob");
        itemTypeKey = new NamespacedKey(this, "kim_jong_un_item_type");

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
        spawnKimMob(player.getLocation());
        player.sendMessage(Component.text("A Kim Jong Un themed mob has appeared.", NamedTextColor.RED));
        return true;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isKimMob(entity)) {
            return;
        }
        event.getDrops().clear();
        World world = entity.getWorld();
        world.dropItemNaturally(entity.getLocation(), createRandomMissilePart());
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
                    spawnKimMob(spawnLocation);
                    player.sendMessage(Component.text("A Kim Jong Un themed mob lurks nearby.", NamedTextColor.DARK_RED));
                }
            }
        }, SPAWN_INTERVAL_TICKS, SPAWN_INTERVAL_TICKS);
    }

    private double randomOffset() {
        return random.nextInt(16) - 8;
    }

    private void spawnKimMob(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        LivingEntity entity = (LivingEntity) world.spawnEntity(location, EntityType.ZOMBIE);
        entity.customName(Component.text("Kim Jong Un", NamedTextColor.RED));
        entity.setCustomNameVisible(true);
        entity.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
        Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(40.0);
        entity.setHealth(40.0);
        if (entity instanceof Zombie zombie) {
            zombie.setAggressive(false);
        }
        entity.getEquipment().setHelmet(createKimHead());
        entity.getEquipment().setChestplate(createBlackArmorPiece(Material.LEATHER_CHESTPLATE));
        entity.getEquipment().setLeggings(createBlackArmorPiece(Material.LEATHER_LEGGINGS));
        entity.getEquipment().setBoots(createBlackArmorPiece(Material.LEATHER_BOOTS));
        entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
    }

    private ItemStack createKimHead() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "KimJongUn");
        try {
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(KIM_SKIN_URL));
            profile.setTextures(textures);
        } catch (MalformedURLException e) {
            getLogger().warning("Invalid Kim Jong Un skin URL configured.");
        }
        meta.setOwnerProfile(profile);
        meta.displayName(Component.text("Kim Jong Un Head", NamedTextColor.RED));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBlackArmorPiece(Material material) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(Color.fromRGB(12, 12, 12));
        meta.displayName(Component.text("Charcoal Suit", NamedTextColor.GRAY));
        item.setItemMeta(meta);
        return item;
    }

    private boolean isKimMob(LivingEntity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        return container.has(mobKey, PersistentDataType.BYTE);
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
        return createPart(Material.IRON_INGOT, "Thermonuclear Nose Cone", "Aerodynamic guidance tip.", "Recovered from a leader's cache.", "item_nose");
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
