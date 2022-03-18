package net.azisaba.spicyAzisaBan.cli.commands.group

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.commands.SABCommand
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLIGroupDeleteCommand: Subcommand("delete", "Delete a group") {
    private val groupName by argument(ArgType.String, "groupName", "Group name")
    private val confirm by option(ArgType.Boolean, "confirm")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        SABCommand.executeDeleteGroup(CLIActor, groupName, confirm ?: false).complete()
        exitProcess(0)
    }
}
