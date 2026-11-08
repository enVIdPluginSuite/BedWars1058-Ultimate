package dev.etran.ultimate.config

/** Options of `config.yml`. */
class GeneralSettings {

    val specs = Specs()

    // ---- arenas -------------------------------------------------------------------------------------------------
    init {
        specs.section("arenas", "WHICH ARENAS RUN ULTIMATE MODE")
    }

    val arenaGroups = specs.stringList(
        "arenas.groups", listOf("ultimate"),
        "BedWars1058 arena groups that play Ultimate mode. An arena whose group is listed here gets ultimates."
    )
    val arenaNames = specs.stringList(
        "arenas.names", emptyList(),
        "Individual arena names that play Ultimate mode, in addition to the groups above."
    )
    val allArenas = specs.bool(
        "arenas.all", false,
        "Run Ultimate mode in EVERY BedWars1058 arena, ignoring groups and names."
    )
    val activationDelay = specs.int(
        "arenas.activation-delay-seconds", 10,
        "Seconds after the game starts before ultimates are switched on. 0 = immediately.", min = 0, max = 3600
    )
    val rememberSelection = specs.bool(
        "arenas.remember-selection", false,
        "Keep each player's chosen ultimate for their next game (until they leave the server)."
    )
    val defaultUltimate = specs.string(
        "arenas.default-ultimate", "NONE",
        "Ultimate given to players who haven't picked one when ultimates switch on. NONE, RANDOM, or an ultimate id (kangaroo, swordsman, healer, frozo, builder, demolition, gatherer)."
    )

    // ---- selection ----------------------------------------------------------------------------------------------
    init {
        specs.section("selection", "PICKING AND SWITCHING ULTIMATES")
    }

    val allowSwitching = specs.bool("selection.allow-switching", true, "Allow changing ultimate after the first pick.")
    val switchCooldown = specs.int(
        "selection.switch-cooldown-seconds", 20,
        "Seconds a player must wait between picks. 0 disables the cooldown.", min = 0, max = 3600
    )
    val firstPickFree = specs.bool(
        "selection.first-pick-free", false,
        "If true, a player's very first pick doesn't start the switch cooldown."
    )
    val allowWhileRespawning = specs.bool(
        "selection.allow-while-respawning", true,
        "Allow picking/switching while waiting to respawn."
    )
    val permissionPerUltimate = specs.bool(
        "selection.permission-per-ultimate", false,
        "Require the permission etranultimate.use.<id> (for example etranultimate.use.healer) to pick each ultimate."
    )
    val selectSound = specs.sound("selection.select-sound", "NOTE_PLING", "Sound when an ultimate is picked.")
    val deniedSound = specs.sound("selection.denied-sound", "VILLAGER_NO", "Sound when a pick is refused.")

    // ---- activation ---------------------------------------------------------------------------------------------
    init {
        specs.section("activation", "WHAT PLAYERS SEE WHEN ULTIMATES SWITCH ON")
    }

    val activationSound = specs.sound("activation.sound", "ENTITY_PLAYER_LEVELUP", "Sound played to the arena when ultimates switch on.")
    val activationChat = specs.bool("activation.chat-message", true, "Send the 'ultimates-enabled' lines from messages.yml.")
    val titleEnabled = specs.bool("activation.title.enabled", false, "Show a title on screen when ultimates switch on.")
    val titleText = specs.text("activation.title.title", "&6&lULTIMATES", "Main title text.")
    val titleSubtitle = specs.text("activation.title.subtitle", "&eOpen the shop to pick one!", "Subtitle text.")
    val titleFadeIn = specs.int("activation.title.fade-in-ticks", 10, "Title fade-in time in ticks (20 ticks = 1 second).", min = 0, max = 400)
    val titleStay = specs.int("activation.title.stay-ticks", 60, "Title stay time in ticks.", min = 0, max = 1200)
    val titleFadeOut = specs.int("activation.title.fade-out-ticks", 10, "Title fade-out time in ticks.", min = 0, max = 400)

    // ---- fall damage --------------------------------------------------------------------------------------------
    init {
        specs.section("fall-damage", "FALL DAMAGE IN ULTIMATE ARENAS")
    }

    val fallDisable = specs.bool("fall-damage.disable", true, "Cancel fall damage in ultimate arenas (the Hypixel mode has none).")
    val fallOnlyUltimatePlayers = specs.bool(
        "fall-damage.only-ultimate-players", false,
        "If true, fall damage is only removed for players who have an ultimate selected."
    )
    val fallMultiplier = specs.double(
        "fall-damage.multiplier", 1.0,
        "Fall damage multiplier used when 'disable' is false (0.5 = half damage).", min = 0.0, max = 10.0, step = 0.1
    )

    // ---- HUD ----------------------------------------------------------------------------------------------------
    init {
        specs.section("hud", "COOLDOWN DISPLAY")
    }

    val hudXpBar = specs.bool("hud.xp-bar", true, "Show the active cooldown on the XP bar (level = seconds left).")
    val hudActionBar = specs.bool("hud.action-bar", false, "Also show the cooldown in the action bar.")
    val hudActionBarFormat = specs.text("hud.action-bar-format", "&e{name} &7- &c{seconds}s", "Action bar text. Placeholders: {name}, {seconds}.")
    val hudUpdateTicks = specs.int("hud.update-ticks", 1, "How often the HUD refreshes, in ticks. Higher = lighter.", min = 1, max = 20)

    // ---- shop button --------------------------------------------------------------------------------------------
    init {
        specs.section("shop-button", "THE 'ULTIMATES' BUTTON ADDED TO THE BEDWARS ITEM SHOP")
    }

    val shopButtonEnabled = specs.bool("shop-button.enabled", true, "Add the Ultimates button to BedWars1058's item shop inventories.")
    val shopButton = ItemOpts(
        specs, "shop-button", "BEACON", "&aUltimates", listOf("&eClick to view!"),
        defSlot = 8, what = "shop button"
    )

    // ---- selector item ------------------------------------------------------------------------------------------
    init {
        specs.section("selector-item", "OPTIONAL HOTBAR ITEM THAT OPENS THE ULTIMATE MENU")
    }

    val selectorEnabled = specs.bool("selector-item.enabled", false, "Give players an item that opens the ultimate menu without visiting the shop.")
    val selectorGiveOnActivation = specs.bool("selector-item.give-on-activation", true, "Give the item when ultimates switch on (and after each respawn).")
    val selector = ItemOpts(
        specs, "selector-item", "NETHER_STAR", "&6&lUltimate Selector",
        listOf("&7Right-click to choose your ultimate."), defGlow = true, defSlot = 4, what = "selector item"
    )

    // ---- menu ---------------------------------------------------------------------------------------------------
    init {
        specs.section("menu", "THE ULTIMATE SELECTION MENU")
    }

    val menuTitle = specs.text("menu.title", "&8Ultimates", "Inventory title (long titles are cut to 32 characters on older servers).")
    val menuRows = specs.int("menu.rows", 3, "Number of rows (1-6). Make sure every ultimate's icon slot fits.", min = 1, max = 6)
    val menuFiller = specs.bool("menu.filler.enabled", true, "Fill empty slots with a pane.")
    val menuFillerItem = ItemOpts(specs, "menu.filler", "GRAY_STAINED_GLASS_PANE", " ", what = "filler pane")
    val menuStatusSelected = specs.text("menu.status-selected", "&cCurrently selected!", "Replaces {status} in an ultimate's lore when it is the player's current pick.")
    val menuStatusUnselected = specs.text("menu.status-unselected", "&eClick to select!", "Replaces {status} in an ultimate's lore otherwise.")
    val menuStatusCooldown = specs.text("menu.status-cooldown", "&cSwitch cooldown: {seconds}s", "Replaces {status} while the player is still on their switch cooldown.")
    val menuBackEnabled = specs.bool("menu.back-button.enabled", true, "Show a button that returns to the BedWars item shop.")
    val menuBack = ItemOpts(specs, "menu.back-button", "ARROW", "&aBack to Shop", listOf("&7Return to the item shop."), defSlot = 22, what = "back button")
    val menuOpenSound = specs.sound("menu.open-sound", "", "Sound when the menu opens.")
    val menuCloseOnSelect = specs.bool("menu.close-on-select", false, "Close the menu after a successful pick (otherwise it stays open and refreshes).")

    // ---- protection ---------------------------------------------------------------------------------------------
    init {
        specs.section("protection", "KEEPING ULTIMATE ITEMS SAFE")
    }

    val preventDrop = specs.bool("protection.prevent-dropping", true, "Players cannot drop ultimate items.")
    val preventMoving = specs.bool("protection.prevent-moving", true, "Ultimate items cannot be moved into chests, the shop or other containers.")
    val removeOnDeath = specs.bool("protection.remove-on-death", true, "Ultimate items never appear in death drops.")

    // ---- compat -------------------------------------------------------------------------------------------------
    init {
        specs.section("compat", "VERSION COMPATIBILITY TUNING (normally leave alone)")
    }

    val blockingWindowMs = specs.int(
        "compat.blocking-window-ms", 275,
        "On 1.9+ swords cannot really block, so 'holding right-click' is detected from repeated interact events. " +
            "A hold counts as active for this many milliseconds after the last event. Raise it if holding feels laggy.",
        min = 100, max = 1000, step = 25
    )
    val blockingItemSuffixes = specs.stringList(
        "compat.blocking-item-suffixes", listOf("SWORD"),
        "Item types (matched by name ending) that count as 'blocking' when right-click is held. SWORD matches every sword tier."
    )

    // ---- reload -------------------------------------------------------------------------------------------------
    init {
        specs.section("reload", "WHAT /bwu reload DOES TO GAMES THAT ARE ALREADY RUNNING")
    }

    val reloadRefreshPlayers = specs.bool("reload.refresh-active-players", true, "Re-give ultimate items (new names, lore, materials) to players in running ultimate games.")
    val reloadActivateRunning = specs.bool("reload.activate-running-arenas", true, "Switch ultimates on in running arenas that newly qualify (e.g. after adding a group).")
    val reloadDeactivateRemoved = specs.bool("reload.deactivate-removed-arenas", true, "Switch ultimates off in running arenas that no longer qualify.")

    // ---- misc ---------------------------------------------------------------------------------------------------
    init {
        specs.section("debug", "DEBUGGING")
    }

    val debug = specs.bool("debug", false, "Log extra information to the console.")
}
