package dev.etran.ultimate.ultimate

import com.andrei1058.bedwars.api.arena.IArena
import com.andrei1058.bedwars.api.events.player.PlayerGeneratorCollectEvent
import dev.etran.ultimate.UltimatePlugin
import dev.etran.ultimate.compat.Materials
import dev.etran.ultimate.compat.Particles
import dev.etran.ultimate.compat.PlayerCompat
import dev.etran.ultimate.compat.Sounds
import dev.etran.ultimate.config.ItemOpts
import dev.etran.ultimate.core.Text
import dev.etran.ultimate.core.UltimateItems
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/**
 * Gatherer: picks up extra diamonds/emeralds from generators and carries a portable ender chest.
 *
 * The chest is a per-game virtual inventory by default. (The vanilla ender chest persists across games and servers,
 * which let players carry items between matches and duplicate resources.)
 */
class Gatherer(plugin: UltimatePlugin) : Ultimate(
    plugin, "gatherer", "Gatherer", "ENDER_CHEST", 16,
    listOf(
        "&7• Duplicate picked up",
        "&7diamonds/emeralds from",
        "&7generators!",
        "&7• Portable Ender Chest!",
        "",
        "{status}"
    ),
    ", {multiplier}, {materials}"
) {

    // ---- duplication ----------------------------------------------------------------------------------------
    private val dupEnabled = specs.bool("duplicate.enabled", true, "Duplicate resources picked up from generators.")
    private val dupMaterials = specs.materialList("duplicate.materials", listOf("DIAMOND", "EMERALD"), "Resources that get duplicated. Add IRON_INGOT / GOLD_INGOT to duplicate those too.")
    private val dupMultiplier = specs.int("duplicate.multiplier", 2, "Total amount you end up with: 2 = double, 3 = triple.", min = 1, max = 10)
    private val dupChance = specs.int("duplicate.chance-percent", 100, "Chance (0-100) that a pickup is duplicated.", min = 0, max = 100, step = 5)
    private val dupSound = specs.sound("duplicate.sound", "", "Sound when a pickup is duplicated.")
    private val dupParticle = specs.particle("duplicate.particle", "", "Particle when a pickup is duplicated.")
    private val dupDropIfFull = specs.bool("duplicate.drop-if-full", true, "Drop the bonus at your feet when your inventory is full (otherwise it is lost).")

    // ---- ender chest ----------------------------------------------------------------------------------------
    private val chestEnabled = specs.bool("ender-chest.enabled", true, "Give the portable ender chest.")
    private val chestItem = ItemOpts(
        specs, "ender-chest.item", "ENDER_CHEST", "&aPortable Ender Chest",
        listOf("&7Right-click to open your ender chest."), what = "ender chest item"
    )
    private val chestVirtual = specs.bool(
        "ender-chest.virtual", true,
        "true = a private chest that only lives for this game (recommended). false = the player's real, persistent ender chest."
    )
    private val chestRows = specs.int("ender-chest.rows", 3, "Rows of the virtual chest.", min = 1, max = 6)
    private val chestTitle = specs.text("ender-chest.title", "&5Ender Chest", "Title of the virtual chest.")
    private val chestOpenSound = specs.sound("ender-chest.open-sound", "", "Sound when opening.")
    private val chestKeepOnDeath = specs.bool("ender-chest.keep-on-death", true, "Chest contents survive death. If false they are dropped.")

    private class ChestHolder : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    private val chests = HashMap<UUID, Inventory>()

    private companion object {
        const val KIND_CHEST = "ender-chest"
    }

    override fun placeholders(): Map<String, String> = mapOf(
        "multiplier" to dupMultiplier.get().toString(),
        "materials" to dupMaterials.get().joinToString(", ") { it.lowercase().replace('_', ' ') }
    )

    override fun onActivate(player: Player, arena: IArena) {
        if (chestEnabled.get()) {
            player.inventory.addItem(chestItem.build(tag = UltimateItems.tagFor(id, KIND_CHEST)))
        }
    }

    override fun onHolderDeath(player: Player, arena: IArena) {
        super.onHolderDeath(player, arena)
        if (!chestKeepOnDeath.get()) {
            chests.remove(player.uniqueId)?.let { inv ->
                for (item in inv.contents) if (item != null && item.type != org.bukkit.Material.AIR) player.world.dropItemNaturally(player.location, item)
                inv.clear()
            }
        }
    }

    override fun onRelinquish(player: Player, switching: Boolean) {
        val inv = chests.remove(player.uniqueId) ?: return
        if (switching) {
            // Still in the game: hand the contents back rather than destroying them.
            for (item in inv.contents) {
                if (item == null || item.type == org.bukkit.Material.AIR) continue
                for (left in player.inventory.addItem(item).values) player.world.dropItemNaturally(player.location, left)
            }
        }
        inv.clear()
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        chests.remove(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (!isHolder(event.player)) return
        if (PlayerCompat.isOffHand(event)) return
        if (UltimateItems.kindOf(event.item, id) != KIND_CHEST) return
        event.isCancelled = true // don't place the chest block

        val player = event.player
        if (ctx(player) == null) return
        Sounds.play(player, chestOpenSound.get())
        if (chestVirtual.get()) player.openInventory(chestFor(player)) else player.openInventory(player.enderChest)
    }

    private fun chestFor(player: Player): Inventory = chests.getOrPut(player.uniqueId) {
        val holder = ChestHolder()
        holder.inv = Bukkit.createInventory(holder, chestRows.get() * 9, Text.color(chestTitle.get()).take(32))
        holder.inv
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onGeneratorPickup(event: PlayerGeneratorCollectEvent) {
        val player = event.player
        if (ctx(player) == null || !dupEnabled.get() || event.isCancelled) return
        val picked = event.itemStack ?: return
        val names = dupMaterials.get().mapNotNull { Materials.resolve(it)?.material?.name }
        if (picked.type.name !in names) return
        if (ThreadLocalRandom.current().nextInt(100) >= dupChance.get()) return

        val extra = picked.amount * (dupMultiplier.get() - 1)
        if (extra <= 0) return
        val bonus = ItemStack(picked.type, extra)
        val leftovers = player.inventory.addItem(bonus)
        if (dupDropIfFull.get()) for (left in leftovers.values) player.world.dropItemNaturally(player.location, left)
        Sounds.play(player, dupSound.get())
        Particles.spawn(player.location.add(0.0, 1.0, 0.0), dupParticle.get(), 6, 0.3, 0.3, 0.3, 0.02)
    }
}
