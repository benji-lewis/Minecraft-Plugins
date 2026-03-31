# Minecraft Plugins

A collection of high-quality Minecraft plugins for Paper 1.21.1 and other server implementations.

## 🌿 Green Party Dimension

**Description**: A Paper plugin that adds *The Verdant Utopia* — a custom dimension themed around the UK Green Party in an absurdly over-the-top fashion. Coal is banned. Composters are sacred. Six councillors will talk at you relentlessly.

**Features**:
- Custom dimension (flat, permanently green)
- 13 NPC councillors with dynamic AI schedules
- **Scripted routine system** — council sessions, debates, site inspections with full choreography
- Quest chains + eco-score tracking
- Environmental violations, recycling, motions, announcements
- Monthly leaderboards & cosmetics
- Custom structures (council chamber, tree farm, compost plant, recycling centre)
- **LocationRegistry** — 21+ named locations for routine placement

**Version**: 1.4.5  
**Compatibility**: Paper 1.21.1  
**Status**: Active development (Phase 5: Routine system complete)

[See plugin README](./GreenPartyDimension/README.md)

---

## Other Plugins

- **CreeperConsent** — Creeper griefing protection
- **Furniture** — Custom furniture system
- **Protestors** — NPC protestor mechanics
- **SpaceTravel** — Dimension travel system
- **Transport** — Advanced transport mechanics
- **Worldcraft** — World building tools
- **Legacy** — Legacy plugin archive

---

## Building

### Build Individual Plugin

```bash
cd GreenPartyDimension
mvn clean package
```

Output: `target/GreenPartyDimension-*.jar`

### Build All Plugins

```bash
for dir in */; do
  if [ -f "$dir/pom.xml" ]; then
    cd "$dir"
    mvn clean package
    cd ..
  fi
done
```

---

## GitHub Actions CI/CD

Each plugin with a `.github/workflows/build.yml` file automatically:
- Builds on push/PR
- Uploads JAR as artifact
- Creates releases on version tags

**Current workflows**:
- ✅ GreenPartyDimension — Automated build & release

---

## Installation

1. Download JAR from [Releases](https://github.com/XFourJeeves/Minecraft-Plugins/releases)
2. Copy to Paper server's `plugins/` directory
3. Restart server
4. Config files auto-generate on first run

---

## License

Each plugin is independently licensed. See individual plugin directories.

---

**"Please remember to reduce, reuse, recycle before your next server session."**  
— The Green Council
