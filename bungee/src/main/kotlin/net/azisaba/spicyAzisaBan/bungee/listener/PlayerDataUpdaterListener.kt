package net.azisaba.spicyAzisaBan.bungee.listener

import net.azisaba.spicyAzisaBan.bungee.BungeePlayerActor
import net.azisaba.spicyAzisaBan.bungee.BungeePlayerConnection
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
import util.kt.promise.rewrite.catch

object PlayerDataUpdaterListener: Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onLogin(e: LoginEvent) {
        PlayerData.createOrUpdate(BungeePlayerConnection(e.connection)).catch { it.printStackTrace() }
    }

    @EventHandler
    fun onPostLogin(e: PostLoginEvent) {
        PlayerData.updatePlayerDataAsync(BungeePlayerActor(e.player), true)
    }

    @EventHandler
    fun onPlayerDisconnect(e: PlayerDisconnectEvent) {
        PlayerData.updatePlayerDataAsync(BungeePlayerActor(e.player), false)
    }
}
