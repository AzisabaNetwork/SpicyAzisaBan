package net.azisaba.spicyAzisaBan.util.contexts

import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.md_5.bungee.api.ProxyServer
import xyz.acrylicstyle.mcutil.common.PlayerProfile

data class PlayerContext(
    val isSuccess: Boolean,
    val profile: PlayerProfile,
): Context {
    companion object {
        fun tabComplete(s: String): List<String> {
            if (ProxyServer.getInstance().players.size > 500) return emptyList()
            return ProxyServer.getInstance().players.map { "player=${it.name}" }.filtr(s)
        }
    }
}
