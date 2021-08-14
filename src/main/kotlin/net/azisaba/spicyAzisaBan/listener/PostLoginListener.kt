package net.azisaba.spicyAzisaBan.listener

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.concurrent.TimeUnit

object PostLoginListener: Listener {
    @EventHandler
    fun onPostLogin(e: PostLoginEvent) {
        ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
            if (!e.player.isConnected) return@schedule
            SpicyAzisaBan.debug("Updating player data of ${e.player.uniqueId} (${e.player.name})")
            PlayerData.createOrUpdate(e.player).thenDo {
                SpicyAzisaBan.debug("Updated player data of ${e.player.uniqueId} (${e.player.name})")
                SpicyAzisaBan.debug(it.toString(), 2)
            }
        }, 3, TimeUnit.SECONDS)
    }
}
