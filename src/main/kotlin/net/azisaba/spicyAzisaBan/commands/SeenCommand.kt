package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.concat
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.isValidIPAddress
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import java.net.InetAddress

object SeenCommand: Command("${SABConfig.prefix}seen"), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.seen")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        Promise.create<Unit> { context ->
            sender.send(SABMessages.Commands.Seen.searching.replaceVariables().translate())
            if (args[0].isValidIPAddress()) {
                val pd = PlayerData.getByIP(args[0]).catch { sender.sendErrorMessage(it) }.complete()
                if (pd == null || pd.isEmpty()) {
                    sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@create context.resolve()
                }
                sender.send(
                    SABMessages.Commands.Seen.layoutIP
                        .replaceVariables(
                            "players_count" to pd.size.toString(),
                            "players" to pd.joinToString(", ") { it.name },
                        )
                        .translate()
                )
            } else {
                var pd = PlayerData.getByName(args[0]).catch { sender.sendErrorMessage(it) }.complete()
                if (pd == null) {
                    pd = PlayerData.getByUUID(args[0]).catch { sender.sendErrorMessage(it) }.complete()
                }
                if (pd == null) {
                    sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@create context.resolve()
                }
                val since = Util.unProcessTime(System.currentTimeMillis() - pd.lastSeen)
                val status = if (ProxyServer.getInstance().getPlayer(pd.uniqueId) == null) {
                    SABMessages.General.offline.translate()
                } else {
                    SABMessages.General.online.translate()
                }
                sender.send(
                    SABMessages.Commands.Seen.layout
                        .replaceVariables(
                            "player" to pd.name,
                            "since" to since,
                            "status" to status,
                            "ip" to pd.ip.toString(),
                            "hostname" to pd.ip?.let { InetAddress.getByName(pd.ip).hostName }.toString(),
                            "name_history" to pd.getUsernameHistory().complete().distinct().joinToString(", "),
                            "ip_history" to pd.getIPAddressHistory().complete().distinct().joinToString(", "),
                        )
                        .translate()
                )
            }
            context.resolve()
        }.catch { sender.sendErrorMessage(it) }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.size == 1) return ProxyServer.getInstance().players
            .filterIndexed { i, _ -> i < 500 }
            .map { it.name }
            .concat(Punishment.recentPunishedPlayers.map { it.name })
            .distinct()
            .filtr(args.last())
        return emptyList()
    }
}