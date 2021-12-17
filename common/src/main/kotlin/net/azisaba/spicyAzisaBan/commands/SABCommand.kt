package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.ReloadableSABConfig
import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.SpicyAzisaBan.Companion.PREFIX
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.ChatColor
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.filtrPermission
import net.azisaba.spicyAzisaBan.util.Util.insert
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.toMinecraft
import net.azisaba.spicyAzisaBan.util.Util.translate
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions
import xyz.acrylicstyle.sql.options.UpsertOptions
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

object SABCommand: Command() {
    override val name = "${SABConfig.prefix}spicyazisaban"
    override val aliases = arrayOf("sab")

    private val commands = listOf(
        "creategroup",
        "deletegroup",
        "group",
        "info",
        "debug",
        "reload",
        "deletepunishmenthistory",
        "deletepunishment",
        "clearcache",
        "link",
        "unlink",
    )
    private val groupCommands = listOf("add", "remove", "info")

    private val groupRemoveConfirmation = mutableMapOf<UUID, String>()

    private fun Actor.sendHelp() {
        send("$PREFIX${ChatColor.GREEN}SpicyAzisaBan commands")
        if (hasPermission("sab.command.spicyazisaban.group")) send("${ChatColor.RED}> ${ChatColor.AQUA}/sab group <group>")
        if (hasPermission("sab.command.spicyazisaban.info")) send("${ChatColor.RED}> ${ChatColor.AQUA}/sab info")
        if (hasPermission("sab.command.spicyazisaban.creategroup")) send("${ChatColor.RED}> ${ChatColor.AQUA}/sab creategroup <group>")
        if (hasPermission("sab.command.spicyazisaban.deletegroup")) send("${ChatColor.RED}> ${ChatColor.AQUA}/sab deletegroup <group>")
        if (hasPermission("sab.command.spicyazisaban.debug")) send("${ChatColor.RED}> ${ChatColor.AQUA}/sab debug [debugLevel = 0-99999]")
        if (hasPermission("sab.command.spicyazisaban.reload")) send("${ChatColor.RED}> ${ChatColor.AQUA}/sab reload")
        if (hasPermission("sab.command.spicyazisaban.deletepunishmenthistory")) send("${ChatColor.RED}> ${ChatColor.AQUA}/sab deletepunishmenthistory <id>")
        if (hasPermission("sab.command.spicyazisaban.deletepunishment")) send("${ChatColor.RED}> ${ChatColor.AQUA}/sab deletepunishment <id>")
        if (hasPermission("sab.command.spicyazisaban.link")) send("${ChatColor.RED}> ${ChatColor.AQUA}/sab link <code>")
        if (hasPermission("sab.command.spicyazisaban.unlink")) send("${ChatColor.RED}> ${ChatColor.AQUA}/sab unlink")
    }

    private fun Actor.sendGroupHelp() {
        send("$PREFIX${ChatColor.GREEN}Group sub commands ${ChatColor.DARK_GRAY}(${ChatColor.GRAY}/sab group <group> ...${ChatColor.DARK_GRAY})")
        send("${ChatColor.RED}> ${ChatColor.AQUA}add ${ChatColor.RED}- ${ChatColor.DARK_GRAY}<${ChatColor.GRAY}group${ChatColor.DARK_GRAY}>")
        send("${ChatColor.RED}> ${ChatColor.AQUA}remove ${ChatColor.RED}- ${ChatColor.DARK_GRAY}<${ChatColor.GRAY}group${ChatColor.DARK_GRAY}>")
        send("${ChatColor.RED}> ${ChatColor.AQUA}info")
    }

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.command.spicyazisaban")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (!commands.any { actor.hasPermission("sab.command.spicyazisaban.$it") }) {
            actor.send("$PREFIX${ChatColor.GREEN}Running ${ChatColor.RED}${ChatColor.BOLD}${SpicyAzisaBan.instance.getPluginName()}${ChatColor.RESET}${ChatColor.AQUA} v${SABConfig.version}${ChatColor.GREEN}.")
            actor.send("$PREFIX${ChatColor.AQUA}You do not have permission to run any subcommand.")
            return
        }
        if (args.isEmpty()) {
            actor.send("$PREFIX${ChatColor.GREEN}Running ${ChatColor.RED}${ChatColor.BOLD}${SpicyAzisaBan.instance.getPluginName()}${ChatColor.RESET}${ChatColor.AQUA} v${SABConfig.version}${ChatColor.GREEN}.")
            actor.send("$PREFIX${ChatColor.GREEN}Use ${ChatColor.AQUA}/sab help${ChatColor.GREEN} to view commands.")
            actor.send("$PREFIX${ChatColor.GREEN}For other commands (such as /gban), please see ${ChatColor.AQUA}${ChatColor.UNDERLINE}https://github.com/AzisabaNetwork/SpicyAzisaBan/issues/1")
            return
        }
        if (!actor.hasPermission("sab.command.spicyazisaban.${args[0].lowercase()}")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        when (args[0].lowercase()) {
            "creategroup" -> {
                if (args.size <= 1) return actor.sendHelp()
                val groupName = args[1]
                if (!groupName.matches(SpicyAzisaBan.GROUP_PATTERN)) {
                    return actor.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
                }
                SpicyAzisaBan.instance.connection.getAllGroups()
                    .then { list ->
                        if (list.any { it.equals(groupName, true) }) {
                            actor.send("$PREFIX${ChatColor.RED}この名前はすでに使用されています。")
                            throw Exception()
                        }
                    }
                    .thenDo {
                        insert {
                            SpicyAzisaBan.instance.connection.groups.insert(
                                InsertOptions.Builder().addValue("id", groupName).build()
                            ).complete()
                        }
                    }
                    .thenDo { SpicyAzisaBan.instance.connection.cachedGroups.set(null) }
                    .then { actor.send("${ChatColor.GREEN}グループ「${ChatColor.GOLD}$groupName${ChatColor.GREEN}」を作成しました。") }
                    .catch {
                        if (it::class.java == Exception::class.java) return@catch
                        actor.send("$PREFIX${ChatColor.RED}グループの作成に失敗しました。")
                        it.printStackTrace()
                    }
            }
            "deletegroup" -> {
                if (args.size <= 1) return actor.sendHelp()
                val groupName = args[1]
                if (!groupName.matches(SpicyAzisaBan.GROUP_PATTERN)) {
                    return actor.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
                }
                SpicyAzisaBan.instance.connection.getAllGroups()
                    .then { list ->
                        if (!list.any { it == groupName }) {
                            actor.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
                            throw Exception()
                        }
                        val a = args.joinToString()
                        if (groupRemoveConfirmation[actor.uniqueId] != a) {
                            actor.send("$PREFIX${ChatColor.GOLD}グループ「${ChatColor.YELLOW}$groupName${ChatColor.GOLD}」を削除しますか？関連付けられているすべての処罰が解除されます。")
                            actor.send("$PREFIX${ChatColor.GOLD}削除するには10秒以内に同じコマンドをもう一度打ってください。")
                            groupRemoveConfirmation[actor.uniqueId] = a
                            SpicyAzisaBan.instance.schedule(10, TimeUnit.SECONDS) {
                                if (groupRemoveConfirmation[actor.uniqueId] == a) {
                                    groupRemoveConfirmation.remove(actor.uniqueId)
                                }
                            }
                            throw Exception()
                        }
                    }
                    .then(SpicyAzisaBan.instance.connection.groups.delete(FindOptions.Builder().addWhere("id", groupName).build()))
                    .then(SpicyAzisaBan.instance.connection.serverGroup.delete(FindOptions.Builder().addWhere("group", groupName).build()))
                    .thenDo {
                        SpicyAzisaBan.instance.connection.cachedGroups.set(null)
                        actor.send("$PREFIX${ChatColor.GREEN}グループに関連付けられた処罰を削除中...")
                        SpicyAzisaBan.instance.connection.punishments
                            .delete(FindOptions.Builder().addWhere("server", groupName).build())
                            .then { it.map { td -> Punishment.fromTableData(td) } }
                            .then { list ->
                                val reason = SABMessages.Commands.Sab.deleteGroupUnpunishReason
                                    .replaceVariables("group" to groupName)
                                    .translate()
                                list.forEach { p ->
                                    SpicyAzisaBan.instance.connection.unpunish.insert(
                                        InsertOptions.Builder()
                                            .addValue("punish_id", p.id)
                                            .addValue("reason", reason)
                                            .addValue("timestamp", System.currentTimeMillis())
                                            .addValue("operator", actor.uniqueId.toString())
                                            .build()
                                    ).complete()
                                }
                            }
                            .catch { actor.sendErrorMessage(it) }
                            .complete()
                        groupRemoveConfirmation.remove(actor.uniqueId)
                    }
                    .then { actor.send("$PREFIX${ChatColor.GREEN}グループ「${ChatColor.GOLD}$groupName${ChatColor.GREEN}」を削除しました。") }
                    .catch {
                        if (it::class.java == Exception::class.java) return@catch
                        actor.send("$PREFIX${ChatColor.RED}グループの削除に失敗しました。")
                        SpicyAzisaBan.LOGGER.warning("Failed to delete group $groupName")
                        it.printStackTrace()
                    }
            }
            "group" -> {
                if (args.size <= 2 || !groupCommands.contains(args[2])) return actor.sendGroupHelp()
                val groupName = args[1]
                if (!groupName.matches(SpicyAzisaBan.GROUP_PATTERN)) {
                    return actor.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
                }
                SpicyAzisaBan.instance.connection.getAllGroups()
                    .then { list ->
                        if (!list.any { it == groupName }) {
                            return@then actor.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
                        }
                        if (args[2] == "add" || args[2] == "remove") {
                            if (args.size <= 3) {
                                return@then actor.sendGroupHelp()
                            } else if (!SpicyAzisaBan.instance.getServers().map { it.value.name }.any { it == args[3] }) {
                                return@then actor.send(SABMessages.Commands.General.invalidServer.replaceVariables().translate())
                            }
                        }
                        when (args[2]) {
                            "add" -> {
                                val server = args[3]
                                SpicyAzisaBan.instance.connection.serverGroup.upsert(
                                    UpsertOptions.Builder()
                                        .addWhere("server", server)
                                        .addValue("group", groupName)
                                        .addValue("server", server)
                                        .build()
                                ).complete()
                                actor.send("$PREFIX${ChatColor.GREEN}グループにサーバー($server)を追加しました。")
                            }
                            "remove" -> {
                                val server = args[3]
                                SpicyAzisaBan.instance.connection.serverGroup.delete(
                                    FindOptions.Builder()
                                        .addWhere("group", groupName)
                                        .addWhere("server", server)
                                        .build()
                                ).complete()
                                actor.send("$PREFIX${ChatColor.GREEN}グループからサーバーを(そのグループに入っている場合は)除外しました。")
                            }
                            "info" -> {
                                val servers = SpicyAzisaBan.instance.connection.getServersByGroup(args[1]).complete()
                                actor.send("$PREFIX${ChatColor.AQUA}グループ: ${ChatColor.RESET}$groupName")
                                actor.send("$PREFIX- ${ChatColor.AQUA}サーバー:")
                                servers.forEach { server ->
                                    actor.send("$PREFIX   - ${ChatColor.GREEN}$server")
                                }
                            }
                            else -> actor.sendHelp()
                        }
                    }
            }
            "info" -> {
                async<Unit> { context ->
                    val dbVersion = SpicyAzisaBan.instance
                        .settings
                        .getDatabaseVersion()
                        .onCatch {}
                        .complete() ?: -1
                    actor.send(SABMessages.Commands.Sab.info.replaceVariables(
                        "server_name" to SpicyAzisaBan.instance.getServerName(),
                        "server_version" to SpicyAzisaBan.instance.getServerVersion(),
                        "db_connected" to SpicyAzisaBan.instance.connection.isConnected().toMinecraft(),
                        "db_version" to dbVersion.toString(),
                        "db_failsafe" to SABConfig.database.failsafe.toMinecraft(),
                        "uptime" to SpicyAzisaBan.getUptime(),
                        "version" to SABConfig.version,
                        "is_devbuild" to SABConfig.devBuild.toMinecraft(),
                        "is_debugbuild" to SABConfig.debugBuild.toMinecraft(),
                        "server_id" to SABConfig.serverId,
                        "is_lockdown" to SpicyAzisaBan.instance.lockdown.toMinecraft(),
                    ).translate())
                    context.resolve()
                }
            }
            "deletepunishmenthistory" -> {
                if (args.size <= 1) return actor.sendHelp()
                val id = try {
                    max(args[1].toLong(), 0)
                } catch (e: NumberFormatException) {
                    return actor.send(SABMessages.Commands.General.invalidNumber.replaceVariables().translate())
                }
                SpicyAzisaBan.instance.connection.punishmentHistory
                    .delete(FindOptions.Builder().addWhere("id", id).build())
                    .then { list ->
                        if (list.isEmpty()) return@then actor.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().translate().format(id))
                        val v = Punishment.fromTableData(list[0]).getVariables().complete()
                        actor.send(SABMessages.Commands.Sab.removedFromPunishmentHistory.replaceVariables(v).translate())
                    }
            }
            "deletepunishment" -> {
                if (args.size <= 1) return actor.sendHelp()
                val id = try {
                    max(args[1].toLong(), 0)
                } catch (e: NumberFormatException) {
                    return actor.send(SABMessages.Commands.General.invalidNumber.replaceVariables().translate())
                }
                SpicyAzisaBan.instance.connection.punishments
                    .delete(FindOptions.Builder().addWhere("id", id).build())
                    .then { list ->
                        if (list.isEmpty()) return@then actor.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().translate().format(id))
                        val v = Punishment.fromTableData(list[0]).getVariables().complete()
                        actor.send(SABMessages.Commands.Sab.removedFromPunishment.replaceVariables(v).translate())
                    }
            }
            "debug" -> {
                if (args.size <= 1) {
                    SpicyAzisaBan.debugLevel = 0
                } else {
                    SpicyAzisaBan.debugLevel = try {
                        min(max(Integer.parseInt(args[1]), 0), 99999)
                    } catch (e: NumberFormatException) {
                        0
                    }
                }
                actor.send(SABMessages.Commands.Sab.setDebugLevel.replaceVariables().format(SpicyAzisaBan.debugLevel).translate())
            }
            "reload" -> {
                try {
                    ReloadableSABConfig.reload()
                    SABMessages.reload()
                } catch (e: Exception) {
                    actor.sendErrorMessage(e)
                    return
                }
                actor.send(SABMessages.Commands.Sab.reloadedConfiguration.replaceVariables().translate())
            }
            "clearcache" -> {
                Punishment.canJoinServerCachedData.clear()
                Punishment.muteCache.clear()
                actor.send(SABMessages.Commands.Sab.clearedCache.replaceVariables().translate())
            }
            "link" -> {
                if (actor !is PlayerActor) {
                    return actor.send("${ChatColor.RED}NO ${ChatColor.AQUA}CONSOLE ${ChatColor.GOLD}ZONE")
                }
                if (args.size <= 1) return
                SpicyAzisaBan.instance.connection
                    .isTableExists("users_linked_accounts")
                    .thenDo { exists ->
                        if (!exists) {
                            return@thenDo actor.send(SABMessages.Commands.Sab.apiTableNotFound.replaceVariables().translate())
                        }
                        actor.send(SABMessages.Commands.Sab.accountLinking.replaceVariables().translate())
                        val rs = SpicyAzisaBan.instance.connection.executeQuery("SELECT `user_id` FROM `users_linked_accounts` WHERE `link_code` = ?", args[1])
                        if (!rs.next()) {
                            rs.statement.close()
                            return@thenDo actor.send(SABMessages.Commands.Sab.accountNoLinkCode.replaceVariables().translate())
                        }
                        val userId = rs.getInt("user_id")
                        rs.statement.close()
                        val usernameResult = SpicyAzisaBan.instance.connection.executeQuery("SELECT `username` FROM `users` WHERE `id` = ?", userId)
                        if (!usernameResult.next()) {
                            usernameResult.statement.close()
                            throw AssertionError("Could not find user for id = $userId")
                        }
                        val username = usernameResult.getString("username")
                        usernameResult.statement.close()
                        SpicyAzisaBan.instance.connection.execute("UPDATE `users_linked_accounts` SET `link_code` = NULL, `expire` = 0, `linked_uuid` = ? WHERE `user_id` = ?", actor.uniqueId.toString(), userId)
                        actor.send(SABMessages.Commands.Sab.accountLinkComplete.replaceVariables("username" to username).translate())
                    }
                    .catch { actor.sendErrorMessage(it) }
            }
            "unlink" -> {
                if (actor !is PlayerActor) {
                    return actor.send("${ChatColor.RED}NO ${ChatColor.AQUA}CONSOLE ${ChatColor.GOLD}ZONE")
                }
                SpicyAzisaBan.instance.connection
                    .isTableExists("users_linked_accounts")
                    .thenDo { exists ->
                        if (!exists) {
                            return@thenDo actor.send(SABMessages.Commands.Sab.apiTableNotFound.replaceVariables().translate())
                        }
                        SpicyAzisaBan.instance.connection.execute("DELETE FROM `users_linked_accounts` WHERE `linked_uuid` = ?", actor.uniqueId.toString())
                        actor.send(SABMessages.Commands.Sab.accountUnlinked.replaceVariables().translate())
                    }
                    .catch { actor.sendErrorMessage(it) }
            }
            else -> actor.sendHelp()
        }
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.command.spicyazisaban")) return emptyList()
        if (args.isEmpty()) return emptyList()
        if (args.size == 1) return commands.filtrPermission(actor, "sab.command.spicyazisaban.", args[0])
        if (args.size == 2) {
            if (actor.hasPermission("sab.command.spicyazisaban.deletegroup") && args[0] == "deletegroup") {
                SpicyAzisaBan.instance.connection.getCachedGroups()?.let { return it.filtr(args[1]) }
            }
            if (actor.hasPermission("sab.command.spicyazisaban.group") && args[0] == "group") {
                SpicyAzisaBan.instance.connection.getCachedGroups()?.let { return it.filtr(args[1]) }
            }
        }
        if (actor.hasPermission("sab.command.spicyazisaban.group") && args[0] == "group") {
            if (args.size == 3) return groupCommands.filtr(args[2])
            if (args.size == 4 && (args[2] == "add" || args[2] == "remove"))
                return SpicyAzisaBan.instance.getServers().values.map { it.name }.filtr(args[3])
        }
        return emptyList()
    }
}
