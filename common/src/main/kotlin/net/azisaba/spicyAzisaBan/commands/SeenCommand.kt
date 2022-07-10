package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.ChatColor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.concat
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.isValidIPAddress
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.sendOrSuppressErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.toUUIDOrNull
import net.azisaba.spicyAzisaBan.util.Util.translate
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import java.net.InetAddress

object SeenCommand: Command() {
    override val name = "${SABConfig.prefix}seen"
    override val permission = "sab.seen"
    private val availableArguments = listOf(listOf("--ambiguous"))

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.seen")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) {
            return actor.send(SABMessages.Commands.Seen.usage.replaceVariables().translate())
        }
        val list = args.toMutableList()
        val ambiguous = list.remove("--ambiguous")
        val target = list[0]
        doSeen(actor, target, ambiguous)
    }

    fun doSeen(actor: Actor, target: String, ambiguous: Boolean): Promise<Unit> =
        async<Unit> { context ->
            actor.send(SABMessages.Commands.Seen.searching.replaceVariables().translate())
            if (target.isValidIPAddress()) {
                val pd = PlayerData.getAllByIP(target).catch { actor.sendErrorMessage(it) }.complete()
                if (pd == null || pd.isEmpty()) {
                    actor.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@async context.resolve()
                }
                val hostname = InetAddress.getByName(target).hostName
                // * = the player has different ip currently
                val players = pd.joinToString(", ") { if (it.ip != target) "${it.name}*" else it.name }
                actor.send(
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
                var pd = PlayerData.getByName(target, ambiguous).catch { actor.sendErrorMessage(it) }.complete()
                if (pd == null && target.toUUIDOrNull() != null) {
                    pd = PlayerData.getByUUID(target).catch { actor.sendOrSuppressErrorMessage<IllegalStateException>(it) }.complete()
                }
                if (pd == null) {
                    actor.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@async context.resolve()
                }
                val since = Util.unProcessTime(System.currentTimeMillis() - pd.lastSeen)
                val status = if (SpicyAzisaBan.instance.getPlayer(pd.uniqueId) == null) {
                    SABMessages.General.offline.translate()
                } else {
                    SABMessages.General.online.translate()
                }
                val ipd = pd.ip
                    ?.let { PlayerData.getAllByIP(it).catch { e -> actor.sendErrorMessage(e) }.complete() }
                    ?.filter { pd2 -> pd2.uniqueId != pd.uniqueId }
                val iPlayers = ipd?.joinToString("${ChatColor.WHITE}, ${ChatColor.GOLD}") {
                    var prefix = ""
                    if (SpicyAzisaBan.instance.getPlayer(it.uniqueId)?.isOnline() == true) prefix += "${ChatColor.GREEN}"
                    var suffix = "${ChatColor.GOLD}"
                    if (it.ip != pd.ip) suffix += "*"
                    "$prefix${it.name}$suffix"
                }.toString()
                actor.send(
                    SABMessages.Commands.Seen.layout
                        .replaceVariables(
                            "player" to pd.name,
                            "uuid" to pd.uuid.toString(),
                            "since" to since,
                            "status" to status,
                            "ip" to pd.ip.toString(),
                            "hostname" to pd.ip?.let { InetAddress.getByName(pd.ip).hostName }.toString(),
                            "name_history" to pd.getUsernameHistory().complete().distinct().joinToString(", "),
                            "ip_history" to pd.getIPAddressHistory().complete().distinct().joinToString(", "),
                            "same_ip_players" to iPlayers,
                            "same_ip_players_count" to ipd?.size.toString(),
                            "first_login" to (pd.firstLogin?.let { SABMessages.formatDate(it) } ?: "null"),
                            "first_login_attempt" to (pd.firstLoginAttempt?.let { SABMessages.formatDate(it) } ?: "null"),
                            "last_login" to (pd.lastLogin?.let { SABMessages.formatDate(it) } ?: "null"),
                            "last_login_attempt" to (pd.lastLoginAttempt?.let { SABMessages.formatDate(it) } ?: "null"),
                        )
                        .translate()
                )
            }
            context.resolve()
        }.catch { actor.sendErrorMessage(it) }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.seen")) return emptyList()
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
