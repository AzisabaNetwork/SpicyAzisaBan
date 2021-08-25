package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.concat
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.isValidIPAddress
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.sendOrSuppressErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.toUUID
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
    private val availableArguments = listOf(listOf("--ambiguous"))

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.seen")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) {
            return sender.send(SABMessages.Commands.Seen.usage.replaceVariables().translate())
        }
        val list = args.toMutableList()
        val ambiguous = list.remove("--ambiguous")
        val target = list[0]
        Promise.create<Unit> { context ->
            sender.send(SABMessages.Commands.Seen.searching.replaceVariables().translate())
            if (target.isValidIPAddress()) {
                val pd = PlayerData.getAllByIP(target).catch { sender.sendErrorMessage(it) }.complete()
                if (pd == null || pd.isEmpty()) {
                    sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@create context.resolve()
                }
                val hostname = InetAddress.getByName(target).hostName
                // * = the player has different ip currently
                val players = pd.joinToString(", ") { if (it.ip != list[0]) "${it.name}*" else it.name }
                sender.send(
                    SABMessages.Commands.Seen.layoutIP
                        .replaceVariables(
                            "hostname" to hostname,
                            "ip_address" to target,
                            "players_count" to pd.size.toString(),
                            "players" to players,
                        )
                        .translate()
                )
            } else {
                var pd = PlayerData.getByName(target, ambiguous).catch { sender.sendErrorMessage(it) }.complete()
                if (pd == null && target.toUUID() != null) {
                    pd = PlayerData.getByUUID(target).catch { sender.sendOrSuppressErrorMessage<IllegalStateException>(it) }.complete()
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
                val ipd = pd.ip
                    ?.let { PlayerData.getAllByIP(it).catch { e -> sender.sendErrorMessage(e) }.complete() }
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
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) {
            return availableArguments
                .concat(listOf(Util.getPlayerNamesWithRecentPunishedPlayers()))
                .filterArgKeys(args)
                .map { if (!it.startsWith("-") && s.startsWith("%")) "%$it%" else it }
                .filtr(s)
        }
        return emptyList()
    }
}
