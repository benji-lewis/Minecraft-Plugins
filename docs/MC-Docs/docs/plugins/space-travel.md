# SpaceTravel Plugin (Player Guide)

## Overview
SpaceTravel introduces interplanetary tools such as launch pads and orbital passes. Launch pads are now placeable blocks that can be activated to launch players. Operators can use the commands below to provide items directly.

## Commands
- `/spacetravel status` - shows which SpaceTravel modules are enabled.
- `/spacetravel reload` - reloads the SpaceTravel configuration.
- `/spacetravel give <player> <item> [amount]` - gives SpaceTravel items. Omit `<player>` to give items to yourself.

### Available items
- `launch-pad` - Launch Pad Beacon
- `orbital-pass` - Orbital Docking Pass
- `route-chart` - Planet Route Chart
- `space-suit` - Space Suit Module
- `asteroid-drill` - Asteroid Drill

## Examples
- `/spacetravel give launch-pad`
- `/spacetravel give Jamie asteroid-drill 1`

## Tips
- Place launch pads on solid blocks, then right-click the pad to launch (cooldowns apply).
- Orbital docking passes teleport players to generated orbital platforms near spawn.
- Route charts apply short travel buffs and respect per-player travel windows.
- Space suit repairs consume the configured repair item to refresh oxygen effects.
- Asteroid drills can trigger bonus yields or hazards depending on server tuning.
- Crafting recipes remain the default way to obtain these items during normal gameplay.
