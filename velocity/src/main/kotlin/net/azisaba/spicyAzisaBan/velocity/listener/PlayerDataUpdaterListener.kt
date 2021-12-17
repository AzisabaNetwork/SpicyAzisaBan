package net.azisaba.spicyAzisaBan.velocity.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.velocity.VelocityPlayerActor

object PlayerDataUpdaterListener {
    @Subscribe
    fun onPostLogin(e: PostLoginEvent) {
        PlayerData.updatePlayerDataAsync(VelocityPlayerActor(e.player), true)
    }

    @Subscribe
    fun onDisconnect(e: DisconnectEvent) {
        PlayerData.updatePlayerDataAsync(VelocityPlayerActor(e.player), false)
    }
}
