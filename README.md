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

## How it works

### Chest / container data structure

Instanced loot is **not** stored in chest block NBT. All per-player rolls live in Minecraft **SavedData** under the overworld data folder (every dimension shares one store; the dimension is part of the key).

| SavedData id | Typical world path | Purpose |
|--------------|--------------------|---------|
| `fairstructureloot:instanced_loot` | `<world>/data/fairstructureloot/instanced_loot.dat` | All opened fair containers and per-player inventories |
| `fairstructureloot:structure_activation` | `…/structure_activation.dat` | Which structure groups are on/off |
| `fairstructureloot:loot_roll_mode` | `…/loot_roll_mode.dat` | `random` or `shared` |
| `fairstructureloot:ancient_city_center_chests` | per-dimension `…/ancient_city_center_chests.dat` | Positions of center chests with no vanilla loot table |

**Container identity** is a stable string key:

```
<dimension>:<x>,<y>,<z>
```

Example: `minecraft:overworld:120,48,-300`

Double chests use one canonical (min) position so both halves share a single folder. Loot table id is **not** part of the key; it is stored inside the folder for validation.

Each entry in `Containers` is a **loot chest folder** with this shape:

| Field | Type | Meaning |
|-------|------|---------|
| `OpenedPlayers` | list of UUID strings | Players who successfully received a roll for this block |
| `Inventories` | map UUID → sparse item list | Each player’s personal remaining loot |
| `GloballyRolled` | bool | Shared mode: first opener has created the template |
| `SharedRoll` | sparse item list | Shared-mode template copied to later openers |
| `LootTable` | optional id | Table used for this folder; mismatch clears roll data |

Inventories are **sparse**: only non-empty slots are written as `{ "Slot": <int>, "Item": <ItemStack> }`.

Conceptual layout:

```text
Containers
  "minecraft:overworld:120,48,-300"
    OpenedPlayers: ["uuid-a", "uuid-b"]
    Inventories:
      "uuid-a": [ { Slot, Item }, … ]
      "uuid-b": [ { Slot, Item }, … ]
    GloballyRolled: false
    SharedRoll: []
    LootTable: "minecraft:chests/…"
```

### How we know who opened a chest

**Server (source of truth)**

1. Player opens a fair container → `InstancedLootSavedData.openContainer`.
2. If that player’s UUID already has an inventory entry → restore their stash (no re-roll).
3. Otherwise generate loot (unique per player in **random** mode, or copy `SharedRoll` in **shared** mode).
4. On a successful non-empty roll → add UUID to `OpenedPlayers`, store their inventory, mark SavedData dirty.
5. Empty rolls are **not** persisted and do **not** count as opened.

**Client (HUD only)**

- Caches of personally opened and globally opened container keys (`OpenedChestCache`).
- Full lists sync on join; live updates when someone opens a chest.
- Cleared on disconnect. Used for the green / orange / black triangle indicator, not for server loot decisions.

### Efficiency (keeping the server light)

| Approach | Why it helps |
|----------|--------------|
| **Event-driven** | No tick loop scanning every chest; work runs on open, break, join, and chunk load |
| **Lazy rolls** | Loot is generated only when a player first successfully opens that container |
| **Cancel vanilla shared fill** | Fair containers skip vanilla `unpackLootTable` so the shared block inventory stays empty |
| **Sparse inventories** | Disk and memory store only non-empty slots |
| **One overworld store** | All dimensions keyed in one SavedData file |
| **Skip empty rolls** | Failed/empty generation does not create folders or dirties |
| **Destroy cleans up** | Breaking (or air-on-unload) removes that container’s folder from SavedData |
| **Chunk scans deferred** | Fair-loot marker sync runs after chunk load (`server.execute`) and only for loaded view-distance chunks |
| **Safe unload** | Nearby chest pair lookups use `getChunkNow` so unload never force-loads chunks |

Folders stay in SavedData until the container is destroyed; they are not evicted on chunk unload. Join sync walks known folders for that player’s opened keys so long-lived worlds grow with opened loot only, not with every structure chest in the world.

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

Output JAR: `build/libs/fairstructureloot-1.0.1-Minecraft26.1.2.jar`

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
