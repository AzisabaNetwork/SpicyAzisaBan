package net.azisaba.spicyAzisaBan.listener

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.util.Util.getIPAddress
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ChatEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import java.util.concurrent.TimeUnit

object CheckMuteListener: Listener {
    @EventHandler
    fun onChat(e: ChatEvent) {
        if (e.sender !is ProxiedPlayer) return
        if (e.message.startsWith("/") && !SABConfig.blockedCommandsWhenMuted.any { s ->
                e.message == "/$s" || e.message.startsWith("/$s ") || e.message.matches("^/.*:$s(\$|\\s+)".toRegex())
        }) return
        val player = e.sender as ProxiedPlayer
        val res = Promise.create<Boolean> { context ->
            ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
                context.resolve(false)
            }, 3, TimeUnit.SECONDS)
            val p = Punishment.canSpeak(player.uniqueId, player.socketAddress.getIPAddress(), player.getServerName()).complete()
            if (p != null) {
                e.isCancelled = true
                player.send(p.getBannedMessage().complete())
            }
            context.resolve(true)
        }.catch {
            SpicyAzisaBan.instance.logger.warning("Could not check punishments for ${player.uniqueId}")
            it.printStackTrace()
            if (SABConfig.database.failsafe) {
                e.isCancelled = true
                player.send(SABMessages.General.error.replaceVariables().translate())
            }
        }.complete()
        if (!res) {
            SpicyAzisaBan.instance.logger.warning("Could not check punishments for ${player.uniqueId} (Timed out)")
            if (SABConfig.database.failsafe) {
                e.isCancelled = true
                player.send(SABMessages.General.error.replaceVariables().translate())
            }
        }
    }
}
