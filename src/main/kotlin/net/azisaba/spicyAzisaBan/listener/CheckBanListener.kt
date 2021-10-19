package net.azisaba.spicyAzisaBan.listener

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.getIPAddress
import net.azisaba.spicyAzisaBan.util.Util.kick
import net.azisaba.spicyAzisaBan.util.Util.reconstructIPAddress
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendDelayed
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import util.kt.promise.rewrite.catch
import java.util.concurrent.TimeUnit

object CheckBanListener: Listener {
    /**
     * Checks whether if the player is allowed to join the target server.
     * - Creates async context and fetch punishments asynchronously
     * - Reject as "timed out" if punishment could not be fetched in 3 seconds
     */
    @EventHandler
    fun onLogin(e: ServerConnectEvent) {
        /**
         * the server before connecting to target server
         */
        val currentServer = e.player.server?.info

        /**
         * formatted ip address
         */
        val ipAddress = e.player.socketAddress.getIPAddress()?.reconstructIPAddress()
        val pair = Punishment.canJoinServerCached(e.player.uniqueId, ipAddress, e.target.name.lowercase())
        if (pair.first) { // true = cached, false = not cached
            val p = pair.second ?: return
            if (p.isExpired()) {
                p.removeIfExpired()
            } else {
                e.isCancelled = true
                if (e.reason.shouldKick()) {
                    e.player.kick(p.getBannedMessage().complete())
                } else {
                    e.player.sendDelayed(100, p.getBannedMessage().complete())
                }
                return
            }
        }
        async<Boolean> { context ->
            Thread({
                ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
                    context.resolve(false)
                }, 3, TimeUnit.SECONDS)
                val p = Punishment.canJoinServer(e.player.uniqueId, ipAddress, e.target.name.lowercase()).complete()
                if (p != null) {
                    SpicyAzisaBan.debug("Kicking ${e.player.name} from ${e.player.server?.info?.name} asynchronously (reason: banned from ${p.server})")
                    SpicyAzisaBan.debug(p.toString(), 2)
                    e.isCancelled = true
                    if (currentServer == null || e.player.server == null) {
                        e.player.kick(p.getBannedMessage().complete())
                    } else if (e.player.server?.info != currentServer) {
                        e.player.plsConnect(currentServer, e.target)
                        e.player.sendDelayed(2000, p.getBannedMessage().complete())
                    } else {
                        SpicyAzisaBan.debug("CheckBanListener - else branch (server: ${e.player.server?.info?.name}, server before check: $currentServer)", 2)
                        e.player.plsConnect(currentServer, e.target)
                        e.player.send(p.getBannedMessage().complete())
                    }
                }
                context.resolve(true)
            }, "SpicyAzisaBan - CheckBanListener Thread").start()
        }.thenDo { res ->
            if (!res) {
                SpicyAzisaBan.instance.logger.warning("Could not check punishments for ${e.player.uniqueId} (Timed out)")
                if (SABConfig.database.failsafe) {
                    e.isCancelled = true
                    if (currentServer == null || e.player.server == null) {
                        e.player.kick(SABMessages.General.error.replaceVariables().translate())
                    } else if (e.player.server?.info != currentServer) {
                        e.player.plsConnect(currentServer, e.target)
                        e.player.sendDelayed(2000, SABMessages.General.error.replaceVariables().translate())
                    } else {
                        e.player.send(SABMessages.General.error.replaceVariables().translate())
                    }
                }
            }
        }.catch {
            SpicyAzisaBan.instance.logger.warning("Could not check punishments for ${e.player.uniqueId}")
            it.printStackTrace()
            if (SABConfig.database.failsafe) {
                e.isCancelled = true
                if (currentServer == null || e.player.server == null) {
                    e.player.kick(SABMessages.General.error.replaceVariables().translate())
                } else if (e.player.server?.info != currentServer) {
                    e.player.plsConnect(currentServer, e.target)
                    e.player.sendDelayed(2000, SABMessages.General.error.replaceVariables().translate())
                } else {
                    SpicyAzisaBan.debug("CheckBanListener - else branch (server: ${e.player.server?.info?.name}, server before check: $currentServer)", 2)
                    e.player.send(SABMessages.General.error.replaceVariables().translate())
                }
            }
        }
    }

    private fun ProxiedPlayer.plsConnect(server: ServerInfo, tryAgainIf: ServerInfo? = null) {
        connect(server)
        if (tryAgainIf != null) {
            ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
                if (this.server.info == tryAgainIf) {
                    connect(server)
                }
            }, 1, TimeUnit.SECONDS)
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
