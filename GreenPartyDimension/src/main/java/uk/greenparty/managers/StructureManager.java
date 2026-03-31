package uk.greenparty.managers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.routines.LocationRegistry;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * StructureManager — Phase 4 auto-generation of 4 themed structures.
 *
 * Builds magnificent landmarks so players know this is a serious utopia,
 * not just a flat green field with composters. (It is also that. But now with buildings.)
 *
 * Structures built once on dimension startup, persisted across restarts.
 *
 * v1.4.1: Dynamic ground detection — scans downward to find solid ground beneath
 *         surface vegetation (flowers, tall grass, etc.) so structures spawn at
 *         the correct Y level on uneven terrain.
 * v1.4.3: Initialize 4 default structures on fresh start so initialiseStructures()
 *         always has entries to detect and build even when structures.json is absent.
 * v1.4.4: Add entrance signs to each structure; fix directional block data
 *         (stairs facing, door hinge/facing, log axis, trapdoor orientation).
 */
public class StructureManager {

    private final GreenPartyPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File dataFile;

    /** Fallback Y if ground scan fails (should rarely be used). */
    private static final int GROUND_Y_FALLBACK = 57;

    /**
     * Scan downward from a reasonable height to find the topmost solid block at (x, z).
     * Skips: air, cave air, void air, water, lava, and any non-solid block (which covers
     * all flowers, short grass, tall grass, ferns, saplings, etc. in Bukkit's model).
     *
     * Returns the Y of the topmost solid block. Structure base (floor) goes at Y = result.
     * The structure walls/interior start at Y = result + 1.
     */
    private int getSolidGroundY(World world, int x, int z) {
        int startY = Math.min(world.getMaxHeight() - 1, 130);
        for (int y = startY; y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            // Skip all non-solid blocks: air variants, water, lava, flowers, grass plants, etc.
            if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) continue;
            if (type == Material.WATER || type == Material.LAVA) continue;
            if (!type.isSolid()) continue; // catches all flowers, SHORT_GRASS, TALL_GRASS, ferns, etc.
            return y; // This is the topmost solid block — floor goes here
        }
        plugin.getLogger().warning("[Structures] Could not find solid ground at " + x + ", " + z + " — using fallback Y=" + GROUND_Y_FALLBACK);
        return GROUND_Y_FALLBACK;
    }

    // Structure records
    private final Map<String, StructureInfo> structures = new LinkedHashMap<>();

    public static class StructureInfo {
        public String id;
        public String displayName;
        public int x, y, z;
        public String description;
        public boolean built;
        public List<String> assignedNpcs = new ArrayList<>();

        public StructureInfo() {}

        public StructureInfo(String id, String displayName, int x, int y, int z, String description) {
            this.id = id;
            this.displayName = displayName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.description = description;
            this.built = false;
        }

        public String locationString() {
            return "§8(" + x + ", " + y + ", " + z + ")";
        }
    }

    public StructureManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void initialiseStructures(World world) {
        if (world == null) return;
        Location spawn = world.getSpawnLocation();
        int sx = spawn.getBlockX();
        int sz = spawn.getBlockZ();

        boolean anyUnbuilt = false;
        for (String name : new String[]{"council_chamber", "compost_plant", "tree_farm", "recycling_centre"}) {
            StructureInfo info = structures.get(name);
            if (info == null || !info.built) { anyUnbuilt = true; break; }
        }

        if (!anyUnbuilt) {
            plugin.getLogger().info("[Structures] All 4 structures already exist — skipping generation.");
            return;
        }

        plugin.getLogger().info("[Structures] Generating Phase 4 structures. The planning committee approved this in record time (2 meetings).");

        // Build with staggered delays to reduce per-tick workload
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            buildCouncilChamber(world, sx, sz);
            saveData();
        }, 30L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            buildCompostPlant(world, sx, sz);
            saveData();
        }, 60L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            buildTreeFarm(world, sx, sz);
            saveData();
        }, 90L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            buildRecyclingCentre(world, sx, sz);
            saveData();
            plugin.getLogger().info("[Structures] All 4 structures complete. The Verdant Utopia is now truly civilised.");
        }, 120L);
    }

    public StructureInfo getStructure(String id) {
        return structures.get(id.toLowerCase().replace(" ", "_"));
    }

    public Collection<StructureInfo> getAllStructures() {
        return structures.values();
    }

    public Location getStructureLocation(String id, World world) {
        StructureInfo info = getStructure(id);
        if (info == null || world == null) return null;
        return new Location(world, info.x, info.y, info.z);
    }

    // ─── Location Registry Integration ───────────────────────────────────────

    /**
     * Register all internal structure locations with the given LocationRegistry.
     *
     * Called from GreenPartyPlugin.onEnable() after structures have been built
     * (with an appropriate scheduler delay so the build tasks have completed).
     * Each structure centre is read from the persisted StructureInfo, and internal
     * points are registered as offsets from that centre.
     *
     * Council Chamber: 8 named positions (centre + presider + 6 seats)
     * Tree Farm:       4 named positions (centre + centre alias + oak bed + birch bed)
     * Compost Plant:   1 named position  (processing centre)
     * Recycling Centre:1 named position  (centre)
     */
    public void registerLocations(LocationRegistry registry, World world) {
        if (registry == null || world == null) return;

        int registered = 0;

        // ── Council Chamber ───────────────────────────────────────────────────
        StructureInfo chamber = structures.get("council_chamber");
        if (chamber != null && chamber.built) {
            int cx = chamber.x, cy = chamber.y, cz = chamber.z;
            Location centre = new Location(world, cx, cy, cz);

            // Centre of the chamber floor
            registry.registerOffset("council_chamber",  centre, 0,  1,  0);
            // Presiding lectern front
            registry.registerOffset("chamber_presider", centre, 0,  1, -2);
            // Seating positions around the podium
            registry.registerOffset("chamber_seat_1",   centre, 0,  1, -4);  // north row centre
            registry.registerOffset("chamber_seat_2",   centre, 4,  1,  4);  // east row centre
            registry.registerOffset("chamber_seat_3",   centre,-4,  1,  4);  // west row centre
            registry.registerOffset("chamber_seat_4",   centre, 6,  1,  0);  // east row back
            registry.registerOffset("chamber_seat_5",   centre,-6,  1,  0);  // west row back
            registry.registerOffset("chamber_seat_6",   centre, 0,  1,  6);  // south row back
            registered += 8;
        } else {
            plugin.getLogger().warning("[LocationRegistry] Council Chamber not yet built — skipping location registration.");
        }

        // ── Tree Farm ─────────────────────────────────────────────────────────
        StructureInfo farm = structures.get("tree_farm");
        if (farm != null && farm.built) {
            int cx = farm.x, cy = farm.y, cz = farm.z;
            Location centre = new Location(world, cx, cy, cz);

            registry.registerOffset("tree_farm",        centre, 0,  1,  0);  // general centre
            registry.registerOffset("tree_farm_centre", centre, 0,  1,  0);  // alias
            registry.registerOffset("tree_farm_oak",    centre,-6,  1,  0);  // west bed
            registry.registerOffset("tree_farm_birch",  centre, 6,  1,  0);  // east bed
            registered += 4;
        } else {
            plugin.getLogger().warning("[LocationRegistry] Tree Farm not yet built — skipping location registration.");
        }

        // ── Compost Plant ─────────────────────────────────────────────────────
        StructureInfo compost = structures.get("compost_plant");
        if (compost != null && compost.built) {
            int cx = compost.x, cy = compost.y, cz = compost.z;
            Location centre = new Location(world, cx, cy, cz);

            registry.registerOffset("compost_processing", centre, 0, 1, 0);
            registered += 1;
        } else {
            plugin.getLogger().warning("[LocationRegistry] Compost Plant not yet built — skipping location registration.");
        }

        // ── Recycling Centre ──────────────────────────────────────────────────
        StructureInfo recycling = structures.get("recycling_centre");
        if (recycling != null && recycling.built) {
            int cx = recycling.x, cy = recycling.y, cz = recycling.z;
            Location centre = new Location(world, cx, cy, cz);

            registry.registerOffset("recycling_centre", centre, 0, 1, 0);
            registered += 1;
        } else {
            plugin.getLogger().warning("[LocationRegistry] Recycling Centre not yet built — skipping location registration.");
        }

        plugin.getLogger().info("[LocationRegistry] Registered " + registered + " structure locations.");
    }

    // ─── Sign helpers ─────────────────────────────────────────────────────────

    /**
     * Place a wall sign (OAK_WALL_SIGN) on the face of a block, attach it, and set its text lines.
     * The sign is placed at (x, y, z) facing the given direction (the face it's attached to is opposite).
     * Lines beyond the available 4 are silently ignored.
     */
    private void placeWallSign(World world, int x, int y, int z, BlockFace facing, String... lines) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.OAK_WALL_SIGN, false);

        // Set the facing on the block data
        org.bukkit.block.data.type.WallSign signData =
            (org.bukkit.block.data.type.WallSign) block.getBlockData();
        signData.setFacing(facing);
        block.setBlockData(signData, false);

        // Set text lines via the Sign tile entity
        if (block.getState() instanceof Sign sign) {
            SignSide front = sign.getSide(Side.FRONT);
            for (int i = 0; i < Math.min(lines.length, 4); i++) {
                front.line(i, Component.text(lines[i])
                    .color(NamedTextColor.DARK_GREEN)
                    .decoration(TextDecoration.BOLD, true));
            }
            sign.update(true, false);
        }
    }

    /**
     * Place a standing sign post (OAK_SIGN) at (x,y,z) facing the given direction.
     */
    private void placeStandingSign(World world, int x, int y, int z, BlockFace facing, String... lines) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.OAK_SIGN, false);

        org.bukkit.block.data.type.Sign signData =
            (org.bukkit.block.data.type.Sign) block.getBlockData();
        // OAK_SIGN rotation is set via WoodType sign — use yaw-based approach
        // Map BlockFace to 16-step rotation: S=0, W=4, N=8, E=12
        float yaw = switch (facing) {
            case NORTH -> 180f;
            case EAST  -> 270f;
            case SOUTH -> 0f;
            case WEST  -> 90f;
            default    -> 0f;
        };
        signData.setRotation(facing);
        block.setBlockData(signData, false);

        if (block.getState() instanceof Sign sign) {
            SignSide front = sign.getSide(Side.FRONT);
            for (int i = 0; i < Math.min(lines.length, 4); i++) {
                front.line(i, Component.text(lines[i])
                    .color(NamedTextColor.DARK_GREEN)
                    .decoration(TextDecoration.BOLD, true));
            }
            sign.update(true, false);
        }
    }

    // ─── Block direction helpers ──────────────────────────────────────────────

    /** Set a stair block with proper facing and half. */
    private void placeStairs(World world, int x, int y, int z, Material mat,
                              BlockFace facing, Stairs.Half half) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(mat, false);
        if (block.getBlockData() instanceof Stairs stairs) {
            stairs.setFacing(facing);
            stairs.setHalf(half);
            block.setBlockData(stairs, false);
        }
    }

    /** Set a door block with proper facing and hinge. Places both lower and upper halves. */
    private void placeDoor(World world, int x, int y, int z, Material mat,
                            BlockFace facing, Door.Hinge hinge) {
        Block lower = world.getBlockAt(x, y, z);
        Block upper = world.getBlockAt(x, y + 1, z);
        lower.setType(mat, false);
        upper.setType(mat, false);
        if (lower.getBlockData() instanceof Door doorData) {
            doorData.setFacing(facing);
            doorData.setHinge(hinge);
            doorData.setHalf(Bisected.Half.BOTTOM);
            lower.setBlockData(doorData, false);
        }
        if (upper.getBlockData() instanceof Door doorData) {
            doorData.setFacing(facing);
            doorData.setHinge(hinge);
            doorData.setHalf(Bisected.Half.TOP);
            upper.setBlockData(doorData, false);
        }
    }

    /** Set a log/wood block with proper axis. */
    private void placeLog(World world, int x, int y, int z, Material mat, Axis axis) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(mat, false);
        if (block.getBlockData() instanceof Orientable log) {
            log.setAxis(axis);
            block.setBlockData(log, false);
        }
    }

    /** Set a trapdoor with proper facing and half. */
    private void placeTrapdoor(World world, int x, int y, int z, Material mat,
                                BlockFace facing, Bisected.Half half, boolean open) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(mat, false);
        if (block.getBlockData() instanceof TrapDoor td) {
            td.setFacing(facing);
            td.setHalf(half);
            td.setOpen(open);
            block.setBlockData(td, false);
        }
    }

    // ─── Structure: Council Chamber ───────────────────────────────────────────

    private void buildCouncilChamber(World world, int sx, int sz) {
        if (structures.containsKey("council_chamber") && structures.get("council_chamber").built) return;

        int cx = sx + 5;
        int cz = sz + 90;
        int cy = getSolidGroundY(world, cx, cz);

        plugin.getLogger().info("[Structures] Building Council Chamber at " + cx + ", " + cy + ", " + cz);

        // 19 wide × 11 tall × 19 deep grand council hall
        // Floor (moss stone bricks)
        fillBlocks(world, cx - 9, cy, cz - 9, cx + 9, cy, cz + 9, Material.MOSSY_COBBLESTONE);

        // Outer walls (dark oak planks + mossy cobblestone)
        buildWalls(world, cx - 9, cy + 1, cz - 9, cx + 9, cy + 9, cz + 9, Material.MOSSY_COBBLESTONE);

        // Inner floor (polished andesite)
        fillBlocks(world, cx - 8, cy + 1, cz - 8, cx + 8, cy + 1, cz + 8, Material.POLISHED_ANDESITE);

        // Ceiling (oak planks)
        fillBlocks(world, cx - 9, cy + 10, cz - 9, cx + 9, cy + 10, cz + 9, Material.OAK_PLANKS);

        // Clear interior
        fillBlocks(world, cx - 8, cy + 2, cz - 8, cx + 8, cy + 9, cz + 8, Material.AIR);

        // Oak log pillars at corners and intervals — vertical axis (Y)
        int[] pillarX = {cx - 7, cx - 3, cx + 3, cx + 7};
        int[] pillarZ = {cz - 7, cz - 3, cz + 3, cz + 7};
        for (int px : pillarX) {
            for (int pz : pillarZ) {
                for (int py = cy + 1; py <= cy + 9; py++) {
                    placeLog(world, px, py, pz, Material.OAK_LOG, Axis.Y);
                }
            }
        }

        // Green stained glass windows on all 4 walls
        setGlassWindows(world, cx - 6, cy + 3, cz - 9, cx + 6, cy + 7, cz - 9, Material.GREEN_STAINED_GLASS_PANE);
        setGlassWindows(world, cx - 6, cy + 3, cz + 9, cx + 6, cy + 7, cz + 9, Material.GREEN_STAINED_GLASS_PANE);
        setGlassWindows(world, cx + 9, cy + 3, cz - 6, cx + 9, cy + 7, cz + 6, Material.GREEN_STAINED_GLASS_PANE);
        setGlassWindows(world, cx - 9, cy + 3, cz - 6, cx - 9, cy + 7, cz + 6, Material.GREEN_STAINED_GLASS_PANE);

        // Emerald block accents above door arches
        world.getBlockAt(cx, cy + 9, cz - 9).setType(Material.EMERALD_BLOCK);
        world.getBlockAt(cx, cy + 9, cz + 9).setType(Material.EMERALD_BLOCK);
        world.getBlockAt(cx + 9, cy + 9, cz).setType(Material.EMERALD_BLOCK);
        world.getBlockAt(cx - 9, cy + 9, cz).setType(Material.EMERALD_BLOCK);

        // North entrance: clear doorway (facing SOUTH = players walk in from north)
        for (int dx = -1; dx <= 1; dx++) {
            world.getBlockAt(cx + dx, cy + 1, cz - 9).setType(Material.AIR);
            world.getBlockAt(cx + dx, cy + 2, cz - 9).setType(Material.AIR);
        }
        // South entrance: clear doorway
        for (int dx = -1; dx <= 1; dx++) {
            world.getBlockAt(cx + dx, cy + 1, cz + 9).setType(Material.AIR);
            world.getBlockAt(cx + dx, cy + 2, cz + 9).setType(Material.AIR);
        }

        // Voting podium (centre, raised 2 blocks)
        fillBlocks(world, cx - 2, cy + 2, cz - 2, cx + 2, cy + 3, cz + 2, Material.QUARTZ_BLOCK);
        world.getBlockAt(cx, cy + 4, cz).setType(Material.LECTERN);

        // Emerald torch accents on podium
        for (int[] pos : new int[][]{{cx - 2, cz - 2}, {cx + 2, cz - 2}, {cx - 2, cz + 2}, {cx + 2, cz + 2}}) {
            world.getBlockAt(pos[0], cy + 4, pos[1]).setType(Material.SEA_LANTERN);
        }

        // Seating (oak stairs in rows around podium) — properly oriented
        buildSeatingRows(world, cx, cy + 1, cz);

        // Corner lanterns for ambiance
        for (int[] corner : new int[][]{{cx - 8, cz - 8}, {cx + 8, cz - 8}, {cx - 8, cz + 8}, {cx + 8, cz + 8}}) {
            world.getBlockAt(corner[0], cy + 10, corner[1]).setType(Material.GLOWSTONE);
            world.getBlockAt(corner[0], cy + 9, corner[1]).setType(Material.OAK_FENCE);
        }

        // Roof decoration - green terracotta pattern
        fillBlocks(world, cx - 4, cy + 11, cz - 4, cx + 4, cy + 11, cz + 4, Material.GREEN_TERRACOTTA);
        world.getBlockAt(cx, cy + 12, cz).setType(Material.EMERALD_BLOCK);

        // ── Entrance sign (north face, above the doorway) ──────────────────
        // Place sign on the outside wall face above the north entrance, text faces outward (SOUTH)
        placeWallSign(world, cx, cy + 3, cz - 9, BlockFace.SOUTH,
            "COUNCIL", "CHAMBER");

        StructureInfo info = new StructureInfo("council_chamber", "§2Council Chamber",
            cx, cy, cz, "Seat of Green Party governance. 94 agenda items pending.");
        info.assignedNpcs.addAll(Arrays.asList("Councillor Wheatgrass", "Policy Officer Sedge"));
        info.built = true;
        structures.put("council_chamber", info);

        plugin.getLogger().info("[Structures] Council Chamber built. The council is pleased. (They have already filed a planning dispute.)");
    }

    private void buildSeatingRows(World world, int cx, int cy, int cz) {
        // 3 rows of seating in a U-shape around the podium — stairs face the podium (inward)
        for (int row = 0; row < 3; row++) {
            int offset = 4 + row;
            // North row — stairs face SOUTH (toward podium)
            for (int x = cx - 6; x <= cx + 6; x++) {
                if (Math.abs(x - cx) > 2 || row > 0) {
                    placeStairs(world, x, cy + row, cz - offset,
                        Material.OAK_STAIRS, BlockFace.SOUTH, Stairs.Half.BOTTOM);
                }
            }
            // East row — stairs face WEST (toward podium)
            for (int z = cz - 6; z <= cz + 6; z++) {
                if (Math.abs(z - cz) > 2 || row > 0) {
                    placeStairs(world, cx + offset, cy + row, z,
                        Material.OAK_STAIRS, BlockFace.WEST, Stairs.Half.BOTTOM);
                }
            }
            // West row — stairs face EAST (toward podium)
            for (int z = cz - 6; z <= cz + 6; z++) {
                if (Math.abs(z - cz) > 2 || row > 0) {
                    placeStairs(world, cx - offset, cy + row, z,
                        Material.OAK_STAIRS, BlockFace.EAST, Stairs.Half.BOTTOM);
                }
            }
        }
    }

    // ─── Structure: Compost Processing Plant ─────────────────────────────────

    private void buildCompostPlant(World world, int sx, int sz) {
        if (structures.containsKey("compost_plant") && structures.get("compost_plant").built) return;

        int cx = sx + 90;
        int cz = sz + 10;
        int cy = getSolidGroundY(world, cx, cz);

        plugin.getLogger().info("[Structures] Building Compost Processing Plant at " + cx + ", " + cy + ", " + cz);

        // Main building: 17 wide × 10 tall × 13 deep (industrial)
        // Floor: stone slabs
        fillBlocks(world, cx - 8, cy, cz - 6, cx + 8, cy, cz + 6, Material.STONE_SLAB);

        // Walls: stone bricks
        buildWalls(world, cx - 8, cy + 1, cz - 6, cx + 8, cy + 9, cz + 6, Material.STONE_BRICKS);

        // Ceiling: iron trapdoors (industrial grating look) — open flat, facing NORTH
        for (int x = cx - 7; x <= cx + 7; x++) {
            for (int z = cz - 5; z <= cz + 5; z++) {
                placeTrapdoor(world, x, cy + 9, z, Material.IRON_TRAPDOOR,
                    BlockFace.NORTH, Bisected.Half.TOP, false);
            }
        }

        // Clear interior
        fillBlocks(world, cx - 7, cy + 1, cz - 5, cx + 7, cy + 8, cz + 5, Material.AIR);

        // Iron bar windows (industrial)
        for (int x = cx - 6; x <= cx + 6; x += 3) {
            for (int y = cy + 3; y <= cy + 7; y++) {
                world.getBlockAt(x, y, cz - 6).setType(Material.IRON_BARS);
                world.getBlockAt(x, y, cz + 6).setType(Material.IRON_BARS);
            }
        }
        for (int z = cz - 5; z <= cz + 5; z += 3) {
            for (int y = cy + 3; y <= cy + 7; y++) {
                world.getBlockAt(cx - 8, y, z).setType(Material.IRON_BARS);
                world.getBlockAt(cx + 8, y, z).setType(Material.IRON_BARS);
            }
        }

        // Entrance doors (north wall, facing SOUTH so they open inward)
        for (int dx = -1; dx <= 1; dx++) {
            world.getBlockAt(cx + dx, cy + 1, cz - 6).setType(Material.AIR);
            world.getBlockAt(cx + dx, cy + 2, cz - 6).setType(Material.AIR);
        }

        // Compost processing rows (3 rows of composters with hoppers beneath)
        for (int row = 0; row < 3; row++) {
            int rowZ = cz - 4 + (row * 3);
            for (int x = cx - 6; x <= cx + 6; x += 2) {
                world.getBlockAt(x, cy + 1, rowZ).setType(Material.COMPOSTER);
                world.getBlockAt(x, cy + 2, rowZ).setType(Material.HOPPER);
                world.getBlockAt(x, cy + 3, rowZ).setType(Material.BARREL);
            }
        }

        // Central processing area: cauldrons and furnaces
        for (int x = cx - 2; x <= cx + 2; x++) {
            world.getBlockAt(x, cy + 1, cz).setType(Material.CAULDRON);
        }
        world.getBlockAt(cx - 4, cy + 1, cz).setType(Material.BLAST_FURNACE);
        world.getBlockAt(cx + 4, cy + 1, cz).setType(Material.BLAST_FURNACE);

        // Overhead pipe system (iron bars + chains)
        for (int x = cx - 6; x <= cx + 6; x++) {
            world.getBlockAt(x, cy + 8, cz - 3).setType(Material.IRON_BARS);
            world.getBlockAt(x, cy + 8, cz + 3).setType(Material.IRON_BARS);
        }
        for (int z = cz - 5; z <= cz + 5; z++) {
            world.getBlockAt(cx, cy + 8, z).setType(Material.CHAIN);
        }

        // Glowstone lighting
        for (int[] pos : new int[][]{{cx - 5, cz - 4}, {cx + 5, cz - 4}, {cx - 5, cz + 4}, {cx + 5, cz + 4}, {cx, cz}}) {
            world.getBlockAt(pos[0], cy + 8, pos[1]).setType(Material.GLOWSTONE);
        }

        // Chimney stacks on roof
        for (int[] chimney : new int[][]{{cx - 6, cz - 5}, {cx + 6, cz - 5}, {cx - 6, cz + 5}, {cx + 6, cz + 5}}) {
            for (int y = cy + 9; y <= cy + 14; y++) {
                world.getBlockAt(chimney[0], y, chimney[1]).setType(Material.COBBLESTONE_WALL);
            }
            world.getBlockAt(chimney[0], cy + 15, chimney[1]).setType(Material.CAMPFIRE);
        }

        // ── Entrance sign above north doorway, facing outward (SOUTH) ──────
        placeWallSign(world, cx, cy + 3, cz - 6, BlockFace.SOUTH,
            "COMPOST", "PROCESSING", "PLANT");

        StructureInfo info = new StructureInfo("compost_plant", "§aCompost Processing Plant",
            cx, cy, cz, "Industrial-scale composting facility. 17 active composters. The smell is intentional.");
        info.assignedNpcs.add("Elder Composting Sage Fern");
        info.built = true;
        structures.put("compost_plant", info);

        plugin.getLogger().info("[Structures] Compost Processing Plant complete. Industrial efficiency meets sustainable living.");
    }

    // ─── Structure: Tree Farm ─────────────────────────────────────────────────

    private void buildTreeFarm(World world, int sx, int sz) {
        if (structures.containsKey("tree_farm") && structures.get("tree_farm").built) return;

        int cx = sx - 90;
        int cz = sz + 30;
        int cy = getSolidGroundY(world, cx, cz);

        plugin.getLogger().info("[Structures] Building Tree Farm at " + cx + ", " + cy + ", " + cz);

        // Large greenhouse: 21 wide × 8 tall × 17 deep
        // Frame: oak logs (vertical Y axis on corners)
        buildGreenhouse(world, cx, cy, cz, 10, 8, 10);

        // Interior: grass floor with water channels
        fillBlocks(world, cx - 9, cy + 1, cz - 9, cx + 9, cy + 1, cz + 9, Material.GRASS_BLOCK);

        // Water channels (4 channels for irrigation)
        for (int x = cx - 8; x <= cx + 8; x += 4) {
            fillBlocks(world, x, cy + 1, cz - 8, x, cy + 1, cz + 8, Material.WATER);
        }

        // Soil beds for saplings (between water channels)
        for (int bed = 0; bed < 3; bed++) {
            int bedX = cx - 6 + (bed * 6);
            fillBlocks(world, bedX - 1, cy + 1, cz - 8, bedX + 1, cy + 1, cz + 8, Material.DIRT);
            // Saplings planted in beds
            Material[] saplings = {Material.OAK_SAPLING, Material.BIRCH_SAPLING, Material.SPRUCE_SAPLING,
                Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.CHERRY_SAPLING};
            int saplingIndex = bed * 2;
            for (int z = cz - 7; z <= cz + 7; z += 3) {
                world.getBlockAt(bedX, cy + 2, z).setType(saplings[saplingIndex % saplings.length]);
                world.getBlockAt(bedX, cy + 2, z + 1).setType(saplings[(saplingIndex + 1) % saplings.length]);
            }
        }

        // Multi-level platforms (oak slabs at y+3, y+5)
        for (int level = 0; level < 2; level++) {
            int py = cy + 3 + (level * 2);
            // Side platforms
            fillBlocks(world, cx - 9, py, cz - 4, cx - 7, py, cz + 4, Material.OAK_SLAB);
            fillBlocks(world, cx + 7, py, cz - 4, cx + 9, py, cz + 4, Material.OAK_SLAB);
            // Ladders to platforms (facing EAST on west side, WEST on east side)
            for (int y = cy + 2; y <= py; y++) {
                Block ladderW = world.getBlockAt(cx - 7, y, cz);
                ladderW.setType(Material.LADDER, false);
                if (ladderW.getBlockData() instanceof org.bukkit.block.data.type.Ladder ladder) {
                    ladder.setFacing(BlockFace.EAST);
                    ladderW.setBlockData(ladder, false);
                }
                Block ladderE = world.getBlockAt(cx + 7, y, cz);
                ladderE.setType(Material.LADDER, false);
                if (ladderE.getBlockData() instanceof org.bukkit.block.data.type.Ladder ladder) {
                    ladder.setFacing(BlockFace.WEST);
                    ladderE.setBlockData(ladder, false);
                }
            }
        }

        // Watering stations (cauldrons with water)
        for (int z = cz - 6; z <= cz + 6; z += 6) {
            world.getBlockAt(cx - 9, cy + 2, z).setType(Material.WATER_CAULDRON);
            world.getBlockAt(cx + 9, cy + 2, z).setType(Material.WATER_CAULDRON);
        }

        // Glow lighting via sea lanterns on ceiling
        for (int x = cx - 6; x <= cx + 6; x += 4) {
            for (int z = cz - 6; z <= cz + 6; z += 4) {
                world.getBlockAt(x, cy + 8, z).setType(Material.SEA_LANTERN);
            }
        }

        // Entrance (south side — cz - 10)
        for (int dx = -1; dx <= 1; dx++) {
            world.getBlockAt(cx + dx, cy + 2, cz - 10).setType(Material.AIR);
            world.getBlockAt(cx + dx, cy + 3, cz - 10).setType(Material.AIR);
        }

        // Composter stations at entrance
        world.getBlockAt(cx - 3, cy + 2, cz - 9).setType(Material.COMPOSTER);
        world.getBlockAt(cx + 3, cy + 2, cz - 9).setType(Material.COMPOSTER);
        world.getBlockAt(cx - 3, cy + 2, cz + 9).setType(Material.COMPOSTER);
        world.getBlockAt(cx + 3, cy + 2, cz + 9).setType(Material.COMPOSTER);

        // ── Entrance sign on south glass wall, facing outward (SOUTH) ──────
        // Place sign on the glass pane above the entrance opening
        placeWallSign(world, cx, cy + 4, cz - 10, BlockFace.SOUTH,
            "TREE FARM");

        StructureInfo info = new StructureInfo("tree_farm", "§2Tree Farm",
            cx, cy, cz, "Active reforestation greenhouse. 6 sapling varieties. 847 trees planned.");
        info.assignedNpcs.addAll(Arrays.asList("Protest Coordinator Meadow", "Recycling Evangelist Bramble"));
        info.built = true;
        structures.put("tree_farm", info);

        plugin.getLogger().info("[Structures] Tree Farm built. The saplings are already filing motions to grow faster.");
    }

    private void buildGreenhouse(World world, int cx, int cy, int cz, int rx, int ry, int rz) {
        // Glass walls
        fillBlocks(world, cx - rx, cy + 1, cz - rz, cx + rx, cy + ry, cz - rz, Material.GLASS);
        fillBlocks(world, cx - rx, cy + 1, cz + rz, cx + rx, cy + ry, cz + rz, Material.GLASS);
        fillBlocks(world, cx - rx, cy + 1, cz - rz, cx - rx, cy + ry, cz + rz, Material.GLASS);
        fillBlocks(world, cx + rx, cy + 1, cz - rz, cx + rx, cy + ry, cz + rz, Material.GLASS);
        // Oak log frame corners — Y axis (vertical)
        for (int y = cy + 1; y <= cy + ry; y++) {
            placeLog(world, cx - rx, y, cz - rz, Material.OAK_LOG, Axis.Y);
            placeLog(world, cx + rx, y, cz - rz, Material.OAK_LOG, Axis.Y);
            placeLog(world, cx - rx, y, cz + rz, Material.OAK_LOG, Axis.Y);
            placeLog(world, cx + rx, y, cz + rz, Material.OAK_LOG, Axis.Y);
        }
        // Glass ceiling
        fillBlocks(world, cx - rx, cy + ry + 1, cz - rz, cx + rx, cy + ry + 1, cz + rz, Material.GLASS);
        // Base frame — X-axis logs along north/south edges, Z-axis logs along east/west edges
        for (int x = cx - rx; x <= cx + rx; x++) {
            placeLog(world, x, cy + 1, cz - rz, Material.OAK_LOG, Axis.X);
            placeLog(world, x, cy + 1, cz + rz, Material.OAK_LOG, Axis.X);
        }
        for (int z = cz - rz; z <= cz + rz; z++) {
            placeLog(world, cx - rx, cy + 1, z, Material.OAK_LOG, Axis.Z);
            placeLog(world, cx + rx, cy + 1, z, Material.OAK_LOG, Axis.Z);
        }
    }

    // ─── Structure: Recycling Centre ─────────────────────────────────────────

    private void buildRecyclingCentre(World world, int sx, int sz) {
        if (structures.containsKey("recycling_centre") && structures.get("recycling_centre").built) return;

        int cx = sx - 10;
        int cz = sz - 90;
        int cy = getSolidGroundY(world, cx, cz);

        plugin.getLogger().info("[Structures] Building Recycling Centre at " + cx + ", " + cy + ", " + cz);

        // Sorting facility: 17 wide × 9 tall × 15 deep
        // Floor: polished blackstone (clean industrial)
        fillBlocks(world, cx - 8, cy, cz - 7, cx + 8, cy, cz + 7, Material.POLISHED_BLACKSTONE);

        // Walls: gray concrete + white concrete mix
        buildWalls(world, cx - 8, cy + 1, cz - 7, cx + 8, cy + 8, cz + 7, Material.GRAY_CONCRETE);

        // White concrete upper band
        for (int x = cx - 8; x <= cx + 8; x++) {
            world.getBlockAt(x, cy + 7, cz - 7).setType(Material.WHITE_CONCRETE);
            world.getBlockAt(x, cy + 8, cz - 7).setType(Material.WHITE_CONCRETE);
            world.getBlockAt(x, cy + 7, cz + 7).setType(Material.WHITE_CONCRETE);
            world.getBlockAt(x, cy + 8, cz + 7).setType(Material.WHITE_CONCRETE);
        }
        for (int z = cz - 7; z <= cz + 7; z++) {
            world.getBlockAt(cx - 8, cy + 7, z).setType(Material.WHITE_CONCRETE);
            world.getBlockAt(cx - 8, cy + 8, z).setType(Material.WHITE_CONCRETE);
            world.getBlockAt(cx + 8, cy + 7, z).setType(Material.WHITE_CONCRETE);
            world.getBlockAt(cx + 8, cy + 8, z).setType(Material.WHITE_CONCRETE);
        }

        // Flat white concrete roof
        fillBlocks(world, cx - 8, cy + 9, cz - 7, cx + 8, cy + 9, cz + 7, Material.WHITE_CONCRETE);

        // Clear interior
        fillBlocks(world, cx - 7, cy + 1, cz - 6, cx + 7, cy + 8, cz + 6, Material.AIR);

        // Smooth quartz interior floor
        fillBlocks(world, cx - 7, cy + 1, cz - 6, cx + 7, cy + 1, cz + 6, Material.SMOOTH_QUARTZ);

        // Window strips
        for (int x = cx - 6; x <= cx + 6; x += 4) {
            for (int y = cy + 3; y <= cy + 6; y++) {
                world.getBlockAt(x, y, cz - 7).setType(Material.CYAN_STAINED_GLASS_PANE);
                world.getBlockAt(x, y, cz + 7).setType(Material.CYAN_STAINED_GLASS_PANE);
            }
        }

        // Entrance (south — cz - 7)
        for (int dx = -1; dx <= 1; dx++) {
            world.getBlockAt(cx + dx, cy + 2, cz - 7).setType(Material.AIR);
            world.getBlockAt(cx + dx, cy + 3, cz - 7).setType(Material.AIR);
        }

        // SORTING STATIONS — 4 colour-coded zones
        // Green zone (organic)
        fillBlocks(world, cx - 6, cy + 2, cz - 5, cx - 2, cy + 2, cz - 5, Material.LIME_CONCRETE);
        for (int x = cx - 6; x <= cx - 2; x++) {
            world.getBlockAt(x, cy + 2, cz - 5).setType(Material.LIME_CONCRETE);
            world.getBlockAt(x, cy + 3, cz - 5).setType(Material.LIME_CONCRETE);
            world.getBlockAt(x, cy + 2, cz - 4).setType(Material.COMPOSTER);
        }

        // Yellow zone (glass/recyclable)
        fillBlocks(world, cx + 2, cy + 2, cz - 5, cx + 6, cy + 2, cz - 5, Material.YELLOW_CONCRETE);
        for (int x = cx + 2; x <= cx + 6; x++) {
            world.getBlockAt(x, cy + 3, cz - 5).setType(Material.YELLOW_CONCRETE);
            world.getBlockAt(x, cy + 2, cz - 4).setType(Material.BARREL);
        }

        // Blue zone (paper/cardboard)
        fillBlocks(world, cx - 6, cy + 2, cz + 4, cx - 2, cy + 2, cz + 4, Material.LIGHT_BLUE_CONCRETE);
        for (int x = cx - 6; x <= cx - 2; x++) {
            world.getBlockAt(x, cy + 3, cz + 4).setType(Material.LIGHT_BLUE_CONCRETE);
            world.getBlockAt(x, cy + 2, cz + 5).setType(Material.CHEST);
        }

        // Red zone (general waste — for chaos players)
        fillBlocks(world, cx + 2, cy + 2, cz + 4, cx + 6, cy + 2, cz + 4, Material.RED_CONCRETE);
        for (int x = cx + 2; x <= cx + 6; x++) {
            world.getBlockAt(x, cy + 3, cz + 4).setType(Material.RED_CONCRETE);
            world.getBlockAt(x, cy + 2, cz + 5).setType(Material.BARREL);
        }

        // Central conveyor belt (iron trapdoors in a line) — flat on top
        for (int z = cz - 5; z <= cz + 5; z++) {
            placeTrapdoor(world, cx,     cy + 2, z, Material.IRON_TRAPDOOR, BlockFace.NORTH, Bisected.Half.TOP, false);
            placeTrapdoor(world, cx - 1, cy + 2, z, Material.IRON_TRAPDOOR, BlockFace.NORTH, Bisected.Half.TOP, false);
            placeTrapdoor(world, cx + 1, cy + 2, z, Material.IRON_TRAPDOOR, BlockFace.NORTH, Bisected.Half.TOP, false);
        }
        // Hopper chain under conveyor
        for (int z = cz - 5; z <= cz + 5; z++) {
            world.getBlockAt(cx, cy + 1, z).setType(Material.HOPPER);
        }

        // Overhead lighting (sea lanterns)
        for (int x = cx - 5; x <= cx + 5; x += 3) {
            for (int z2 = cz - 5; z2 <= cz + 5; z2 += 3) {
                world.getBlockAt(x, cy + 8, z2).setType(Material.SEA_LANTERN);
            }
        }

        // Recycling bins around the exterior
        for (int i = -6; i <= 6; i += 3) {
            world.getBlockAt(cx + i, cy + 1, cz - 8).setType(Material.BARREL);
            world.getBlockAt(cx + i, cy + 1, cz + 8).setType(Material.BARREL);
        }

        // Logo on roof: emerald block pattern
        world.getBlockAt(cx, cy + 10, cz).setType(Material.EMERALD_BLOCK);
        world.getBlockAt(cx - 1, cy + 10, cz).setType(Material.EMERALD_BLOCK);
        world.getBlockAt(cx + 1, cy + 10, cz).setType(Material.EMERALD_BLOCK);
        world.getBlockAt(cx, cy + 10, cz - 1).setType(Material.EMERALD_BLOCK);
        world.getBlockAt(cx, cy + 10, cz + 1).setType(Material.EMERALD_BLOCK);

        // ── Entrance sign above south doorway, facing outward (SOUTH) ───────
        placeWallSign(world, cx, cy + 4, cz - 7, BlockFace.SOUTH,
            "RECYCLING", "CENTRE");

        StructureInfo info = new StructureInfo("recycling_centre", "§bRecycling Centre",
            cx, cy, cz, "4-zone sorting facility. Organic, recyclable, paper, and general waste. The council is very proud.");
        info.assignedNpcs.add("Recycling Evangelist Bramble");
        info.built = true;
        structures.put("recycling_centre", info);

        plugin.getLogger().info("[Structures] Recycling Centre complete. 4 sorting zones operational. The bins are already overfull.");
    }

    // ─── Block Helpers ────────────────────────────────────────────────────────

    private void fillBlocks(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material mat) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    world.getBlockAt(x, y, z).setType(mat, false);
                }
            }
        }
    }

    private void buildWalls(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material mat) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                world.getBlockAt(x, y, z1).setType(mat, false);
                world.getBlockAt(x, y, z2).setType(mat, false);
            }
            for (int z = z1; z <= z2; z++) {
                world.getBlockAt(x1, y, z).setType(mat, false);
                world.getBlockAt(x2, y, z).setType(mat, false);
            }
        }
    }

    private void setGlassWindows(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material mat) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() != Material.AIR) {
                        b.setType(mat, false);
                    }
                }
            }
        }
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    /**
     * Seeds the structures map with un-built placeholder entries for the 4 default
     * structures. Called before loadData() so that if structures.json doesn't exist
     * (fresh start or deleted file) the map is never empty, and initialiseStructures()
     * will find !info.built entries and proceed to generate them.
     *
     * Placeholder coordinates are (0, 0, 0) — they are overwritten by the real build
     * methods (which compute proper coordinates from spawn) before saving.
     */
    private void initializeDefaultStructures() {
        if (!structures.containsKey("council_chamber")) {
            structures.put("council_chamber", new StructureInfo(
                "council_chamber", "§2Council Chamber", 0, 0, 0,
                "Seat of Green Party governance. 94 agenda items pending."));
        }
        if (!structures.containsKey("compost_plant")) {
            structures.put("compost_plant", new StructureInfo(
                "compost_plant", "§aCompost Processing Plant", 0, 0, 0,
                "Industrial-scale composting facility. 17 active composters. The smell is intentional."));
        }
        if (!structures.containsKey("tree_farm")) {
            structures.put("tree_farm", new StructureInfo(
                "tree_farm", "§2Tree Farm", 0, 0, 0,
                "Active reforestation greenhouse. 6 sapling varieties. 847 trees planned."));
        }
        if (!structures.containsKey("recycling_centre")) {
            structures.put("recycling_centre", new StructureInfo(
                "recycling_centre", "§bRecycling Centre", 0, 0, 0,
                "4-zone sorting facility. Organic, recyclable, paper, and general waste. The council is very proud."));
        }
    }

    private void loadData() {
        // Seed defaults FIRST so the map is never empty on a fresh start.
        // Any persisted data loaded below will overwrite these placeholders.
        initializeDefaultStructures();

        dataFile = new File(plugin.getDataFolder(), "structures.json");
        if (!dataFile.exists()) return;

        try (Reader r = new FileReader(dataFile)) {
            Type t = new TypeToken<Map<String, StructureInfo>>() {}.getType();
            Map<String, StructureInfo> loaded = gson.fromJson(r, t);
            if (loaded != null) structures.putAll(loaded);
        } catch (IOException e) {
            plugin.getLogger().warning("[Structures] Could not load structure data: " + e.getMessage());
        }
    }

    public void saveData() {
        try (Writer w = new FileWriter(dataFile)) {
            gson.toJson(structures, w);
        } catch (IOException e) {
            plugin.getLogger().warning("[Structures] Could not save structure data: " + e.getMessage());
        }
    }

    // ─── Player Info ──────────────────────────────────────────────────────────

    public void sendStructureInfo(Player player, String id) {
        StructureInfo info = getStructure(id);
        if (info == null) {
            player.sendMessage("§cUnknown structure: §7" + id);
            player.sendMessage("§7Known structures: §acouncil_chamber§7, §acompost_plant§7, §atree_farm§7, §arecycling_centre");
            return;
        }

        player.sendMessage("§2§l===== " + info.displayName + " §2§l=====");
        player.sendMessage("§7Location: " + info.locationString());
        player.sendMessage("§7Status: " + (info.built ? "§aBuilt ✓" : "§cPending construction"));
        player.sendMessage("§7Description: §f" + info.description);
        if (!info.assignedNpcs.isEmpty()) {
            player.sendMessage("§7Assigned NPCs: §a" + String.join("§7, §a", info.assignedNpcs));
        }
        player.sendMessage("§2§l" + "=".repeat(30));
    }
}
