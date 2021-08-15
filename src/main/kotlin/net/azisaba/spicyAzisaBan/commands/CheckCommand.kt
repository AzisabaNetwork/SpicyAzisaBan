package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.isValidIPAddress
import net.azisaba.spicyAzisaBan.util.Util.plus
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.IPAddressContext
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.options.FindOptions
import java.net.InetAddress

object CheckCommand: Command("${SABConfig.prefix}check"), TabExecutor {
    private val availableArguments = listOf(listOf("target="), listOf("--ip", "-i"), listOf("--only", "-o"))

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.check")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) {
            return sender.send(SABMessages.Commands.Check.usage.replaceVariables().translate())
        }
        val arguments = ArgumentParser(args.joinToString(" "))
        val target = arguments.getString("target")
        if (target.isNullOrBlank()) {
            return sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
        }
        Promise.create<Unit> { context ->
            sender.send(SABMessages.Commands.Check.searching.replaceVariables().translate())
            if (arguments.contains("ip") || arguments.contains("-i") || target.isValidIPAddress()) {
                val ip = if (target.isValidIPAddress()) {
                    target
                } else {
                    var pd = PlayerData.getByName(target).catch { sender.sendErrorMessage(it) }.complete()
                    if (pd == null) {
                        pd = PlayerData.getByUUID(target).catch { sender.sendErrorMessage(it) }.complete()
                    }
                    pd?.ip
                }
                if (ip == null) {
                    sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@create context.resolve()
                }
                val pd = PlayerData.getByIP(ip).catch { sender.sendErrorMessage(it) }.complete()
                if (pd == null || pd.isEmpty()) {
                    sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@create context.resolve()
                }
                val punishments = if (arguments.contains("only") || arguments.contains("-o")) {
                    SpicyAzisaBan.instance.connection.punishmentHistory
                        .findAll(FindOptions.Builder().addWhere("target", ip).build())
                        .then { list -> list.map { td -> Punishment.fromTableData(td) } }
                        .complete()
                } else {
                    var where = pd.joinToString(" OR ") { "`target` = ?" }
                    if (where.isNotBlank()) where = " OR $where"
                    val sql = "SELECT * FROM `punishmentHistory` WHERE `target` = ?$where"
                    SQLConnection.logSql(sql)
                    val st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, ip)
                    pd.forEachIndexed { index, playerData ->
                        st.setObject(index + 2, playerData.uniqueId.toString())
                    }
                    val ps = mutableListOf<Punishment>()
                    val rs = st.executeQuery()
                    while (rs.next()) ps.add(Punishment.fromResultSet(rs))
                    st.close()
                    ps
                }
                val muteCount = punishments.count { it.type.isMute() }
                val banCount = punishments.count { it.type.isBan() }
                val warningCount = punishments.count { it.type == PunishmentType.WARNING }
                val cautionCount = punishments.count { it.type == PunishmentType.CAUTION }
                val kickCount = punishments.count { it.type == PunishmentType.KICK }
                val noteCount = punishments.count { it.type == PunishmentType.NOTE }
                sender.send(
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
                        )
                        .translate()
                )
            } else {
                var pd = PlayerData.getByName(target).catch { sender.sendErrorMessage(it) }.complete()
                if (pd == null) {
                    pd = PlayerData.getByUUID(target).catch { sender.sendErrorMessage(it) }.complete()
                }
                if (pd == null) {
                    sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@create context.resolve()
                }
                val punishments = if (arguments.contains("only") || arguments.contains("-o")) {
                    SpicyAzisaBan.instance.connection.punishmentHistory
                        .findAll(FindOptions.Builder().addWhere("target", pd.uniqueId.toString()).build())
                        .then { list -> list.map { td -> Punishment.fromTableData(td) } }
                        .complete()
                } else {
                    val sql = "SELECT * FROM `punishmentHistory` WHERE `target` = ? OR `target` = ?"
                    SQLConnection.logSql(sql)
                    val st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, pd.uniqueId.toString())
                    st.setObject(2, pd.ip ?: pd.uniqueId.toString())
                    val ps = mutableListOf<Punishment>()
                    val rs = st.executeQuery()
                    while (rs.next()) ps.add(Punishment.fromResultSet(rs))
                    st.close()
                    ps
                }
                val muteCount = punishments.count { it.type.isMute() }
                val banCount = punishments.count { it.type.isBan() }
                val warningCount = punishments.count { it.type == PunishmentType.WARNING }
                val cautionCount = punishments.count { it.type == PunishmentType.CAUTION }
                val kickCount = punishments.count { it.type == PunishmentType.KICK }
                val noteCount = punishments.count { it.type == PunishmentType.NOTE }
                sender.send(
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
                        )
                        .translate()
                )
            }
            context.resolve()
        }.catch { sender.sendErrorMessage(it) }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("target=")) return IPAddressContext.tabComplete(s) // it says IPAddressContext but no
        return emptyList()
    }
}