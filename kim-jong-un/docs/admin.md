# Kim Jong Un 3 — Admin Guide

## Requirements
- PaperMC 1.21+ (built against 1.21.4)
- Java 21+
- No additional plugin dependencies required

## Installation
1. Build the plugin: `./gradlew jar` (from `kim-jong-un-3/`).
2. Start the server to load the plugin and generate `plugins/KimJongUn3/config.yml`.

## Resource Pack Notes
Kim Jong Un 3 ships with resource-pack assets. You can add custom textures and models under
`kim-jong-un-3/src/main/resources/resourcepack/` if you want bespoke visuals, then regenerate and distribute your resource pack.
The texture generator writes to `kim-jong-un-3/build/generated/resourcepack/` by default and is invoked automatically during `processResources`.
Run `python3 kim-jong-un-3/scripts/generate_textures.py` to regenerate the shipped textures before rebuilding the plugin or resource pack.

## Configuration
See `plugins/KimJongUn3/config.yml`:
- `spawn.interval-seconds`: how often the spawn check runs.
- `spawn.chance-per-interval`: probability to spawn on each tick.
- `spawn.max-active`: maximum simultaneous Kim Jong Un 3 mobs.
- `spawn.min-distance-from-player` / `spawn.max-distance-from-player`:
  spawn distance range.
- `drops.icbm-core-chance`: chance for the ICBM core to drop per mob kill.
- `launch.*`: missile ascent ticks, step size, explosion power, fireworks.
- `icbm.*`: ICBM flight profile, nuclear blast, mushroom cloud, and fallout tuning.
- `auto-update.*`: GitHub Actions artifact auto-updater settings (optional GitHub token).
- `skin.player-name`: Minecraft username whose skin is rendered on Kim Jong Un 3.
- `skin.custom-model-data`: optional custom model data for the mob head.

## Commands & Permissions
### `/kimjongun3 give <player> <item>`
Gives a part or assembled item.
- Permission: `kimjongun3.admin`
- Items: `missile_nose`, `missile_body`, `missile_engine`, `launchpad_base`, `launchpad_control`, `launchpad_support`, `icbm_core`, `missile`, `icbm`, `launchpad`
- Radiation suit items: `radiation_helmet`, `radiation_chestplate`, `radiation_leggings`, `radiation_boots`

### `/kimjongun3 spawn`
Spawns the mob at your location.
- Permission: `kimjongun3.admin`

### Permissions Summary
- `kimjongun3.admin` — admin commands (including spawn).
- `kimjongun3.use` — place launchpads and launch missiles.

## Auto-updater Notes
The auto-updater checks the latest successful `main` workflow run for the `kim-jong-un-3-plugin` artifact and
stages the jar in the `plugins/update/` folder. For private workflows or stricter rate limits, set
`auto-update.github-token` to a GitHub token with `actions:read` access.
