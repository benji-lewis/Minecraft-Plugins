# CreeperConsent - Paper Minecraft Plugin

A fun and interactive plugin that asks players for permission before creepers explode.

## Features

- 🎮 Full-screen GUI popup when a creeper's fuse is lit
- 👤 Only prompts the player who triggered the creeper
- ⏱️ Creeper waits indefinitely for player response
- ⚙️ Fully configurable messages
- 🛑 Players can allow explosion or despawn the creeper
- 💾 No persistence between prompts (fresh decision each time)

## Requirements

- **Paper 1.21.1** or compatible version
- Java 21+
- Maven 3.8+

## Building

```bash
mvn clean package
```

The compiled plugin JAR will be at `target/CreeperConsent-1.0.0.jar`

## Installation

1. Place `CreeperConsent-1.0.0.jar` in your server's `plugins/` directory
2. Restart the server
3. Configure `plugins/CreeperConsent/config.yml` as needed
4. Restart again to apply changes

## Configuration

Edit `plugins/CreeperConsent/config.yml`:

```yaml
# Enable or disable the plugin
enabled: true

# Messages shown to the player
messages:
  title: "A creeper has asked for permission to explode"
  accept: "Sure, let's go"
  deny: "Nah, not today"
```

## How It Works

### Core Flow

1. **Creeper is triggered** (damaged by a player)
2. **ExplosionPrimeEvent fires** with the creeper's fuse lit
3. **Plugin intercepts** and cancels the explosion
4. **GUI popup opens** for the triggering player
5. **Player chooses:**
   - ✅ "Sure, let's go" → Creeper explodes immediately
   - ❌ "Nah, not today" → Creeper is despawned
   - 🤐 No response → Creeper hangs indefinitely (waiting)

### Technical Details

- **Event Hook:** `ExplosionPrimeEvent` (cancels explosion before fuse triggers)
- **GUI:** 9-slot chest inventory with custom formatted items
  - Accept button (Lime Wool) at slot 3
  - Deny button (Red Wool) at slot 5
  - Gray filler wool in remaining slots
- **Damager Tracking:** `DamageListener` tracks which entity damaged the creeper
- **Pending State:** `CreeperManager` maintains state of pending creepers per player
- **Player Targeting:** Only the player who triggered the creeper sees the prompt

## File Structure

```
CreeperConsent/
├── src/main/
│   ├── java/com/xfour/creeperconsent/
│   │   ├── CreeperConsent.java           (Main plugin class)
│   │   ├── config/
│   │   │   └── ConfigManager.java        (Configuration loader)
│   │   ├── gui/
│   │   │   └── ExplosionPromptGUI.java   (GUI builder & handler)
│   │   ├── listener/
│   │   │   ├── ExplosionListener.java    (Explosion event handler)
│   │   │   ├── InventoryClickListener.java (GUI click handler)
│   │   │   └── DamageListener.java       (Damage tracker)
│   │   └── util/
│   │       └── CreeperManager.java       (Creeper state management)
│   └── resources/
│       ├── plugin.yml                     (Plugin metadata)
│       └── config.yml                     (Default configuration)
├── pom.xml                                (Maven build file)
└── README.md                              (This file)
```

## Testing Instructions

### Prerequisites

- Paper 1.21.1 server running locally or remotely
- Operator/admin permissions
- Creative mode or survival mode with creepers spawned

### Test Scenario 1: Basic Acceptance

1. Start server with plugin installed
2. Spawn a creeper near you (Creative: `/summon minecraft:creeper`)
3. Hit the creeper with a sword
4. GUI popup should appear immediately
5. Click "Sure, let's go" (green wool, left button)
6. Creeper should explode
7. **Expected:** Block damage visible at creeper location

### Test Scenario 2: Denial

1. Spawn a new creeper
2. Hit the creeper
3. GUI popup appears
4. Click "Nah, not today" (red wool, right button)
5. Creeper should vanish
6. **Expected:** No explosion, no creeper entity remains

### Test Scenario 3: Indefinite Wait

1. Spawn a creeper
2. Hit the creeper
3. GUI popup appears
4. Close the GUI without clicking (Esc key)
5. Wait 10+ seconds
6. **Expected:** Creeper remains frozen, no explosion
7. Approach creeper again to verify it's still there (frozen state)

### Test Scenario 4: Multiple Players

1. Spawn a creeper
2. Player A hits the creeper → GUI opens only for Player A
3. Player B is nearby but should NOT see any GUI
4. Player A clicks "Sure, let's go"
5. Creeper explodes
6. **Expected:** Only Player A saw the prompt; Player B unaffected

### Test Scenario 5: Configuration Reload

1. Edit `plugins/CreeperConsent/config.yml`:
   ```yaml
   messages:
     accept: "Kaboom time!"
     deny: "Nope"
   ```
2. Restart server
3. Spawn and trigger a creeper
4. **Expected:** GUI shows new custom messages

### Test Scenario 6: Plugin Disabled

1. Edit `config.yml`: `enabled: false`
2. Restart server
3. Spawn a creeper and hit it
4. **Expected:** Creeper explodes normally (plugin inactive)

### Debugging

Enable detailed logging by checking `logs/latest.log`:

```
[... Server thread/INFO]: CreeperConsent enabled! Creepers will now ask for permission.
[... Server thread/FINE]: Creeper explosion prompt shown to PlayerName
[... Server thread/FINE]: PlayerName allowed creeper to explode.
[... Server thread/FINE]: PlayerName denied creeper explosion. Creeper despawned.
```

### Troubleshooting

| Issue | Solution |
|-------|----------|
| GUI doesn't appear | Ensure plugin is enabled in config.yml; check logs for errors |
| All players see the GUI | Verify `DamageListener` is tracking damager correctly |
| Creeper explodes instantly | Confirm `ExplosionPrimeEvent` is being cancelled |
| Plugin won't load | Check Java version (21+) and Paper version (1.21.1) |
| Creeper hangs after response | Verify `CreeperManager.removePending()` is called in listeners |

## Architecture Notes

### Separation of Concerns

- **ConfigManager:** Loads and caches configuration
- **ExplosionListener:** Detects creeper fuse trigger, shows GUI
- **DamageListener:** Tracks who damaged which creeper
- **InventoryClickListener:** Handles player responses from GUI
- **CreeperManager:** Maintains state of pending creepers per player
- **ExplosionPromptGUI:** Builds and formats the inventory UI

### Thread Safety

- Uses `HashMap` for creeper state (single-threaded server model)
- All listeners run on main server thread
- No async operations needed

### Performance

- Minimal overhead; only checks ExplosionPrimeEvent
- No persistent database or network calls
- Cleanup happens immediately on response or player disconnect

## Future Enhancements

- [ ] Per-world enable/disable
- [ ] Permission nodes (creeperconsent.prompt, creeperconsent.deny)
- [ ] Sound effects on GUI open/response
- [ ] Configurable timeout (auto-explode or despawn after X seconds)
- [ ] Multi-language support
- [ ] Statistics/logging of player responses

## License

No license specified. Modify and distribute freely for your server.

## Author

Created by Benji for XFour

---

**Enjoy asking your creepers nicely!** 🧨
