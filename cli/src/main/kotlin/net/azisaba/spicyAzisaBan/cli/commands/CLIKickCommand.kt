package net.azisaba.spicyAzisaBan.cli.commands

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.cli.util.CLIUtil
import net.azisaba.spicyAzisaBan.commands.KickCommand
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLIKickCommand: Subcommand("kick", "Kick a player from server") {
    private val player by argument(ArgType.String, "player", "Target player")
    private val reason by argument(ArgType.String, "reason", "Reason for kick")
    private val server by option(ArgType.String, "server", "s", "Server to apply for")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        KickCommand.doKick(
            CLIActor,
            CLIUtil.genericArgumentParser.parse("player=$player")
                .get(Contexts.PLAYER, CLIActor)
                .complete()
                .apply { if (!isSuccess) exitProcess(1) },
            CLIUtil.genericArgumentParser.parse("server=${server ?: "global"}")
                .get(Contexts.SERVER_NO_PERM_CHECK, CLIActor)
                .complete()
                .apply { if (!isSuccess) exitProcess(1) },
            ReasonContext(reason),
        )
        exitProcess(0)
    }
}
