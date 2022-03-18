package net.azisaba.spicyAzisaBan.cli.commands.group

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.commands.SABCommand
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLIGroupCreateCommand: Subcommand("create", "Create a group") {
    private val groupName by argument(ArgType.String, "groupName", "Group name")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        SABCommand.executeCreateGroup(CLIActor, groupName).complete()
        exitProcess(0)
    }
}
