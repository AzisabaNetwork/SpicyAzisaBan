package net.azisaba.spicyAzisaBan.velocity.listener

import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.punishment.PunishmentChecker
import net.azisaba.spicyAzisaBan.velocity.VelocityPlayerActor
import net.azisaba.spicyAzisaBan.velocity.util.VelocityUtil.toVelocity
import net.kyori.adventure.text.TextComponent

object EventListeners {
    @Subscribe
    fun onLogin(e: LoginEvent) {
        PunishmentChecker.checkGlobalBan(VelocityPlayerActor(e.player)) { reason ->
            e.result = ResultedEvent.ComponentResult.denied(TextComponent.ofChildren(*reason.toVelocity()))
        }
    }

    @Subscribe
    fun onServerPreConnect(e: ServerPreConnectEvent) {
        PunishmentChecker.checkLocalBan(ServerInfo(e.originalServer.serverInfo.name, e.originalServer.serverInfo.address), VelocityPlayerActor(e.player)) {
            e.result = ServerPreConnectEvent.ServerResult.denied()
        }
    }

    @Subscribe
    fun onPlayerChat(e: PlayerChatEvent) {
        PunishmentChecker.checkMute(VelocityPlayerActor(e.player), e.message) {
            e.result = PlayerChatEvent.ChatResult.denied()
        }
    }
}
