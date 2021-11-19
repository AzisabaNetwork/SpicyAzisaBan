package net.azisaba.spicyAzisaBan.bungee.command

import net.azisaba.spicyAzisaBan.bungee.BungeeActor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.TabExecutor
import net.md_5.bungee.api.plugin.Command as BCommand

class BungeeCommand(private val command: Command): BCommand(command.name, command.permission, *command.aliases), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        command.execute(BungeeActor.of(sender), args)
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        return command.onTabComplete(BungeeActor.of(sender), args)
    }
}
