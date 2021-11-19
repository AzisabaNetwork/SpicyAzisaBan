package net.azisaba.spicyAzisaBan.bungee

import net.azisaba.spicyAzisaBan.bungee.util.BungeeUtil.toBungee
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.UUID

open class BungeeActor(val sender: CommandSender): Actor {
    companion object {
        @JvmStatic
        fun of(sender: CommandSender) =
            when (sender) {
                is ProxiedPlayer -> BungeePlayerActor(sender)
                else -> BungeeActor(sender)
            }
    }

    override val name: String = sender.name
    override val uniqueId = UUID(0, 0)

    override fun sendMessage(component: Component) {
        sender.sendMessage(component.toBungee())
    }

    override fun sendMessage(vararg components: Component) {
        sender.sendMessage(*components.toBungee())
    }

    override fun hasPermission(permission: String): Boolean = sender.hasPermission(permission)
}
