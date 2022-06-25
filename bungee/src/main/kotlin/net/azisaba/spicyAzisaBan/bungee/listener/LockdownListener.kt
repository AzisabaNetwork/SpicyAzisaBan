package net.azisaba.spicyAzisaBan.bungee.listener

import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.PreLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

object LockdownListener : Listener {
    @EventHandler
    fun onLogin(e: PreLoginEvent) {
        e.isCancelled = true
        e.setCancelReason(*TextComponent.fromLegacyText("現在サーバーには参加できません。サーバー管理者へお問い合わせください。 &8(SAB: Initialization error)"))
    }
}
