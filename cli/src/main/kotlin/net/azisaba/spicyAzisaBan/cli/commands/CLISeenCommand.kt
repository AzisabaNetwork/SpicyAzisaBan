package net.azisaba.spicyAzisaBan.cli.commands

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.commands.SeenCommand
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLISeenCommand: Subcommand("seen", "Shows the IP address, alt accounts and more information.") {
    private val target by argument(ArgType.String, "target", "Target player/IP address to search")
    private val ambiguous by option(ArgType.Boolean, "ambiguous", "a", "Insert % at the head and tail of the query")
    private val includeDummy by option(ArgType.Boolean, "include-dummy", "d", "Include 'dummy' players in result")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        SeenCommand.doSeen(CLIActor, target, ambiguous ?: false, includeDummy ?: false).complete()
        exitProcess(0)
    }
}
