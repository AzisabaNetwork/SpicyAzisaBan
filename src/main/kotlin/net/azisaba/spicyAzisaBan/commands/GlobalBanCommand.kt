package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.concat
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.toMinecraft
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.get
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.ArgumentParser
import util.promise.rewrite.Promise

object GlobalBanCommand: Command("gban"), TabExecutor {
    private val availableArguments = listOf("player=", "reason=\"\"", "server=", "-s")

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission(PunishmentType.BAN.perm)) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return sender.send(SABMessages.Commands.Gban.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        Promise.create<Unit> { context ->
            val player = arguments.get(Contexts.PLAYER, sender).complete().apply { if (!isSuccess) return@create }
            val server = arguments.get(Contexts.SERVER, sender).complete().apply { if (!isSuccess) return@create }
            val reason = arguments.get(Contexts.REASON, sender).complete()
            sender.send("Player: ${player.profile.name} (UUID: ${player.profile.uniqueId})")
            sender.send("Reason: ${reason.text}")
            sender.send("Server: ${server.name} (isGroup: ${server.isGroup.toMinecraft()}${ChatColor.RESET})")
            sender.send("Silent: ${arguments.contains("-s").toMinecraft()}")
            context.resolve()
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("player=")) {
            if (ProxyServer.getInstance().players.size > 500) return emptyList()
            return ProxyServer.getInstance().players.map { "player=${it.name}" }.filtr(s)
        } else if (s.startsWith("server=")) {
            return ProxyServer.getInstance()
                .servers
                .keys
                .concat(SpicyAzisaBan.instance.connection.getCachedGroups())
                .map { "server=${it}" }
                .filtr(s)
        }
        return emptyList()
    }
}
