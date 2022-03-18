package net.azisaba.spicyAzisaBan.cli.commands

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.cli.util.LongArgType
import net.azisaba.spicyAzisaBan.commands.AddProofCommand
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLIAddProofCommand: Subcommand("add-proof", "Add a proof to a punishment") {
    private val id by argument(LongArgType, "id", "Punishment ID")
    private val text by argument(ArgType.String, "text", "Proof text")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        AddProofCommand.execute(CLIActor, id, text)
        exitProcess(0)
    }
}
