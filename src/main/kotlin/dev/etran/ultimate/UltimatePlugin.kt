package dev.etran.ultimate

import com.andrei1058.bedwars.api.BedWars
import dev.etran.ultimate.command.UltimateCommand
import dev.etran.ultimate.compat.McVersion
import dev.etran.ultimate.config.ConfigManager
import dev.etran.ultimate.config.ConfigReport
import dev.etran.ultimate.core.ArenaTracker
import dev.etran.ultimate.core.BlockingTracker
import dev.etran.ultimate.core.Cooldowns
import dev.etran.ultimate.core.Text
import dev.etran.ultimate.core.UltimateItems
import dev.etran.ultimate.gui.ChatPrompts
import dev.etran.ultimate.gui.MenuListener
import dev.etran.ultimate.listener.ArenaListener
import dev.etran.ultimate.listener.ShopListener
import dev.etran.ultimate.ultimate.Builder
import dev.etran.ultimate.ultimate.Demolition
import dev.etran.ultimate.ultimate.Frozo
import dev.etran.ultimate.ultimate.Gatherer
import dev.etran.ultimate.ultimate.Healer
import dev.etran.ultimate.ultimate.Kangaroo
import dev.etran.ultimate.ultimate.Swordsman
import dev.etran.ultimate.ultimate.UltimateManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class ReloadResult(val report: dev.etran.ultimate.config.ConfigReport, val playersRefreshed: Int)

class UltimatePlugin : JavaPlugin() {

    companion object {
        lateinit var instance: UltimatePlugin
            private set
    }

    lateinit var bedWars: BedWars
        private set
    lateinit var configs: dev.etran.ultimate.config.ConfigManager
        private set
    lateinit var cooldowns: dev.etran.ultimate.core.Cooldowns
        private set
    lateinit var manager: dev.etran.ultimate.ultimate.UltimateManager
        private set
    lateinit var arenas: dev.etran.ultimate.core.ArenaTracker
        private set
    lateinit var blocking: dev.etran.ultimate.core.BlockingTracker
        private set
    lateinit var prompts: dev.etran.ultimate.gui.ChatPrompts
        private set

    private val fallImmunity = HashMap<UUID, Long>()
    private var tickTask: BukkitTask? = null
    private var tickErrors = 0

    override fun onEnable() {
        instance = this

        val registration = server.servicesManager.getRegistration(BedWars::class.java)
        if (registration == null) {
            logger.severe("BedWars1058 is installed but its API is not available. Disabling.")
            server.pluginManager.disablePlugin(this)
            return
        }
        bedWars = registration.provider

        configs = _root_ide_package_.dev.etran.ultimate.config.ConfigManager(this)
        cooldowns = _root_ide_package_.dev.etran.ultimate.core.Cooldowns(this)
        manager = _root_ide_package_.dev.etran.ultimate.ultimate.UltimateManager(this)
        arenas = _root_ide_package_.dev.etran.ultimate.core.ArenaTracker(this)
        blocking = _root_ide_package_.dev.etran.ultimate.core.BlockingTracker(this)
        prompts = _root_ide_package_.dev.etran.ultimate.gui.ChatPrompts(this)

        manager.register(_root_ide_package_.dev.etran.ultimate.ultimate.Kangaroo(this))
        manager.register(_root_ide_package_.dev.etran.ultimate.ultimate.Swordsman(this))
        manager.register(_root_ide_package_.dev.etran.ultimate.ultimate.Healer(this))
        manager.register(_root_ide_package_.dev.etran.ultimate.ultimate.Frozo(this))
        manager.register(_root_ide_package_.dev.etran.ultimate.ultimate.Builder(this))
        manager.register(_root_ide_package_.dev.etran.ultimate.ultimate.Demolition(this))
        manager.register(_root_ide_package_.dev.etran.ultimate.ultimate.Gatherer(this))

        val report = configs.loadAll()
        logReport(report)

        val pm = server.pluginManager
        pm.registerEvents(blocking, this)
        pm.registerEvents(prompts, this)
        pm.registerEvents(_root_ide_package_.dev.etran.ultimate.gui.MenuListener(this), this)
        pm.registerEvents(_root_ide_package_.dev.etran.ultimate.listener.ArenaListener(this), this)
        pm.registerEvents(_root_ide_package_.dev.etran.ultimate.listener.ShopListener(this), this)

        val command = _root_ide_package_.dev.etran.ultimate.command.UltimateCommand(this)
        getCommand("bwultimate")?.let {
            it.executor = command
            it.tabCompleter = command
        }

        tickTask = server.scheduler.runTaskTimer(this, Runnable { tick() }, 1L, 1L)

        // Games that were already running when the plugin loaded (e.g. via a plugin manager) should not be left out.
        arenas.syncWithConfig()

        val bw = server.pluginManager.getPlugin("BedWars1058")?.description?.version ?: "?"
        logger.info("Enabled on Minecraft ${McVersion.current} (${server.bukkitVersion}) with BedWars1058 $bw; ${manager.all().size} ultimates loaded.")
    }

    override fun onDisable() {
        tickTask?.cancel()
        server.scheduler.cancelTasks(this)
        if (::arenas.isInitialized) arenas.shutdown()
        if (::manager.isInitialized) manager.shutdown()
        if (::cooldowns.isInitialized) cooldowns.clearAll()
    }

    private fun tick() {
        try {
            cooldowns.tick()
        } catch (t: Throwable) {
            tickFailure("cooldowns", t)
        }
        for (ultimate in manager.all()) {
            try {
                ultimate.tick()
            } catch (t: Throwable) {
                tickFailure(ultimate.id, t)
            }
        }
        if (fallImmunity.isNotEmpty()) {
            val now = System.currentTimeMillis()
            fallImmunity.values.removeAll { it < now }
        }
    }

    /** One misbehaving ultimate must not take the others down; log the first few failures only. */
    private fun tickFailure(what: String, t: Throwable) {
        if (tickErrors++ < 5) {
            logger.warning("Error while ticking $what: $t")
            if (configs.general.debug.get()) t.printStackTrace()
        }
    }

    // ---- reload -------------------------------------------------------------------------------------------------

    fun reloadAll(): ReloadResult {
        val report = configs.loadAll()
        tickErrors = 0
        for (ultimate in manager.all()) ultimate.onReload()
        arenas.syncWithConfig()
        val refreshed = if (configs.general.reloadRefreshPlayers.get()) manager.refreshAll() else 0
        logReport(report)
        return ReloadResult(report, refreshed)
    }

    /** Tells [to] how a reload went (errors, warnings, how many players were refreshed). */
    fun sendReloadSummary(to: CommandSender, result: ReloadResult) {
        val msgs = configs.messages
        val report = result.report
        if (report.errors.isNotEmpty()) {
            reply(to, msgs.reloadErrors.get(), "count" to report.errors.size.toString())
            for (e in report.errors) tell(to, msgs.reloadLine.get(), "text" to e)
        }
        if (report.warnings.isNotEmpty()) {
            reply(to, msgs.reloadWarnings.get(), "count" to report.warnings.size.toString())
            for (w in report.warnings.take(15)) tell(to, msgs.reloadLine.get(), "text" to w)
            if (report.warnings.size > 15) tell(to, msgs.reloadLine.get(), "text" to "... and ${report.warnings.size - 15} more (see console)")
        }
        if (report.errors.isEmpty() && report.warnings.isEmpty()) {
            reply(to, msgs.reloadOk.get(), "count" to report.files.toString(), "players" to result.playersRefreshed.toString())
        }
    }

    private fun logReport(report: ConfigReport) {
        for (error in report.errors) logger.severe(error)
        for (warning in report.warnings) logger.warning(warning)
    }

    // ---- messaging ----------------------------------------------------------------------------------------------

    /** Sends a configurable message (no prefix). Blank text sends nothing. `{key}` placeholders are replaced. */
    fun tell(to: CommandSender, text: String, vararg placeholders: Pair<String, String>) {
        if (text.isBlank()) return
        Text.send(to, Text.apply(text, placeholders.toMap()))
    }

    /** Like [tell] but with the configured chat prefix; used for command feedback. */
    fun reply(to: CommandSender, text: String, vararg placeholders: Pair<String, String>) {
        if (text.isBlank()) return
        Text.send(to, configs.messages.prefix.get() + Text.apply(text, placeholders.toMap()))
    }

    fun actionBar(player: Player, text: String) {
        try {
            bedWars.versionSupport.playAction(player, text)
        } catch (ignored: Throwable) {
        }
    }

    fun title(player: Player, title: String, subtitle: String, fadeIn: Int, stay: Int, fadeOut: Int) {
        try {
            bedWars.versionSupport.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut)
        } catch (ignored: Throwable) {
        }
    }

    fun debug(message: String) {
        if (::configs.isInitialized && configs.general.debug.get()) logger.info("[debug] $message")
    }

    // ---- helpers shared by ultimates -----------------------------------------------------------------------------

    fun grantFallImmunity(player: Player, seconds: Double) {
        fallImmunity[player.uniqueId] = System.currentTimeMillis() + (seconds * 1000).toLong()
    }

    fun isFallImmune(uuid: UUID): Boolean = (fallImmunity[uuid] ?: 0L) > System.currentTimeMillis()

    /** Repeating task that can cancel itself from inside its body. */
    fun runTimer(delay: Long, period: Long, body: (BukkitTask) -> Unit): BukkitTask {
        var holder: BukkitTask? = null
        val task = Bukkit.getScheduler().runTaskTimer(this, Runnable { holder?.let(body) }, delay, period)
        holder = task
        return task
    }

    // ---- selector item ------------------------------------------------------------------------------------------

    fun removeSelectorItem(player: Player) {
        val inv = player.inventory
        for (slot in 0 until inv.size) {
            val item = inv.getItem(slot) ?: continue
            if (item.type != Material.AIR && UltimateItems.tagOf(item) == UltimateItems.SELECTOR) inv.setItem(slot, null)
        }
    }

    fun giveSelectorItem(player: Player) {
        removeSelectorItem(player)
        val general = configs.general
        if (!general.selectorEnabled.get() || !general.selectorGiveOnActivation.get()) return
        val arena = arenas.activeArenaOf(player) ?: return
        if (arena.isSpectator(player)) return

        val item = general.selector.build(tag = UltimateItems.SELECTOR)
        val slot = general.selector.slot!!.get()
        val inv = player.inventory
        val current = inv.getItem(slot)
        if (current == null || current.type == Material.AIR) {
            inv.setItem(slot, item)
        } else {
            for (left in inv.addItem(item).values) player.world.dropItemNaturally(player.location, left)
        }
    }
}
