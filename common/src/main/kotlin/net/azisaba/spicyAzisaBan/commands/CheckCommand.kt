package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.ChatColor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.isValidIPAddress
import net.azisaba.spicyAzisaBan.util.Util.plus
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.IPAddressContext
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.sql.options.FindOptions
import java.net.InetAddress

object CheckCommand: Command() {
    override val name = "${SABConfig.prefix}check"
    override val permission = "sab.check"
    private val availableArguments = listOf(listOf("target=", "id="), listOf("--ip", "-i"), listOf("--only", "-o"), listOf("server="), listOf("--no-group-lookup"))

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.check")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) {
            return actor.send(SABMessages.Commands.Check.usage.replaceVariables().translate())
        }
        val arguments = genericArgumentParser.parse(args.joinToString(" "))
        val target = arguments.getArgument("target") ?: arguments.unhandledArguments().getOrNull(0)
        val pid = arguments.getArgument("id")?.toLongOrNull()
        if (target.isNullOrBlank() && pid == null) {
            return actor.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
        }
        if (!target.isNullOrBlank() && pid != null) {
            return actor.send(SABMessages.Commands.Check.cannotUseTargetAndID.replaceVariables().translate())
        }
        if (pid != null) {
            Punishment.fetchPunishmentById(pid).thenDo { p ->
                if (p == null) {
                    return@thenDo actor.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().translate().format(pid))
                }
                actor.send(p.getHistoryMessage().complete())
            }
            return
        }
        if (target.isNullOrBlank()) {
            return actor.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
        }
        val only = arguments.containsUnhandledArgument("only") || arguments.containsShortArgument('o')
        val noGroupLookup = arguments.containsUnhandledArgument("no-group-lookup")
        val specifiedServer = arguments.containsArgumentKey("server")
        val whereServer = if (specifiedServer) " AND `server` = ?" else ""
        async<Unit> { context ->
            actor.send(SABMessages.Commands.Check.searching.replaceVariables().translate())
            val server = arguments.get(Contexts.SERVER_NO_PERM_CHECK, actor)
                .complete()
                .apply { if (!isSuccess) return@async }
                .name
            if (arguments.containsUnhandledArgument("ip") || arguments.containsShortArgument('i') || target.isValidIPAddress()) {
                val ip = if (target.isValidIPAddress()) {
                    target
                } else {
                    var pd = PlayerData.getByName(target).catch { actor.sendErrorMessage(it) }.complete()
                    if (pd == null) {
                        pd = PlayerData.getByUUID(target).catch { actor.sendErrorMessage(it) }.complete()
                    }
                    pd?.ip
                }
                if (ip == null) {
                    actor.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@async context.resolve()
                }
                val pd = PlayerData.getByIP(ip).catch { actor.sendErrorMessage(it) }.complete()
                if (pd == null || pd.isEmpty()) {
                    actor.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@async context.resolve()
                }
                val punishments = if (only) {
                    SpicyAzisaBan.instance.connection.punishmentHistory
                        .findAll(FindOptions.Builder().addWhere("target", ip).apply { if (specifiedServer) addWhere("server", server) }.build())
                        .then { list -> list.map { td -> Punishment.fromTableData(td) } }
                        .complete()
                } else {
                    var where = pd.joinToString(" OR ") { "`target` = ?" }
                    if (where.isNotBlank()) where = " OR $where"
                    val sql = "SELECT * FROM `punishments` WHERE `target` = ?$where$whereServer"
                    val start = System.currentTimeMillis()
                    val st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setString(1, ip)
                    pd.forEachIndexed { index, playerData ->
                        st.setString(index + 2, playerData.uniqueId.toString())
                    }
                    if (specifiedServer) st.setString(pd.size + 2, server)
                    val ps = mutableListOf<Punishment>()
                    val rs = st.executeQuery()
                    while (rs.next()) ps.add(Punishment.fromResultSet(rs))
                    st.close()
                    SQLConnection.logSql(sql, System.currentTimeMillis() - start)
                    ps
                }
                val banInfo = Punishment.canJoinServer(null, ip, server, noGroupLookup)
                    .then {
                        if (it == null) {
                            SABMessages.General.none.translate()
                        } else {
                            SABMessages.Commands.Check.muteInfo.replaceVariables(it.getVariables().complete()).translate()
                        }
                    }
                    .complete()
                val muteInfo = Punishment.canSpeak(null, ip, server, true, noGroupLookup)
                    .then {
                        if (it == null) {
                            SABMessages.General.none.translate()
                        } else {
                            SABMessages.Commands.Check.muteInfo.replaceVariables(it.getVariables().complete()).translate()
                        }
                    }
                    .complete()
                val muteCount = punishments.count { it.type.isMute() }
                val banCount = punishments.count { it.type.isBan() }
                val warningCount = punishments.count { it.type == PunishmentType.WARNING }
                val cautionCount = punishments.count { it.type == PunishmentType.CAUTION }
                val kickCount = punishments.count { it.type == PunishmentType.KICK }
                val noteCount = punishments.count { it.type == PunishmentType.NOTE }
                actor.send(
                    SABMessages.Commands.Check.layoutIP
                        .replaceVariables(
                            "ip" to ip,
                            "hostname" to InetAddress.getByName(ip).hostName,
                            "mute_count" to (if (muteCount == 0) ChatColor.GREEN else ChatColor.RED) + muteCount.toString(),
                            "ban_count" to (if (banCount == 0) ChatColor.GREEN else ChatColor.RED) + banCount.toString(),
                            "warning_count" to (if (warningCount == 0) ChatColor.GREEN else ChatColor.RED) + warningCount.toString(),
                            "caution_count" to (if (cautionCount == 0) ChatColor.GREEN else ChatColor.RED) + cautionCount.toString(),
                            "kick_count" to kickCount.toString(),
                            "note_count" to noteCount.toString(),
                            "ban_info" to banInfo,
                            "mute_info" to muteInfo,
                        )
                        .translate()
                )
            } else {
                var pd = PlayerData.getByName(target).catch { actor.sendErrorMessage(it) }.complete()
                if (pd == null) {
                    pd = PlayerData.getByUUID(target).catch { actor.sendErrorMessage(it) }.complete()
                }
                if (pd == null) {
                    actor.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@async context.resolve()
                }
                val punishments = if (arguments.containsUnhandledArgument("only") || arguments.containsShortArgument('o')) {
                    SpicyAzisaBan.instance.connection.punishmentHistory
                        .findAll(FindOptions.Builder().addWhere("target", pd.uniqueId.toString()).apply { if (specifiedServer) addWhere("server", server) }.build())
                        .then { list -> list.map { td -> Punishment.fromTableData(td) } }
                        .complete()
                } else {
                    val sql = "SELECT * FROM `punishmentHistory` WHERE `target` = ? OR `target` = ?$whereServer"
                    val start = System.currentTimeMillis()
                    val st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setString(1, pd.uniqueId.toString())
                    st.setString(2, pd.ip ?: pd.uniqueId.toString())
                    if (specifiedServer) st.setString(3, server)
                    val ps = mutableListOf<Punishment>()
                    val rs = st.executeQuery()
                    while (rs.next()) ps.add(Punishment.fromResultSet(rs))
                    st.close()
                    SQLConnection.logSql(sql, System.currentTimeMillis() - start)
                    ps
                }
                val banInfo = Punishment.canJoinServer(pd.uniqueId, if (only) null else pd.ip, server, noGroupLookup)
                    .then {
                        if (it == null) {
                            SABMessages.General.none.translate()
                        } else {
                            SABMessages.Commands.Check.banInfo.replaceVariables(it.getVariables().complete()).translate()
                        }
                    }
                    .complete()
                val muteInfo = Punishment.canSpeak(pd.uniqueId, if (only) null else pd.ip, server, true, noGroupLookup)
                    .then {
                        if (it == null) {
                            SABMessages.General.none.translate()
                        } else {
                            SABMessages.Commands.Check.muteInfo.replaceVariables(it.getVariables().complete()).translate()
                        }
                    }
                    .complete()
                val muteCount = punishments.count { it.type.isMute() }
                val banCount = punishments.count { it.type.isBan() }
                val warningCount = punishments.count { it.type == PunishmentType.WARNING }
                val cautionCount = punishments.count { it.type == PunishmentType.CAUTION }
                val kickCount = punishments.count { it.type == PunishmentType.KICK }
                val noteCount = punishments.count { it.type == PunishmentType.NOTE }
                actor.send(
                    SABMessages.Commands.Check.layout
                        .replaceVariables(
                            "name" to pd.name,
                            "uuid" to pd.uniqueId.toString(),
                            "ip" to pd.ip.toString(),
                            "hostname" to pd.ip?.let { InetAddress.getByName(it).hostName }.toString(),
                            "mute_count" to (if (muteCount == 0) ChatColor.GREEN else ChatColor.RED) + muteCount.toString(),
                            "ban_count" to (if (banCount == 0) ChatColor.GREEN else ChatColor.RED) + banCount.toString(),
                            "warning_count" to (if (warningCount == 0) ChatColor.GREEN else ChatColor.RED) + warningCount.toString(),
                            "caution_count" to (if (cautionCount == 0) ChatColor.GREEN else ChatColor.RED) + cautionCount.toString(),
                            "kick_count" to kickCount.toString(),
                            "note_count" to noteCount.toString(),
                            "ban_info" to banInfo,
                            "mute_info" to muteInfo,
                        )
                        .translate()
                )
            }
            context.resolve()
        }.catch { actor.sendErrorMessage(it) }
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.check")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("target=")) return IPAddressContext.tabComplete(s) // it says IPAddressContext but no
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        return emptyList()
    }
}
