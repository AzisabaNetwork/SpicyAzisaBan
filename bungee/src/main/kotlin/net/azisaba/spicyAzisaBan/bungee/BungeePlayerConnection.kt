package net.azisaba.spicyAzisaBan.bungee

import net.azisaba.spicyAzisaBan.common.PlayerConnection
import net.md_5.bungee.api.connection.PendingConnection
import java.net.SocketAddress
import java.util.UUID

class BungeePlayerConnection(private val connection: PendingConnection): PlayerConnection {
    override val name: String? = connection.name

    override val uniqueId: UUID = connection.uniqueId

    override fun getRemoteAddress(): SocketAddress = connection.socketAddress
}