# PETA Volunteers

Spawns a PETA volunteer NPC when players kill animals. Volunteers walk slowly, shout shaming messages in chat, and flee when attacked before despawning.

## Behavior
- When a player kills an animal, there is a 30% chance to spawn a PETA volunteer nearby.
- Volunteers wander at a slow pace and shout shaming messages from an expanded message pool to nearby players without needing interaction.
- When attacked, volunteers scream, run away, can take damage, and despawn shortly after.

## Configuration
- `config.yml` includes the `shaming-messages` list used for volunteer chat lines.

## Compatibility
- Built for PaperMC.
- Uses Paper plugin metadata (`paper-plugin.yml`).
- Also intended to stay compatible with Nova where applicable.
