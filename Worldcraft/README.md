# Worldcraft

Worldcraft is a PaperMC plugin that brings every country flag into Minecraft. Players can generate handheld map flags and place flag poles that display the same textures, with all images generated at runtime so no PNG files are committed to the repo.

## Features

- `/worldcraft flag <country>` gives a map item rendered with the selected country flag.
- `/worldcraft flagpole <country>` places a flag pole and mounts the flag as a map item.
- `/worldcraft reload` reloads configuration and refreshes the country list.
- Flag textures are generated at runtime using either downloaded assets, procedural generation, or a hybrid of both; no PNG assets are stored in the repository.

## Configuration

See `src/main/resources/config.yml` for defaults. Key options include:

- `modules.countries.countries-source-url`: URL for the country code list.
- `modules.countries.texture-mode`: `download`, `procedural`, or `hybrid` flag texture generation.
- `modules.countries.flag-texture-url-template`: URL template for flag PNGs (runtime download and cache).
- `modules.countries.cache-ttl-hours`: Cache duration for country list and flag images.
- `modules.countries.pole-*`: Materials and height for the flag pole structure.

## Permissions

- `worldcraft.use` (default: true)

## Building

```bash
mvn -f Worldcraft/pom.xml test
mvn -f Worldcraft/pom.xml package
```
