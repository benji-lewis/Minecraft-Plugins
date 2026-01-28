# Kim Jong Un 3 — Admin Guide

## Requirements
- PaperMC 1.21+ (built against 1.21.4)
- Java 21+
- Nova 0.22+

## Installation
1. Build the addon: `./gradlew addonJar -PoutDir="<path to your server addons folder>"` (from `kim-jong-un-3/`).
2. Start the server to load the addon and generate `plugins/KimJongUn3/config.yml`.

## Resource Pack Notes
Kim Jong Un 3 is a Nova addon, so its items are integrated into Nova's generated resource pack. You can add custom textures and models under
`kim-jong-un-3/src/main/resources/resourcepack/` if you want bespoke visuals, then regenerate Nova's resource pack as you normally would.
The texture generator writes to `kim-jong-un-3/build/generated/resourcepack/` by default and is invoked automatically during `processResources`.
Run `python3 kim-jong-un-3/scripts/generate_textures.py` to regenerate the shipped textures before rebuilding the addon or Nova pack.

## Configuration
See `plugins/KimJongUn3/config.yml`:
- `spawn.interval-seconds`: how often the spawn check runs.
- `spawn.chance-per-interval`: probability to spawn on each tick.
- `spawn.max-active`: maximum simultaneous Kim Jong Un 3 mobs.
- `spawn.min-distance-from-player` / `spawn.max-distance-from-player`:
  spawn distance range.
- `launch.*`: missile ascent ticks, step size, explosion power, fireworks.
- `skin.player-name`: Minecraft username whose skin is rendered on Kim Jong Un 3.
- `skin.custom-model-data`: optional custom model data for the mob head.

## Commands & Permissions
### `/kimjongun3 give <player> <item>`
Gives a part or assembled item.
- Permission: `kimjongun3.admin`
- Items: `missile_nose`, `missile_body`, `missile_engine`, `launchpad_base`, `launchpad_control`, `launchpad_support`, `missile`, `launchpad`

### `/kimjongun3 spawn`
Spawns the mob at your location.
- Permission: `kimjongun3.admin`

### Permissions Summary
- `kimjongun3.admin` — admin commands (including spawn).
- `kimjongun3.use` — place launchpads and launch missiles.
