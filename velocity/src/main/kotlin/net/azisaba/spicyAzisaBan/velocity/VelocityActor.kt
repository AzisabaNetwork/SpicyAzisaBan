package net.azisaba.spicyAzisaBan.velocity

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.Player
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.velocity.util.VelocityUtil.toVelocity
import net.kyori.adventure.text.TextComponent
import java.util.UUID

open class VelocityActor(val source: CommandSource): Actor {
    companion object {
        @JvmStatic
        fun of(source: CommandSource) =
            when (source) {
                is Player -> VelocityPlayerActor(source)
                else -> VelocityActor(source)
            }
    }

    override val name: String
        get() = when (source) {
            is ConsoleCommandSource -> "CONSOLE"
            is Player -> source.username
            else -> "<Unknown [${source::class.java.simpleName}]>"
        }

    override val uniqueId = UUID(0, 0)

    override fun sendMessage(component: Component) {
        source.sendMessage(component.toVelocity())
    }

    override fun sendMessage(vararg components: Component) {
        source.sendMessage(TextComponent.ofChildren(*components.toVelocity()))
    }

    override fun hasPermission(permission: String): Boolean = source.hasPermission(permission)
}
