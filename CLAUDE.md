# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew build                # produces build/libs/YetAnotherThirst-1.20.1-1.4.0.jar
./gradlew runClient            # launch Minecraft client with mod loaded
./gradlew runServer            # launch dedicated server
./gradlew runData              # regenerate data (outputs to src/generated/resources/)
```

Gradle wrapper is the only build tool — do not use Maven. Java 17 required. No test suite exists.

## Architecture

**Minecraft Forge 1.20.1 mod** using SpongePowered Mixin and ForgeGradle 6.

### Package layout

| Package | Purpose |
|---|---|
| `dev.ghen.thirst` | Mod entrypoint (`Thirst.java`) — wires all registrations and event listeners |
| `dev.ghen.thirst.api` | Public API (`ThirstHelper`) — entry point for other mods |
| `dev.ghen.thirst.content` | Gameplay: thirst ticking, purity system, item/effect registries |
| `dev.ghen.thirst.foundation` | Infrastructure: capabilities, config, GUI, mixins, networking, utilities |
| `dev.ghen.thirst.compat.create` | Create mod integration (SandFilter block + Ponder scenes) |

### Core thirst system

- **`IThirst`** (capability interface) — defines thirst/quenched/exhaustion state contract
- **`PlayerThirst`** implements `IThirst` — per-player data, tick logic, NBT serialization, and client sync via `PlayerThirstSyncMessage`
- **`PlayerThirstManager`** — Forge event subscriber that calls `PlayerThirst.tick()` each player tick and handles capability attach/clone/death events
- **`ModCapabilities`** — registers the `PLAYER_THIRST` capability

### Water purity system

`WaterPurity` maps items to purity levels (PURIFIED, ACCEPTABLE, SLIGHTLY_DIRTY, DIRTY, VERY_DIRTY). Purity is stored as NBT on fluid containers. When drinking, `WaterPurity.givePurityEffects()` applies mob effects before `cap.drink()` is called.

### Config system

Five separate ForgeConfig files set up in `Thirst()` constructor:
- `CommonConfig` — game rules (depletion rates, damage, regen behavior)
- `ClientConfig` — HUD display options
- `ItemSettingsConfig` — per-item thirst values
- `KeyWordConfig` — keyword-based item matching for unknown items
- `ContainerConfig` — fluid container purity overrides

### Networking

`ThirstModPacketHandler` (SimpleChannel) — server→client sync of thirst state. Three messages: `PlayerThirstSyncMessage`, `DrinkByHandMessage`, `DrinkRainMessage`.

### Mixin strategy

Mixins in `foundation/mixin/` inject into vanilla classes (`MixinFoodData`, `MixinItemStack`, `MixinPotionItem`, etc.) and mod classes (sub-packages per mod: `create/`, `toughasnails/`, `brewinandchewin/`, etc.). Registered in `thirst.mixin.json`.

### Mod compatibility

Compatibility is gated by `ModList.get().isLoaded(modId)` in `Thirst.java`. Per-mod behavior flags are static booleans on `PlayerThirst` (e.g. `checkVampirismEffects`, `checkFDEffects`). Soft dependencies: Create, AppleSkin, Cold Sweat, Farmers Delight, Tough as Nails, Jade, Vampirism, Botania, Brewin and Chewin, Farmers Respite.
