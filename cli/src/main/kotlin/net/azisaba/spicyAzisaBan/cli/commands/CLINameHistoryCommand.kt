package net.azisaba.spicyAzisaBan.cli.commands

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.commands.NameHistoryCommand
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLINameHistoryCommand: Subcommand("name-history", "Show name history of a player") {
    private val player by argument(ArgType.String, "player", "Player name")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        NameHistoryCommand.execute(CLIActor, arrayOf(player))
        exitProcess(0)
    }
}
