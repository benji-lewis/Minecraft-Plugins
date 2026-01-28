# Kim Jong Un 2 — Admin Guide

## Requirements
- PaperMC 1.20+ (built against 1.21.4)
- Java 17+
- Optional: Nova (plugin will run without it; custom models rely on a resource pack, not Nova)

## Installation
1. Build the plugin: `mvn -f kim-jong-un-2/pom.xml package`.
2. Copy `kim-jong-un-2/target/kim-jong-un-2-1.0.0.jar` into your server `plugins/` folder.
3. Copy the `resourcepack/` folder from this plugin into your server resource pack, or merge it into your existing pack.
4. Restart the server to generate `plugins/KimJongUn2/config.yml`.

## Resource Pack Setup
The plugin ships model overrides for:
- `iron_ingot` (missile parts)
- `netherite_scrap` (launchpad parts)
- `nether_star` (assembled missile + launchpad)

You must merge the provided files under:
```
kim-jong-un-2/src/main/resources/resourcepack/
```
into your resource pack so the custom model data values map to the 3D models. Nova is not required for the visuals; the resource pack alone drives the 3D appearance.

This repository intentionally excludes binary texture files. Add your own PNG textures at:
- `assets/kimjongun2/textures/item/missile.png`
- `assets/kimjongun2/textures/item/launchpad.png`
- `assets/kimjongun2/textures/item/missile_part.png`
- `assets/kimjongun2/textures/item/launchpad_part.png`
You can also generate placeholder textures by running
`python3 kim-jong-un-2/scripts/generate_textures.py`, which writes PNGs into the resource pack folder.

### Nova Resource Pack Users (No Config Changes)
If your server already uses the Nova resource pack, you do not need to change Nova configuration. Just merge the files from
`kim-jong-un-2/src/main/resources/resourcepack/` into the Nova resource pack folder and regenerate/redistribute the pack as you normally would for Nova.
The custom model data values in `config.yml` are already aligned with the provided overrides, so no Nova item config edits are required.

## Configuration
See `plugins/KimJongUn2/config.yml`:
- `spawn.interval-seconds`: how often the spawn check runs.
- `spawn.chance-per-interval`: probability to spawn on each tick.
- `spawn.max-active`: maximum simultaneous Kim Jong Un 2 mobs.
- `spawn.min-distance-from-player` / `spawn.max-distance-from-player`:
  spawn distance range.
- `launch.*`: missile ascent ticks, step size, explosion power, fireworks.
- `skin.player-name`: Minecraft username whose skin is rendered on Kim Jong Un 2.
- `models.*`: custom model data values (ensure they match your resource pack overrides).

## Commands & Permissions
### `/kimjongun2 give <player> <item>`
Gives a part or assembled item.
- Permission: `kimjongun2.admin`
- Items: `missile_nose`, `missile_body`, `missile_engine`, `launchpad_base`, `launchpad_control`, `launchpad_support`, `missile`, `launchpad`

### `/kimjongun2 spawn`
Spawns the mob at your location.
- Permission: `kimjongun2.spawn`

### Permissions Summary
- `kimjongun2.admin` — admin commands.
- `kimjongun2.spawn` — spawn command access.
- `kimjongun2.use` — place launchpads and launch missiles.

## Nova Note
If you want to treat the launchpad/missile as Nova items/blocks, the provided custom model data files are compatible with Nova item packs. Map the same model data IDs in your Nova addon configuration as needed.
