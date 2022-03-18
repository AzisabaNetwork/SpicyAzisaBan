package net.azisaba.spicyAzisaBan.cli.commands

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.cli.util.LongArgType
import net.azisaba.spicyAzisaBan.commands.UnPunishCommand
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLIUnpunishCommand: Subcommand("unpunish", "Removes a punishment") {
    private val id by argument(LongArgType, "id", "Punishment ID")
    private val reason by argument(ArgType.String, "reason", "Reason for removing a punishment")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        UnPunishCommand.doUnPunish(CLIActor, id, ReasonContext(reason))
        exitProcess(0)
    }
}
