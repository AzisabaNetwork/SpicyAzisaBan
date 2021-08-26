package net.azisaba.spicyAzisaBan.listener

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import util.kt.promise.rewrite.catch
import java.util.concurrent.TimeUnit

object PostLoginListener: Listener {
    @EventHandler
    fun onPostLogin(e: PostLoginEvent) {
        ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
            if (!e.player.isConnected) return@schedule
            updatePlayerDataAsync(e.player)
        }, 3, TimeUnit.SECONDS)
    }

    fun updatePlayerDataAsync(player: ProxiedPlayer) {
        SpicyAzisaBan.debug("Updating player data of ${player.uniqueId} (${player.name})")
        PlayerData.createOrUpdate(player).thenDo {
            SpicyAzisaBan.debug("Updated player data of ${player.uniqueId} (${player.name})")
            SpicyAzisaBan.debug(it.toString(), 2)
        }.catch { it.printStackTrace() }
    }
}
