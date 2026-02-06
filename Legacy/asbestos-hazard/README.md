# AsbestosHazard

AsbestosHazard is a Paper plugin that adds asbestos drops when mining and applies negative effects to players who keep asbestos in their inventory.

## Features

- **Random asbestos drops**: Mining rock blocks at deep overworld depths has a 12% chance to drop asbestos.
- **Escalating inventory effects**: Holding asbestos applies slowness and poison that worsen with longer exposure and larger asbestos stacks.
- **Chunk status command**: Use `/asbestoschunk` to check whether your current chunk is within the asbestos zone.

## Build

```bash
mvn clean package
```

## Install

Place the generated jar from `target/` into your Paper server's `plugins` directory and restart the server.
