package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.isValidIPAddress
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.toIntOr
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.IPAddressContext
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object HistoryCommand: Command("${SABConfig.prefix}history"), TabExecutor {
    private val availableArguments =
        listOf(
            listOf("target="),
            listOf("page="),
            listOf("--active"),
            listOf("--all"),
            listOf("--ip", "-i"),
            listOf("--only", "-o"),
        )

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.history")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) {
            return sender.send(SABMessages.Commands.History.usage.replaceVariables().translate())
        }
        val arguments = ArgumentParser(args.joinToString(" "))
        val target = arguments.getString("target")
        if (target.isNullOrBlank()) {
            return sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
        }
        val active = arguments.contains("active")
        val all = arguments.contains("all")
        val ipOpt = arguments.contains("ip") || arguments.contains("-i")
        val only = arguments.contains("only") || arguments.contains("-o")
        if (active && all) return sender.send(SABMessages.Commands.History.invalidArguments.replaceVariables().translate())
        var page = max(1, arguments.getString("page")?.toIntOr(1) ?: 1)
        val tableName = if (active) "punishments" else "punishmentHistory"
        val left = if (!all) "LEFT OUTER JOIN unpunish ON ($tableName.id = unpunish.punish_id)" else ""
        val extraWhere = if (!all) "AND unpunish.punish_id IS NULL" else ""
        Promise.create<Unit> { context ->
            val punishments = if (ipOpt || target.isValidIPAddress()) {
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
                if (only) {
                    var sql = "SELECT $tableName.* FROM `$tableName` $left WHERE `target` = ? $extraWhere ORDER BY `start` DESC LIMIT ${(page - 1) * 2}, 2"
                    SQLConnection.logSql(sql)
                    var st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, ip)
                    val ps = Punishment.readAllFromResultSet(st.executeQuery()).also { st.close() }
                    sql = "SELECT COUNT(*) FROM `$tableName` $left WHERE `target` = ? $extraWhere"
                    SQLConnection.logSql(sql)
                    st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, ip)
                    val rs = st.executeQuery()
                    rs.next()
                    val count = rs.getInt(1)
                    Pair(ps, count)
                } else {
                    var where = pd.joinToString(" OR ") { "`target` = ?" }
                    if (where.isNotBlank()) where = " OR $where"
                    var sql = "SELECT $tableName.* FROM `$tableName` $left WHERE (`target` = ?$where) $extraWhere ORDER BY `start` DESC LIMIT ${(page - 1) * 2}, 2"
                    SQLConnection.logSql(sql)
                    var st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, ip)
                    pd.forEachIndexed { i, p -> st.setObject(i + 2, p.uniqueId.toString()) }
                    val ps = Punishment.readAllFromResultSet(st.executeQuery()).also { st.close() }
                    sql = "SELECT COUNT(*) FROM `$tableName` $left WHERE (`target` = ?$where) $extraWhere"
                    SQLConnection.logSql(sql)
                    st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, ip)
                    pd.forEachIndexed { i, p -> st.setObject(i + 2, p.uniqueId.toString()) }
                    val rs = st.executeQuery()
                    rs.next()
                    val count = rs.getInt(1)
                    Pair(ps, count)
                }
            } else {
                var pd = PlayerData.getByName(target).catch { sender.sendErrorMessage(it) }.complete()
                if (pd == null) {
                    pd = PlayerData.getByUUID(target).catch { sender.sendErrorMessage(it) }.complete()
                }
                if (pd == null) {
                    sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
                    return@create context.resolve()
                }
                if (only) {
                    var sql = "SELECT $tableName.* FROM `$tableName` $left WHERE `target` = ? $extraWhere ORDER BY `start` DESC LIMIT ${(page - 1) * 2}, 2"
                    SQLConnection.logSql(sql)
                    var st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, pd.uniqueId.toString())
                    val ps = Punishment.readAllFromResultSet(st.executeQuery()).also { st.close() }
                    sql = "SELECT COUNT(*) FROM `$tableName` $left WHERE `target` = ? $extraWhere"
                    SQLConnection.logSql(sql)
                    st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, pd.uniqueId.toString())
                    val rs = st.executeQuery()
                    rs.next()
                    val count = rs.getInt(1)
                    Pair(ps, count)
                } else {
                    var sql = "SELECT $tableName.* FROM `$tableName` $left WHERE (`target` = ? OR `target` = ?) $extraWhere ORDER BY `start` DESC LIMIT ${(page - 1) * 2}, 2"
                    SQLConnection.logSql(sql)
                    var st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, pd.uniqueId.toString())
                    st.setObject(2, pd.ip ?: pd.uniqueId.toString())
                    val ps = Punishment.readAllFromResultSet(st.executeQuery()).also { st.close() }
                    sql = "SELECT COUNT(*) FROM `$tableName` $left WHERE (`target` = ? OR `target` = ?) $extraWhere"
                    SQLConnection.logSql(sql)
                    st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
                    st.setObject(1, pd.uniqueId.toString())
                    st.setObject(2, pd.ip ?: pd.uniqueId.toString())
                    val rs = st.executeQuery()
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
            val text = TextComponent()
            val backText = TextComponent("${if (page > 1) page - 1 else "-"} << ")
            if (page > 1) {
                backText.color = ChatColor.YELLOW
                backText.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(SABMessages.General.previousPage.translate()))
                backText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/${SABConfig.prefix}history target=$target page=${page - 1} ${if (active) "--active" else ""} ${if (all) "--all" else ""} ${if (ipOpt) "-i" else ""} ${if (only) "-o" else ""}")
            } else {
                backText.color = ChatColor.GRAY
            }
            val nextText = TextComponent(" >> ${if (page < maxPage) page + 1 else "-"}")
            if (page < maxPage) {
                nextText.color = ChatColor.YELLOW
                nextText.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(SABMessages.General.nextPage.translate()))
                nextText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/${SABConfig.prefix}history target=$target page=${page + 1} ${if (active) "--active" else ""} ${if (all) "--all" else ""} ${if (ipOpt) "-i" else ""} ${if (only) "-o" else ""}")
            } else {
                nextText.color = ChatColor.GRAY
            }
            text.addExtra(backText)
            text.addExtra(TextComponent("|").apply { color = ChatColor.WHITE })
            text.addExtra(nextText)
            val msgs = punishments.first.map { it.getHistoryMessage().complete() }
            sender.send(header)
            msgs.forEach { sender.send(it) }
            sender.send(footer)
            sender.sendMessage(text)
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