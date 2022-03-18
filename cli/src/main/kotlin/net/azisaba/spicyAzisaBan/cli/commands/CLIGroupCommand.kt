package net.azisaba.spicyAzisaBan.cli.commands

import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.cli.commands.group.CLIGroupCreateCommand
import net.azisaba.spicyAzisaBan.cli.commands.group.CLIGroupDeleteCommand
import net.azisaba.spicyAzisaBan.cli.commands.group.CLIGroupInfoCommand
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.translate
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLIGroupCommand: Subcommand("group", "Manages a group") {
    init {
        subcommands(
            CLIGroupCreateCommand,
            CLIGroupDeleteCommand,
            CLIGroupInfoCommand,
        )
    }

    override fun execute() {
        exitProcess(0)
    }

    /**
     * Validates the group name.
     * @return true if invalid; false otherwise
     */
    fun validateGroupName(actor: Actor, groupName: String): Boolean {
        if (!groupName.matches(SpicyAzisaBan.GROUP_PATTERN)) {
            actor.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
            return true
        }
        if (!SpicyAzisaBan.instance.connection.isGroupExists(groupName).complete()) {
            actor.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
            return true
        }
        return false
    }
}
