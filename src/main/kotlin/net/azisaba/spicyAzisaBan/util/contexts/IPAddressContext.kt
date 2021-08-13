package net.azisaba.spicyAzisaBan.util.contexts

import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.util.Util.concat
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.md_5.bungee.api.ProxyServer

data class IPAddressContext(
    val isSuccess: Boolean,
    val ip: String,
): Context {
    companion object {
        fun tabComplete(s: String): List<String> =
            ProxyServer.getInstance().players
                .filterIndexed { i, _ -> i < 500 }
                .map { "target=${it.name}" }
                .concat(Punishment.recentPunishedPlayers.map { "target=${it.name}" })
                .distinct()
                .filtr(s)
    }
}
