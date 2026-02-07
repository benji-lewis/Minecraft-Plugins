# Transport Plugin

The Transport plugin adds configurable vehicle systems for trains, planes, cars, rail networks, and spaceships. Each vehicle category is a module you can enable or tune through `config.yml`.

## Commands
- `/transport status` - lists module enablement and loaded module count.
- `/transport reload` - reloads the configuration file.
- `/transport give <player> <item> [amount]` - gives a transport item to a player (omit `<player>` to give to yourself).

## Crafting & Usage
Transport vehicles and kits are crafted via custom recipes and used by right-clicking with the crafted item. See the player documentation for full recipes and usage guidance.

## Modules

### Trains
- Config path: `modules.trains`
- Key options: `max-cars`, `default-speed`, `whistle-sound`

### Planes
- Config path: `modules.planes`
- Key options: `cruise-altitude`, `max-speed`, `runway-length`

### Cars
- Config path: `modules.cars`
- Key options: `max-speed`, `default-fuel`, `horn-sound`

### Rail systems
- Config path: `modules.rail-systems`
- Key options: `rail-types`, `signal-interval`

### Spaceships
- Config path: `modules.spaceships`
- Key options: `max-speed`, `auto-launch`
- Optional SpaceTravel integration: set `modules.spaceships.integration.enable-space-travel` to `true` and ensure the SpaceTravel plugin is installed.

## Configuration
See `config.yml` for module toggles and tuning options.

## Player Documentation
Player documentation lives in `docs/MC-Docs/docs/plugins/transport.md`.
