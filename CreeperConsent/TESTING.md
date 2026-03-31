# CreeperConsent - Complete Testing Guide

## Quick Start Test (2 minutes)

```bash
# 1. Build the plugin
mvn clean package

# 2. Copy to test server
cp target/CreeperConsent-1.0.0.jar /path/to/server/plugins/

# 3. Start Paper 1.21.1 server
cd /path/to/server
./start.sh

# 4. In game, enable debug logs (optional)
# In console or via /say

# 5. Trigger a creeper
# /summon minecraft:creeper
# Hit it with a sword
# GUI should appear with two buttons
```

---

## Full Test Suite

### Test 1: Plugin Loads Successfully

**Setup:**
- Fresh Paper 1.21.1 server
- CreeperConsent JAR in plugins folder

**Steps:**
1. Start server
2. Wait for startup complete

**Expected Output:**
```
[... INFO]: CreeperConsent enabled! Creepers will now ask for permission.
```

**Pass Criteria:**
- ✅ No error messages
- ✅ Message appears in logs
- ✅ `/plugins` command shows "CreeperConsent" in enabled plugins

---

### Test 2: GUI Opens on Creeper Hit

**Setup:**
- Plugin loaded and enabled
- Admin/OP user in Creative mode
- Empty test world

**Steps:**
1. Run `/summon minecraft:creeper`
2. Wait for creeper to spawn
3. Hit creeper with sword 1-2 times
4. Observe

**Expected Behavior:**
- ✅ Inventory GUI opens immediately after hit
- ✅ Title bar reads "§6§lCreeper Consent"
- ✅ Two buttons visible: lime wool (left) and red wool (right)
- ✅ Filler gray wool in remaining slots
- ✅ No explosion sound or particles

**Failure Modes:**
- ❌ Creeper explodes immediately → ExplosionPrimeEvent not cancelling
- ❌ GUI doesn't open → Listener not registered or damager not tracked
- ❌ GUI opens for wrong player → Multiple players nearby

---

### Test 3: "Sure, let's go" - Allow Explosion

**Setup:**
- Previous test GUI is open
- Creeper nearby

**Steps:**
1. Click lime wool button (left side, slot 3)
2. Observe results

**Expected Behavior:**
- ✅ Inventory closes automatically
- ✅ Creeper explodes 1-2 ticks after click
- ✅ Block damage visible at creeper location
- ✅ Creeper entity is gone
- ✅ Log entry: `[... FINE]: PlayerName allowed creeper to explode.`

**Failure Modes:**
- ❌ Inventory stays open → Click not detected
- ❌ No explosion → Creeper state not removed
- ❌ Wrong creeper explodes → Slot detection off or damager tracking failed

---

### Test 4: "Nah, not today" - Despawn

**Setup:**
- Spawn new creeper
- Hit it to open GUI

**Steps:**
1. Click red wool button (right side, slot 5)
2. Observe creeper

**Expected Behavior:**
- ✅ Inventory closes automatically
- ✅ Creeper disappears (entity removed)
- ✅ No explosion sound or damage
- ✅ No creeper in `/entity list` output
- ✅ Log entry: `[... FINE]: PlayerName denied creeper explosion. Creeper despawned.`

**Failure Modes:**
- ❌ Creeper still visible → `remove()` call failed
- ❌ Creeper explodes after click → Wrong event flow
- ❌ Can still hit invisible creeper → Entity not fully removed

---

### Test 5: No Response - Indefinite Hang

**Setup:**
- GUI is open
- Creeper nearby

**Steps:**
1. Press Esc to close GUI without clicking
2. Wait 5-10 seconds
3. Check if creeper still exists

**Expected Behavior:**
- ✅ Inventory closes
- ✅ Creeper remains frozen (no explosion after 10+ seconds)
- ✅ Can see creeper entity in game
- ✅ No sounds or particles
- ✅ Log shows closure: `[... FINE]: PlayerName closed the creeper consent GUI without responding.`

**Verification:**
- Approach creeper → Fuse should still be lit visually
- Damage creeper again → Should trigger another GUI (new prompt, no persistence)

**Failure Modes:**
- ❌ Creeper explodes after 10 seconds → Timeout logic exists (shouldn't)
- ❌ Can't interact with creeper again → State cleanup on close

---

### Test 6: Multiple Creepers at Once

**Setup:**
- Spawn 3 creepers in different locations
- You and another player nearby

**Steps:**
1. Hit Creeper A (damager tracking test)
   - GUI opens for you
2. Have other player hit Creeper B nearby
   - GUI should open for OTHER PLAYER only
   - Your Creeper A should still be frozen
3. Other player clicks to accept
   - Creeper B explodes
   - You still see GUI for Creeper A
4. You click to deny
   - Creeper A despawns
   - Creeper C still alive (untouched)

**Expected Behavior:**
- ✅ Each player sees GUI only for creepers they triggered
- ✅ Creeper states are independent
- ✅ Multiple creepers can be pending simultaneously
- ✅ Log entries show both players' actions

**Failure Modes:**
- ❌ GUI shows for both players → Damager tracking wrong
- ❌ Other player's action affects your creeper → Global state issue
- ❌ Can only have one pending creeper → Manager capacity issue

---

### Test 7: Configuration - Custom Messages

**Setup:**
- Server stopped
- Plugin running at least once

**Steps:**
1. Edit `plugins/CreeperConsent/config.yml`:
   ```yaml
   enabled: true
   messages:
     title: "Boom Permission"
     accept: "YES BOOM"
     deny: "NO BOOM"
   ```
2. Restart server
3. Spawn and hit creeper
4. Observe GUI text

**Expected Behavior:**
- ✅ GUI title still shows "Creeper Consent" (hardcoded)
- ✅ Lime wool button displays "YES BOOM"
- ✅ Red wool button displays "NO BOOM"
- ✅ Log shows: `[... INFO]: Config loaded: enabled=true`

**Failure Modes:**
- ❌ Messages unchanged → Config not reloading
- ❌ Plugin fails to start → Invalid YAML syntax
- ❌ Messages are gray text → Color codes not parsing

---

### Test 8: Configuration - Disabled Plugin

**Setup:**
- Server running with plugin
- config.yml accessible

**Steps:**
1. Stop server
2. Edit config.yml: `enabled: false`
3. Restart server
4. Spawn and hit creeper
5. Observe explosion

**Expected Behavior:**
- ✅ Log shows: `[... INFO]: CreeperConsent is disabled in config. Plugin loaded but inactive.`
- ✅ No GUI opens
- ✅ Creeper explodes normally
- ✅ Explosion damage as expected

**Failure Modes:**
- ❌ GUI still appears → Enabled flag not checked
- ❌ Plugin fails to load → Null pointer on disabled plugin

---

### Test 9: Player Disconnect During Prompt

**Setup:**
- GUI is open for Player A
- Creeper is frozen
- Another player nearby

**Steps:**
1. Player A disconnects (quit game/network drop)
2. Wait 5 seconds
3. Check if creeper is still there
4. Other player hits creeper
   - Should trigger new GUI for other player

**Expected Behavior:**
- ✅ Creeper remains frozen (pending state persists)
- ✅ Other players can still interact with it
- ✅ Other player gets their own GUI
- ✅ No crashes or errors in logs

**Tolerance:**
- Pending state can remain after disconnect (acceptable)
- Cleanup on full server restart is fine

**Failure Modes:**
- ❌ Crash on disconnect → Null pointer in state tracking
- ❌ Creeper instantly explodes → Disconnect cleanup triggers explosion

---

### Test 10: Rapid Successive Creepers

**Setup:**
- You alone in world
- Rapid-fire summon command

**Steps:**
1. Run multiple /summon commands in console:
   ```
   /summon minecraft:creeper
   /summon minecraft:creeper
   /summon minecraft:creeper
   ```
2. Hit each creeper in quick succession
3. Cycle through all GUI prompts

**Expected Behavior:**
- ✅ Each creeper gets its own GUI prompt
- ✅ Can respond to one and get the next
- ✅ States don't interfere
- ✅ Logs show each action sequentially

**Failure Modes:**
- ❌ GUIs don't appear → Event queue backed up
- ❌ Wrong creeper explodes → State confusion
- ❌ Memory issues → Large HashMap leaks

---

### Test 11: Edge Case - Null Damager

**Setup:**
- Natural creeper explosion (no player interaction)

**Steps:**
1. Spawn creeper far from players: `/summon minecraft:creeper ~ ~ ~100`
2. Light it with flint & steel at a distance
3. Wait for it to explode naturally

**Expected Behavior:**
- ✅ No GUI opens (null damager handled gracefully)
- ✅ Creeper explodes normally
- ✅ No errors in logs
- ✅ No crash

**Failure Modes:**
- ❌ GUI appears for random player → Null check failed
- ❌ Crash with NullPointerException → Damager null not handled
- ❌ Creeper hangs forever → Missing null check

---

### Test 12: Edge Case - Creeper Despawned Before Response

**Setup:**
- GUI is open
- Creeper somehow despawned (manual /kill or environment)

**Steps:**
1. Trigger creeper GUI
2. In another terminal: `/kill @e[type=creeper]`
3. Click GUI button
4. Observe behavior

**Expected Behavior:**
- ✅ Click succeeds (handled gracefully)
- ✅ No errors in logs
- ✅ No crash
- ✅ Inventory closes

**Tolerance:**
- Can safely handle dead/null creeper reference
- No action taken if creeper already gone

**Failure Modes:**
- ❌ Crash with NullPointerException → isValid() check missing
- ❌ Block damage from already-dead creeper → Remove check missing

---

## Performance Test

**Setup:**
- Server with 10+ active players
- Mod to spawn 50+ creepers continuously

**Steps:**
1. Have all players trigger creepers
2. Accept/deny decisions at random
3. Monitor server performance
4. Check `/perf` or Timings report

**Expected Metrics:**
- ✅ TPS stays above 15 (playable)
- ✅ No memory leaks over 1 hour
- ✅ Cleanup happens promptly
- ✅ Logs show expected entries

**Failure Modes:**
- ❌ TPS drops significantly → Inefficient listeners
- ❌ Memory grows continuously → State not cleaned up

---

## Manual Test Checklist

Use this to track test runs:

```
Date: ___________
Tester: _________
Build: _________

□ Test 1: Plugin Loads
  Notes: _____________________
  Result: [PASS / FAIL]

□ Test 2: GUI Opens
  Notes: _____________________
  Result: [PASS / FAIL]

□ Test 3: Accept Button
  Notes: _____________________
  Result: [PASS / FAIL]

□ Test 4: Deny Button
  Notes: _____________________
  Result: [PASS / FAIL]

□ Test 5: Indefinite Wait
  Notes: _____________________
  Result: [PASS / FAIL]

□ Test 6: Multiple Creepers
  Notes: _____________________
  Result: [PASS / FAIL]

□ Test 7: Custom Config
  Notes: _____________________
  Result: [PASS / FAIL]

□ Test 8: Disabled Plugin
  Notes: _____________________
  Result: [PASS / FAIL]

□ Test 9: Player Disconnect
  Notes: _____________________
  Result: [PASS / FAIL]

□ Test 10: Rapid Creepers
  Notes: _____________________
  Result: [PASS / FAIL]

□ Test 11: Null Damager
  Notes: _____________________
  Result: [PASS / FAIL]

□ Test 12: Dead Creeper
  Notes: _____________________
  Result: [PASS / FAIL]

Overall Result: [PASS / FAIL]
```

---

## Debugging Tips

### Enable Fine Logging

Add to server properties or Bukkit config:
```
logger-prefix: CreeperConsent
```

### Check Listener Registration

In console:
```
/plugins
# Should show "CreeperConsent" in enabled list
```

### Verify Creeper State

Use debug commands:
```
/summon minecraft:creeper
/entity list
# Should show creeper entity with UUID
```

### Monitor Event Flow

Check logs for:
```
CreeperConsent enabled
Creeper explosion prompt shown to PlayerName
PlayerName allowed creeper to explode
PlayerName denied creeper explosion
```

### Test with Minimal World

- Empty creative world
- No other plugins
- Single player
- This isolates CreeperConsent issues

### Java Debugging

Add to server launch flags:
```
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
```

Then attach IDE debugger to port 5005 for step-by-step debugging.

---

## Regression Tests

After any code changes, run:
1. Plugin loads (Test 1)
2. GUI opens (Test 2)
3. Accept button (Test 3)
4. Deny button (Test 4)
5. Multiple players (Test 6)
6. Config reload (Test 7)

If all 6 pass, regression is unlikely.

---

## Acceptance Criteria (Final)

Plugin is **COMPLETE** when:

- ✅ All 12 test cases pass
- ✅ No errors in logs during testing
- ✅ GUI is clean and readable
- ✅ Config loads without issues
- ✅ Multiple players handled correctly
- ✅ No memory leaks
- ✅ Graceful null/edge case handling
- ✅ Documentation complete and accurate
