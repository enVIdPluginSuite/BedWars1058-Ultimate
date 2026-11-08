# Option reference

Generated from the plugin's option declarations - every option below exists in the YAML files, in `/bwu config` and in the in-game editor. Ranges are enforced: values outside them are clamped when read from a file and rejected when typed in-game.

## `config.yml`


### arenas
_Which arenas run ultimate mode_

| Option | Type | Default | Description |
|---|---|---|---|
| `arenas.groups` | list | 1 line(s) | BedWars1058 arena groups that play Ultimate mode. An arena whose group is listed here gets ultimates.  |
| `arenas.names` | list | _empty_ | Individual arena names that play Ultimate mode, in addition to the groups above.  |
| `arenas.all` | true/false | `false` | Run Ultimate mode in EVERY BedWars1058 arena, ignoring groups and names.  |
| `arenas.activation-delay-seconds` | whole number | `10` | Seconds after the game starts before ultimates are switched on. 0 = immediately. Range 0 to 3600. |
| `arenas.remember-selection` | true/false | `false` | Keep each player's chosen ultimate for their next game (until they leave the server).  |
| `arenas.default-ultimate` | text | `NONE` | Ultimate given to players who haven't picked one when ultimates switch on. NONE, RANDOM, or an ultimate id (kangaroo, swordsman, healer, frozo, builder, demolition, gatherer).  |

### selection
_Picking and switching ultimates_

| Option | Type | Default | Description |
|---|---|---|---|
| `selection.allow-switching` | true/false | `true` | Allow changing ultimate after the first pick.  |
| `selection.switch-cooldown-seconds` | whole number | `20` | Seconds a player must wait between picks. 0 disables the cooldown. Range 0 to 3600. |
| `selection.first-pick-free` | true/false | `false` | If true, a player's very first pick doesn't start the switch cooldown.  |
| `selection.allow-while-respawning` | true/false | `true` | Allow picking/switching while waiting to respawn.  |
| `selection.permission-per-ultimate` | true/false | `false` | Require the permission etranultimate.use.<id> (for example etranultimate.use.healer) to pick each ultimate.  |
| `selection.select-sound` | sound | `NOTE_PLING` | Sound when an ultimate is picked. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `selection.denied-sound` | sound | `VILLAGER_NO` | Sound when a pick is refused. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |

### activation
_What players see when ultimates switch on_

| Option | Type | Default | Description |
|---|---|---|---|
| `activation.sound` | sound | `ENTITY_PLAYER_LEVELUP` | Sound played to the arena when ultimates switch on. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `activation.chat-message` | true/false | `true` | Send the 'ultimates-enabled' lines from messages.yml.  |
| `activation.title.enabled` | true/false | `false` | Show a title on screen when ultimates switch on.  |
| `activation.title.title` | text (&-colours) | `&6&lULTIMATES` | Main title text.  |
| `activation.title.subtitle` | text (&-colours) | `&eOpen the shop to pick one!` | Subtitle text.  |
| `activation.title.fade-in-ticks` | whole number | `10` | Title fade-in time in ticks (20 ticks = 1 second). Range 0 to 400. |
| `activation.title.stay-ticks` | whole number | `60` | Title stay time in ticks. Range 0 to 1200. |
| `activation.title.fade-out-ticks` | whole number | `10` | Title fade-out time in ticks. Range 0 to 400. |

### fall-damage
_Fall damage in ultimate arenas_

| Option | Type | Default | Description |
|---|---|---|---|
| `fall-damage.disable` | true/false | `true` | Cancel fall damage in ultimate arenas (the Hypixel mode has none).  |
| `fall-damage.only-ultimate-players` | true/false | `false` | If true, fall damage is only removed for players who have an ultimate selected.  |
| `fall-damage.multiplier` | number | `1.0` | Fall damage multiplier used when 'disable' is false (0.5 = half damage). Range 0.0 to 10.0. |

### hud
_Cooldown display_

| Option | Type | Default | Description |
|---|---|---|---|
| `hud.xp-bar` | true/false | `true` | Show the active cooldown on the XP bar (level = seconds left).  |
| `hud.action-bar` | true/false | `false` | Also show the cooldown in the action bar.  |
| `hud.action-bar-format` | text (&-colours) | `&e{name} &7- &c{seconds}s` | Action bar text. Placeholders: {name}, {seconds}.  |
| `hud.update-ticks` | whole number | `1` | How often the HUD refreshes, in ticks. Higher = lighter. Range 1 to 20. |

### shop-button
_The 'ultimates' button added to the bedwars item shop_

| Option | Type | Default | Description |
|---|---|---|---|
| `shop-button.enabled` | true/false | `true` | Add the Ultimates button to BedWars1058's item shop inventories.  |
| `shop-button.material` | material | `BEACON` | Material of the shop button. Modern and legacy names both work.  |
| `shop-button.name` | text (&-colours) | `&aUltimates` | Display name of the shop button. Supports & colour codes.  |
| `shop-button.lore` | list of text | 1 line(s) | Lore lines of the shop button. Supports & colour codes and the placeholders listed in the section header.  |
| `shop-button.glow` | true/false | `false` | Adds an enchantment glint to the shop button.  |
| `shop-button.slot` | whole number | `8` | Inventory slot of the shop button (0 = top-left). Range 0 to 53. |

### selector-item
_Optional hotbar item that opens the ultimate menu_

| Option | Type | Default | Description |
|---|---|---|---|
| `selector-item.enabled` | true/false | `false` | Give players an item that opens the ultimate menu without visiting the shop.  |
| `selector-item.give-on-activation` | true/false | `true` | Give the item when ultimates switch on (and after each respawn).  |
| `selector-item.material` | material | `NETHER_STAR` | Material of the selector item. Modern and legacy names both work.  |
| `selector-item.name` | text (&-colours) | `&6&lUltimate Selector` | Display name of the selector item. Supports & colour codes.  |
| `selector-item.lore` | list of text | 1 line(s) | Lore lines of the selector item. Supports & colour codes and the placeholders listed in the section header.  |
| `selector-item.glow` | true/false | `true` | Adds an enchantment glint to the selector item.  |
| `selector-item.slot` | whole number | `4` | Inventory slot of the selector item (0 = top-left). Range 0 to 53. |

### menu
_The ultimate selection menu_

| Option | Type | Default | Description |
|---|---|---|---|
| `menu.title` | text (&-colours) | `&8Ultimates` | Inventory title (long titles are cut to 32 characters on older servers).  |
| `menu.rows` | whole number | `3` | Number of rows (1-6). Make sure every ultimate's icon slot fits. Range 1 to 6. |
| `menu.filler.enabled` | true/false | `true` | Fill empty slots with a pane.  |
| `menu.filler.material` | material | `GRAY_STAINED_GLASS_PANE` | Material of the filler pane. Modern and legacy names both work.  |
| `menu.filler.name` | text (&-colours) | ` ` | Display name of the filler pane. Supports & colour codes.  |
| `menu.filler.lore` | list of text | _empty_ | Lore lines of the filler pane. Supports & colour codes and the placeholders listed in the section header.  |
| `menu.filler.glow` | true/false | `false` | Adds an enchantment glint to the filler pane.  |
| `menu.status-selected` | text (&-colours) | `&cCurrently selected!` | Replaces {status} in an ultimate's lore when it is the player's current pick.  |
| `menu.status-unselected` | text (&-colours) | `&eClick to select!` | Replaces {status} in an ultimate's lore otherwise.  |
| `menu.status-cooldown` | text (&-colours) | `&cSwitch cooldown: {seconds}s` | Replaces {status} while the player is still on their switch cooldown.  |
| `menu.back-button.enabled` | true/false | `true` | Show a button that returns to the BedWars item shop.  |
| `menu.back-button.material` | material | `ARROW` | Material of the back button. Modern and legacy names both work.  |
| `menu.back-button.name` | text (&-colours) | `&aBack to Shop` | Display name of the back button. Supports & colour codes.  |
| `menu.back-button.lore` | list of text | 1 line(s) | Lore lines of the back button. Supports & colour codes and the placeholders listed in the section header.  |
| `menu.back-button.glow` | true/false | `false` | Adds an enchantment glint to the back button.  |
| `menu.back-button.slot` | whole number | `22` | Inventory slot of the back button (0 = top-left). Range 0 to 53. |
| `menu.open-sound` | sound | _blank_ | Sound when the menu opens. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `menu.close-on-select` | true/false | `false` | Close the menu after a successful pick (otherwise it stays open and refreshes).  |

### protection
_Keeping ultimate items safe_

| Option | Type | Default | Description |
|---|---|---|---|
| `protection.prevent-dropping` | true/false | `true` | Players cannot drop ultimate items.  |
| `protection.prevent-moving` | true/false | `true` | Ultimate items cannot be moved into chests, the shop or other containers.  |
| `protection.remove-on-death` | true/false | `true` | Ultimate items never appear in death drops.  |

### compat
_Version compatibility tuning (normally leave alone)_

| Option | Type | Default | Description |
|---|---|---|---|
| `compat.blocking-window-ms` | whole number | `275` | On 1.9+ swords cannot really block, so 'holding right-click' is detected from repeated interact events. A hold counts as active for this many milliseconds after the last event. Raise it if holding feels laggy. Range 100 to 1000. |
| `compat.blocking-item-suffixes` | list | 1 line(s) | Item types (matched by name ending) that count as 'blocking' when right-click is held. SWORD matches every sword tier.  |

### reload
_What /bwu reload does to games that are already running_

| Option | Type | Default | Description |
|---|---|---|---|
| `reload.refresh-active-players` | true/false | `true` | Re-give ultimate items (new names, lore, materials) to players in running ultimate games.  |
| `reload.activate-running-arenas` | true/false | `true` | Switch ultimates on in running arenas that newly qualify (e.g. after adding a group).  |
| `reload.deactivate-removed-arenas` | true/false | `true` | Switch ultimates off in running arenas that no longer qualify.  |

### Other

| Option | Type | Default | Description |
|---|---|---|---|
| `debug` | true/false | `false` | Log extra information to the console.  |

## `messages.yml`


### general
_Common_

| Option | Type | Default | Description |
|---|---|---|---|
| `general.prefix` | text (&-colours) | `&8[&6Ultimate&8] &r` | Put in front of command feedback. Chat lines for players in-game don't use it.  |
| `general.no-permission` | text (&-colours) | `&cYou don't have permission to do that.` | Shown when a command is denied.  |
| `general.players-only` | text (&-colours) | `&cOnly players can use this.` | Shown when the console runs a player-only command.  |
| `general.unknown-command` | text (&-colours) | `&cUnknown subcommand. Use &e/bwu help&c.` | Shown for an unknown subcommand.  |
| `general.unknown-ultimate` | text (&-colours) | `&cThere is no ultimate called &e{id}&c. Available: &f{list}` | Placeholders: {id}, {list}.  |
| `general.unknown-player` | text (&-colours) | `&cPlayer &e{name}&c is not online.` | Placeholder: {name}.  |

### selection
_Picking ultimates_

| Option | Type | Default | Description |
|---|---|---|---|
| `selection.selected` | text (&-colours) | `&aSwitched ultimate to &6{ultimate}&a!` | Placeholder: {ultimate}.  |
| `selection.selected-pending` | text (&-colours) | `&7Selected &6{ultimate}&7. It activates when ultimates switch on.` | Shown when picking before ultimates are enabled in the arena.  |
| `selection.already-selected` | text (&-colours) | `&cYou already have this ultimate selected!` | Shown when re-picking the current ultimate.  |
| `selection.disabled` | text (&-colours) | `&cThe {ultimate} ultimate is disabled.` | Placeholder: {ultimate}.  |
| `selection.not-in-game` | text (&-colours) | `&cYou must be in a BedWars game to pick an ultimate.` | Shown outside of games.  |
| `selection.not-ultimate-arena` | text (&-colours) | `&cThis arena doesn't play Ultimate mode.` | Shown when picking in a normal arena.  |
| `selection.switch-cooldown` | text (&-colours) | `&cYou must wait {seconds} seconds before switching ultimate!` | Placeholder: {seconds}.  |
| `selection.switching-disabled` | text (&-colours) | `&cYou can't change your ultimate once you picked one.` | Shown when selection.allow-switching is false.  |
| `selection.no-ultimate-permission` | text (&-colours) | `&cYou don't have permission to use the {ultimate} ultimate.` | Placeholder: {ultimate}.  |
| `selection.cant-while-respawning` | text (&-colours) | `&cYou can't change your ultimate while respawning.` | Shown when selection.allow-while-respawning is false.  |
| `selection.default-assigned` | text (&-colours) | `&7You were given the &6{ultimate}&7 ultimate. Open the shop to change it.` | Shown when arenas.default-ultimate gives a player an ultimate.  |

### activation
_Ultimates switching on_

| Option | Type | Default | Description |
|---|---|---|---|
| `activation.ultimates-enabled` | list of text | 6 line(s) | Lines sent to every player in the arena when ultimates switch on.  |

### reload
_Reload and diagnostics_

| Option | Type | Default | Description |
|---|---|---|---|
| `reload.success` | text (&-colours) | `&aReloaded all configuration files &7({count} files, {players} players refreshed).` | Placeholders: {count}, {players}.  |
| `reload.warnings-header` | text (&-colours) | `&eReloaded with {count} warning(s):` | Placeholder: {count}.  |
| `reload.errors-header` | text (&-colours) | `&cReload finished with {count} error(s) - the affected files kept their previous values:` | Placeholder: {count}.  |
| `reload.line` | text (&-colours) | `&7 - &f{text}` | One line per problem. Placeholder: {text}.  |

### config
_In-game config editor_

| Option | Type | Default | Description |
|---|---|---|---|
| `config.set` | text (&-colours) | `&aSet &e{path} &ain &f{file} &ato &f{value}&a.` | Placeholders: {file}, {path}, {value}.  |
| `config.reset` | text (&-colours) | `&aReset &e{path} &ain &f{file} &ato its default (&f{value}&a).` | Placeholders: {file}, {path}, {value}.  |
| `config.get` | text (&-colours) | `&e{path} &7= &f{value} &8(default: {default})` | Placeholders: {path}, {value}, {default}.  |
| `config.invalid` | text (&-colours) | `&cInvalid value for &e{path}&c: {reason}` | Placeholders: {path}, {reason}.  |
| `config.unknown-file` | text (&-colours) | `&cUnknown file &e{id}&c. Choose from: &f{list}` | Placeholders: {id}, {list}.  |
| `config.unknown-option` | text (&-colours) | `&cUnknown option &e{path}&c in &f{file}&c. Use &e/bwu config list {file}` | Placeholders: {file}, {path}.  |
| `config.write-failed` | text (&-colours) | `&cCould not save the change: {reason}` | Placeholder: {reason}.  |
| `config.added-missing` | text (&-colours) | `&aAdded {count} missing option(s) to the files.` | Placeholder: {count}.  |
| `config.prompt-value` | text (&-colours) | `&6Type the new value for &e{path} &6in chat &7(or &ccancel&7). Current: &f{value}` | Placeholders: {path}, {value}.  |
| `config.prompt-list` | text (&-colours) | `&6Editing list &e{path}&6. Type: &eadd <text>&6, &eset <n> <text>&6, &eremove <n>&6, &eclear&6, &edone &6or &ecancel&6.` | Placeholder: {path}.  |
| `config.prompt-cancelled` | text (&-colours) | `&7Cancelled.` | Shown when an edit prompt is cancelled.  |
| `config.prompt-list-line` | text (&-colours) | `&7{index}. &f{text}` | One line of the list preview. Placeholders: {index}, {text}.  |

## `ultimates/kangaroo.yml`

Placeholders usable in this file's icon lore: `{status}` and `{cooldown}`, `{keep-chance}`, `{keep-percent}`, `{speed-seconds}` (read live from the options below).

| `enabled` | true/false | `true` | Turn this ultimate on or off. A disabled ultimate disappears from the menu, and players holding it lose it on /bwu reload.  |
| `display-name` | text (&-colours) | `Kangaroo` | Name shown in menus and chat messages.  |

### icon
_Menu icon - lore placeholders: {status}, {cooldown}, {keep-chance}, {keep-percent}, {speed-seconds}_

| Option | Type | Default | Description |
|---|---|---|---|
| `icon.material` | material | `RABBIT_FOOT` | Material of the menu icon. Modern and legacy names both work.  |
| `icon.name` | text (&-colours) | `&aKangaroo` | Display name of the menu icon. Supports & colour codes.  |
| `icon.lore` | list of text | 8 line(s) | Lore lines of the menu icon. Supports & colour codes and the placeholders listed in the section header.  |
| `icon.glow` | true/false | `false` | Adds an enchantment glint to the menu icon.  |
| `icon.slot` | whole number | `10` | Inventory slot of the menu icon (0 = top-left). Range 0 to 53. |

### double-jump

| Option | Type | Default | Description |
|---|---|---|---|
| `double-jump.enabled` | true/false | `true` | Enable the double jump.  |
| `double-jump.max-jumps` | whole number | `1` | Mid-air jumps available before the cooldown starts. Range 1 to 10. |
| `double-jump.cooldown-seconds` | number | `10.0` | Cooldown after the last jump. 0 = no cooldown. Range 0.0 to 600.0. |
| `double-jump.horizontal-power` | number | `0.5` | Forward push of the jump (multiplies your look direction). Range 0.0 to 5.0. |
| `double-jump.vertical-power` | number | `1.0` | Upward push of the jump. Range 0.0 to 5.0. |
| `double-jump.rejump-delay-ticks` | whole number | `6` | With max-jumps above 1: ticks before the next mid-air jump becomes available. Range 1 to 100. |
| `double-jump.sound` | sound | `ENTITY_BAT_TAKEOFF` | Sound when you jump. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `double-jump.particle` | particle | `CLOUD` | Particle puffed under your feet when you jump. Blank disables it.  |
| `double-jump.particle-count` | whole number | `8` | Number of particles. Range 1 to 100. |
| `double-jump.ready-sound` | sound | _blank_ | Sound when the double jump is available again. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `double-jump.reset-cooldown-on-kill` | true/false | `false` | A kill instantly refreshes the double jump.  |

### keep-resources

| Option | Type | Default | Description |
|---|---|---|---|
| `keep-resources.enabled` | true/false | `true` | Keep part of your resources when you die (if your bed is still alive).  |
| `keep-resources.chance-percent` | whole number | `50` | Chance (0-100) that the effect triggers on a given death. Range 0 to 100. |
| `keep-resources.keep-fraction` | number | `0.5` | Share of eligible item stacks that are kept (0.5 = half of your stacks, rounded down). Range 0.0 to 1.0. |
| `keep-resources.resources-only` | true/false | `true` | Only resource stacks are eligible. If false, every inventory stack is.  |
| `keep-resources.resource-materials` | list of materials | 4 line(s) | Materials that count as resources when resources-only is on.  |

### milk

| Option | Type | Default | Description |
|---|---|---|---|
| `milk.enabled` | true/false | `true` | Receive milk buckets when you destroy a bed.  |
| `milk.amount` | whole number | `1` | Milk buckets received. Range 1 to 16. |

### speed-on-kill

| Option | Type | Default | Description |
|---|---|---|---|
| `speed-on-kill.enabled` | true/false | `true` | Gain speed after a kill.  |
| `speed-on-kill.amplifier` | whole number | `1` | Speed level minus one (1 = Speed II). Range 0 to 10. |
| `speed-on-kill.duration-seconds` | number | `5.0` | How long the speed lasts. Range 0.5 to 600.0. |
| `speed-on-kill.show-particles` | true/false | `false` | Show potion particles while the speed is active.  |

### messages
_Messages sent by this ultimate_

| Option | Type | Default | Description |
|---|---|---|---|
| `messages.kept-resources` | text (&-colours) | `&7Kangaroo kept &e{count} &7stack(s) of your resources!` | Sent after respawn. Placeholder: {count}. Blank = no message.  |
| `messages.milk` | text (&-colours) | _blank_ | Sent when you receive milk. Blank = no message.  |

## `ultimates/swordsman.yml`

Placeholders usable in this file's icon lore: `{status}` and `{cooldown}`, `{recall-window}`, `{kill-hearts}`, `{max-charge}` (read live from the options below).

| `enabled` | true/false | `true` | Turn this ultimate on or off. A disabled ultimate disappears from the menu, and players holding it lose it on /bwu reload.  |
| `display-name` | text (&-colours) | `Swordsman` | Name shown in menus and chat messages.  |

### icon
_Menu icon - lore placeholders: {status}, {cooldown}, {recall-window}, {kill-hearts}, {max-charge}_

| Option | Type | Default | Description |
|---|---|---|---|
| `icon.material` | material | `GOLDEN_SWORD` | Material of the menu icon. Modern and legacy names both work.  |
| `icon.name` | text (&-colours) | `&aSwordsman` | Display name of the menu icon. Supports & colour codes.  |
| `icon.lore` | list of text | 10 line(s) | Lore lines of the menu icon. Supports & colour codes and the placeholders listed in the section header.  |
| `icon.glow` | true/false | `false` | Adds an enchantment glint to the menu icon.  |
| `icon.slot` | whole number | `11` | Inventory slot of the menu icon (0 = top-left). Range 0 to 53. |

### charge

| Option | Type | Default | Description |
|---|---|---|---|
| `charge.max-ticks` | whole number | `40` | Ticks of holding right-click for a full-power dash (20 ticks = 1 second). Range 5 to 200. |
| `charge.sound` | sound | `NOTE_PLING` | Sound played while charging. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `charge.sound-interval-ticks` | whole number | `5` | Ticks between charge sounds. Range 1 to 40. |
| `charge.show-xp-bar` | true/false | `true` | Show charge progress on the XP bar.  |

### dash

| Option | Type | Default | Description |
|---|---|---|---|
| `dash.cooldown-seconds` | number | `10.0` | Cooldown after a dash. 0 = no cooldown. Range 0.0 to 600.0. |
| `dash.speed-per-charge-tick` | number | `0.08` | Forward speed gained per tick of charge. Range 0.0 to 1.0. |
| `dash.lift-per-charge-tick` | number | `0.03` | Upward speed gained per tick of charge. Range 0.0 to 1.0. |
| `dash.max-speed` | number | `3.2` | Upper limit for the forward speed multiplier. Range 0.1 to 10.0. |
| `dash.min-charge-ticks` | whole number | `1` | Releasing before this many ticks of charge does nothing. Range 1 to 200. |
| `dash.sound` | sound | `FIREWORK_LAUNCH` | Sound when dashing. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `dash.particle` | particle | `CLOUD` | Particle burst at the start of a dash. Blank disables it.  |
| `dash.particle-count` | whole number | `12` | Number of particles. Range 1 to 100. |
| `dash.fall-immunity-seconds` | number | `3.0` | No fall damage for this long after a dash. Range 0.0 to 60.0. |

### recall

| Option | Type | Default | Description |
|---|---|---|---|
| `recall.enabled` | true/false | `true` | Allow snapping back to the dash origin.  |
| `recall.window-seconds` | number | `5.0` | Seconds after a dash during which recall is possible. Range 0.5 to 60.0. |
| `recall.sound` | sound | `ENDERMAN_TELEPORT` | Sound when recalling. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `recall.particle` | particle | `PORTAL` | Particle at the origin and destination of a recall. Blank disables it.  |
| `recall.particle-count` | whole number | `20` | Number of particles. Range 1 to 200. |
| `recall.clear-velocity` | true/false | `true` | Stop all movement on arrival.  |

### on-kill

| Option | Type | Default | Description |
|---|---|---|---|
| `on-kill.reset-cooldown` | true/false | `true` | A kill refreshes the dash cooldown.  |
| `on-kill.heal-hearts` | number | `1.0` | Hearts healed on a kill (never above max health). 0 = none. Range 0.0 to 50.0. |
| `on-kill.sound` | sound | _blank_ | Sound when a kill refreshes you. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |

## `ultimates/healer.yml`

Placeholders usable in this file's icon lore: `{status}` and `{cooldown}`, `{potion-cooldown}`, `{hearts}`, `{radius}`, `{zone-seconds}` (read live from the options below).

| `enabled` | true/false | `true` | Turn this ultimate on or off. A disabled ultimate disappears from the menu, and players holding it lose it on /bwu reload.  |
| `display-name` | text (&-colours) | `Healer` | Name shown in menus and chat messages.  |

### icon
_Menu icon - lore placeholders: {status}, {cooldown}, {potion-cooldown}, {hearts}, {radius}, {zone-seconds}_

| Option | Type | Default | Description |
|---|---|---|---|
| `icon.material` | material | `GOLDEN_APPLE` | Material of the menu icon. Modern and legacy names both work.  |
| `icon.name` | text (&-colours) | `&aHealer` | Display name of the menu icon. Supports & colour codes.  |
| `icon.lore` | list of text | 8 line(s) | Lore lines of the menu icon. Supports & colour codes and the placeholders listed in the section header.  |
| `icon.glow` | true/false | `false` | Adds an enchantment glint to the menu icon.  |
| `icon.slot` | whole number | `12` | Inventory slot of the menu icon (0 = top-left). Range 0 to 53. |

### health

| Option | Type | Default | Description |
|---|---|---|---|
| `health.bonus-enabled` | true/false | `true` | Raise max health while this ultimate is active.  |
| `health.max-hearts` | number | `15.0` | Max health in hearts while active (10 = normal). Range 1.0 to 100.0. |
| `health.fill-on-activate` | true/false | `true` | Fill health to the new maximum when the ultimate is applied (selection and each respawn).  |
| `health.restore-hearts` | number | `10.0` | Max health in hearts restored when the ultimate is removed. Range 1.0 to 100.0. |

### self-heal

| Option | Type | Default | Description |
|---|---|---|---|
| `self-heal.enabled` | true/false | `true` | Allow healing yourself by holding right-click with a sword.  |
| `self-heal.cooldown-seconds` | number | `20.0` | Cooldown after starting a self-heal. Range 0.0 to 600.0. |
| `self-heal.heal-amount` | number | `1.0` | Health restored per pulse (1.0 = half a heart). Range 0.5 to 40.0. |
| `self-heal.interval-ticks` | whole number | `20` | Ticks between pulses. Range 1 to 200. |
| `self-heal.max-duration-seconds` | number | `0.0` | Stop healing after this long. 0 = until you are at full health. Range 0.0 to 600.0. |
| `self-heal.require-damage` | true/false | `true` | Don't start (or burn the cooldown) while already at full health.  |
| `self-heal.start-sound` | sound | `NOTE_PLING` | Sound when the heal starts. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `self-heal.pulse-particle` | particle | `HEART` | Particle on each pulse. Blank disables it.  |

### potion

| Option | Type | Default | Description |
|---|---|---|---|
| `potion.item.name` | text (&-colours) | `&aHealer Potion` | Display name of the healer potion. Supports & colour codes.  |
| `potion.item.lore` | list of text | 2 line(s) | Lore lines of the healer potion. Supports & colour codes and the placeholders listed in the section header.  |
| `potion.item.glow` | true/false | `false` | Adds an enchantment glint to the healer potion.  |
| `potion.cooldown-seconds` | number | `20.0` | Seconds until the potion is returned after being thrown. Range 0.0 to 600.0. |
| `potion.give-back` | true/false | `true` | Return the potion after the cooldown. If false it is a one-time item per life.  |
| `potion.zone.effect` | potion effect | `REGENERATION` | Effect applied to players standing in the zone.  |
| `potion.zone.amplifier` | whole number | `0` | Effect level minus one. Range 0 to 10. |
| `potion.zone.effect-duration-ticks` | whole number | `100` | Duration of each application, in ticks. Range 1 to 2400. |
| `potion.zone.radius` | number | `2.5` | Zone radius in blocks (a box, like the original plugin). Range 0.5 to 20.0. |
| `potion.zone.duration-seconds` | number | `30.0` | How long the zone lasts. Range 1.0 to 600.0. |
| `potion.zone.interval-ticks` | whole number | `20` | Ticks between applications. Range 1 to 200. |
| `potion.zone.affect-thrower` | true/false | `true` | The thrower benefits from their own zone.  |
| `potion.zone.affect-teammates` | true/false | `true` | Teammates benefit from the zone.  |
| `potion.zone.particle` | particle | `VILLAGER_HAPPY` | Particle drawn in rings across the zone. Blank disables it.  |
| `potion.zone.particle-density` | number | `1.0` | Particle amount multiplier (0.5 = half as many, 0 = none). Range 0.0 to 5.0. |
| `potion.zone.land-sound` | sound | _blank_ | Sound where the potion lands. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |

### messages
_Messages sent by this ultimate_

| Option | Type | Default | Description |
|---|---|---|---|
| `messages.potion-ready` | text (&-colours) | _blank_ | Sent when the potion comes back. Blank = no message.  |

## `ultimates/frozo.yml`

Placeholders usable in this file's icon lore: `{status}` and `{cooldown}`, `{snowballs}`, `{max-snowballs}`, `{radius}` (read live from the options below).

| `enabled` | true/false | `true` | Turn this ultimate on or off. A disabled ultimate disappears from the menu, and players holding it lose it on /bwu reload.  |
| `display-name` | text (&-colours) | `Frozo` | Name shown in menus and chat messages.  |

### icon
_Menu icon - lore placeholders: {status}, {cooldown}, {snowballs}, {max-snowballs}, {radius}_

| Option | Type | Default | Description |
|---|---|---|---|
| `icon.material` | material | `SNOW_BLOCK` | Material of the menu icon. Modern and legacy names both work.  |
| `icon.name` | text (&-colours) | `&aFrozo` | Display name of the menu icon. Supports & colour codes.  |
| `icon.lore` | list of text | 7 line(s) | Lore lines of the menu icon. Supports & colour codes and the placeholders listed in the section header.  |
| `icon.glow` | true/false | `false` | Adds an enchantment glint to the menu icon.  |
| `icon.slot` | whole number | `13` | Inventory slot of the menu icon (0 = top-left). Range 0 to 53. |

### potion

| Option | Type | Default | Description |
|---|---|---|---|
| `potion.item.name` | text (&-colours) | `&aFrozo Potion` | Display name of the frozo potion. Supports & colour codes.  |
| `potion.item.lore` | list of text | 1 line(s) | Lore lines of the frozo potion. Supports & colour codes and the placeholders listed in the section header.  |
| `potion.item.glow` | true/false | `false` | Adds an enchantment glint to the frozo potion.  |
| `potion.cooldown-seconds` | number | `20.0` | Seconds until the potion is returned after being thrown. Range 0.0 to 600.0. |
| `potion.give-back` | true/false | `true` | Return the potion after the cooldown. If false it is a one-time item per life.  |
| `potion.use-vanilla-radius` | true/false | `false` | Use the game's own splash area instead of 'radius' below.  |
| `potion.radius` | number | `4.0` | Splash radius in blocks (sphere). Range 0.5 to 30.0. |
| `potion.splash-particle` | particle | `SNOWBALL` | Particle burst where the potion lands. Blank disables it.  |
| `potion.splash-particle-count` | whole number | `60` | Number of particles. Range 1 to 500. |
| `potion.splash-sound` | sound | `GLASS` | Sound where the potion lands. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |

### enemy-effect

| Option | Type | Default | Description |
|---|---|---|---|
| `enemy-effect.enabled` | true/false | `true` | Apply an effect to enemies hit by the splash.  |
| `enemy-effect.type` | potion effect | `SLOW` | Potion effect given to enemies.  |
| `enemy-effect.amplifier` | whole number | `1` | Effect level minus one (1 = Slowness II). Range 0 to 10. |
| `enemy-effect.duration-seconds` | number | `60.0` | Effect duration. Range 0.5 to 1200.0. |

### ally-effect

| Option | Type | Default | Description |
|---|---|---|---|
| `ally-effect.enabled` | true/false | `true` | Apply an effect to teammates hit by the splash.  |
| `ally-effect.type` | potion effect | `SPEED` | Potion effect given to teammates.  |
| `ally-effect.amplifier` | whole number | `5` | Effect level minus one (5 = Speed VI). Range 0 to 10. |
| `ally-effect.duration-seconds` | number | `30.0` | Effect duration. Range 0.5 to 1200.0. |
| `ally-effect.include-thrower` | true/false | `true` | The thrower counts as a teammate.  |

### on-kill

| Option | Type | Default | Description |
|---|---|---|---|
| `on-kill.snowballs-enabled` | true/false | `true` | Receive snowballs on a kill.  |
| `on-kill.snowballs` | whole number | `2` | Snowballs received per kill. Range 1 to 64. |
| `on-kill.max-snowballs` | whole number | `16` | Never carry more than this many Frozo snowballs. Range 1 to 64. |
| `on-kill.snowball-name` | text (&-colours) | `&bFrozo Snowball` | Name of the snowball item.  |

### snowball-hit

| Option | Type | Default | Description |
|---|---|---|---|
| `snowball-hit.enabled` | true/false | `false` | Frozo snowballs also slow enemies they hit (off = plain snowballs, like the original).  |
| `snowball-hit.effect` | potion effect | `SLOW` | Effect applied on a snowball hit.  |
| `snowball-hit.amplifier` | whole number | `0` | Effect level minus one. Range 0 to 10. |
| `snowball-hit.duration-seconds` | number | `3.0` | Effect duration. Range 0.5 to 120.0. |

### messages
_Messages sent by this ultimate_

| Option | Type | Default | Description |
|---|---|---|---|
| `messages.potion-ready` | text (&-colours) | _blank_ | Sent when the potion comes back. Blank = no message.  |

## `ultimates/builder.yml`

Placeholders usable in this file's icon lore: `{status}` and `{wall-width}`, `{wall-height}`, `{bridge-length}`, `{kill-wool}`, `{wool-interval}` (read live from the options below).

| `enabled` | true/false | `true` | Turn this ultimate on or off. A disabled ultimate disappears from the menu, and players holding it lose it on /bwu reload.  |
| `display-name` | text (&-colours) | `Builder` | Name shown in menus and chat messages.  |

### icon
_Menu icon - lore placeholders: {status}, {wall-width}, {wall-height}, {bridge-length}, {kill-wool}, {wool-interval}_

| Option | Type | Default | Description |
|---|---|---|---|
| `icon.material` | material | `BRICKS` | Material of the menu icon. Modern and legacy names both work.  |
| `icon.name` | text (&-colours) | `&aBuilder` | Display name of the menu icon. Supports & colour codes.  |
| `icon.lore` | list of text | 10 line(s) | Lore lines of the menu icon. Supports & colour codes and the placeholders listed in the section header.  |
| `icon.glow` | true/false | `false` | Adds an enchantment glint to the menu icon.  |
| `icon.slot` | whole number | `14` | Inventory slot of the menu icon (0 = top-left). Range 0 to 53. |

### tools

| Option | Type | Default | Description |
|---|---|---|---|
| `tools.wall-item.material` | material | `BRICKS` | Material of the wall tool. Modern and legacy names both work.  |
| `tools.wall-item.name` | text (&-colours) | `&aBuilder Tool &7- &eWall` | Display name of the wall tool. Supports & colour codes.  |
| `tools.wall-item.lore` | list of text | 3 line(s) | Lore lines of the wall tool. Supports & colour codes and the placeholders listed in the section header.  |
| `tools.wall-item.glow` | true/false | `false` | Adds an enchantment glint to the wall tool.  |
| `tools.bridge-item.material` | material | `BRICKS` | Material of the bridge tool. Modern and legacy names both work.  |
| `tools.bridge-item.name` | text (&-colours) | `&aBuilder Tool &7- &eBridge` | Display name of the bridge tool. Supports & colour codes.  |
| `tools.bridge-item.lore` | list of text | 3 line(s) | Lore lines of the bridge tool. Supports & colour codes and the placeholders listed in the section header.  |
| `tools.bridge-item.glow` | true/false | `false` | Adds an enchantment glint to the bridge tool.  |
| `tools.build-sound` | sound | `ENTITY_CHICKEN_EGG` | Sound played for each row of wall / each bridge block. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |

### wool

| Option | Type | Default | Description |
|---|---|---|---|
| `wool.interval-ticks` | whole number | `20` | Ticks between free wool deliveries (20 = every second). Range 1 to 1200. |
| `wool.amount` | whole number | `1` | Wool per delivery. Range 1 to 64. |
| `wool.max-in-inventory` | whole number | `256` | Stop delivering once you carry this much wool. 0 = unlimited. Range 0 to 2304. |
| `wool.on-activate` | whole number | `0` | Wool handed out the moment the ultimate is applied. Range 0 to 576. |
| `wool.on-kill` | whole number | `16` | Wool received per kill. Range 0 to 576. |

### wall

| Option | Type | Default | Description |
|---|---|---|---|
| `wall.width` | whole number | `5` | Blocks wide. Range 1 to 15. |
| `wall.height` | whole number | `5` | Blocks tall. Range 1 to 15. |
| `wall.row-delay-ticks` | whole number | `2` | Ticks between rows (0 = fastest). Range 0 to 20. |
| `wall.top-face-only` | true/false | `true` | Walls can only be started by clicking the top of a block.  |
| `wall.cooldown-seconds` | number | `0.0` | Cooldown between walls. Range 0.0 to 600.0. |

### bridge

| Option | Type | Default | Description |
|---|---|---|---|
| `bridge.length` | whole number | `10` | Blocks long. Range 1 to 64. |
| `bridge.block-delay-ticks` | whole number | `2` | Ticks between blocks (0 = fastest). Range 0 to 20. |
| `bridge.stop-at-entities` | true/false | `true` | Stop the bridge when it would trap a player or mob.  |
| `bridge.cooldown-seconds` | number | `0.0` | Cooldown between bridges. Range 0.0 to 600.0. |

### bed-protection

| Option | Type | Default | Description |
|---|---|---|---|
| `bed-protection.enabled` | true/false | `true` | Right-clicking your own bed with a tool wraps it in wool.  |
| `bed-protection.cover-top` | true/false | `true` | Also cover the top of the bed.  |
| `bed-protection.max-distance` | number | `1.0` | How far the clicked block may be from your team's bed position. Range 0.0 to 8.0. |
| `bed-protection.cooldown-seconds` | number | `0.0` | Cooldown between bed protections. Range 0.0 to 600.0. |

### messages
_Messages sent by this ultimate_

| Option | Type | Default | Description |
|---|---|---|---|
| `messages.cant-place` | text (&-colours) | `&cYou can't place blocks here!` | Starting position is protected.  |
| `messages.cant-build-bridge` | text (&-colours) | `&cYou can't build a bridge here!` | Bridge ran into a protected area.  |
| `messages.cant-build-wall` | text (&-colours) | `&cYou can't build a wall here!` | Wall ran into a protected area.  |
| `messages.cant-protect-bed` | text (&-colours) | `&cYou can only protect your team's bed!` | Clicked someone else's bed.  |
| `messages.out-of-wool` | text (&-colours) | `&cYou have ran out of wool blocks!` | No wool left in the inventory.  |
| `messages.on-cooldown` | text (&-colours) | `&cThat tool is on cooldown for {seconds}s.` | Placeholder: {seconds}.  |

## `ultimates/demolition.yml`

Placeholders usable in this file's icon lore: `{status}` and `{cooldown}`, `{streak}`, `{depth}` (read live from the options below).

| `enabled` | true/false | `true` | Turn this ultimate on or off. A disabled ultimate disappears from the menu, and players holding it lose it on /bwu reload.  |
| `display-name` | text (&-colours) | `Demolition` | Name shown in menus and chat messages.  |

### icon
_Menu icon - lore placeholders: {status}, {cooldown}, {streak}, {depth}_

| Option | Type | Default | Description |
|---|---|---|---|
| `icon.material` | material | `FLINT_AND_STEEL` | Material of the menu icon. Modern and legacy names both work.  |
| `icon.name` | text (&-colours) | `&aDemolition` | Display name of the menu icon. Supports & colour codes.  |
| `icon.lore` | list of text | 10 line(s) | Lore lines of the menu icon. Supports & colour codes and the placeholders listed in the section header.  |
| `icon.glow` | true/false | `false` | Adds an enchantment glint to the menu icon.  |
| `icon.slot` | whole number | `15` | Inventory slot of the menu icon (0 = top-left). Range 0 to 53. |

### ignite

| Option | Type | Default | Description |
|---|---|---|---|
| `ignite.item.material` | material | `FLINT_AND_STEEL` | Material of the flint and steel. Modern and legacy names both work.  |
| `ignite.item.name` | text (&-colours) | `&aDemolition Ultimate` | Display name of the flint and steel. Supports & colour codes.  |
| `ignite.item.lore` | list of text | 2 line(s) | Lore lines of the flint and steel. Supports & colour codes and the placeholders listed in the section header.  |
| `ignite.item.glow` | true/false | `false` | Adds an enchantment glint to the flint and steel.  |
| `ignite.cooldown-seconds` | number | `10.0` | Cooldown after burning wool. Range 0.0 to 600.0. |
| `ignite.cooldown-on-miss` | true/false | `false` | Also start the cooldown when you click something that can't burn.  |
| `ignite.only-player-placed` | true/false | `true` | Only blocks placed by players during the game can burn (map blocks are safe).  |
| `ignite.burnable-suffixes` | list | 1 line(s) | Block types that burn, matched by the end of their name (WOOL covers every colour). Add e.g. CARPET or TERRACOTTA.  |
| `ignite.max-depth` | whole number | `20` | How many blocks away from the ignition point the fire can spread. Range 1 to 100. |
| `ignite.max-blocks` | whole number | `600` | Hard cap on blocks burned per ignition (protects server performance). Range 1 to 5000. |
| `ignite.spread-diagonally` | true/false | `false` | Fire also jumps across diagonal contacts.  |
| `ignite.burn-delay-min-ticks` | whole number | `10` | Shortest delay before a block disappears. Range 0 to 400. |
| `ignite.burn-delay-max-ticks` | whole number | `40` | Longest delay before a block disappears. Range 1 to 400. |
| `ignite.place-fire` | true/false | `true` | Place a visible fire block on top of burning wool.  |
| `ignite.sound` | sound | `FIRE_IGNITE` | Sound on ignition. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `ignite.particle` | particle | `FLAME` | Particle on each burning block. Blank disables it.  |
| `ignite.particle-count` | whole number | `3` | Particles per burning block. Range 1 to 30. |

### death-tnt

| Option | Type | Default | Description |
|---|---|---|---|
| `death-tnt.enabled` | true/false | `true` | Drop primed TNT where this player dies.  |
| `death-tnt.count` | whole number | `1` | How many TNT entities. Range 1 to 10. |
| `death-tnt.fuse-ticks` | whole number | `80` | Fuse length in ticks (80 = 4 seconds). Range 1 to 400. |
| `death-tnt.incendiary` | true/false | `false` | TNT sets blocks on fire.  |
| `death-tnt.team-immunity` | true/false | `true` | Teammates take no damage from this TNT.  |

### kill-streak

| Option | Type | Default | Description |
|---|---|---|---|
| `kill-streak.enabled` | true/false | `true` | Earn items for kill streaks.  |
| `kill-streak.kills-required` | whole number | `3` | Kills needed per reward. Range 1 to 50. |
| `kill-streak.rewards` | list of materials | 2 line(s) | Possible rewards (any material).  |
| `kill-streak.mode` | choice | `RANDOM` | RANDOM picks a reward each time; CYCLE goes through the list in order. One of: RANDOM, CYCLE. |
| `kill-streak.amount` | whole number | `1` | Items per reward. Range 1 to 64. |
| `kill-streak.reset-on-death` | true/false | `false` | Dying resets your progress.  |

### messages
_Messages sent by this ultimate_

| Option | Type | Default | Description |
|---|---|---|---|
| `messages.streak-reward` | text (&-colours) | _blank_ | Sent when a kill streak pays out. Placeholder: {item}. Blank = none.  |

### creeper

| Option | Type | Default | Description |
|---|---|---|---|
| `creeper.enabled` | true/false | `true` | Earn a creeper egg when you break a bed.  |
| `creeper.item.material` | material | `CREEPER_SPAWN_EGG` | Material of the creeper egg. Modern and legacy names both work.  |
| `creeper.item.name` | text (&-colours) | `&aCreeper Egg` | Display name of the creeper egg. Supports & colour codes.  |
| `creeper.item.lore` | list of text | 1 line(s) | Lore lines of the creeper egg. Supports & colour codes and the placeholders listed in the section header.  |
| `creeper.item.glow` | true/false | `false` | Adds an enchantment glint to the creeper egg.  |
| `creeper.eggs-per-bed` | whole number | `1` | Eggs received per bed. Range 1 to 16. |
| `creeper.powered` | true/false | `false` | Spawn charged creepers (bigger explosion).  |
| `creeper.team-immunity` | true/false | `true` | Teammates take no damage from this creeper.  |
| `creeper.despawn-after-seconds` | whole number | `0` | Remove the creeper after this long. 0 = never. Range 0 to 600. |

### messages
_Messages sent by this ultimate_

| Option | Type | Default | Description |
|---|---|---|---|
| `messages.cant-spawn-creeper` | text (&-colours) | `&cYou can't spawn a creeper here!` | Target spot is protected.  |

## `ultimates/gatherer.yml`

Placeholders usable in this file's icon lore: `{status}` and `{multiplier}`, `{materials}` (read live from the options below).

| `enabled` | true/false | `true` | Turn this ultimate on or off. A disabled ultimate disappears from the menu, and players holding it lose it on /bwu reload.  |
| `display-name` | text (&-colours) | `Gatherer` | Name shown in menus and chat messages.  |

### icon
_Menu icon - lore placeholders: {status}, {multiplier}, {materials}_

| Option | Type | Default | Description |
|---|---|---|---|
| `icon.material` | material | `ENDER_CHEST` | Material of the menu icon. Modern and legacy names both work.  |
| `icon.name` | text (&-colours) | `&aGatherer` | Display name of the menu icon. Supports & colour codes.  |
| `icon.lore` | list of text | 6 line(s) | Lore lines of the menu icon. Supports & colour codes and the placeholders listed in the section header.  |
| `icon.glow` | true/false | `false` | Adds an enchantment glint to the menu icon.  |
| `icon.slot` | whole number | `16` | Inventory slot of the menu icon (0 = top-left). Range 0 to 53. |

### duplicate

| Option | Type | Default | Description |
|---|---|---|---|
| `duplicate.enabled` | true/false | `true` | Duplicate resources picked up from generators.  |
| `duplicate.materials` | list of materials | 2 line(s) | Resources that get duplicated. Add IRON_INGOT / GOLD_INGOT to duplicate those too.  |
| `duplicate.multiplier` | whole number | `2` | Total amount you end up with: 2 = double, 3 = triple. Range 1 to 10. |
| `duplicate.chance-percent` | whole number | `100` | Chance (0-100) that a pickup is duplicated. Range 0 to 100. |
| `duplicate.sound` | sound | _blank_ | Sound when a pickup is duplicated. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `duplicate.particle` | particle | _blank_ | Particle when a pickup is duplicated. Blank disables it.  |
| `duplicate.drop-if-full` | true/false | `true` | Drop the bonus at your feet when your inventory is full (otherwise it is lost).  |

### ender-chest

| Option | Type | Default | Description |
|---|---|---|---|
| `ender-chest.enabled` | true/false | `true` | Give the portable ender chest.  |
| `ender-chest.item.material` | material | `ENDER_CHEST` | Material of the ender chest item. Modern and legacy names both work.  |
| `ender-chest.item.name` | text (&-colours) | `&aPortable Ender Chest` | Display name of the ender chest item. Supports & colour codes.  |
| `ender-chest.item.lore` | list of text | 1 line(s) | Lore lines of the ender chest item. Supports & colour codes and the placeholders listed in the section header.  |
| `ender-chest.item.glow` | true/false | `false` | Adds an enchantment glint to the ender chest item.  |
| `ender-chest.virtual` | true/false | `true` | true = a private chest that only lives for this game (recommended). false = the player's real, persistent ender chest.  |
| `ender-chest.rows` | whole number | `3` | Rows of the virtual chest. Range 1 to 6. |
| `ender-chest.title` | text (&-colours) | `&5Ender Chest` | Title of the virtual chest.  |
| `ender-chest.open-sound` | sound | _blank_ | Sound when opening. Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch.  |
| `ender-chest.keep-on-death` | true/false | `true` | Chest contents survive death. If false they are dropped.  |

