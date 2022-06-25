package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.ChatColor
import net.azisaba.spicyAzisaBan.common.chat.ClickEvent
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.chat.HoverEvent
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getNonParamStringAt
import net.azisaba.spicyAzisaBan.util.Util.isValidIPAddress
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.toIntOr
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.IPAddressContext
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object HistoryCommand: Command() {
    override val name = "${SABConfig.prefix}history"
    override val permission = "sab.history"
    private val availableArguments =
        listOf(
            listOf("target="),
            listOf("page="),
            listOf("--active"),
            listOf("--all"),
            listOf("--ip", "-i"),
            listOf("--only", "-o"),
        )

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.history")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) {
            return actor.send(SABMessages.Commands.History.usage.replaceVariables().translate())
        }
        val arguments = ArgumentParser(args.joinToString(" "))
        val target = arguments.parsedRawOptions["target"] ?: arguments.getNonParamStringAt(0)
        if (target.isNullOrBlank()) {
            return actor.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
        }
        val active = arguments.contains("active")
        val all = arguments.contains("all")
        val ipOpt = arguments.contains("ip") || arguments.contains("-i")
        val only = arguments.contains("only") || arguments.contains("-o")
        if (active && all) return actor.send(SABMessages.Commands.History.invalidArguments.replaceVariables().translate())
        var page = max(1, arguments.parsedRawOptions["page"]?.toIntOr(1) ?: 1)
        val tableName = if (active) "punishments" else "punishmentHistory"
        val left = if (!all) "LEFT OUTER JOIN unpunish ON ($tableName.id = unpunish.punish_id)" else ""
        val extraWhere = if (!all) "AND unpunish.punish_id IS NULL" else ""
        async<Unit> { context ->
            val punishments = if (ipOpt || target.isValidIPAddress()) {
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
                if (only) {
                    var sql = "SELECT $tableName.* FROM `$tableName` $left WHERE `target` = ? $extraWhere ORDER BY `start` DESC LIMIT ${(page - 1) * 2}, 2"
                    val start = System.currentTimeMillis()
                    var st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, ip)
                    val ps = Punishment.readAllFromResultSet(st.executeQuery()).also { st.close() }
                    SQLConnection.logSql(sql, System.currentTimeMillis() - start)
                    sql = "SELECT COUNT(*) FROM `$tableName` $left WHERE `target` = ? $extraWhere"
                    val start2 = System.currentTimeMillis()
                    st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, ip)
                    val rs = st.executeQuery()
                    SQLConnection.logSql(sql, System.currentTimeMillis() - start2)
                    rs.next()
                    val count = rs.getInt(1)
                    Pair(ps, count)
                } else {
                    var where = pd.joinToString(" OR ") { "`target` = ?" }
                    if (where.isNotBlank()) where = " OR $where"
                    var sql = "SELECT $tableName.* FROM `$tableName` $left WHERE (`target` = ?$where) $extraWhere ORDER BY `start` DESC LIMIT ${(page - 1) * 2}, 2"
                    val start = System.currentTimeMillis()
                    var st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, ip)
                    pd.forEachIndexed { i, p -> st.setObject(i + 2, p.uniqueId.toString()) }
                    SQLConnection.logSql(sql, System.currentTimeMillis() - start)
                    val ps = Punishment.readAllFromResultSet(st.executeQuery()).also { st.close() }
                    sql = "SELECT COUNT(*) FROM `$tableName` $left WHERE (`target` = ?$where) $extraWhere"
                    val start2 = System.currentTimeMillis()
                    st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, ip)
                    pd.forEachIndexed { i, p -> st.setObject(i + 2, p.uniqueId.toString()) }
                    val rs = st.executeQuery()
                    SQLConnection.logSql(sql, System.currentTimeMillis() - start2)
                    rs.next()
                    val count = rs.getInt(1)
                    Pair(ps, count)
                }
            } else {
                var pd = PlayerData.getByName(target).catch { actor.sendErrorMessage(it) }.complete()
                if (pd == null) {
                    pd = PlayerData.getByUUID(target).catch { actor.sendErrorMessage(it) }.complete()
                }
                if (pd == null) {
                    actor.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@async context.resolve()
                }
                if (only) {
                    var sql = "SELECT $tableName.* FROM `$tableName` $left WHERE `target` = ? $extraWhere ORDER BY `start` DESC LIMIT ${(page - 1) * 2}, 2"
                    val start = System.currentTimeMillis()
                    var st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, pd.uniqueId.toString())
                    val ps = Punishment.readAllFromResultSet(st.executeQuery()).also { st.close() }
                    SQLConnection.logSql(sql, System.currentTimeMillis() - start)
                    sql = "SELECT COUNT(*) FROM `$tableName` $left WHERE `target` = ? $extraWhere"
                    val start2 = System.currentTimeMillis()
                    st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, pd.uniqueId.toString())
                    val rs = st.executeQuery()
                    SQLConnection.logSql(sql, System.currentTimeMillis() - start2)
                    rs.next()
                    val count = rs.getInt(1)
                    Pair(ps, count)
                } else {
                    var sql = "SELECT $tableName.* FROM `$tableName` $left WHERE (`target` = ? OR `target` = ?) $extraWhere ORDER BY `start` DESC LIMIT ${(page - 1) * 2}, 2"
                    val start = System.currentTimeMillis()
                    var st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, pd.uniqueId.toString())
                    st.setObject(2, pd.ip ?: pd.uniqueId.toString())
                    val ps = Punishment.readAllFromResultSet(st.executeQuery()).also { st.close() }
                    SQLConnection.logSql(sql, System.currentTimeMillis() - start)
                    sql = "SELECT COUNT(*) FROM `$tableName` $left WHERE (`target` = ? OR `target` = ?) $extraWhere"
                    val start2 = System.currentTimeMillis()
                    st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, pd.uniqueId.toString())
                    st.setObject(2, pd.ip ?: pd.uniqueId.toString())
                    val rs = st.executeQuery()
                    SQLConnection.logSql(sql, System.currentTimeMillis() - start2)
                    rs.next()
                    val count = rs.getInt(1)
                    Pair(ps, count)
                }
            }
            val maxPage = ceil(punishments.second / 2.0).toInt()
            page = min(page, maxPage)
            val header = SABMessages.Commands.History.header.replaceVariables("target" to target).translate()
            val footer = SABMessages.Commands.History.footer
                .replaceVariables(
                    "current_page" to page.toString(),
                    "max_page" to maxPage.toString(),
                    "count" to punishments.second.toString(),
                )
                .translate()
            val text = Component.text("")
            val backText = Component.text("${if (page > 1) page - 1 else "-"} << ")
            if (page > 1) {
                backText.setColor(ChatColor.YELLOW)
                backText.setHoverEvent(HoverEvent.Action.SHOW_TEXT, Component.fromLegacyText(SABMessages.General.previousPage.translate()))
                backText.setClickEvent(ClickEvent.Action.RUN_COMMAND, "/${SABConfig.prefix}history target=$target page=${page - 1} ${if (active) "--active" else ""} ${if (all) "--all" else ""} ${if (ipOpt) "-i" else ""} ${if (only) "-o" else ""}")
            } else {
                backText.setColor(ChatColor.GRAY)
            }
            val nextText = Component.text(" >> ${if (page < maxPage) page + 1 else "-"}")
            if (page < maxPage) {
                nextText.setColor(ChatColor.YELLOW)
                nextText.setHoverEvent(HoverEvent.Action.SHOW_TEXT, Component.fromLegacyText(SABMessages.General.nextPage.translate()))
                nextText.setClickEvent(ClickEvent.Action.RUN_COMMAND, "/${SABConfig.prefix}history target=$target page=${page + 1} ${if (active) "--active" else ""} ${if (all) "--all" else ""} ${if (ipOpt) "-i" else ""} ${if (only) "-o" else ""}")
            } else {
                nextText.setColor(ChatColor.GRAY)
            }
            text.addChildren(backText)
            text.addChildren(Component.text("|").apply { setColor(ChatColor.WHITE) })
            text.addChildren(nextText)
            val msgs = punishments.first.map { it.getHistoryMessage().complete() }
            actor.send(header)
            msgs.forEach { actor.send(it) }
            actor.send(footer)
            actor.sendMessage(text)
            context.resolve()
        }.catch { actor.sendErrorMessage(it) }
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.history")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("target=")) return IPAddressContext.tabComplete(s) // it says IPAddressContext but no
        return emptyList()
    }
}
