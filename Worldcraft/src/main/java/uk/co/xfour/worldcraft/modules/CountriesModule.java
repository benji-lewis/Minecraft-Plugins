package uk.co.xfour.worldcraft.modules;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import uk.co.xfour.worldcraft.WorldcraftPlugin;
import uk.co.xfour.worldcraft.flags.CountryInfo;
import uk.co.xfour.worldcraft.flags.CountryLoadResult;
import uk.co.xfour.worldcraft.flags.CountryRegistry;
import uk.co.xfour.worldcraft.flags.FlagMapRenderer;
import uk.co.xfour.worldcraft.flags.FlagPoleBuilder;
import uk.co.xfour.worldcraft.flags.FlagTextureService;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Module that registers commands for country flags and flag poles.
 */
public class CountriesModule implements Module, CommandExecutor, TabCompleter {
    private final WorldcraftPlugin plugin;
    private final CountryRegistry countryRegistry;
    private FlagTextureService flagTextureService;
    private FlagPoleBuilder flagPoleBuilder;
    private volatile boolean ready;

    /**
     * Creates the countries module for Worldcraft.
     *
     * @param plugin plugin instance
     */
    public CountriesModule(WorldcraftPlugin plugin) {
        this.plugin = plugin;
        Duration cacheTtl = Duration.ofHours(plugin.getConfig().getLong("modules.countries.cache-ttl-hours", 168));
        String countriesUrl = plugin.getConfig().getString("modules.countries.countries-source-url", "https://flagcdn.com/en/codes.json");
        this.countryRegistry = new CountryRegistry(plugin, countriesUrl, cacheTtl);
        reloadServices(cacheTtl);
    }

    @Override
    public void start() {
        reloadCountriesAsync();
    }

    @Override
    public void stop() {
        ready = false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /worldcraft <flag|flagpole|reload> <country>");
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("reload")) {
            plugin.reloadConfig();
            Duration cacheTtl = Duration.ofHours(plugin.getConfig().getLong("modules.countries.cache-ttl-hours", 168));
            reloadServices(cacheTtl);
            reloadCountriesAsync();
            sender.sendMessage(ChatColor.GREEN + "Worldcraft configuration reloaded.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (!ready) {
            sender.sendMessage(ChatColor.YELLOW + "Country list is still loading. Please try again shortly.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Please specify a country.");
            return true;
        }
        String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Optional<CountryInfo> country = countryRegistry.findCountry(query);
        if (country.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Unknown country: " + query);
            return true;
        }
        if (action.equals("flag")) {
            giveFlag(player, country.get());
            return true;
        }
        if (action.equals("flagpole")) {
            giveFlagPole(player, country.get());
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "Usage: /worldcraft <flag|flagpole|reload> <country>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = List.of("flag", "flagpole", "reload");
            return options.stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length >= 2 && (args[0].equalsIgnoreCase("flag") || args[0].equalsIgnoreCase("flagpole"))) {
            String current = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            return countryRegistry.getCountryNames().stream()
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(current.toLowerCase(Locale.ROOT)))
                    .limit(20)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void reloadServices(Duration cacheTtl) {
        String template = plugin.getConfig().getString("modules.countries.flag-texture-url-template", "https://flagcdn.com/w320/{code}.png");
        String modeValue = plugin.getConfig().getString("modules.countries.texture-mode", "hybrid");
        FlagTextureService.TextureMode mode = parseTextureMode(modeValue);
        this.flagTextureService = new FlagTextureService(plugin, template, cacheTtl, mode);
        Material poleMaterial = Material.matchMaterial(plugin.getConfig().getString("modules.countries.pole-material", "OAK_FENCE"));
        Material mountMaterial = Material.matchMaterial(plugin.getConfig().getString("modules.countries.pole-mount-material", "SMOOTH_STONE"));
        int poleHeight = plugin.getConfig().getInt("modules.countries.pole-height", 3);
        if (poleMaterial == null) {
            poleMaterial = Material.OAK_FENCE;
        }
        if (mountMaterial == null) {
            mountMaterial = Material.SMOOTH_STONE;
        }
        this.flagPoleBuilder = new FlagPoleBuilder(poleMaterial, mountMaterial, poleHeight);
    }

    private FlagTextureService.TextureMode parseTextureMode(String modeValue) {
        if (modeValue == null) {
            return FlagTextureService.TextureMode.HYBRID;
        }
        try {
            return FlagTextureService.TextureMode.valueOf(modeValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return FlagTextureService.TextureMode.HYBRID;
        }
    }

    private void reloadCountriesAsync() {
        ready = false;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            CountryLoadResult result = countryRegistry.loadCountries();
            ready = result.success();
            if (!result.success()) {
                plugin.getLogger().warning(result.message());
            } else {
                plugin.getLogger().info(result.message());
            }
        });
    }

    private void giveFlag(Player player, CountryInfo country) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                BufferedImage image = flagTextureService.loadFlagImage(country.code());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack mapItem = createFlagMapItem(player.getWorld(), image, country.name());
                    player.getInventory().addItem(mapItem);
                    player.sendMessage(ChatColor.GREEN + "Delivered flag for " + country.name() + ".");
                });
            } catch (Exception ex) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.RED + "Failed to load flag."));
            }
        });
    }

    private void giveFlagPole(Player player, CountryInfo country) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                BufferedImage image = flagTextureService.loadFlagImage(country.code());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack mapItem = createFlagMapItem(player.getWorld(), image, country.name());
                    boolean placed = flagPoleBuilder.placeFlagPole(player, mapItem);
                    if (placed) {
                        player.sendMessage(ChatColor.GREEN + "Placed a flag pole for " + country.name() + ".");
                    } else {
                        player.sendMessage(ChatColor.RED + "Not enough space to place the flag pole.");
                    }
                });
            } catch (Exception ex) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.RED + "Failed to load flag."));
            }
        });
    }

    private ItemStack createFlagMapItem(World world, BufferedImage image, String name) {
        MapView mapView = Bukkit.createMap(world);
        mapView.setTrackingPosition(false);
        mapView.setUnlimitedTracking(false);
        mapView.setScale(MapView.Scale.FARTHEST);
        mapView.getRenderers().clear();
        mapView.addRenderer(new FlagMapRenderer(image));
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setMapView(mapView);
        meta.setDisplayName(ChatColor.AQUA + name + " Flag");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Generated by Worldcraft");
        lore.add(ChatColor.DARK_GRAY + "No bundled PNGs required");
        meta.setLore(lore);
        mapItem.setItemMeta(meta);
        return mapItem;
    }
}
