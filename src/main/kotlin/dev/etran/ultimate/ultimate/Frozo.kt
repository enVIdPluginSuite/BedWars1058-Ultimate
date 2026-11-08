package dev.etran.ultimate.ultimate

import com.andrei1058.bedwars.api.arena.IArena
import dev.bw1058.ultimate.UltimatePlugin
import dev.bw1058.ultimate.compat.Effects
import dev.bw1058.ultimate.compat.Particles
import dev.bw1058.ultimate.compat.Sounds
import dev.bw1058.ultimate.config.ItemOpts
import dev.bw1058.ultimate.core.ItemFactory
import dev.bw1058.ultimate.core.PotionTools
import dev.bw1058.ultimate.core.Text
import dev.bw1058.ultimate.core.UltimateItems
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.entity.ThrownPotion
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.meta.PotionMeta

/** Frozo: a throwable potion that slows enemies (and speeds allies), and snowballs on kills. */
class Frozo(plugin: UltimatePlugin) : Ultimate(
    plugin, "frozo", "Frozo", "SNOW_BLOCK", 13,
    listOf(
        "&7• Throw down potion to slow",
        "&7down enemies!",
        "&7• {snowballs} Snowballs on kill! ({max-snowballs} max).",
        "",
        "&7Cooldown: &a{cooldown} Seconds",
        "",
        "{status}"
    ),
    ", {cooldown}, {snowballs}, {max-snowballs}, {radius}"
) {

    // ---- potion ---------------------------------------------------------------------------------------------
    private val potionItem = ItemOpts(
        specs, "potion.item", "SPLASH_POTION", "&aFrozo Potion",
        listOf("&7Throw to slow down enemies."), what = "frozo potion", withMaterial = false
    )
    private val cooldown = specs.double("potion.cooldown-seconds", 20.0, "Seconds until the potion is returned after being thrown.", min = 0.0, max = 600.0, step = 1.0)
    private val giveBack = specs.bool("potion.give-back", true, "Return the potion after the cooldown. If false it is a one-time item per life.")
    private val useVanillaRadius = specs.bool("potion.use-vanilla-radius", false, "Use the game's own splash area instead of 'radius' below.")
    private val radius = specs.double("potion.radius", 4.0, "Splash radius in blocks (sphere).", min = 0.5, max = 30.0, step = 0.5)
    private val splashParticle = specs.particle("potion.splash-particle", "SNOWBALL", "Particle burst where the potion lands.")
    private val splashParticleCount = specs.int("potion.splash-particle-count", 60, "Number of particles.", min = 1, max = 500, step = 10)
    private val splashSound = specs.sound("potion.splash-sound", "GLASS", "Sound where the potion lands.")

    // ---- enemy effect ---------------------------------------------------------------------------------------
    private val enemyEnabled = specs.bool("enemy-effect.enabled", true, "Apply an effect to enemies hit by the splash.")
    private val enemyEffect = specs.effect("enemy-effect.type", "SLOW", "Potion effect given to enemies.")
    private val enemyAmplifier = specs.int("enemy-effect.amplifier", 1, "Effect level minus one (1 = Slowness II).", min = 0, max = 10)
    private val enemySeconds = specs.double("enemy-effect.duration-seconds", 60.0, "Effect duration.", min = 0.5, max = 1200.0, step = 1.0)

    // ---- ally effect ----------------------------------------------------------------------------------------
    private val allyEnabled = specs.bool("ally-effect.enabled", true, "Apply an effect to teammates hit by the splash.")
    private val allyEffect = specs.effect("ally-effect.type", "SPEED", "Potion effect given to teammates.")
    private val allyAmplifier = specs.int("ally-effect.amplifier", 5, "Effect level minus one (5 = Speed VI).", min = 0, max = 10)
    private val allySeconds = specs.double("ally-effect.duration-seconds", 30.0, "Effect duration.", min = 0.5, max = 1200.0, step = 1.0)
    private val allyIncludeSelf = specs.bool("ally-effect.include-thrower", true, "The thrower counts as a teammate.")

    // ---- kill reward ----------------------------------------------------------------------------------------
    private val snowballsEnabled = specs.bool("on-kill.snowballs-enabled", true, "Receive snowballs on a kill.")
    private val snowballsPerKill = specs.int("on-kill.snowballs", 2, "Snowballs received per kill.", min = 1, max = 64)
    private val maxSnowballs = specs.int("on-kill.max-snowballs", 16, "Never carry more than this many Frozo snowballs.", min = 1, max = 64, step = 4)
    private val snowballName = specs.text("on-kill.snowball-name", "&bFrozo Snowball", "Name of the snowball item.")

    private val snowSlow = specs.bool("snowball-hit.enabled", false, "Frozo snowballs also slow enemies they hit (off = plain snowballs, like the original).")
    private val snowSlowEffect = specs.effect("snowball-hit.effect", "SLOW", "Effect applied on a snowball hit.")
    private val snowSlowAmplifier = specs.int("snowball-hit.amplifier", 0, "Effect level minus one.", min = 0, max = 10)
    private val snowSlowSeconds = specs.double("snowball-hit.duration-seconds", 3.0, "Effect duration.", min = 0.5, max = 120.0, step = 0.5)

    // ---- messages -------------------------------------------------------------------------------------------
    private val msgPotionReady = specs.text("messages.potion-ready", "", "Sent when the potion comes back. Blank = no message.")

    private companion object {
        const val POTION = "frozo:potion"
        const val KIND = "potion"
        const val SNOWBALL = "snowball"
    }

    override fun placeholders(): Map<String, String> = mapOf(
        "cooldown" to Text.seconds(cooldown.get()),
        "snowballs" to snowballsPerKill.get().toString(),
        "max-snowballs" to maxSnowballs.get().toString(),
        "radius" to Text.seconds(radius.get())
    )

    private fun effectForDisplay() = Effects.create(enemyEffect.get(), 200, enemyAmplifier.get())

    private fun makePotion() = potionItem.buildOn(
        ItemFactory.splashPotionBase(effectForDisplay()), tag = UltimateItems.tagFor(id, KIND)
    ) { meta -> effectForDisplay()?.let { (meta as? PotionMeta)?.addCustomEffect(it, true) } }

    override fun onActivate(player: Player, arena: IArena) {
        player.inventory.addItem(makePotion())
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onLaunch(event: ProjectileLaunchEvent) {
        val potion = event.entity as? ThrownPotion ?: return
        val player = potion.shooter as? Player ?: return
        if (ctx(player) == null) return
        if (PotionTools.heldTag(player) != UltimateItems.tagFor(id, KIND)) return

        PotionTools.mark(plugin, potion, UltimateItems.tagFor(id, KIND))
        if (giveBack.get()) {
            plugin.cooldowns.start(player, POTION, cooldown.get(), "Frozo Potion") {
                Bukkit.getScheduler().runTaskLater(plugin, Runnable { returnPotion(player) }, 1L)
            }
        }
    }

    private fun returnPotion(player: Player) {
        if (!player.isOnline || ctx(player) == null) return
        if (UltimateItems.holds(player, id, KIND)) return
        for (left in player.inventory.addItem(makePotion()).values) player.world.dropItemNaturally(player.location, left)
        plugin.tell(player, msgPotionReady.get())
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onSplash(event: PotionSplashEvent) {
        val potion = event.potion
        if (PotionTools.markOf(potion) != UltimateItems.tagFor(id, KIND)) return
        event.isCancelled = true

        val thrower = potion.shooter as? Player ?: return
        val c = ctx(thrower) ?: return
        val team = c.team
        val center = potion.location

        val targets: List<Player> = if (useVanillaRadius.get()) {
            event.affectedEntities.filterIsInstance<Player>()
        } else {
            val r2 = radius.get() * radius.get()
            c.arena.players.filter { it.world == center.world && it.location.distanceSquared(center) <= r2 }
        }

        val enemy = if (enemyEnabled.get()) Effects.create(enemyEffect.get(), (enemySeconds.get() * 20).toInt(), enemyAmplifier.get()) else null
        val ally = if (allyEnabled.get()) Effects.create(allyEffect.get(), (allySeconds.get() * 20).toInt(), allyAmplifier.get()) else null

        for (target in targets) {
            if (c.arena.isSpectator(target) || c.arena.isReSpawning(target)) continue
            val isAlly = team != null && team.isMember(target)
            if (isAlly) {
                if (ally != null && (target != thrower || allyIncludeSelf.get())) target.addPotionEffect(ally, true)
            } else if (enemy != null) {
                target.addPotionEffect(enemy, true)
            }
        }
        Particles.spawn(center, splashParticle.get(), splashParticleCount.get(), radius.get() / 2, 0.5, radius.get() / 2, 0.05)
        Sounds.play(center, splashSound.get())
    }

    override fun onKill(killer: Player, victim: Player, arena: IArena) {
        if (!snowballsEnabled.get()) return
        val material = try {
            plugin.bedWars.versionSupport.materialSnowball()
        } catch (ignored: Throwable) {
            null
        } ?: return

        val have = UltimateItems.count(killer, id, SNOWBALL)
        val grant = Math.min(snowballsPerKill.get(), maxSnowballs.get() - have)
        if (grant <= 0) return
        val item = ItemFactory.decorate(
            org.bukkit.inventory.ItemStack(material, grant), snowballName.get(), emptyList(), false,
            tag = UltimateItems.tagFor(id, SNOWBALL)
        )
        for (left in killer.inventory.addItem(item).values) killer.world.dropItemNaturally(killer.location, left)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onSnowballHit(event: EntityDamageByEntityEvent) {
        if (!snowSlow.get()) return
        val snowball = event.damager as? Snowball ?: return
        val thrower = snowball.shooter as? Player ?: return
        val victim = event.entity as? Player ?: return
        val c = ctx(thrower) ?: return
        if (c.team != null && c.team.isMember(victim)) return
        Effects.create(snowSlowEffect.get(), (snowSlowSeconds.get() * 20).toInt(), snowSlowAmplifier.get())
            ?.let { victim.addPotionEffect(it, true) }
    }
}
