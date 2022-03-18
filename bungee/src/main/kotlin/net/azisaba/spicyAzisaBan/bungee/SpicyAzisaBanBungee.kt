package net.azisaba.spicyAzisaBan.bungee

import net.azisaba.spicyAzisaBan.PlatformType
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.bungee.command.BungeeCommand
import net.azisaba.spicyAzisaBan.bungee.util.BungeeUtil.toCommon
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.common.scheduler.ScheduledTask
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import java.io.File
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit

class SpicyAzisaBanBungee: SpicyAzisaBan() {
    private val server = ProxyServer.getInstance()

    override fun getPlatformType(): PlatformType = PlatformType.BUNGEECORD

    override fun getPluginName(): String = BungeePlugin.instance.description.name

    override fun getServerName(): String = server.name

    override fun getServerVersion(): String = server.version

    override fun getServers(): Map<String, ServerInfo> {
        return server.servers.mapValues { (_, info) -> ServerInfo(info.name, info.socketAddress) }
    }

    override fun getPlayers(): List<PlayerActor> = server.players.map { BungeePlayerActor(it) }

    override fun getPlayer(uuid: UUID): PlayerActor? = server.getPlayer(uuid)?.let { BungeePlayerActor(it) }

    override fun schedule(time: Long, unit: TimeUnit, runnable: () -> Unit): ScheduledTask {
        val task = server.scheduler.schedule(BungeePlugin.instance, runnable, time, unit)
        return object: ScheduledTask(runnable) {
            override fun cancel() {
                task.cancel()
            }
        }
    }

    override fun createTextComponentFromLegacyText(legacyText: String): Array<Component> {
        return TextComponent.fromLegacyText(legacyText).toCommon()
    }

    override fun createTextComponent(content: String): Component {
        return TextComponent(content).toCommon()
    }

    override fun registerCommand(command: Command) {
        server.pluginManager.registerCommand(BungeePlugin.instance, BungeeCommand(command))
    }

    override fun executeCommand(actor: Actor, command: String) {
        server.pluginManager.dispatchCommand((actor as BungeeActor).sender, command)
    }

    override fun getConsoleActor(): Actor = BungeeActor(server.console)

    override fun getDataFolder(): Path = File("./plugins/SpicyAzisaBan").toPath()
}
