package net.azisaba.spicyAzisaBan.velocity

import com.velocitypowered.api.proxy.ProxyServer
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.common.scheduler.ScheduledTask
import net.azisaba.spicyAzisaBan.velocity.command.VelocityCommand
import net.azisaba.spicyAzisaBan.velocity.util.VelocityComponent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.UUID
import java.util.concurrent.TimeUnit
import net.kyori.adventure.text.Component as KComponent

class SpicyAzisaBanVelocity(private val server: ProxyServer): SpicyAzisaBan() {
    override fun getPluginName() = "SpicyAzisaBan"

    override fun getServerName(): String = server.version.name

    override fun getServerVersion(): String = server.version.version

    override fun getServers(): Map<String, ServerInfo> {
        val map = mutableMapOf<String, ServerInfo>()
        server.allServers.forEach { server ->
            map[server.serverInfo.name] = ServerInfo(server.serverInfo.name, server.serverInfo.address)
        }
        return map
    }

    override fun getPlayers(): List<PlayerActor> = server.allPlayers.map { VelocityPlayerActor(it) }

    override fun getPlayer(uuid: UUID): PlayerActor? = server.getPlayer(uuid).map { VelocityPlayerActor(it) }.orElse(null)

    override fun schedule(time: Long, unit: TimeUnit, runnable: () -> Unit): ScheduledTask {
        val task = server.scheduler.buildTask(VelocityPlugin.instance, runnable).delay(time, unit).schedule()
        return object: ScheduledTask(runnable) {
            override fun cancel() {
                task.cancel()
            }
        }
    }

    override fun createTextComponentFromLegacyText(legacyText: String): Array<Component> {
        return arrayOf(VelocityComponent(
            LegacyComponentSerializer.builder()
                .character('\u00a7')
                .extractUrls()
                .build()
                .deserialize(legacyText)
        ))
    }

    override fun createTextComponent(content: String): Component {
        return VelocityComponent(KComponent.text(content))
    }

    override fun registerCommand(command: Command) {
        val meta = server.commandManager.metaBuilder(command.name)
            .aliases(*command.aliases)
            .build()
        server.commandManager.register(meta, VelocityCommand(command))
    }

    override fun executeCommand(actor: Actor, command: String) {
        server.commandManager.executeAsync((actor as VelocityActor).source, command)
    }

    override fun getConsoleActor(): Actor = VelocityActor(server.consoleCommandSource)
}
