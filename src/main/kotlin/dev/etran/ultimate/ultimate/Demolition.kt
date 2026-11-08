package dev.etran.ultimate.ultimate

import com.andrei1058.bedwars.api.arena.IArena
import com.andrei1058.bedwars.api.events.player.PlayerBedBreakEvent
import dev.bw1058.ultimate.UltimatePlugin
import dev.bw1058.ultimate.compat.Materials
import dev.bw1058.ultimate.compat.Particles
import dev.bw1058.ultimate.compat.PlayerCompat
import dev.bw1058.ultimate.compat.Sounds
import dev.bw1058.ultimate.config.ItemOpts
import dev.bw1058.ultimate.core.Text
import dev.bw1058.ultimate.core.UltimateItems
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Creeper
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/**
 * Demolition: burns connected wool with its flint and steel, drops primed TNT where it dies, earns TNT / fireballs
 * from kill streaks and a creeper egg for every bed it breaks.
 */
class Demolition(plugin: UltimatePlugin) : Ultimate(
    plugin, "demolition", "Demolition", "FLINT_AND_STEEL", 15,
    listOf(
        "&7• Burn wool in a radius!",
        "&7• Drops TNT on death!",
        "&7• Earns a creeper egg on bed",
        "&7destroy!",
        "&7• Grants TNT or a Fireball",
        "&7every {streak} kills!",
        "",
        "&7Cooldown: &a{cooldown} Seconds",
        "",
        "{status}"
    ),
    ", {cooldown}, {streak}, {depth}"
) {

    // ---- ignite ---------------------------------------------------------------------------------------------
    private val igniteItem = ItemOpts(
        specs, "ignite.item", "FLINT_AND_STEEL", "&aDemolition Ultimate",
        listOf("&7Right-click wool to set a", "&7chain reaction."), what = "flint and steel"
    )
    private val cooldown = specs.double("ignite.cooldown-seconds", 10.0, "Cooldown after burning wool.", min = 0.0, max = 600.0, step = 1.0)
    private val cooldownOnMiss = specs.bool("ignite.cooldown-on-miss", false, "Also start the cooldown when you click something that can't burn.")
    private val onlyPlaced = specs.bool("ignite.only-player-placed", true, "Only blocks placed by players during the game can burn (map blocks are safe).")
    private val burnSuffixes = specs.stringList("ignite.burnable-suffixes", listOf("WOOL"), "Block types that burn, matched by the end of their name (WOOL covers every colour). Add e.g. CARPET or TERRACOTTA.")
    private val maxDepth = specs.int("ignite.max-depth", 20, "How many blocks away from the ignition point the fire can spread.", min = 1, max = 100)
    private val maxBlocks = specs.int("ignite.max-blocks", 600, "Hard cap on blocks burned per ignition (protects server performance).", min = 1, max = 5000, step = 50)
    private val diagonals = specs.bool("ignite.spread-diagonally", false, "Fire also jumps across diagonal contacts.")
    private val burnMin = specs.int("ignite.burn-delay-min-ticks", 10, "Shortest delay before a block disappears.", min = 0, max = 400)
    private val burnMax = specs.int("ignite.burn-delay-max-ticks", 40, "Longest delay before a block disappears.", min = 1, max = 400)
    private val placeFire = specs.bool("ignite.place-fire", true, "Place a visible fire block on top of burning wool.")
    private val igniteSound = specs.sound("ignite.sound", "FIRE_IGNITE", "Sound on ignition.")
    private val burnParticle = specs.particle("ignite.particle", "FLAME", "Particle on each burning block.")
    private val burnParticleCount = specs.int("ignite.particle-count", 3, "Particles per burning block.", min = 1, max = 30)

    // ---- death TNT ------------------------------------------------------------------------------------------
    private val tntEnabled = specs.bool("death-tnt.enabled", true, "Drop primed TNT where this player dies.")
    private val tntCount = specs.int("death-tnt.count", 1, "How many TNT entities.", min = 1, max = 10)
    private val tntFuse = specs.int("death-tnt.fuse-ticks", 80, "Fuse length in ticks (80 = 4 seconds).", min = 1, max = 400, step = 10)
    private val tntIncendiary = specs.bool("death-tnt.incendiary", false, "TNT sets blocks on fire.")
    private val tntTeamImmune = specs.bool("death-tnt.team-immunity", true, "Teammates take no damage from this TNT.")

    // ---- kill streak ----------------------------------------------------------------------------------------
    private val streakEnabled = specs.bool("kill-streak.enabled", true, "Earn items for kill streaks.")
    private val streakKills = specs.int("kill-streak.kills-required", 3, "Kills needed per reward.", min = 1, max = 50)
    private val streakRewards = specs.materialList("kill-streak.rewards", listOf("TNT", "FIREBALL"), "Possible rewards (any material).")
    private val streakMode = specs.choice("kill-streak.mode", "RANDOM", listOf("RANDOM", "CYCLE"), "RANDOM picks a reward each time; CYCLE goes through the list in order.")
    private val streakAmount = specs.int("kill-streak.amount", 1, "Items per reward.", min = 1, max = 64)
    private val streakResetOnDeath = specs.bool("kill-streak.reset-on-death", false, "Dying resets your progress.")
    private val msgReward = specs.text("messages.streak-reward", "", "Sent when a kill streak pays out. Placeholder: {item}. Blank = none.")

    // ---- creeper --------------------------------------------------------------------------------------------
    private val creeperEnabled = specs.bool("creeper.enabled", true, "Earn a creeper egg when you break a bed.")
    private val creeperItem = ItemOpts(
        specs, "creeper.item", "CREEPER_SPAWN_EGG", "&aCreeper Egg",
        listOf("&7Right-click to spawn a creeper."), what = "creeper egg"
    )
    private val creeperAmount = specs.int("creeper.eggs-per-bed", 1, "Eggs received per bed.", min = 1, max = 16)
    private val creeperPowered = specs.bool("creeper.powered", false, "Spawn charged creepers (bigger explosion).")
    private val creeperTeamImmune = specs.bool("creeper.team-immunity", true, "Teammates take no damage from this creeper.")
    private val creeperLifetime = specs.int("creeper.despawn-after-seconds", 0, "Remove the creeper after this long. 0 = never.", min = 0, max = 600, step = 5)
    private val msgCantSpawn = specs.text("messages.cant-spawn-creeper", "&cYou can't spawn a creeper here!", "Target spot is protected.")

    private val streak = HashMap<UUID, Int>()
    private val cycle = HashMap<UUID, Int>()

    private companion object {
        const val IGNITE = "demolition:ignite"
        const val KIND_IGNITE = "ignite"
        const val KIND_EGG = "creeper-egg"
        const val META_TEAM = "bw1058ultimate-team"
        val FACES = arrayOf(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)
    }

    override fun placeholders(): Map<String, String> = mapOf(
        "cooldown" to Text.seconds(cooldown.get()),
        "streak" to streakKills.get().toString(),
        "depth" to maxDepth.get().toString()
    )

    override fun onActivate(player: Player, arena: IArena) {
        player.inventory.addItem(igniteItem.build(tag = UltimateItems.tagFor(id, KIND_IGNITE), unbreakable = true))
    }

    override fun onRelinquish(player: Player, switching: Boolean) {
        streak.remove(player.uniqueId)
        cycle.remove(player.uniqueId)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        streak.remove(event.player.uniqueId)
        cycle.remove(event.player.uniqueId)
    }

    // ---- death / kills --------------------------------------------------------------------------------------

    override fun onHolderDeath(player: Player, arena: IArena) {
        super.onHolderDeath(player, arena)
        if (streakResetOnDeath.get()) streak.remove(player.uniqueId)
        if (!tntEnabled.get()) return
        val team = arena.getTeam(player)
        repeat(tntCount.get()) {
            val tnt = player.world.spawn(player.location, TNTPrimed::class.java)
            tnt.fuseTicks = tntFuse.get()
            tnt.setIsIncendiary(tntIncendiary.get())
            if (team != null && tntTeamImmune.get()) tnt.setMetadata(META_TEAM, FixedMetadataValue(plugin, team.name))
        }
    }

    override fun onKill(killer: Player, victim: Player, arena: IArena) {
        if (!streakEnabled.get()) return
        val count = (streak[killer.uniqueId] ?: 0) + 1
        if (count < streakKills.get()) {
            streak[killer.uniqueId] = count
            return
        }
        streak.remove(killer.uniqueId)

        val rewards = streakRewards.get().mapNotNull { Materials.resolve(it) }
        if (rewards.isEmpty()) return
        val pick = if (streakMode.get().equals("CYCLE", true)) {
            val index = (cycle[killer.uniqueId] ?: 0) % rewards.size
            cycle[killer.uniqueId] = index + 1
            rewards[index]
        } else rewards[ThreadLocalRandom.current().nextInt(rewards.size)]

        val item = pick.toItem(streakAmount.get())
        for (left in killer.inventory.addItem(item).values) killer.world.dropItemNaturally(killer.location, left)
        plugin.tell(killer, msgReward.get(), "item" to pick.material.name.lowercase().replace('_', ' '))
    }

    @EventHandler
    fun onBedBreak(event: PlayerBedBreakEvent) {
        val player = event.player
        if (ctx(player) == null || !creeperEnabled.get()) return
        val egg = creeperItem.build(tag = UltimateItems.tagFor(id, KIND_EGG)).also { it.amount = creeperAmount.get() }
        for (left in player.inventory.addItem(egg).values) player.world.dropItemNaturally(player.location, left)
    }

    // ---- team immunity --------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onExplosionDamage(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val damager: Entity = event.damager
        if (damager !is TNTPrimed && damager !is Creeper) return
        if (!damager.hasMetadata(META_TEAM)) return
        val arena = plugin.arenas.arenaOf(victim) ?: return
        val team = arena.getTeam(victim) ?: return
        if (damager.getMetadata(META_TEAM).firstOrNull()?.asString() == team.name) event.isCancelled = true
    }

    // ---- interaction ----------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        if (!isHolder(event.player)) return
        if (PlayerCompat.isOffHand(event)) return
        val kind = UltimateItems.kindOf(event.item, id) ?: return
        val player = event.player
        event.isCancelled = true // never light a real fire / spawn a real mob
        val c = ctx(player) ?: return

        if (kind == KIND_IGNITE && event.action == Action.RIGHT_CLICK_BLOCK) {
            ignite(player, c, event.clickedBlock ?: return)
        } else if (kind == KIND_EGG && event.action == Action.RIGHT_CLICK_BLOCK) {
            spawnCreeper(player, c, event)
        }
    }

    private fun isBurnable(arena: IArena, block: Block): Boolean {
        val name = block.type.name
        if (burnSuffixes.get().none { name.endsWith(it.trim().uppercase()) }) return false
        return !onlyPlaced.get() || arena.isBlockPlaced(block)
    }

    private fun ignite(player: Player, c: UltimateManager.Context, clicked: Block) {
        if (plugin.cooldowns.isActive(player.uniqueId, IGNITE)) return
        val arena = c.arena
        if (!isBurnable(arena, clicked)) {
            if (cooldownOnMiss.get()) plugin.cooldowns.start(player, IGNITE, cooldown.get(), "Ignite")
            return
        }
        plugin.cooldowns.start(player, IGNITE, cooldown.get(), "Ignite")
        Sounds.play(player, igniteSound.get())

        // Breadth-first flood fill through connected burnable blocks, bounded by depth and total count.
        val burning = LinkedHashSet<Block>()
        var frontier = ArrayDeque<Block>()
        burning.add(clicked)
        frontier.add(clicked)
        var depth = 0
        while (frontier.isNotEmpty() && depth < maxDepth.get() && burning.size < maxBlocks.get()) {
            val next = ArrayDeque<Block>()
            for (block in frontier) {
                for (neighbor in neighbors(block)) {
                    if (burning.size >= maxBlocks.get()) break
                    if (neighbor in burning || !isBurnable(arena, neighbor)) continue
                    burning.add(neighbor)
                    next.add(neighbor)
                }
            }
            frontier = next
            depth++
        }

        val rnd = ThreadLocalRandom.current()
        val lo = Math.min(burnMin.get(), burnMax.get())
        val hi = Math.max(burnMin.get(), burnMax.get())
        for (block in burning) {
            val above = block.getRelative(BlockFace.UP)
            if (placeFire.get() && above.type == Material.AIR) above.type = Materials.lookup("FIRE") ?: Material.AIR
            Particles.spawn(block.location.add(0.5, 1.0, 0.5), burnParticle.get(), burnParticleCount.get(), 0.3, 0.2, 0.3, 0.01)

            val delay = if (hi > lo) rnd.nextInt(lo, hi + 1) else lo
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                if (!plugin.arenas.isActive(arena)) return@Runnable
                if (isBurnable(arena, block)) {
                    block.type = Material.AIR
                    arena.removePlacedBlock(block)
                }
                val fire = block.getRelative(BlockFace.UP)
                if (fire.type.name == "FIRE") fire.type = Material.AIR
            }, delay.toLong())
        }
    }

    private fun neighbors(block: Block): List<Block> {
        val result = ArrayList<Block>(26)
        if (diagonals.get()) {
            for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
                if (dx == 0 && dy == 0 && dz == 0) continue
                result.add(block.getRelative(dx, dy, dz))
            }
        } else {
            for (face in FACES) result.add(block.getRelative(face))
        }
        return result
    }

    private fun spawnCreeper(player: Player, c: UltimateManager.Context, event: PlayerInteractEvent) {
        val clicked = event.clickedBlock ?: return
        val target = clicked.getRelative(event.blockFace)
        if (c.arena.isProtected(target.location)) {
            plugin.tell(player, msgCantSpawn.get())
            return
        }
        val creeper = target.world.spawn(target.location.add(0.5, 0.0, 0.5), Creeper::class.java)
        if (creeperPowered.get()) creeper.isPowered = true
        val team = c.team
        if (team != null && creeperTeamImmune.get()) creeper.setMetadata(META_TEAM, FixedMetadataValue(plugin, team.name))
        if (creeperLifetime.get() > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable { if (creeper.isValid) creeper.remove() }, creeperLifetime.get() * 20L)
        }

        if (player.gameMode != GameMode.CREATIVE) {
            val inv = player.inventory
            val held: ItemStack? = inv.getItem(inv.heldItemSlot)
            if (held != null && UltimateItems.kindOf(held, id) == KIND_EGG) {
                if (held.amount > 1) held.amount = held.amount - 1 else inv.setItem(inv.heldItemSlot, null)
            }
        }
        player.updateInventory()
    }
}
