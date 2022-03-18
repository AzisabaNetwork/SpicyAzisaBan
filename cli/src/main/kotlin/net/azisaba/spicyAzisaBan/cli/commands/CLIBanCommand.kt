package net.azisaba.spicyAzisaBan.cli.commands

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.commands.BanCommand
import net.azisaba.spicyAzisaBan.commands.TempBanCommand
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import util.ArgumentParser
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLIBanCommand: Subcommand("ban", "Ban a player") {
    private val player by argument(ArgType.String, "player", "Target player")
    private val reason by argument(ArgType.String, "reason", "Reason for ban")
    private val server by option(ArgType.String, "server", "s", "Server to apply for")
    private val all by option(ArgType.Boolean, "all", "a", "Apply same punishment for players in same IPs")
    private val time by option(ArgType.String, "time", "t", "Duration for ban")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        if (time == null) {
            BanCommand.doBan(
                CLIActor,
                ArgumentParser("player=$player")
                    .get(Contexts.PLAYER, CLIActor)
                    .complete()
                    .apply { if (!isSuccess) exitProcess(1) },
                ArgumentParser("server=${server ?: "global"}")
                    .get(Contexts.SERVER_NO_PERM_CHECK, CLIActor)
                    .complete()
                    .apply { if (!isSuccess) exitProcess(1) },
                ReasonContext(reason),
                all ?: false,
            )
        } else {
            TempBanCommand.doTempBan(
                CLIActor,
                ArgumentParser("player=$player")
                    .get(Contexts.PLAYER, CLIActor)
                    .complete()
                    .apply { if (!isSuccess) exitProcess(1) },
                ArgumentParser("server=${server ?: "global"}")
                    .get(Contexts.SERVER_NO_PERM_CHECK, CLIActor)
                    .complete()
                    .apply { if (!isSuccess) exitProcess(1) },
                ReasonContext(reason),
                ArgumentParser("time=\"$time\"")
                    .get(Contexts.TIME, CLIActor)
                    .complete()
                    .apply { if (!isSuccess) exitProcess(1) },
                all ?: false,
            )
        }
        exitProcess(0)
    }
}
