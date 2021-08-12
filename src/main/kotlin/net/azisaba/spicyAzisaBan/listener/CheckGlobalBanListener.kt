package net.azisaba.spicyAzisaBan.listener

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
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
        val uuid = e.connection.uniqueId
        var done = false
        Promise.create<Unit> { context ->
            ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
                if (!done) context.reject(Exception())
            }, 3, TimeUnit.SECONDS)
            Punishment.fetchActivePunishmentsByTarget(uuid.toString(), "global").thenDo {
                // possible punishments:
                // BAN
                // TEMP_BAN
                // IP_BAN
                // TEMP_IP_BAN
                // TODO: check temp bans (with expiration date)
                val p = it.find { p -> p.type == PunishmentType.BAN }
                if (p != null) {
                    e.isCancelled = true
                    e.setCancelReason(*TextComponent.fromLegacyText(SABMessages.Commands.GBan.layout.replaceVariables(p.getVariables().complete()).translate()))
                }
            }.thenDo { done = true }.complete()
            context.resolve()
        }.catch {
            SpicyAzisaBan.instance.logger.warning("Could not check punishments for ${e.connection.uniqueId}")
            if (SABConfig.database.failsafe) {
                e.isCancelled = true
                e.setCancelReason(*TextComponent.fromLegacyText(SABMessages.General.error.replaceVariables().translate()))
            }
        }.complete()
    }
}
