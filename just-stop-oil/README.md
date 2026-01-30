# Just Stop Oil

Just Stop Oil is a PaperMC plugin that reacts to players placing lava by spawning a protester villager who shouts messages at nearby players. The protester despawns after a short time, and you can tune the behavior through `config.yml`.

## Features
- Spawns a "Just Stop Oil Protester" villager when a player places lava (chance based).
- Sends configurable protest messages to nearby players.
- Protesters flee and shout when attacked.
- Protesters spray temporary orange wool onto nearby blocks with particles.
- `/juststopoil spawn <count>` spawns protesters at your location (up to a configurable limit).

## Configuration
The following settings are available in `src/main/resources/config.yml`:
- `protest.spawn-chance`: Chance to spawn a protester when lava is placed.
- `protest.radius`: Radius in blocks to send messages.
- `protest.prefix`: Message prefix (supports `&` color codes).
- `protest.messages`: List of messages to broadcast.
- `protest.shout-interval-min-ticks`: Minimum ticks between shouts.
- `protest.shout-interval-max-ticks`: Maximum ticks between shouts.
- `protest.shout-chance`: Chance that a scheduled shout actually happens.
- `protest.despawn-delay-ticks`: How long before the protester despawns after spawning or being attacked.
- `protest.spray.enabled`: Enables the orange spray animation.
- `protest.spray.radius`: Block radius used to find spray targets.
- `protest.spray.interval-ticks`: Ticks between spray bursts.
- `protest.spray.duration-ticks`: Total duration for the spray animation.
- `protest.spray.blocks-per-burst`: Number of blocks painted per burst.
- `protest.spray.paint-ticks`: How long sprayed blocks stay orange before reverting.
- `protest.command.max-spawn`: Max number of protesters that can be spawned by command.
