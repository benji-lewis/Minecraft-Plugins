# SpaceTravel Plugin

SpaceTravel adds configurable interplanetary travel systems such as launch pads, orbital stations, and route planners. Each area is modular and can be toggled in `config.yml`.

## Commands
- `/spacetravel status` - lists module enablement and loaded module count.
- `/spacetravel reload` - reloads the configuration file.
- `/spacetravel give <player> <item> [amount]` - gives a space travel item to a player (omit `<player>` to give to yourself).

## Crafting & Usage
Space travel tools are crafted via custom recipes and used by right-clicking with the crafted item. Launch pads are now placeable blocks that can be activated for cooldown-controlled launches. See the player documentation for full recipes and usage guidance.

## Modules

### Launch pads
- Config path: `modules.launch-pads`
- Key options: `cooldown-seconds`, `launch-sound`

### Orbital stations
- Config path: `modules.orbital-stations`
- Key options: `max-docks`, `airlocks-required`
- Orbital docks are generated around world spawn when docking passes are used.

### Planet routes
- Config path: `modules.planet-routes`
- Key options: `default-window-hours`, `route-limit`
- Route charts apply a travel buff and respect per-player travel window cooldowns.

### Space suits
- Config path: `modules.space-suits`
- Key options: `oxygen-seconds`, `repair-item`
- Oxygen time controls how long survival effects remain active.

### Asteroid mining
- Config path: `modules.asteroid-mining`
- Key options: `bonus-ore-chance`, `hazard-rate`
- Bonus yields and hazard effects are tuned with these values.

## Configuration
See `config.yml` for module toggles and tuning options.

## Player Documentation
Player documentation lives in `docs/MC-Docs/docs/plugins/space-travel.md`.
