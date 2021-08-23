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
import net.md_5.bungee.api.ChatColor
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
        if (args.isEmpty()) {
            return sender.send(SABMessages.Commands.Seen.usage.replaceVariables().translate())
        }
        Promise.create<Unit> { context ->
            sender.send(SABMessages.Commands.Seen.searching.replaceVariables().translate())
            if (args[0].isValidIPAddress()) {
                val pd = PlayerData.getAllByIP(args[0]).catch { sender.sendErrorMessage(it) }.complete()
                if (pd == null || pd.isEmpty()) {
                    sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@create context.resolve()
                }
                val hostname = InetAddress.getByName(args[0]).hostName
                // * = the player has different ip currently
                val players = pd.joinToString(", ") { if (it.ip != args[0]) "${it.name}*" else it.name }
                sender.send(
                    SABMessages.Commands.Seen.layoutIP
                        .replaceVariables(
                            "hostname" to hostname,
                            "ip_address" to args[0],
                            "players_count" to pd.size.toString(),
                            "players" to players,
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
                val ipd = pd.ip?.let { PlayerData.getAllByIP(it).catch { e -> sender.sendErrorMessage(e) }.complete() }
                    ?.filter { pd2 -> pd2.uniqueId != pd.uniqueId }
                val iPlayers = ipd?.joinToString("${ChatColor.WHITE}, ${ChatColor.GOLD}") {
                    var prefix = ""
                    if (ProxyServer.getInstance().getPlayer(it.uniqueId)?.isConnected == true) prefix += "${ChatColor.GREEN}"
                    var suffix = "${ChatColor.GOLD}"
                    if (it.ip != pd.ip) suffix += "*"
                    "$prefix${it.name}$suffix"
                }.toString()
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
                            "same_ip_players" to iPlayers,
                            "same_ip_players_count" to ipd?.size.toString(),
                        )
                        .translate()
                )
            }
            context.resolve()
        }.catch { sender.sendErrorMessage(it) }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (!sender.hasPermission("sab.seen")) return emptyList()
        if (args.size == 1) return ProxyServer.getInstance().players
            .filterIndexed { i, _ -> i < 500 }
            .map { it.name }
            .concat(Punishment.recentPunishedPlayers.map { it.name })
            .distinct()
            .filtr(args.last())
        return emptyList()
    }
}