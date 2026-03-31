# CreeperConsent - Deployment Guide

## Quick Deployment (5 minutes)

### 1. Build the Plugin

```bash
cd /path/to/CreeperConsent
mvn clean package
```

Output: `target/CreeperConsent-1.0.0.jar`

### 2. Install on Server

```bash
# Copy JAR to server plugins folder
cp target/CreeperConsent-1.0.0.jar /path/to/server/plugins/

# Restart server
cd /path/to/server
./stop.sh
./start.sh
```

### 3. Verify Installation

Check server logs:
```
[CreeperConsent] CreeperConsent enabled! Creepers will now ask for permission.
```

Or run `/plugins` in-game—should see "CreeperConsent" listed.

---

## Configuration

### Default Config Location
```
server/plugins/CreeperConsent/config.yml
```

### Edit Configuration

```bash
nano server/plugins/CreeperConsent/config.yml
```

Example custom config:
```yaml
enabled: true

messages:
  title: "A creeper has asked for permission to explode"
  accept: "Sure, let's go"
  deny: "Nah, not today"
```

**Changes take effect on server restart.**

---

## Troubleshooting Deployment

### Plugin doesn't load

**Check:**
- ✅ JAR is in `plugins/` folder
- ✅ Paper 1.21.1 or compatible
- ✅ Java 21+ installed
- ✅ No other plugins conflicting (test alone first)

**Action:**
```bash
# Remove, restart, check logs
rm plugins/CreeperConsent-1.0.0.jar
./stop.sh
./start.sh
# Look for errors mentioning "CreeperConsent"
```

### GUI doesn't appear when creeper is hit

**Check:**
- ✅ Plugin enabled in config.yml
- ✅ You have OP/admin permissions
- ✅ Creative or Survival mode (both work)
- ✅ Actually hitting the creeper (not nearby)

**Action:**
```bash
# Enable debug logging (optional)
# Check latest.log for FINE-level entries
tail -f logs/latest.log | grep CreeperConsent
```

### Creeper explodes instead of prompting

**Check:**
- ✅ Plugin loaded successfully (check startup logs)
- ✅ ExplosionPrimeEvent isn't cancelled by another plugin
- ✅ No other plugins modifying creeper behavior

**Action:**
```bash
# Test with minimal plugins
# Disable all other plugins except CreeperConsent
# Restart and retest
```

### Wrong player sees GUI

**Check:**
- ✅ Only the player who hits the creeper should see it
- ✅ If multiple players nearby, only damager gets GUI

**Action:**
This is expected behavior—only the triggering player sees the prompt.

### Creeper doesn't explode after clicking "Sure"

**Check:**
- ✅ You actually clicked the green/lime wool button
- ✅ The button was on the left side (slot 3)
- ✅ Inventory closed after click

**Action:**
- Spawn new creeper and try again
- Check logs for error entries

---

## Server Requirements

| Requirement | Version |
|-------------|---------|
| Server | Paper 1.21.1 |
| Java | 21+ |
| RAM | 512MB+ (no major overhead) |
| Disk | ~2MB (JAR size) |

---

## Performance Impact

- **CPU:** Minimal (event-based, only on creeper damage)
- **Memory:** <5MB typical (HashMap tracking)
- **Network:** None (local-only)
- **TPS Impact:** <0.1 (negligible)

---

## Uninstalling

### Remove Plugin

```bash
# Stop server
./stop.sh

# Remove JAR
rm plugins/CreeperConsent-1.0.0.jar

# Remove config (optional)
rm -r plugins/CreeperConsent/

# Restart
./start.sh
```

### Restore Default Behavior

After removal, creepers will explode normally on player damage.

---

## Updates

### Checking for Updates

Current version: **1.0.0**

Check releases: https://example.com/releases (substitute with actual URL)

### Upgrading

1. Stop server
2. Backup config:
   ```bash
   cp -r plugins/CreeperConsent/ plugins/CreeperConsent.backup/
   ```
3. Replace JAR:
   ```bash
   cp CreeperConsent-1.0.1.jar plugins/
   ```
4. Restart server
5. Check logs for compatibility messages

### Rollback

```bash
rm plugins/CreeperConsent-1.0.1.jar
cp plugins/CreeperConsent.backup/CreeperConsent-1.0.0.jar plugins/
./stop.sh
./start.sh
```

---

## Multi-Server Deployment

### Copy to Multiple Servers

```bash
# Build once
mvn clean package

# Deploy to all servers
for server in server1 server2 server3; do
  cp target/CreeperConsent-1.0.0.jar /data/$server/plugins/
done

# Restart all
for server in server1 server2 server3; do
  ssh user@$server "cd /data/$server && ./stop.sh && ./start.sh"
done
```

### Per-Server Configuration

Each server has its own config at:
```
/data/server1/plugins/CreeperConsent/config.yml
/data/server2/plugins/CreeperConsent/config.yml
etc.
```

Edit each independently.

---

## Production Checklist

Before deploying to production:

- [ ] Plugin builds without errors (`mvn clean package`)
- [ ] JAR exists: `target/CreeperConsent-1.0.0.jar`
- [ ] Testing passes on dev server (see TESTING.md)
- [ ] Config file reviewed and customized
- [ ] Server backup taken
- [ ] Admin notified of deployment
- [ ] Deployment window scheduled (optional)
- [ ] Rollback plan documented

---

## Monitoring

### Logs to Watch

```bash
tail -f logs/latest.log | grep CreeperConsent
```

**Normal entries:**
```
[CreeperConsent] CreeperConsent enabled! Creepers will now ask for permission.
[CreeperConsent] Creeper explosion prompt shown to PlayerName
[CreeperConsent] PlayerName allowed creeper to explode.
[CreeperConsent] PlayerName denied creeper explosion. Creeper despawned.
```

**Error entries:** (report these)
```
[CreeperConsent] NullPointerException...
[CreeperConsent] ClassNotFoundException...
```

---

## Support

If you encounter issues:

1. **Check this guide** (you're reading it!)
2. **Review TESTING.md** for expected behavior
3. **Check server logs** for errors
4. **Verify requirements** (Paper 1.21.1, Java 21+)
5. **Test in isolation** (no other plugins)
6. **Contact:** Benji or XFour support

---

## License & Attribution

CreeperConsent v1.0.0
Created for Paper 1.21.1
No external dependencies
