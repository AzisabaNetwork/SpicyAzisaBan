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
    private val public by option(ArgType.Boolean, "public", description = "whether if proof is public (viewable by player who got punished) or not")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        AddProofCommand.execute(CLIActor, id, text, public ?: false)
        exitProcess(0)
    }
}
