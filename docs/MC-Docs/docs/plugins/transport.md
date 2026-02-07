# Transport Plugin (Player Guide)

## Overview
The Transport plugin adds custom vehicle and rail items. Use the commands below to review module status or receive items directly from a server operator.

## Commands
- `/transport status` - shows which Transport modules are enabled.
- `/transport reload` - reloads the Transport configuration.
- `/transport give <player> <item> [amount]` - gives Transport items. Omit `<player>` to give items to yourself.

### Available items
- `train-engine` - Train Engine
- `plane-frame` - Plane Frame
- `car-chassis` - Car Chassis
- `rail-kit` - Rail Kit
- `spaceship-core` - Spaceship Core

## Examples
- `/transport give train-engine`
- `/transport give Alex plane-frame 2`

## Tips
- Train engines must be placed on rails to deploy successfully.
- Planes require a clear runway before they can launch into cruise altitude.
- Cars consume fuel over time and will shut down when fuel runs out.
- Rail kits can place powered rails when sneaking (if high-speed rails are enabled).
- Operators can hand out items to help players skip crafting for testing or events.
- Crafting recipes remain the default way to obtain these items during normal gameplay.
