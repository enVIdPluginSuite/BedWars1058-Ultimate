# BedWars1058-Ultimate

Hypixel-style **Ultimate** mode for [BedWars1058](https://github.com/andrei1058/BedWars1058): every player
picks one of seven ultimates (Kangaroo, Swordsman, Healer, Frozo, Builder, Demolition, Gatherer) from the item
shop. Written in Kotlin, built with Gradle, runs on **1.8 through 26.3**.

Everything is configurable: **343 options** across 9 YAML files, all editable **in-game** (GUI or `/bwu config`),
and the whole plugin **reloads live** with `/bwu reload`.

- [Install](#install) · [Build](#build) · [The ultimates](#the-ultimates) · [Configuration](#configuration) ·
  [Commands](#commands--permissions) · [Compatibility](#version-compatibility) ·
  [Fixes over the original](#what-was-fixed-compared-to-the-original) · [Testing](#testing)
- Full option list: [`docs/OPTIONS.md`](docs/OPTIONS.md) (generated from the code, so it can't go stale)

## Install

1. Put `BedWars1058-Ultimate-<version>.jar` in `plugins/` next to BedWars1058.
2. Start the server. Config files appear in `plugins/BedWars1058-Ultimate/`.
3. Give some arenas the BedWars1058 group `ultimate` (the default), or change `arenas.groups` /
   `arenas.names` / `arenas.all` in `config.yml` to match your arenas.
4. In a game, ten seconds after the start (configurable) players open the item shop and click the **Ultimates**
   button, or use `/bwu menu`.

Requires BedWars1058 and a JVM that your Minecraft version needs (Java 8 for 1.8; **Java 25 for 26.1+**, that is
the server's own requirement, the plugin itself is compiled for Java 8 bytecode).

## Build

```
./gradlew build            # -> build/libs/BedWars1058-Ultimate-<version>.jar  (the shaded one, no -plain)
./gradlew test             # unit tests
./gradlew compatTest       # checks the compiled plugin against the Bukkit API of 19 Minecraft versions
```

- Kotlin **2.2.21**, Gradle (wrapper included), Java 8 bytecode (`-Xjdk-release=8`, so only Java 8 APIs can be used).
- Compiled against the **Spigot 1.8.8 API**: anything that compiles exists on 1.8. Newer-only features go through
  `compat/` (reflection, fails soft).
- The Kotlin runtime is shaded **and relocated** to `dev.bw1058.ultimate.libs.kotlin`, so it can't clash with the
  other Kotlin plugins in your series.
- `libs/bedwars-plugin-23.3.jar` is the BedWars1058 API used at compile time (taken from the project you supplied).

## The ultimates

| Ultimate | Does |
|---|---|
| **Kangaroo** | Double jump (configurable jumps, power, cooldown). Keeps part of its resources on death. Milk on bed break. Speed on kill. |
| **Swordsman** | Hold right-click with a sword to charge a dash, release to launch. Hold again within the recall window to snap back. A kill refreshes the dash and heals. |
| **Healer** | More max health. Hold right-click with a sword to heal yourself. Throw a potion to leave a healing zone for your team. |
| **Frozo** | Throw a potion: enemies are slowed, teammates sped up. Snowballs on kill (optionally slowing snowballs). |
| **Builder** | Free team wool over time and per kill. One tool for walls, bridges and instant bed protection (left-click switches mode). |
| **Demolition** | Right-click wool to burn the whole connected structure. Drops primed TNT on death. TNT/fireball kill streaks. Creeper egg per bed. |
| **Gatherer** | Duplicates diamonds/emeralds collected from generators. Portable ender chest (private to the game by default). |

## Configuration

`plugins/BedWars1058-Ultimate/`

```
config.yml          arenas, switching rules, fall damage, HUD, shop button, selector item, menu, protection, reload behaviour
messages.yml        every chat message (colour codes, placeholders)
ultimates/
  kangaroo.yml  swordsman.yml  healer.yml  frozo.yml  builder.yml  demolition.yml  gatherer.yml
```

Each ultimate file contains its on/off switch, display name, **menu icon** (material, name, lore, glow, slot),
**every number of its abilities** (cooldowns, powers, radii, durations, amounts, chances, particles, sounds, item
names and lore) and its own messages. Icon lore can use live placeholders such as `{cooldown}`, so the menu text
always matches the real values.

Every file is generated with a comment above every option (description, allowed range/choices, default). Deleting a
line is always safe: missing options use their default. Invalid values are reported and fall back to the default;
numbers outside their range are clamped.

### Editing in-game

- `/bwu config` opens a GUI: pick a file, then browse its sections. Click a boolean to toggle it, left/right-click a
  number to step it (shift = ×10, middle-click to type an exact value), click a choice to cycle it, click text to
  type it in chat, click lists to add/remove/edit lines in chat, click with an item on your cursor to set a material,
  **shift + right-click resets to default**.
- Or from chat/console, with tab completion on file names, option paths and values:
  `/bwu config set kangaroo double-jump.cooldown-seconds 7.5`, `/bwu config get ...`, `/bwu config reset ...`,
  `/bwu config list kangaroo double-jump`, `/bwu enable|disable <ultimate>`.
- Edits change **only that one line** of the YAML file; your comments and formatting stay (Bukkit's own `save()`
  drops comments on servers older than 1.18).
- Ability numbers apply immediately. Item names/lore/materials apply to players when you press *Apply to players*
  in the GUI, run `/bwu refresh`, or `/bwu reload`.

### Reloading

`/bwu reload` re-reads every file. A file with a YAML syntax error is **not** applied (the previous values keep
running) and the error is shown; other files still reload. After a reload, running games are updated according to the
`reload:` section of `config.yml` (refresh players' items, activate arenas that newly qualify, deactivate arenas that no
longer do). `/bwu config validate` lists bad values and unknown/typo'd keys, `/bwu config update` appends options that
a newer version of the plugin added to your files.

## Commands & permissions

| Command | Permission |
|---|---|
| `/bwu list`, `/bwu info <ultimate>` | none |
| `/bwu menu`, `/bwu select <ultimate>` | `bw1058ultimate.use` (default: everyone) |
| `/bwu reload`, `/bwu refresh` | `bw1058ultimate.reload` |
| `/bwu config …`, `/bwu enable\|disable <ultimate>` | `bw1058ultimate.config` |
| `/bwu give <player> <ultimate>`, `/bwu clear <player>` | `bw1058ultimate.give` |
| `/bwu status` | `bw1058ultimate.admin` |

`bw1058ultimate.admin` includes all of the above (default: op). With `selection.permission-per-ultimate: true`,
picking needs `bw1058ultimate.use.<ultimate>` (or `.use.*`). Aliases: `/bwu`, `/ultimates`, `/bwultimate`.

## Version compatibility

The plugin makes **no use of server internals (NMS)**. Everything version-specific is either BedWars1058's own
`VersionSupport` (item tags, title/action bar, inventory names, wool colouring, unbreakable items) or the small
`compat/` package:

- **`McVersion`** understands both `1.x.y` and the year-based `26.1`, `26.3`, ... numbering.
- **`Sounds`, `Particles`, `Materials`, `Effects`** accept any era's names (`NOTE_PLING`, `BLOCK_NOTE_BLOCK_PLING`,
  `GOLD_SWORD`, `GOLDEN_SWORD`, `VILLAGER_HAPPY`, `HAPPY_VILLAGER`, `SLOW`, `SLOWNESS`, `RED_WOOL`, ...) and map them
  to what the server has. Modern coloured-block names work on 1.8 (`RED_WOOL` -> `WOOL:14`).
- Calls whose shape changed across versions (`Player#setMaxHealth`, off-hand, `Sound`/`Particle` constants,
  legacy data values) go through reflection with fallbacks.

## Project layout

```
src/main/kotlin/dev/bw1058/ultimate/
  UltimatePlugin.kt        entry point, tick loop, reload
  compat/                  McVersion, Sounds, Particles, Materials, Effects, PlayerCompat (everything version-specific)
  config/                  Opt/Specs (option declarations), ConfigFile, YamlWriter, YamlEditor, ConfigManager, settings
  core/                    ArenaTracker, Cooldowns, BlockingTracker, ItemFactory, UltimateItems, ...
  ultimate/                Ultimate (base class), UltimateManager and the seven ultimates
  gui/                     Menu framework, selection menu, in-game config editor, chat prompts
  listener/                game lifecycle + global rules, shop integration
  command/                 /bwu
src/test/                  unit tests, class-file checker (ApiCompatTest) and its negative-control fixture
docs/OPTIONS.md            generated option reference
```

### Adding an ultimate or an option

An option is one line in the ultimate's class:

```kotlin
private val power = specs.double("double-jump.power", 1.0, "Upward push of the jump.", min = 0.0, max = 5.0, step = 0.1)
// ... power.get() anywhere
```

That one line creates the commented YAML entry, validation, the `/bwu config` tab completion, the in-game editor
button and the entry in `docs/OPTIONS.md` (regenerate with `./gradlew test --tests '*DocsGeneratorTest*'`).
A new ultimate extends `Ultimate`, implements `onActivate`, and is registered in `UltimatePlugin.onEnable`.

## Developer note
This plugin is coded with AI assistance and architected completely by me. Regardless, use at your own risk.

## License
This plugin is under the MIT License, check the License file for more details.