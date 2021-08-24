package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.toIntOr
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.PunishmentTypeContext
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import net.azisaba.spicyAzisaBan.util.contexts.get
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

object BanListCommand: Command("${SABConfig.prefix}banlist"), TabExecutor {
    private val availableArguments =
        listOf(
            listOf("--help"),
            listOf("page="),
            listOf("server="),
            listOf("type="),
            listOf("--active"),
            listOf("--all"),
        )

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.banlist")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        val arguments = ArgumentParser(args.joinToString(" "))
        if (arguments.contains("help")) {
            return sender.send(SABMessages.Commands.BanList.usage.replaceVariables().translate())
        }
        val punishmentType = arguments.get(Contexts.PUNISHMENT_TYPE, sender).complete().apply { if (!isSuccess) return }.type
        val active = arguments.contains("active")
        val all = arguments.contains("all")
        if (active && all) return sender.send(SABMessages.Commands.BanList.invalidArguments.replaceVariables().translate())
        var page = max(1, arguments.getString("page")?.toIntOr(1) ?: 1)
        val tableName = if (active) "punishments" else "punishmentHistory"
        val left = if (!all) "LEFT OUTER JOIN unpunish ON ($tableName.id = unpunish.punish_id)" else ""
        val whereList = mutableListOf<Pair<String, List<Any>>>()
        if (punishmentType != null) whereList.add("`type` = ?" to listOf(punishmentType.name))
        if (!all) whereList.add("unpunish.punish_id IS NULL" to emptyList())
        Promise.create<Unit> { context ->
            val server = if (arguments.containsKey("server")) {
                arguments.get(Contexts.SERVER_NO_PERM_CHECK, sender).complete().apply { if (!isSuccess) return@create context.resolve() }.name
            } else {
                null
            }
            if (server != null) whereList.add("`server` = ?" to listOf(server))
            val where = if (whereList.isEmpty()) "" else " WHERE ${whereList.joinToString(" AND ") { it.first }} "
            var sql = "SELECT $tableName.* FROM `$tableName` $left $where ORDER BY `start` DESC LIMIT ${(page - 1) * 2}, 2"
            SQLConnection.logSql(sql)
            var st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
            var i = 0
            whereList.forEach { (_, list) -> list.forEach { value -> st.setObject(++i, value) } }
            val punishments = Punishment.readAllFromResultSet(st.executeQuery()).also { st.close() }
            sql = "SELECT COUNT(*) FROM `$tableName` $left $where"
            SQLConnection.logSql(sql)
            st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
            i = 0
            whereList.forEach { (_, list) -> list.forEach { value -> st.setObject(++i, value) } }
            val rs = st.executeQuery()
            rs.next()
            val count = rs.getInt(1)
            val maxPage = ceil(count / 2.0).toInt()
            page = min(page, maxPage)
            val header = SABMessages.Commands.BanList.header.replaceVariables().translate()
            val footer = SABMessages.Commands.BanList.footer
                .replaceVariables(
                    "current_page" to page.toString(),
                    "max_page" to maxPage.toString(),
                    "count" to count.toString(),
                )
                .translate()
            val text = TextComponent()
            val backText = TextComponent("${if (page > 1) page - 1 else "-"} << ")
            if (page > 1) {
                backText.color = ChatColor.YELLOW
                backText.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(SABMessages.General.previousPage.translate()))
                backText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/${SABConfig.prefix}banlist page=${page - 1} ${if (active) "--active" else ""} ${if (all) "--all" else ""} ${if (server != null) "server=\"$server\"" else ""} ${if (punishmentType != null) "type=${punishmentType.name}" else ""}")
            } else {
                backText.color = ChatColor.GRAY
            }
            val nextText = TextComponent(" >> ${if (page < maxPage) page + 1 else "-"}")
            if (page < maxPage) {
                nextText.color = ChatColor.YELLOW
                nextText.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(SABMessages.General.nextPage.translate()))
                nextText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/${SABConfig.prefix}banlist page=${page + 1} ${if (active) "--active" else ""} ${if (all) "--all" else ""} ${if (server != null) "server=\"$server\"" else ""} ${if (punishmentType != null) "type=${punishmentType.name}" else ""}")
            } else {
                nextText.color = ChatColor.GRAY
            }
            text.addExtra(backText)
            text.addExtra(TextComponent("|").apply { color = ChatColor.WHITE })
            text.addExtra(nextText)
            val msgs = punishments.map { it.getHistoryMessage().complete() }
            sender.send(header)
            msgs.forEach { sender.send(it) }
            sender.send(footer)
            sender.sendMessage(text)
            context.resolve()
        }.catch { sender.sendErrorMessage(it) }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (!sender.hasPermission("sab.banlist")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        if (s.startsWith("type=")) return PunishmentTypeContext.tabComplete(s)
        return emptyList()
    }
}
