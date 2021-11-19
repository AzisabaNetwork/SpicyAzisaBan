package net.azisaba.spicyAzisaBan.bungee.listener

import net.azisaba.spicyAzisaBan.bungee.BungeePlayerActor
import net.azisaba.spicyAzisaBan.bungee.BungeePlayerConnection
import net.azisaba.spicyAzisaBan.bungee.util.BungeeUtil.toBungee
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.punishment.PunishmentChecker
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ChatEvent
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

object EventListeners: Listener {
    @EventHandler
    fun onLogin(e: LoginEvent) {
        PunishmentChecker.checkGlobalBan(BungeePlayerConnection(e.connection)) { reason ->
            e.isCancelled = true
            e.setCancelReason(*reason.toBungee())
        }
    }

    @EventHandler
    fun onServerConnect(e: ServerConnectEvent) {
        PunishmentChecker.checkLocalBan(ServerInfo(e.target.name, e.target.socketAddress), BungeePlayerActor(e.player)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onChat(e: ChatEvent) {
        if (e.sender !is ProxiedPlayer) return
        PunishmentChecker.checkMute(BungeePlayerActor(e.sender as ProxiedPlayer), e.message) {
            e.isCancelled = true
        }
    }
}
