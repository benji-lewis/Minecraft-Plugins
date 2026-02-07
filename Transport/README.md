# Transport Plugin

The Transport plugin adds configurable vehicle systems for trains, planes, cars, rail networks, and spaceships. Each vehicle category is a module you can enable or tune through `config.yml`.

## Commands
- `/transport status` - lists module enablement and loaded module count.
- `/transport reload` - reloads the configuration file.
- `/transport give <player> <item> [amount]` - gives a transport item to a player (omit `<player>` to give to yourself).

## Crafting & Usage
Transport vehicles and kits are crafted via custom recipes and used by right-clicking with the crafted item. Planes require a clear runway to deploy, and cars now run on fuel. See the player documentation for full recipes and usage guidance.

## Modules

### Trains
- Config path: `modules.trains`
- Key options: `max-cars`, `default-speed`, `whistle-sound`
- Train engines must be deployed on rails and will inherit the configured speed.

### Planes
- Config path: `modules.planes`
- Key options: `cruise-altitude`, `max-speed`, `runway-length`
- Planes maintain a cruise altitude after launch and require a clear runway.

### Cars
- Config path: `modules.cars`
- Key options: `max-speed`, `default-fuel`, `horn-sound`
- Cars use configurable fuel reserves and shut down when fuel runs out.

### Rail systems
- Config path: `modules.rail-systems`
- Key options: `rail-types`, `signal-interval`

### Spaceships
- Config path: `modules.spaceships`
- Key options: `max-speed`, `auto-launch`
- Optional SpaceTravel integration: set `modules.spaceships.integration.enable-space-travel` to `true` and ensure the SpaceTravel plugin is installed.
  Spaceships can auto-dock at orbital stations when integration is enabled.

## Configuration
See `config.yml` for module toggles and tuning options.

## Player Documentation
Player documentation lives in `docs/MC-Docs/docs/plugins/transport.md`.
