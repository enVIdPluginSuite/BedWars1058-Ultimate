package dev.etran.ultimate.ultimate

import com.andrei1058.bedwars.api.arena.IArena
import com.andrei1058.bedwars.api.arena.team.ITeam
import dev.etran.ultimate.UltimatePlugin
import dev.etran.ultimate.compat.Blocks
import dev.etran.ultimate.compat.Materials
import dev.etran.ultimate.compat.PlayerCompat
import dev.etran.ultimate.compat.Sounds
import dev.etran.ultimate.config.ItemOpts
import dev.etran.ultimate.core.Text
import dev.etran.ultimate.core.UltimateItems
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * Builder: a steady supply of team wool plus a tool that builds walls, bridges and bed protection instantly.
 */
class Builder(plugin: UltimatePlugin) : Ultimate(
    plugin, "builder", "Builder", "BRICKS", 14,
    listOf(
        "&7• Build bridges!",
        "&7• Build walls!",
        "&7• Instantly protect your bed!",
        "&7• {kill-wool} wool on kill!",
        "",
        "&7Left click to switch mode, place to activate!",
        "",
        "&7&oPsst, try right clicking your bed with this!",
        "",
        "{status}"
    ),
    ", {wall-width}, {wall-height}, {bridge-length}, {kill-wool}, {wool-interval}"
) {

    // ---- tools ----------------------------------------------------------------------------------------------
    private val wallItem = ItemOpts(
        specs, "tools.wall-item", "BRICKS", "&aBuilder Tool &7- &eWall",
        listOf("&7Right-click the ground to build a wall.", "&7Right-click your bed to protect it.", "&7Left-click to switch to Bridge mode."),
        what = "wall tool"
    )
    private val bridgeItem = ItemOpts(
        specs, "tools.bridge-item", "BRICKS", "&aBuilder Tool &7- &eBridge",
        listOf("&7Right-click a block to build a bridge.", "&7Right-click your bed to protect it.", "&7Left-click to switch to Wall mode."),
        what = "bridge tool"
    )
    private val toolSound = specs.sound("tools.build-sound", "ENTITY_CHICKEN_EGG", "Sound played for each row of wall / each bridge block.")

    // ---- wool supply ----------------------------------------------------------------------------------------
    private val woolInterval = specs.int("wool.interval-ticks", 20, "Ticks between free wool deliveries (20 = every second).", min = 1, max = 1200, step = 5)
    private val woolAmount = specs.int("wool.amount", 1, "Wool per delivery.", min = 1, max = 64)
    private val woolMax = specs.int("wool.max-in-inventory", 256, "Stop delivering once you carry this much wool. 0 = unlimited.", min = 0, max = 2304, step = 16)
    private val woolStart = specs.int("wool.on-activate", 0, "Wool handed out the moment the ultimate is applied.", min = 0, max = 576, step = 16)
    private val woolOnKill = specs.int("wool.on-kill", 16, "Wool received per kill.", min = 0, max = 576, step = 4)

    // ---- wall -----------------------------------------------------------------------------------------------
    private val wallWidth = specs.int("wall.width", 5, "Blocks wide.", min = 1, max = 15)
    private val wallHeight = specs.int("wall.height", 5, "Blocks tall.", min = 1, max = 15)
    private val wallDelay = specs.int("wall.row-delay-ticks", 2, "Ticks between rows (0 = fastest).", min = 0, max = 20)
    private val wallTopOnly = specs.bool("wall.top-face-only", true, "Walls can only be started by clicking the top of a block.")
    private val wallCooldown = specs.double("wall.cooldown-seconds", 0.0, "Cooldown between walls.", min = 0.0, max = 600.0, step = 1.0)

    // ---- bridge ---------------------------------------------------------------------------------------------
    private val bridgeLength = specs.int("bridge.length", 10, "Blocks long.", min = 1, max = 64)
    private val bridgeDelay = specs.int("bridge.block-delay-ticks", 2, "Ticks between blocks (0 = fastest).", min = 0, max = 20)
    private val bridgeStopOnEntity = specs.bool("bridge.stop-at-entities", true, "Stop the bridge when it would trap a player or mob.")
    private val bridgeCooldown = specs.double("bridge.cooldown-seconds", 0.0, "Cooldown between bridges.", min = 0.0, max = 600.0, step = 1.0)

    // ---- bed protection -------------------------------------------------------------------------------------
    private val bedEnabled = specs.bool("bed-protection.enabled", true, "Right-clicking your own bed with a tool wraps it in wool.")
    private val bedTop = specs.bool("bed-protection.cover-top", true, "Also cover the top of the bed.")
    private val bedDistance = specs.double("bed-protection.max-distance", 1.0, "How far the clicked block may be from your team's bed position.", min = 0.0, max = 8.0, step = 0.5)
    private val bedCooldown = specs.double("bed-protection.cooldown-seconds", 0.0, "Cooldown between bed protections.", min = 0.0, max = 600.0, step = 1.0)

    // ---- messages -------------------------------------------------------------------------------------------
    private val msgCantPlace = specs.text("messages.cant-place", "&cYou can't place blocks here!", "Starting position is protected.")
    private val msgCantBridge = specs.text("messages.cant-build-bridge", "&cYou can't build a bridge here!", "Bridge ran into a protected area.")
    private val msgCantWall = specs.text("messages.cant-build-wall", "&cYou can't build a wall here!", "Wall ran into a protected area.")
    private val msgCantBed = specs.text("messages.cant-protect-bed", "&cYou can only protect your team's bed!", "Clicked someone else's bed.")
    private val msgNoWool = specs.text("messages.out-of-wool", "&cYou have ran out of wool blocks!", "No wool left in the inventory.")
    private val msgCooldown = specs.text("messages.on-cooldown", "&cThat tool is on cooldown for {seconds}s.", "Placeholder: {seconds}.")

    private companion object {
        const val WALL = "wall"
        const val BRIDGE = "bridge"
        val SIDES = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
    }

    private val lastWool = HashMap<UUID, Long>()
    private val tasks = HashMap<UUID, MutableList<BukkitTask>>()

    override fun placeholders(): Map<String, String> = mapOf(
        "wall-width" to wallWidth.get().toString(),
        "wall-height" to wallHeight.get().toString(),
        "bridge-length" to bridgeLength.get().toString(),
        "kill-wool" to woolOnKill.get().toString(),
        "wool-interval" to Text.seconds(woolInterval.get() / 20.0)
    )

    private fun tool(kind: String): ItemStack =
        (if (kind == WALL) wallItem else bridgeItem).build(tag = UltimateItems.tagFor(id, kind))

    override fun onActivate(player: Player, arena: IArena) {
        lastWool[player.uniqueId] = System.currentTimeMillis()
        player.inventory.addItem(tool(WALL))
        val team = arena.getTeam(player)
        if (woolStart.get() > 0) giveWool(player, team, woolStart.get())
    }

    override fun onDeactivate(player: Player) {
        lastWool.remove(player.uniqueId)
        tasks.remove(player.uniqueId)?.forEach { it.cancel() }
    }

    // ---- wool -----------------------------------------------------------------------------------------------

    private fun teamWool(team: ITeam?): ItemStack {
        val vs = plugin.bedWars.versionSupport
        return try {
            val base = ItemStack(vs.woolMaterial())
            if (team != null) vs.colourItem(base, team) else base
        } catch (ignored: Throwable) {
            Materials.resolve("WHITE_WOOL")?.toItem() ?: ItemStack(Material.STONE)
        }
    }

    private fun woolCount(player: Player): Int {
        var total = 0
        for (item in player.inventory.contents) if (item != null && Materials.isWool(item)) total += item.amount
        return total
    }

    private fun giveWool(player: Player, team: ITeam?, amount: Int) {
        if (amount <= 0) return
        var remaining = amount
        val max = woolMax.get()
        if (max > 0) remaining = Math.min(remaining, max - woolCount(player))
        while (remaining > 0) {
            val stack = teamWool(team)
            stack.amount = Math.min(remaining, 64)
            remaining -= stack.amount
            for (left in player.inventory.addItem(stack).values) player.world.dropItemNaturally(player.location, left)
        }
    }

    /** Uses up one wool block from the inventory. */
    private fun takeWool(player: Player): Boolean {
        val inv = player.inventory
        for (slot in 0 until 36) {
            val item = inv.getItem(slot) ?: continue
            if (!Materials.isWool(item)) continue
            if (item.amount > 1) item.amount = item.amount - 1 else inv.setItem(slot, null)
            return true
        }
        return false
    }

    private fun hasWool(player: Player): Boolean = woolCount(player) > 0

    private fun place(block: Block, team: ITeam?, arena: IArena) {
        Blocks.setFrom(block, teamWool(team))
        arena.addPlacedBlock(block)
    }

    override fun tick() {
        val now = System.currentTimeMillis()
        val interval = woolInterval.get() * 50L
        forEachHolder { player, c ->
            val last = lastWool[player.uniqueId] ?: now
            if (now - last >= interval) {
                lastWool[player.uniqueId] = now
                giveWool(player, c.team, woolAmount.get())
            }
        }
    }

    override fun onKill(killer: Player, victim: Player, arena: IArena) {
        giveWool(killer, arena.getTeam(killer), woolOnKill.get())
    }

    // ---- interaction ----------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        if (!isHolder(event.player)) return
        if (PlayerCompat.isOffHand(event)) return
        val kind = UltimateItems.kindOf(event.item, id) ?: return
        if (kind != WALL && kind != BRIDGE) return
        event.isCancelled = true // never let the brick item place a real block / break blocks

        val player = event.player
        val c = ctx(player) ?: return
        when (event.action) {
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                player.inventory.setItem(player.inventory.heldItemSlot, tool(if (kind == WALL) BRIDGE else WALL))
                player.updateInventory()
            }
            Action.RIGHT_CLICK_BLOCK -> {
                val clicked = event.clickedBlock ?: return
                if (plugin.bedWars.versionSupport.isBed(clicked.type)) {
                    if (bedEnabled.get()) protectBed(player, c, clicked)
                } else if (kind == WALL) {
                    buildWall(player, c, clicked, event.blockFace)
                } else {
                    buildBridge(player, c, clicked, event.blockFace)
                }
                player.updateInventory()
            }
            else -> {}
        }
    }

    private fun onCooldown(player: Player, key: String, seconds: Double): Boolean {
        if (plugin.cooldowns.isActive(player.uniqueId, key)) {
            plugin.tell(player, msgCooldown.get(), "seconds" to Math.ceil(plugin.cooldowns.remainingSeconds(player.uniqueId, key)).toInt().toString())
            return true
        }
        return false
    }

    private fun track(player: Player, task: BukkitTask) {
        tasks.getOrPut(player.uniqueId) { ArrayList() }.add(task)
    }

    private fun yawToFace(yaw: Float): BlockFace =
        SIDES[Math.round(yaw / 90f) and 3].oppositeFace

    private fun buildWall(player: Player, c: UltimateManager.Context, clicked: Block, clickedFace: BlockFace) {
        if (wallTopOnly.get() && clickedFace != BlockFace.UP) return
        if (onCooldown(player, "builder:wall", wallCooldown.get())) return
        val arena = c.arena
        val start = clicked.getRelative(clickedFace)
        if (arena.isProtected(start.location)) {
            plugin.tell(player, msgCantPlace.get())
            return
        }

        val facing = yawToFace(player.eyeLocation.yaw)
        val alongX = facing == BlockFace.NORTH || facing == BlockFace.SOUTH
        val width = wallWidth.get()
        val base = ArrayList<Block>()
        for (offset in -((width - 1) / 2)..(width / 2)) {
            base.add(if (alongX) start.getRelative(offset, 0, 0) else start.getRelative(0, 0, offset))
        }

        plugin.cooldowns.start(player, "builder:wall", wallCooldown.get(), "Wall", hud = false)
        var row = 0
        track(player, plugin.runTimer(0L, Math.max(1, wallDelay.get()).toLong()) { task ->
            if (!player.isOnline || ctx(player) == null) {
                task.cancel()
                return@runTimer
            }
            for (b in base) {
                val block = b.getRelative(0, row, 0)
                if (arena.isProtected(block.location)) {
                    plugin.tell(player, msgCantWall.get())
                    task.cancel()
                    return@runTimer
                }
                if (block.type != Material.AIR) continue
                if (!takeWool(player)) {
                    plugin.tell(player, msgNoWool.get())
                    task.cancel()
                    return@runTimer
                }
                place(block, c.team, arena)
            }
            Sounds.play(player, toolSound.get())
            if (++row >= wallHeight.get()) task.cancel()
        })
    }

    private fun entityOccupies(block: Block, player: Player): Boolean {
        val bx = block.x + 0.5
        val bz = block.z + 0.5
        for (e in player.getNearbyEntities(12.0, 12.0, 12.0) + player) {
            val l = e.location
            if (l.world != block.world) continue
            if (Math.abs(l.x - bx) < 0.8 && Math.abs(l.z - bz) < 0.8 && l.y < block.y + 1 && l.y + 1.8 > block.y) return true
        }
        return false
    }

    private fun buildBridge(player: Player, c: UltimateManager.Context, clicked: Block, clickedFace: BlockFace) {
        if (onCooldown(player, "builder:bridge", bridgeCooldown.get())) return
        val arena = c.arena
        if (arena.isProtected(clicked.getRelative(clickedFace).location)) {
            plugin.tell(player, msgCantPlace.get())
            return
        }
        val facing = yawToFace(player.eyeLocation.yaw)
        val direction = if (clickedFace == BlockFace.UP || clickedFace == BlockFace.DOWN) facing else clickedFace

        plugin.cooldowns.start(player, "builder:bridge", bridgeCooldown.get(), "Bridge", hud = false)
        var block = clicked
        var placed = 0
        track(player, plugin.runTimer(0L, Math.max(1, bridgeDelay.get()).toLong()) { task ->
            if (!player.isOnline || ctx(player) == null) {
                task.cancel()
                return@runTimer
            }
            block = block.getRelative(direction)
            if (arena.isProtected(block.location)) {
                plugin.tell(player, msgCantBridge.get())
                task.cancel()
                return@runTimer
            }
            if (block.type != Material.AIR || (bridgeStopOnEntity.get() && entityOccupies(block, player))) {
                task.cancel()
                return@runTimer
            }
            if (!takeWool(player)) {
                plugin.tell(player, msgNoWool.get())
                task.cancel()
                return@runTimer
            }
            place(block, c.team, arena)
            Sounds.play(player, toolSound.get())
            if (++placed >= bridgeLength.get()) task.cancel()
        })
    }

    /** The other half of a bed, found by looking for an adjacent bed block (works on every version, no MaterialData). */
    private fun otherHalf(block: Block): Block? {
        val vs = plugin.bedWars.versionSupport
        for (face in SIDES) {
            val rel = block.getRelative(face)
            if (vs.isBed(rel.type)) return rel
        }
        return null
    }

    private fun protectBed(player: Player, c: UltimateManager.Context, clicked: Block) {
        if (onCooldown(player, "builder:bed", bedCooldown.get())) return
        val team = c.team ?: return
        val bedPos = team.bed
        val maxDist = bedDistance.get()
        if (bedPos == null || bedPos.world != clicked.world || bedPos.distanceSquared(clicked.location) > maxDist * maxDist) {
            plugin.tell(player, msgCantBed.get())
            return
        }
        if (!hasWool(player)) {
            plugin.tell(player, msgNoWool.get())
            return
        }

        val halves = listOfNotNull(clicked, otherHalf(clicked))
        val faces = if (bedTop.get()) SIDES.toList() + BlockFace.UP else SIDES.toList()
        plugin.cooldowns.start(player, "builder:bed", bedCooldown.get(), "Bed", hud = false)
        Sounds.play(player, toolSound.get())
        for (face in faces) {
            for (part in halves) {
                val target = part.getRelative(face)
                if (target.type != Material.AIR || c.arena.isProtected(target.location)) continue
                if (!takeWool(player)) {
                    plugin.tell(player, msgNoWool.get())
                    return
                }
                place(target, team, c.arena)
            }
        }
    }
}
