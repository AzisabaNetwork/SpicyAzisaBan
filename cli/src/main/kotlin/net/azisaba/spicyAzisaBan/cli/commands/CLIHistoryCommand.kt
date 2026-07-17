package net.azisaba.spicyAzisaBan.cli.commands

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.commands.HistoryCommand
import kotlin.math.max
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLIHistoryCommand: Subcommand("history", "Shows the punishment history of a player or IP address") {
    private val target by argument(ArgType.String, "target", "Target player, UUID, or IP address")
    private val active by option(ArgType.Boolean, "active", "e", "Only show active punishments. Conflicts with --all option.")
    private val all by option(ArgType.Boolean, "all", "a", "Show all punishments. Conflicts with --active option.")
    private val page by option(ArgType.Int, "page", "p", "# of page to show")
    private val ip by option(ArgType.Boolean, "ip", "i", "Search by the target player's IP address")
    private val only by option(ArgType.Boolean, "only", "o", "Only search the exact target")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        HistoryCommand.execute(
            CLIActor,
            target,
            active ?: false,
            all ?: false,
            max(1, page ?: 1),
            ip ?: false,
            only ?: false,
        ).complete()
        exitProcess(0)
    }
}
