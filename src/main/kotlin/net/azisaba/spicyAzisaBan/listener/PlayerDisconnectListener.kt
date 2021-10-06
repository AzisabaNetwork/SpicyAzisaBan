package net.azisaba.spicyAzisaBan.listener

import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

object PlayerDisconnectListener: Listener {
    @EventHandler
    fun onPlayerDisconnect(e: PlayerDisconnectEvent) {
        LoginListener.updatePlayerDataAsync(e.player, false)
    }
}
