package net.azisaba.spicyAzisaBan.velocity.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object LockdownListener {
    @Subscribe
    fun onLogin(e: PreLoginEvent) {
        e.result = PreLoginEvent.PreLoginComponentResult.denied(
            Component.text("現在サーバーには参加できません。サーバー管理者へお問い合わせください。")
                .append(Component.text("(SAB: Initialization error)").color(NamedTextColor.DARK_GRAY))
        )
    }
}
