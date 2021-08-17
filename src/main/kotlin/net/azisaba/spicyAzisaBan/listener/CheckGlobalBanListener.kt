package net.azisaba.spicyAzisaBan.listener

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.util.Util.getIPAddress
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import java.util.concurrent.TimeUnit

object CheckGlobalBanListener: Listener {
    @EventHandler
    fun onLogin(e: LoginEvent) {
        val res = Promise.create<Boolean> { context ->
            ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
                context.resolve(false)
            }, 3, TimeUnit.SECONDS)
            val p = Punishment.canJoinServer(e.connection.uniqueId, e.connection.socketAddress.getIPAddress(), "global").complete()
            if (p != null) {
                e.isCancelled = true
                e.setCancelReason(*TextComponent.fromLegacyText(p.getBannedMessage().complete()))
            }
            context.resolve(true)
        }.catch {
            SpicyAzisaBan.instance.logger.warning("Could not check punishments for ${e.connection.uniqueId}")
            it.printStackTrace()
            if (SABConfig.database.failsafe) {
                e.isCancelled = true
                e.setCancelReason(*TextComponent.fromLegacyText(SABMessages.General.error.replaceVariables().translate()))
            }
        }.complete()
        if (!res) {
            SpicyAzisaBan.instance.logger.warning("Could not check punishments for ${e.connection.uniqueId} (Timed out)")
            if (SABConfig.database.failsafe) {
                e.isCancelled = true
                e.setCancelReason(*TextComponent.fromLegacyText(SABMessages.General.error.replaceVariables().translate()))
            }
        }
    }
}
