package net.azisaba.spicyAzisaBan.cli.actor

import net.azisaba.spicyAzisaBan.cli.util.SimpleComponent
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.title.Title
import util.UUIDUtil
import java.net.SocketAddress
import java.util.UUID

object CLIActor: PlayerActor, Actor {
    override fun getServer(): ServerInfo? = null

    override fun disconnect(reason: Component) = throw AssertionError("Cannot disconnect CLIActor")

    override fun disconnect(vararg reason: Component) = throw AssertionError("Cannot disconnect CLIActor")

    override fun connect(server: ServerInfo) = throw AssertionError("Cannot connect CLIActor to server")

    override fun sendTitle(title: Title) {}

    override fun clearTitle() {}

    override fun isOnline(): Boolean = true

    override val name: String = "CONSOLE"
    override val uniqueId: UUID = UUIDUtil.NIL

    override fun sendMessage(component: Component) {
        println((component as SimpleComponent).getText())
    }

    override fun sendMessage(vararg components: Component) {
        sendMessage(SimpleComponent("").apply { addChildren(*components.map { it as SimpleComponent }.toTypedArray()) })
    }

    override fun hasPermission(permission: String): Boolean = true // all permissions granted

    override fun getRemoteAddress(): SocketAddress = throw AssertionError("Cannot get socket address of CLIActor")
}
