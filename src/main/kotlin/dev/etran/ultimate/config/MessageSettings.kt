package dev.etran.ultimate.config

/** Options of `messages.yml`. Ultimate-specific messages live in each ultimate's own file. */
class MessageSettings {

    val specs = Specs()

    init {
        specs.section("general", "COMMON")
    }

    val prefix = specs.text("general.prefix", "&8[&6Ultimate&8] &r", "Put in front of command feedback. Chat lines for players in-game don't use it.")
    val noPermission = specs.text("general.no-permission", "&cYou don't have permission to do that.", "Shown when a command is denied.")
    val playersOnly = specs.text("general.players-only", "&cOnly players can use this.", "Shown when the console runs a player-only command.")
    val unknownCommand = specs.text("general.unknown-command", "&cUnknown subcommand. Use &e/bwu help&c.", "Shown for an unknown subcommand.")
    val unknownUltimate = specs.text("general.unknown-ultimate", "&cThere is no ultimate called &e{id}&c. Available: &f{list}", "Placeholders: {id}, {list}.")
    val unknownPlayer = specs.text("general.unknown-player", "&cPlayer &e{name}&c is not online.", "Placeholder: {name}.")

    init {
        specs.section("selection", "PICKING ULTIMATES")
    }

    val selected = specs.text("selection.selected", "&aSwitched ultimate to &6{ultimate}&a!", "Placeholder: {ultimate}.")
    val selectedPending = specs.text("selection.selected-pending", "&7Selected &6{ultimate}&7. It activates when ultimates switch on.", "Shown when picking before ultimates are enabled in the arena.")
    val alreadySelected = specs.text("selection.already-selected", "&cYou already have this ultimate selected!", "Shown when re-picking the current ultimate.")
    val disabled = specs.text("selection.disabled", "&cThe {ultimate} ultimate is disabled.", "Placeholder: {ultimate}.")
    val notInGame = specs.text("selection.not-in-game", "&cYou must be in a BedWars game to pick an ultimate.", "Shown outside of games.")
    val notUltimateArena = specs.text("selection.not-ultimate-arena", "&cThis arena doesn't play Ultimate mode.", "Shown when picking in a normal arena.")
    val switchCooldown = specs.text("selection.switch-cooldown", "&cYou must wait {seconds} seconds before switching ultimate!", "Placeholder: {seconds}.")
    val switchingDisabled = specs.text("selection.switching-disabled", "&cYou can't change your ultimate once you picked one.", "Shown when selection.allow-switching is false.")
    val noUltimatePermission = specs.text("selection.no-ultimate-permission", "&cYou don't have permission to use the {ultimate} ultimate.", "Placeholder: {ultimate}.")
    val cantWhileRespawning = specs.text("selection.cant-while-respawning", "&cYou can't change your ultimate while respawning.", "Shown when selection.allow-while-respawning is false.")
    val defaultAssigned = specs.text("selection.default-assigned", "&7You were given the &6{ultimate}&7 ultimate. Open the shop to change it.", "Shown when arenas.default-ultimate gives a player an ultimate.")

    init {
        specs.section("activation", "ULTIMATES SWITCHING ON")
    }

    val ultimatesEnabled = specs.textList(
        "activation.ultimates-enabled",
        listOf(
            "&a▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
            "&f&l                       BedWars Ultimate",
            "",
            "&e&l                 Ultimates have been enabled!",
            "",
            "&a▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        ),
        "Lines sent to every player in the arena when ultimates switch on."
    )

    init {
        specs.section("reload", "RELOAD AND DIAGNOSTICS")
    }

    val reloadOk = specs.text("reload.success", "&aReloaded all configuration files &7({count} files, {players} players refreshed).", "Placeholders: {count}, {players}.")
    val reloadWarnings = specs.text("reload.warnings-header", "&eReloaded with {count} warning(s):", "Placeholder: {count}.")
    val reloadErrors = specs.text("reload.errors-header", "&cReload finished with {count} error(s) - the affected files kept their previous values:", "Placeholder: {count}.")
    val reloadLine = specs.text("reload.line", "&7 - &f{text}", "One line per problem. Placeholder: {text}.")

    init {
        specs.section("config", "IN-GAME CONFIG EDITOR")
    }

    val configSet = specs.text("config.set", "&aSet &e{path} &ain &f{file} &ato &f{value}&a.", "Placeholders: {file}, {path}, {value}.")
    val configReset = specs.text("config.reset", "&aReset &e{path} &ain &f{file} &ato its default (&f{value}&a).", "Placeholders: {file}, {path}, {value}.")
    val configGet = specs.text("config.get", "&e{path} &7= &f{value} &8(default: {default})", "Placeholders: {path}, {value}, {default}.")
    val configInvalid = specs.text("config.invalid", "&cInvalid value for &e{path}&c: {reason}", "Placeholders: {path}, {reason}.")
    val configUnknownFile = specs.text("config.unknown-file", "&cUnknown file &e{id}&c. Choose from: &f{list}", "Placeholders: {id}, {list}.")
    val configUnknownOption = specs.text("config.unknown-option", "&cUnknown option &e{path}&c in &f{file}&c. Use &e/bwu config list {file}", "Placeholders: {file}, {path}.")
    val configWriteFailed = specs.text("config.write-failed", "&cCould not save the change: {reason}", "Placeholder: {reason}.")
    val configAddedMissing = specs.text("config.added-missing", "&aAdded {count} missing option(s) to the files.", "Placeholder: {count}.")
    val promptValue = specs.text("config.prompt-value", "&6Type the new value for &e{path} &6in chat &7(or &ccancel&7). Current: &f{value}", "Placeholders: {path}, {value}.")
    val promptList = specs.text(
        "config.prompt-list",
        "&6Editing list &e{path}&6. Type: &eadd <text>&6, &eset <n> <text>&6, &eremove <n>&6, &eclear&6, &edone &6or &ecancel&6.",
        "Placeholder: {path}."
    )
    val promptCancelled = specs.text("config.prompt-cancelled", "&7Cancelled.", "Shown when an edit prompt is cancelled.")
    val promptListLine = specs.text("config.prompt-list-line", "&7{index}. &f{text}", "One line of the list preview. Placeholders: {index}, {text}.")
}
