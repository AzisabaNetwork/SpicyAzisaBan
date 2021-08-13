package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.PlayerContext
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise

object MuteCommand: Command("mute"), TabExecutor {
    private val availableArguments = listOf("player=", "reason=\"\"", "server=", "--all")

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (sender !is ProxiedPlayer) return sender.send("${ChatColor.RED}This command cannot be used from console!")
        if (!sender.hasPermission(PunishmentType.MUTE.perm)) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return sender.send(SABMessages.Commands.Mute.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        Promise.create<Unit> { context ->
            if (!arguments.containsKey("server")) {
                val serverName = sender.server.info.name
                val group = SpicyAzisaBan.instance.connection.getGroupByServer(serverName).complete()
                arguments.parsedOptions["server"] = group ?: serverName
            }
            val player = arguments.get(Contexts.PLAYER, sender).complete().apply { if (!isSuccess) return@create context.resolve() }
            val server = arguments.get(Contexts.SERVER, sender).complete().apply { if (!isSuccess) return@create context.resolve() }
            val reason = arguments.get(Contexts.REASON, sender).complete()
            val p = Punishment
                .createByPlayer(player.profile, reason.text, sender.uniqueId, PunishmentType.MUTE, -1, server.name)
                .insert()
                .catch {
                    SpicyAzisaBan.instance.logger.warning("Something went wrong while handling /mute from ${sender.name}!")
                    sender.sendErrorMessage(it)
                }
                .complete() ?: return@create context.resolve()
            p.notifyToAll().complete()
            if (arguments.contains("all")) {
                p.applyToSameIPs(player.profile.uniqueId).catch { sender.sendErrorMessage(it) }.complete()
            }
            ProxyServer.getInstance().getPlayer(player.profile.uniqueId)?.send(SABMessages.Commands.Mute.layout1.replaceVariables(p.getVariables().complete()).translate())
            sender.send(SABMessages.Commands.Mute.done.replaceVariables(p.getVariables().complete()).translate())
            context.resolve()
        }.catch {
            sender.sendErrorMessage(it)
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("player=")) return PlayerContext.tabComplete(s)
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.MUTE, args, sender.getServerName())
        return emptyList()
    }
}
