package net.azisaba.spicyAzisaBan.common

import java.net.SocketAddress
import java.util.UUID

interface PlayerConnection {
    val name: String?
    val uniqueId: UUID

    /**
     * Get a remote address for a player. For BungeeCord, the result might not be InetSocketAddress. For Velocity,
     * the result is always InetSocketAddress.
     * @return remote address of player
     */
    fun getRemoteAddress(): SocketAddress
}
