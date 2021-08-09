package net.azisaba.spicyAzisaBan.listener

import net.azisaba.spicyAzisaBan.util.Util
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

object PreloadPermissionsOnJoinListener: Listener {
    @EventHandler
    fun onJoin(e: PostLoginEvent) {
        Util.preloadPermissions(e.player)
    }
}
