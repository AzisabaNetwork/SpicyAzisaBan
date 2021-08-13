package net.azisaba.spicyAzisaBan.listener

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

object PostLoginListener: Listener {
    @EventHandler
    fun onPostLogin(e: PostLoginEvent) {
        PlayerData.createOrUpdate(e.player).thenDo {
            SpicyAzisaBan.debug("Updated player data of ${e.player.uniqueId}!")
            SpicyAzisaBan.debug(it.toString(), 2)
        }
    }
}
