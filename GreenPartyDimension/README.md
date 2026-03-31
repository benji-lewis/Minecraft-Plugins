# 🌿 Green Party Dimension — The Verdant Utopia

> *"A plugin so green it makes the Amazon jealous. Probably."*
> 
> *Certified 100% renewable (it's not, it runs on Java)*

A Paper 1.21.1 plugin that adds **The Verdant Utopia** — a custom dimension themed around the UK Green Party in an absurdly over-the-top fashion. Coal is banned. Composters are sacred. Six councillors will talk at you relentlessly. You will love it.

Compatible with **Geyser** for cross-platform (Java + Bedrock) play. Because the environment belongs to everyone.

---

## 📦 Installation

1. **Requirements:**
   - Paper 1.21.1 server (get it from [papermc.io](https://papermc.io))
   - Java 21+
   - Geyser (optional, for Bedrock players) — [geysermc.org](https://geysermc.org)

2. **Install the plugin:**
   ```
   cp GreenPartyDimension-1.0.0.jar plugins/
   ```

3. **Start/restart your server.**

4. **The Verdant Utopia** world will be auto-created. It takes about 5 seconds. The planning committee approved this.

5. **Geyser players** will be teleported using the same commands — no special config needed.

---

## 🎮 Commands

| Command | Description |
|---|---|
| `/greenparty` or `/gp` or `/greens` | Main command — shows help |
| `/greenparty teleport` | Teleport to The Verdant Utopia |
| `/greenparty leave` | Return to the overworld |
| `/greenparty info` | Dimension info |
| `/greenparty quest` | View your quests |
| `/greenparty quest assign` | Get up to 3 quests |
| `/greenparty quest list` | See all 7 available quests |
| `/greenparty manifesto` | Read the 15-point manifesto (required reading) |
| `/greenparty council` | Meet the Green Council NPCs |
| `/greenportal` | *(Admin)* Direct teleport |
| `/greenreset [player]` | *(Admin)* Reset quest progress |

---

## 🌍 The Verdant Utopia

A flat, overwhelmingly green dimension with:

- **Permanent midday sun** ☀️ (maximum solar energy)
- **Peaceful mode** — hostile mobs were politely asked to leave (Motion 47b)
- **No fall damage** — Health & Safety regulation 4.7b
- **No hunger** — Universal Basic Sustenance, policy 12
- **No fire spreading** — banned by council decree, Motion 22
- **Emerald ore** scattered throughout — symbolic prosperity
- **Zero coal** — banned, see §4 of the manifesto
- **Flowers, trees, and grass** everywhere — obviously

### 🏛️ The Green Council

Six NPC councillors spawn near the world spawn. Right-click them to receive wisdom:

| Councillor | Role | Specialty |
|---|---|---|
| Councillor Wheatgrass | Chair | Composting rota, petitions |
| Recycling Evangelist Bramble | Material Reuse | Recycling everything |
| Elder Composting Sage Fern | Ancient Wisdom | Very old, knows about compost |
| Protest Coordinator Meadow | Direct Action | Placards, mostly |
| Policy Officer Sedge | Bureaucracy | 247-page policy documents |
| Fundraising Captain Sorrel | Finance | Tote bags, exclusively |

---

## 📋 Quests

Seven quests approved after **14 committee meetings**:

| Quest | Task | Reward |
|---|---|---|
| The Great Compost Crisis of Chunk 17 | Collect 64 dirt | 500 XP + compost gear |
| One Tree for Every Slogan | Plant 10 saplings | 750 XP + 64 saplings |
| Down With Fossil Fuels (The Coal Blocks) | Mine 16 coal ore | 600 XP + green wool |
| Wind Power or Bust | Place 5 note blocks | 400 XP + note blocks |
| Emergency Biodiversity Survey | Right-click 10 mobs | 1000 XP + emeralds |
| Phase 1 of the Green Infrastructure Plan | Place 9 green concrete | 800 XP + more green concrete |
| The Petition (We Need More Signatures) | Collect 32 paper | 300 XP + books |

Quests auto-assign when you enter the dimension. The council is very pleased by this system. They held 3 meetings about it.

---

## 🚪 The Green Portal

You can also build a portal to enter the dimension without commands:

1. **Build a frame** out of any leaf blocks, log blocks, moss blocks, or green wool (minimum 2×3 opening)
2. **Right-click the frame** with a **Composter** (or a water bucket, or bone meal)
3. Step through!

Right-click again to return.

> *"The portal design was approved after 8 meetings. The first 4 were about whether it should be made of oak or birch leaves."*

---

## ⚙️ Configuration

Edit `plugins/GreenPartyDimension/config.yml` to customise:
- NPC councillor names and dialogue
- Quest objectives and rewards
- Messages (all appropriately preachy by default)
- Dimension settings

---

## 🤝 Geyser Compatibility

This plugin works with Geyser out of the box:

- All commands work identically for Bedrock players
- NPC interaction (right-click) works on both platforms
- Portal activation works on both platforms
- No Floodgate dependency required

Bedrock players can use `/greenparty teleport` from day one.

---

## 📜 The Manifesto (abbreviated)

1. Everything shall be green
2. Coal is banned
3. Emeralds are the currency of environmental virtue  
4. All meat blocks are "legacy protein"
5. Dirt is sacred — handle with reverence
6. TNT requires a 14-week environmental impact assessment
7. The Nether is banned on environmental grounds (too hot)
8. Say "reduce, reuse, recycle" before logging off

*Full 15-point manifesto available via `/greenparty manifesto`*

---

## 🐛 Known Issues / Notes

- The world uses a **flat custom generator** — terrain is intentionally flat and green (this is a feature, not a bug; the council voted on it)
- Quest progress is **stored in memory** (not persisted to disk) — restarting the server resets progress. The council is aware. It's in the agenda.
- The periodic Green Council broadcast fires every 5 minutes in the dimension. This is also intentional.
- Coal drops are automatically replaced with green dye. The council considers this justice.

---

## 📁 File Structure

```
plugins/
  GreenPartyDimension/
    config.yml          ← Configuration
the_verdant_utopia/     ← The world folder (auto-created)
```

---

*This plugin was created in the spirit of absurdist political comedy. It is not affiliated with the actual UK Green Party, though we imagine they'd approve of the composting mechanics.*

*"Please remember to reduce, reuse, recycle before your next server session."*  
*— The Green Council*
