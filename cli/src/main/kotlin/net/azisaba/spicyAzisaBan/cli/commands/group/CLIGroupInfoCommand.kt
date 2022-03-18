package net.azisaba.spicyAzisaBan.cli.commands.group

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.cli.commands.CLIGroupCommand
import net.azisaba.spicyAzisaBan.common.ChatColor
import net.azisaba.spicyAzisaBan.util.Util.send
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLIGroupInfoCommand: Subcommand("info", "Shows some information about the group") {
    private val group by argument(ArgType.String, "group", "Group name")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        if (CLIGroupCommand.validateGroupName(CLIActor, group)) exitProcess(1)
        val servers = SpicyAzisaBan.instance.connection.getServersByGroup(group).complete()
        CLIActor.send("${SpicyAzisaBan.PREFIX}${ChatColor.AQUA}グループ: ${ChatColor.RESET}$group")
        CLIActor.send("${SpicyAzisaBan.PREFIX}- ${ChatColor.AQUA}サーバー:")
        servers.forEach { server ->
            CLIActor.send("${SpicyAzisaBan.PREFIX}   - ${ChatColor.GREEN}$server")
        }
        exitProcess(0)
    }
}
