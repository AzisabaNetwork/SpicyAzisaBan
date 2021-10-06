package net.azisaba.spicyAzisaBan.listener

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
import util.kt.promise.rewrite.catch

object LoginListener: Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onLogin(e: LoginEvent) {
        PlayerData.createOrUpdate(e.connection).catch { it.printStackTrace() }
    }

    @EventHandler
    fun onPostLogin(e: PostLoginEvent) {
        updatePlayerDataAsync(e.player, true)
    }

    fun updatePlayerDataAsync(player: ProxiedPlayer, login: Boolean) {
        SpicyAzisaBan.debug("Updating player data of ${player.uniqueId} (${player.name})")
        PlayerData.createOrUpdate(player, login).thenDo {
            SpicyAzisaBan.debug("Updated player data of ${player.uniqueId} (${player.name})")
            SpicyAzisaBan.debug(it.toString(), 2)
        }.catch { it.printStackTrace() }
    }
}
