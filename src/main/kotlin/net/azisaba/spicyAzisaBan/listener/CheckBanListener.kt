package net.azisaba.spicyAzisaBan.listener

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.kick
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
        var done = false
        Promise.create<Unit> { context ->
            ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
                if (!done) context.reject(Exception())
            }, 3, TimeUnit.SECONDS)
            Punishment.fetchActivePunishmentsByTarget(e.player.uniqueId.toString(), e.target.name.lowercase()).thenDo {
                // possible punishments:
                // BAN
                // TEMP_BAN
                // IP_BAN
                // TEMP_IP_BAN
                // TODO: check temp bans (with expiration date)
                val p = it.find { p -> p.type == PunishmentType.BAN }
                if (p != null) {
                    e.isCancelled = true
                    if (e.reason.shouldKick()) {
                        e.player.kick(SABMessages.Commands.GBan.layout.replaceVariables(p.getVariables().complete()).translate())
                    } else {
                        e.player.send(SABMessages.Commands.GBan.layout.replaceVariables(p.getVariables().complete()).translate())
                    }
                }
            }.thenDo { done = true }.complete()
            context.resolve()
        }.catch {
            SpicyAzisaBan.instance.logger.warning("Could not check punishments for ${e.player.uniqueId}")
            if (SABConfig.database.failsafe) {
                e.isCancelled = true
                if (e.reason.shouldKick()) {
                    e.player.kick(SABMessages.General.error.replaceVariables().translate())
                } else {
                    e.player.send(SABMessages.General.error.replaceVariables().translate())
                }
            }
        }.complete()
    }

    fun ServerConnectEvent.Reason.shouldKick() = when (this) {
        ServerConnectEvent.Reason.JOIN_PROXY,
        ServerConnectEvent.Reason.KICK_REDIRECT,
        ServerConnectEvent.Reason.LOBBY_FALLBACK,
        ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT -> true
        else -> false
    }
}
