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
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.async
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
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object BanListCommand: Command() {
    override val name = "${SABConfig.prefix}banlist"
    override val permission = "sab.banlist"
    private val availableArguments =
        listOf(
            listOf("--help"),
            listOf("page="),
            listOf("server="),
            listOf("type="),
            listOf("--active"),
            listOf("--all"),
        )

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.banlist")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        val arguments = ArgumentParser(args.joinToString(" "))
        if (arguments.contains("help")) {
            return actor.send(SABMessages.Commands.BanList.usage.replaceVariables().translate())
        }
        val punishmentType = arguments.get(Contexts.PUNISHMENT_TYPE, actor).complete().apply { if (!isSuccess) return }.type
        val active = arguments.contains("active")
        val all = arguments.contains("all")
        if (active && all) return actor.send(SABMessages.Commands.BanList.invalidArguments.replaceVariables().translate())
        val page = max(1, arguments.parsedRawOptions["page"]?.toIntOr(1) ?: 1)
        val server = if (arguments.containsKey("server")) {
            arguments.get(Contexts.SERVER_NO_PERM_CHECK, actor).complete().apply { if (!isSuccess) return }.name
        } else {
            null
        }
        execute(actor, punishmentType, active, all, page, server)
    }

    fun execute(actor: Actor, punishmentType: PunishmentType?, active: Boolean, all: Boolean, page: Int, server: String?): Promise<Unit> {
        if (active && all) {
            actor.send(SABMessages.Commands.BanList.invalidArguments.replaceVariables().translate())
            return Promise.resolve(null)
        }
        val tableName = if (active) "punishments" else "punishmentHistory"
        val left = if (!all) "LEFT OUTER JOIN unpunish ON ($tableName.id = unpunish.punish_id)" else ""
        val whereList = mutableListOf<Pair<String, List<Any>>>()
        if (punishmentType != null) whereList.add("`type` = ?" to listOf(punishmentType.name))
        if (!all) whereList.add("unpunish.punish_id IS NULL" to emptyList())
        return async<Unit> { context ->
            if (server != null) whereList.add("`server` = ?" to listOf(server))
            val where = if (whereList.isEmpty()) "" else " WHERE ${whereList.joinToString(" AND ") { it.first }} "
            var rs = SpicyAzisaBan.instance.connection.executeQuery(
                "SELECT $tableName.* FROM `$tableName` $left $where ORDER BY `start` DESC LIMIT ${(page - 1) * 2}, 2",
                *whereList.flatMap { (_, list) -> list }.toTypedArray(),
            )
            val punishments = Punishment.readAllFromResultSet(rs)
            rs.statement.close()
            rs = SpicyAzisaBan.instance.connection.executeQuery(
                "SELECT COUNT(*) FROM `$tableName` $left $where",
                *whereList.flatMap { (_, list) -> list }.toTypedArray(),
            )
            rs.next()
            val count = rs.getInt(1)
            rs.statement.close()
            val maxPage = ceil(count / 2.0).toInt()
            val newPage = min(page, maxPage)
            val header = SABMessages.Commands.BanList.header.replaceVariables().translate()
            val footer = SABMessages.Commands.BanList.footer
                .replaceVariables(
                    "current_page" to newPage.toString(),
                    "max_page" to maxPage.toString(),
                    "count" to count.toString(),
                )
                .translate()
            val text = Component.text("")
            val backText = Component.text("${if (newPage > 1) newPage - 1 else "-"} << ", ChatColor.GRAY)
            if (newPage > 1) {
                backText.setColor(ChatColor.YELLOW)
                backText.setHoverEvent(HoverEvent.Action.SHOW_TEXT, Component.fromLegacyText(SABMessages.General.previousPage.translate()))
                backText.setClickEvent(ClickEvent.Action.RUN_COMMAND, "/${SABConfig.prefix}banlist page=${newPage - 1} ${if (active) "--active" else ""} ${if (all) "--all" else ""} ${if (server != null) "server=\"$server\"" else ""} ${if (punishmentType != null) "type=${punishmentType.name}" else ""}")
            }
            val nextText = Component.text(" >> ${if (newPage < maxPage) newPage + 1 else "-"}", ChatColor.GRAY)
            if (newPage < maxPage) {
                nextText.setColor(ChatColor.YELLOW)
                nextText.setHoverEvent(HoverEvent.Action.SHOW_TEXT, Component.fromLegacyText(SABMessages.General.nextPage.translate()))
                nextText.setClickEvent(ClickEvent.Action.RUN_COMMAND, "/${SABConfig.prefix}banlist page=${newPage + 1} ${if (active) "--active" else ""} ${if (all) "--all" else ""} ${if (server != null) "server=\"$server\"" else ""} ${if (punishmentType != null) "type=${punishmentType.name}" else ""}")
            }
            text.addChildren(backText)
            text.addChildren(Component.text("|", ChatColor.WHITE))
            text.addChildren(nextText)
            val msgs = punishments.map { it.getHistoryMessage().complete() }
            actor.send(header)
            msgs.forEach { actor.send(it) }
            actor.send(footer)
            actor.sendMessage(text)
            context.resolve()
        }.catch { actor.sendErrorMessage(it) }
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.banlist")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        if (s.startsWith("type=")) return PunishmentTypeContext.tabComplete(s)
        return emptyList()
    }
}
