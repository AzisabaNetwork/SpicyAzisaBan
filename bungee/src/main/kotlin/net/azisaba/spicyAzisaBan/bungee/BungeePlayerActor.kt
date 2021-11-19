package net.azisaba.spicyAzisaBan.bungee

import net.azisaba.spicyAzisaBan.bungee.util.BungeeUtil.toBungee
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.title.Title
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.net.SocketAddress
import java.util.UUID

class BungeePlayerActor(private val player: ProxiedPlayer): BungeeActor(player), PlayerActor {
    override val uniqueId: UUID = player.uniqueId

    override fun getServer(): ServerInfo? = player.server?.info?.let { ServerInfo(it.name, it.socketAddress) }

    override fun disconnect(reason: Component) {
        player.disconnect(reason.toBungee())
    }

    override fun disconnect(vararg reason: Component) {
        player.disconnect(*reason.toBungee())
    }

    override fun getRemoteAddress(): SocketAddress = player.socketAddress

    override fun connect(server: ServerInfo) {
        val definedServer = ProxyServer.getInstance().getServerInfo(server.name)
            ?: throw IllegalArgumentException("Server ${server.name} is not defined")
        player.connect(definedServer)
    }

    override fun sendTitle(title: Title) {
        val bungee = ProxyServer.getInstance().createTitle()
        bungee.title(*title.title.toBungee())
        bungee.subTitle(*title.subTitle.toBungee())
        bungee.fadeIn(title.fadeIn)
        bungee.stay(title.stay)
        bungee.fadeOut(title.fadeOut)
        player.sendTitle(bungee)
    }

    override fun clearTitle() {
        sendTitle(Title(arrayOf(Component.text(" ")), emptyArray(), 0, 1, 0))
    }

    override fun isOnline(): Boolean = player.isConnected
}
