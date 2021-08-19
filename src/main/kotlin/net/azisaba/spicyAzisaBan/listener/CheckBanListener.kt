package net.azisaba.spicyAzisaBan.listener

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.util.Util.getIPAddress
import net.azisaba.spicyAzisaBan.util.Util.kick
import net.azisaba.spicyAzisaBan.util.Util.reconstructIPAddress
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import java.util.concurrent.TimeUnit

object CheckBanListener: Listener {
    @EventHandler
    fun onLogin(e: ServerConnectEvent) {
        val res = Promise.create<Boolean> { context ->
            ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
                context.resolve(false)
            }, 3, TimeUnit.SECONDS)
            val p = Punishment.canJoinServer(e.player.uniqueId, e.player.socketAddress.getIPAddress()?.reconstructIPAddress(), e.target.name.lowercase()).complete()
            if (p != null) {
                e.isCancelled = true
                if (e.reason.shouldKick()) {
                    e.player.kick(p.getBannedMessage().complete())
                } else {
                    e.player.send(p.getBannedMessage().complete())
                }
            }
            context.resolve(true)
        }.catch {
            SpicyAzisaBan.instance.logger.warning("Could not check punishments for ${e.player.uniqueId}")
            it.printStackTrace()
            if (SABConfig.database.failsafe) {
                e.isCancelled = true
                if (e.reason.shouldKick()) {
                    e.player.kick(SABMessages.General.error.replaceVariables().translate())
                } else {
                    e.player.send(SABMessages.General.error.replaceVariables().translate())
                }
            }
        }.complete()
        if (!res) {
            SpicyAzisaBan.instance.logger.warning("Could not check punishments for ${e.player.uniqueId} (Timed out)")
            if (SABConfig.database.failsafe) {
                e.isCancelled = true
                if (e.reason.shouldKick()) {
                    e.player.kick(SABMessages.General.error.replaceVariables().translate())
                } else {
                    e.player.send(SABMessages.General.error.replaceVariables().translate())
                }
            }
        }
    }

    private fun ServerConnectEvent.Reason.shouldKick() = when (this) {
        ServerConnectEvent.Reason.JOIN_PROXY,
        ServerConnectEvent.Reason.KICK_REDIRECT,
        ServerConnectEvent.Reason.LOBBY_FALLBACK,
        ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT -> true
        else -> false
    }
}
