package net.azisaba.spicyAzisaBan.bungee.listener

import net.azisaba.spicyAzisaBan.bungee.BungeePlayerActor
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

object PlayerDataUpdaterListener: Listener {
    @EventHandler
    fun onPostLogin(e: PostLoginEvent) {
        PlayerData.updatePlayerDataAsync(BungeePlayerActor(e.player), true)
    }

    @EventHandler
    fun onPlayerDisconnect(e: PlayerDisconnectEvent) {
        PlayerData.updatePlayerDataAsync(BungeePlayerActor(e.player), false)
    }
}
