package net.azisaba.spicyAzisaBan.cli

import net.azisaba.spicyAzisaBan.PlatformType
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.cli.util.SimpleComponent
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.common.scheduler.ScheduledTask
import java.io.File
import java.io.OutputStream
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.SimpleFormatter

class SpicyAzisaBanCLI: SpicyAzisaBan() {
    init {
        LOGGER.useParentHandlers = false
        LOGGER.addHandler(object : ConsoleHandler() {
            private val formatter = SimpleFormatter()

            override fun setOutputStream(out: OutputStream?) {
                super.setOutputStream(System.out)
            }

            override fun close() {
                flush()
            }

            override fun getFormatter(): Formatter = formatter
        })
    }

    override fun doEnable() {
        debugLevel = CLIMain.debugLevel.value ?: 0
        super.doEnable()
        shutdownTimer()
    }

    override fun getPlatformType(): PlatformType = PlatformType.CLI
    override fun getPluginName(): String = "SpicyAzisaBan"
    override fun getServerName(): String = "cli"
    override fun getServerVersion(): String = ""
    override fun getServers(): Map<String, ServerInfo> = emptyMap()
    override fun getPlayers(): List<PlayerActor> = emptyList()
    override fun getPlayer(uuid: UUID): PlayerActor? = null

    override fun schedule(time: Long, unit: TimeUnit, runnable: () -> Unit): ScheduledTask {
        // silently discard schedule request
        return object : ScheduledTask(runnable) {
            override fun cancel() {}
        }
    }

    override fun createTextComponentFromLegacyText(legacyText: String): Array<Component> =
        arrayOf(SimpleComponent.fromLegacyText(legacyText))

    override fun createTextComponent(content: String): Component = SimpleComponent(content)
    override fun registerCommand(command: Command) {}
    override fun executeCommand(actor: Actor, command: String) = throw AssertionError("Cannot execute command on CLI")
    override fun getConsoleActor(): Actor = CLIActor
    override fun getDataFolder(): Path = File(".").toPath()
}