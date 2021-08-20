package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.util.Util.concat
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor

object WhereCommand: Command("${SABConfig.prefix}where", null), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.where")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return sender.send(SABMessages.Commands.Warning.usage.replaceVariables().translate())
        val player = ProxyServer.getInstance().getPlayer(args[0])
            ?: return sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
        val server = player.server?.info?.name.toString()
        sender.send(SABMessages.Commands.Where.result
            .replaceVariables("player" to player.name, "server" to server)
            .translate())
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (!sender.hasPermission("sab.where")) return emptyList()
        if (args.size == 1) return ProxyServer.getInstance().players
            .filterIndexed { i, _ -> i < 500 }
            .map { it.name }
            .concat(Punishment.recentPunishedPlayers.map { it.name })
            .distinct()
            .filtr(args.last())
        return emptyList()
    }
}
