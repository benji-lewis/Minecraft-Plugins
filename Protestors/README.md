# Protestors

`Protestors` is a single PaperMC monoplugin that combines module logic from the existing plugins in this repository.

## Modules

Each module can be turned on/off in `config.yml` under `modules`:

- `just-stop-oil`
- `peta-volunteers`
- `debt-collector`
- `asbestos-hazard`
- `kim-jong-un`

## Build

```bash
mvn -f Protestors/pom.xml clean package
```

## Test

```bash
mvn -f Protestors/pom.xml test
```
