<p align="center">
  <img alt="Fair Structure Loot mod icon" width="128" src="docs/assets/icon.png" />
</p>

# Fair Structure Loot

Fabric mod for **Minecraft Java 26.1.2** — per-player instanced loot for structure chests, barrels, pots, dispensers, vaults, and trial spawner rewards. A focused [Lootr](https://www.curseforge.com/minecraft/mc-mods/lootr-fabric)-style experience scoped to registered structure groups, using vanilla loot tables so balance stays faithful to vanilla.

## Requirements

- Java 25+
- [Fabric Loader](https://fabricmc.net/use/) 0.19.2+ for Minecraft 26.1.2
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.149.0+26.1.2

## Features

### Per-player loot

Each player gets their own roll when opening a fair loot container. Items you take stay taken; re-opening restores your personal stash for that block.

| Action | Behavior |
|--------|----------|
| **Open** chest, barrel, or decorated pot | Your personal inventory for that container |
| **Break** container | Drops your remaining instanced loot (or closest online player's roll for explosions/pistons). Breaking without sneak shows a hint toast and is blocked until you sneak-break. |
| **HUD** (client) | <img alt="Unopened" src="docs/assets/hud-green-triangle.png" style="vertical-align:middle;height:1.1em" /> Unopened · <img alt="Shared roll taken by someone else" src="docs/assets/hud-orange-triangle.png" style="vertical-align:middle;height:1.1em" /> Shared roll taken by someone else · <img alt="You opened" src="docs/assets/hud-black-triangle.png" style="vertical-align:middle;height:1.1em" /> You opened |

**Always per-player** (loot and indicator): End ship elytra barrel (`end_ship`), ancient city center chest.

### Loot roll modes

Default: **random** (unique roll per player). Admins can switch to **shared** (first opener rolls; others copy).

```
/fairstructureloot set loot random
/fairstructureloot set loot shared
```

### Structure coverage

22 toggleable groups. Core rare structures (including End ship wings and buried treasure) and other high-value loot default **on**; common overworld loot defaults **off**.

| Default on | Default off |
|------------|-------------|
| End cities, **End ship wings**, ancient cities, bastions, trial chambers, buried treasure | Abandoned mineshafts, desert pyramids, jungle temples, igloos |
| Strongholds, woodland mansions, nether fortresses | Ruined portals, underwater ruins, simple dungeons, trail ruins |
| Shipwrecks, pillager outposts | Ocean ruin archaeology, desert wells, villages (16 chest tables) |

Trial chambers include chests, barrels, pots, dispensers, vaults, and spawner completion/ominous rewards.

Archaeology brush tables are registered for some groups but only **chest/dispenser** containers use instanced loot today.

### Special conversions

- **End city ships** — Elytra item frames become instanced barrels (one elytra per player) when `end_ship` is active (`end_city` controls city treasure chests only)
- **Ancient city center** — Golden-apple center chest tracked without a vanilla loot table

## Commands

Requires gamemaster permission. Root: `/fairstructureloot`

```
/fairstructureloot set <group|all> activate|deactivate
/fairstructureloot list structures
/fairstructureloot set loot <random|shared>
```

When a group is **deactivated**, its containers use vanilla shared loot. Activation state syncs to clients on join and when admins change settings.

## Mod API

Other mods can register structure groups at init:

```java
FairStructureLootAPI.registerGroup(new StructureGroup(
    Identifier.fromNamespaceAndPath("mymod", "custom_dungeon"),
    "custom_dungeon",
    true,
    Set.of(Identifier.parse("mymod:chests/custom_dungeon"))
));
```

Players toggle custom groups with `/fairstructureloot set custom_dungeon activate|deactivate`.

## Build

From the mod directory:

```bash
./gradlew build
```

From the workspace root (shared Gradle cache):

```powershell
scripts/gradle.ps1 FairStructureLoot build
```

Output JAR: `build/libs/fairstructureloot-1.0.0-Minecraft26.1.2.jar`

## Install

1. Install Fabric Loader for **26.1.2**
2. Install Fabric API **0.149.0+26.1.2**
3. Place the mod JAR in the instance `mods/` folder (client required for HUD indicator)

**Existing worlds:** chunks generated before this mod keep old blocks. End elytra frames convert to barrels when their chunk loads. Explore new areas or regenerate chunks for best results.

## Development

- **Doc images:** `docs/assets/` (mod icon, HUD triangles). Toast icon: `docs/toast-icon-three-triangles.png`. Regenerate all: `python scripts/generate-readme-assets.py`
- **Audit history & Cursor rules:** [FSLAudit.md](FSLAudit.md) (agent guidance; local `.cursor/rules/` is gitignored)

## License

[CC0 1.0 Universal](LICENSE)

## Repository

Source: [github.com/marcosdherrero/FairStructureLoot](https://github.com/marcosdherrero/FairStructureLoot)
