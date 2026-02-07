# SpaceTravel Plugin

SpaceTravel adds configurable interplanetary travel systems such as launch pads, orbital stations, and route planners. Each area is modular and can be toggled in `config.yml`.

## Commands
- `/spacetravel status` - lists module enablement and loaded module count.
- `/spacetravel reload` - reloads the configuration file.

## Crafting & Usage
Space travel tools are crafted via custom recipes and used by right-clicking with the crafted item. See the player documentation for full recipes and usage guidance.

## Modules

### Launch pads
- Config path: `modules.launch-pads`
- Key options: `cooldown-seconds`, `launch-sound`

### Orbital stations
- Config path: `modules.orbital-stations`
- Key options: `max-docks`, `airlocks-required`

### Planet routes
- Config path: `modules.planet-routes`
- Key options: `default-window-hours`, `route-limit`

### Space suits
- Config path: `modules.space-suits`
- Key options: `oxygen-seconds`, `repair-item`

### Asteroid mining
- Config path: `modules.asteroid-mining`
- Key options: `bonus-ore-chance`, `hazard-rate`

## Configuration
See `config.yml` for module toggles and tuning options.

## Player Documentation
Player documentation lives in `docs/MC-Docs/docs/plugins/space-travel.md`.
