package net.azisaba.spicyAzisaBan.util.contexts

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.util.Util.concat
import net.azisaba.spicyAzisaBan.util.Util.filtr
import xyz.acrylicstyle.mcutil.common.PlayerProfile

data class PlayerContext(
    val isSuccess: Boolean,
    val profile: PlayerProfile,
): Context {
    companion object {
        fun tabComplete(s: String): List<String> =
            SpicyAzisaBan.instance.getPlayers()
                .filterIndexed { i, _ -> i < 500 }
                .map { "player=${it.name}" }
                .concat(Punishment.recentPunishedPlayers.map { "player=${it.name}" })
                .distinct()
                .filtr(s)
    }
}
