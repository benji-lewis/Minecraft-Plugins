# Kim Jong Un — Admin Guide

## Requirements
- PaperMC 1.21+ (built against 1.21.4)
- Java 21+
- No additional plugin dependencies required

## Installation
1. Build the plugin: `./gradlew jar` (from `kim-jong-un/`).
2. Start the server to load the plugin and generate `plugins/KimJongUn/config.yml`.

## Resource Pack Notes
Kim Jong Un ships with resource-pack assets. You can add custom textures and models under
`kim-jong-un/src/main/resources/resourcepack/` if you want bespoke visuals, then regenerate and distribute your resource pack.
The texture generator writes to `kim-jong-un/build/generated/resourcepack/` by default and is invoked automatically during `processResources`.
Run `python3 kim-jong-un/scripts/generate_textures.py` to regenerate the shipped textures before rebuilding the plugin or resource pack.

## Configuration
See `plugins/KimJongUn/config.yml`:
- `spawn.interval-seconds`: how often the spawn check runs.
- `spawn.chance-per-interval`: probability to spawn on each tick.
- `spawn.max-active`: maximum simultaneous Kim Jong Un mobs.
- `spawn.min-distance-from-player` / `spawn.max-distance-from-player`:
  spawn distance range.
- `drops.icbm-core-chance`: chance for the ICBM core to drop per mob kill.
- `launch.*`: missile ascent ticks, step size, explosion power, fireworks.
- `icbm.*`: ICBM flight profile, nuclear blast, mushroom cloud, and fallout tuning.
- `auto-update.*`: GitHub Actions artifact auto-updater settings (optional GitHub token).
- `skin.player-name`: Minecraft username whose skin is rendered on Kim Jong Un.
- `skin.custom-model-data`: optional custom model data for the mob head.

## Commands & Permissions
### `/kimjongun give <player> <item>`
Gives a part or assembled item.
- Permission: `kimjongun.admin`
- Items: `missile_nose`, `missile_body`, `missile_engine`, `launchpad_base`, `launchpad_control`, `launchpad_support`, `icbm_core`, `missile`, `icbm`, `launchpad`
- Radiation suit items: `radiation_helmet`, `radiation_chestplate`, `radiation_leggings`, `radiation_boots`

### `/kimjongun spawn`
Spawns the mob at your location.
- Permission: `kimjongun.admin`

### Permissions Summary
- `kimjongun.admin` — admin commands (including spawn).
- `kimjongun.use` — place launchpads and launch missiles.

## Auto-updater Notes
The auto-updater checks the latest successful `main` workflow run for the `kim-jong-un-plugin` artifact and
stages the jar in the `plugins/update/` folder. For private workflows or stricter rate limits, set
`auto-update.github-token` to a GitHub token with `actions:read` access.
