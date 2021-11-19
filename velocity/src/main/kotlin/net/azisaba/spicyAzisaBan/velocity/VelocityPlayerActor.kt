package net.azisaba.spicyAzisaBan.velocity

import com.velocitypowered.api.proxy.Player
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.title.Title
import net.azisaba.spicyAzisaBan.velocity.util.VelocityUtil.toVelocity
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.util.Ticks
import java.net.SocketAddress
import java.util.UUID
import java.util.concurrent.CompletionException
import net.kyori.adventure.title.Title as ATitle

class VelocityPlayerActor(private val player: Player): VelocityActor(player), PlayerActor {
    override val name: String = player.username
    override val uniqueId: UUID = player.uniqueId

    override fun getServer(): ServerInfo? =
        player.currentServer
            .map { it.serverInfo }
            .map { ServerInfo(it.name, it.address) }
            .orElse(null)

    override fun disconnect(reason: Component) {
        return player.disconnect(reason.toVelocity())
    }

    override fun disconnect(vararg reason: Component) {
        return player.disconnect(TextComponent.ofChildren(*reason.toVelocity()))
    }

    override fun getRemoteAddress(): SocketAddress = player.remoteAddress

    override fun connect(server: ServerInfo) {
        val definedServer = VelocityPlugin.instance.server.getServer(server.name)
            .orElseThrow { IllegalArgumentException("Server ${server.name} is not defined") }
        try {
            player.createConnectionRequest(definedServer).connectWithIndication().join()
        } catch (e: CompletionException) {
            // i guess i'll just ignore
        }
    }

    override fun sendTitle(title: Title) {
        val adventure = ATitle.title(
            TextComponent.ofChildren(*title.title.toVelocity()),
            TextComponent.ofChildren(*title.subTitle.toVelocity()),
            ATitle.Times.of(
                Ticks.duration(title.fadeIn.toLong()),
                Ticks.duration(title.stay.toLong()),
                Ticks.duration(title.fadeOut.toLong())
            ),
        )
        player.showTitle(adventure)
    }

    override fun clearTitle() {
        player.clearTitle()
    }

    override fun isOnline(): Boolean = player.isActive
}
