# Furniture Plugin (Player Guide)

## Overview
The Furniture plugin adds decorative items that are grouped into themed modules. Furniture pieces place real blocks (or wall decor) and can be broken to recover the original item. Operators can use the commands below to hand out furniture pieces directly.

## Commands
- `/furniture status` - shows which Furniture modules are enabled.
- `/furniture reload` - reloads the Furniture configuration.
- `/furniture give <player> <item> [amount]` - gives Furniture items. Omit `<player>` to give items to yourself.

### Default item keys
These keys come from the default configuration. If your server has custom items, use the keys listed in `config.yml`.

- Seating: `oak-armchair`, `spruce-sofa`, `crimson-bench`, `leather-recliner`
- Tables: `oak-dining-table`, `birch-coffee-table`, `granite-side-table`, `glass-console-table`
- Storage: `wardrobe`, `cabinet`, `dresser`, `bookshelf`
- Lighting: `chandelier`, `floor-lamp`, `wall-sconce`, `lantern-stand`
- Decor: `rug`, `wall-clock`, `vase`, `painting-set`
- Outdoor: `patio-chair`, `garden-bench`, `pergola`, `firepit`
- Kitchen: `oven-range`, `kitchen-island`, `pantry-shelf`, `sink-unit`
- Bedroom: `canopy-bed`, `bunk-bed`, `bedside-table`, `vanity`
- Bathroom: `bathtub`, `sink-basin`, `towel-rack`, `mirror`

## Examples
- `/furniture give oak-armchair`
- `/furniture give Riley chandelier 2`

## Tips
- Item keys are defined per module in `config.yml`.
- Wall decor (mirrors, wall clocks, sconces) must be placed on the side of blocks.
- Breaking placed furniture returns the original item, unless the player is in creative mode.
- Crafting recipes remain the default way to obtain these items during normal gameplay.
