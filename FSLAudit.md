# Fair Structure Loot — Audit & Development Notes

Development audits and follow-up tracking for this mod. User-facing docs live in [README.md](README.md).

---

## Cursor rules (project)

These mirror `.cursor/rules/fairstructureloot.mdc` for contributors without Cursor rule sync.

### Stack

- Minecraft **26.1.2**, Fabric Loader **0.19.2+**, Fabric API **0.149.0+26.1.2**, Java **25**
- Package `net.berkle.fairstructureloot` · Loom 1.16-SNAPSHOT
- Version bumps: workspace `versions.properties` → `scripts/sync-versions.ps1` → rebuild → `scripts/sync-minecraft-api.ps1`

### Design constraints

- Instanced loot uses vanilla loot tables and `LootTable.fill`; balance stays vanilla-faithful
- All container inventories persist in **overworld** `InstancedLootSavedData` (dimension in key)
- Structure groups registered in `StructureGroupRegistry`; toggles in `StructureActivationSavedData`
- Prefer Fabric events; mixins only where vanilla shared inventory must be intercepted
- Client: HUD indicator + packet caches only — no loot generation on client

### Mixin safety (26.1.2)

- Verify targets with `.minecraft-api/<version>/` or `javap` — compile success ≠ runtime mixin success
- **Block entity unload:** never `level.getBlockState()` during chunk unload; use `getChunkSource().getChunkNow()` + `chunk.getBlockState(pos)` (C2ME/async chunk systems deadlock otherwise)
- Avoid `@ModifyVariable` on compiler-generated lambda names; prefer `@Redirect` with `@Shadow` for static helpers
- Trial spawner ominous drops: capture target entity via redirect on `selectEntityToSpawnItemAbove`

### Where to document changes

| Change type | Document in |
|-------------|-------------|
| User-facing features / commands | `README.md` |
| Efficiency, correctness, tech debt | This file (new dated section below) |
| Cursor/agent guidance | `.cursor/rules/fairstructureloot.mdc` + this section |

---

## Audit history

### 2026-07-09 — Pre-GitHub release audit (v1.0.0)

**Scope:** Full mod review before standalone GitHub upload; README split; runtime fixes from playtesting.

#### Summary

| Area | Status |
|------|--------|
| Core chest/barrel/pot flow | Solid — event-driven, lazy generation |
| Structure groups | 21 registered groups; mod API `FairStructureLootAPI.registerGroup` |
| Trial vaults | `VaultBlockEntityServerMixin` — per-unlocker instanced roll |
| Trial spawner completion | Per detected player per vanilla eject cycle |
| Trial spawner ominous | Per-player roll; vanilla entity targeting via redirect |
| Destroy / break loot | Player break + explosion/piston via unload handler (air check) |
| C2ME compatibility | **Fixed** unload deadlock (see Correctness) |

#### Efficiency — solid

| Area | Assessment |
|------|------------|
| Event-driven design | No server tick polling; work on chunk load, join, open, break |
| Loot generation | Lazy on first open; vanilla `LootTable.fill` |
| Shared loot skip | `unpackLootTable` cancel avoids empty shared chests |
| HUD indicator | Cached fair-loot marker; opened state from client sets |
| Empty rolls | Retried with alternate seeds; empty results not persisted |
| SavedData scope | Single overworld store; dimension in container keys |

#### Efficiency — watch items

| Concern | Impact | Notes |
|---------|--------|-------|
| Join sync scans all folders | Grows with server age | `collectOpenedContainerKeysForPlayer` iterates every stored folder |
| Join fair-loot position burst | One packet per visible fair container | `InstancedChestScanner.syncLoadedChunks` on join; batch payload exists but per-chest still used in some paths |
| Inventory `setChanged` → dirty | Normal for stash mods | Every slot change rewrites player list |
| End chunk ItemFrame scan | Low after conversion | Every End `CHUNK_LOAD` AABB query; cache converted chunks if profiling shows cost |
| SavedData growth | Long-running worlds | One folder per fair container any player opened; destroy handler clears on break |

#### Correctness — implemented

- Per-player menus via vanilla `createMenu` redirect
- Barrels/pots use `RandomizableContainerBlockEntity.stillValid`
- Loot params: `ORIGIN`, `THIS_ENTITY`, `withLuck` (chest context)
- Break drops breaking player's loot; other destruction uses closest online player
- Elytra conversion: End only, active `end_ship` group, frame → instanced barrel in room air; scans on entity load, deferred chunk load, and player join
- Client caches cleared on disconnect
- **C2ME unload fix:** `LootChestBreakHandler.onBlockEntityUnload` uses `getChunkNow()` instead of `level.getBlockState()` — prevents 40–80s server thread freeze on quit/save (gamemode commands also blocked while frozen)
- **Trial spawner mixins:** Redirect-based ominous target capture; completion rewards iterate vanilla detected-player eject cycle

#### Correctness — known gaps

| Gap | Severity | Notes |
|-----|----------|-------|
| Archaeology brush loot | Low | Tables registered for some groups; no brush mixin yet — chest/dispenser only |
| Toggle-off HUD staleness | Low | Deactivated groups may show stale ▲ until reconnect |
| Orphaned SavedData | Low | Containers removed without unload/air path may leave folders |

#### Recommended follow-ups

1. **Nice-to-have** — Per-player opened-key index for faster join sync
2. **Nice-to-have** — End chunk elytra conversion cache
3. **Nice-to-have** — Clear fair-loot client markers when structure group deactivated
4. **Future** — Archaeology instancing mixin if brush loot should be per-player

#### Files touched this session (audit housekeeping)

- `README.md` — GitHub-facing user docs; audit section removed
- `FSLAudit.md` — created (this file)
- `.cursor/rules/fairstructureloot.mdc` — project Cursor rules

---

*Add a new `### YYYY-MM-DD — …` section above this line when performing future audits.*
