package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.ReloadableSABConfig
import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.SpicyAzisaBan.Companion.PREFIX
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getUniqueId
import net.azisaba.spicyAzisaBan.util.Util.insert
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.toMinecraft
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions
import xyz.acrylicstyle.sql.options.UpsertOptions
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

object SABCommand: Command("${SABConfig.prefix}spicyazisaban", null, "sab"), TabExecutor {
    private val commands = listOf("creategroup", "deletegroup", "group", "info", "debug", "reload")
    private val groupCommands = listOf("add", "remove", "info")

    private val groupRemoveConfirmation = mutableMapOf<UUID, String>()

    private fun CommandSender.sendHelp() {
        send("$PREFIX${ChatColor.GREEN}SpicyAzisaBan commands")
        send("${ChatColor.RED}> ${ChatColor.AQUA}/sab group <group>")
        send("${ChatColor.RED}> ${ChatColor.AQUA}/sab info")
        send("${ChatColor.RED}> ${ChatColor.AQUA}/sab creategroup <group>")
        send("${ChatColor.RED}> ${ChatColor.AQUA}/sab deletegroup <group>")
        send("${ChatColor.RED}> ${ChatColor.AQUA}/sab debug [debugLevel = 0-99999]")
        send("${ChatColor.RED}> ${ChatColor.AQUA}/sab reload")
    }

    private fun CommandSender.sendGroupHelp() {
        send("$PREFIX${ChatColor.GREEN}Group sub commands ${ChatColor.DARK_GRAY}(${ChatColor.GRAY}/sab group <group> ...${ChatColor.DARK_GRAY})")
        send("${ChatColor.RED}> ${ChatColor.AQUA}add ${ChatColor.RED}- ${ChatColor.DARK_GRAY}<${ChatColor.GRAY}group${ChatColor.DARK_GRAY}>")
        send("${ChatColor.RED}> ${ChatColor.AQUA}remove ${ChatColor.RED}- ${ChatColor.DARK_GRAY}<${ChatColor.GRAY}group${ChatColor.DARK_GRAY}>")
        send("${ChatColor.RED}> ${ChatColor.AQUA}info")
    }

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.command.spicyazisaban")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) {
            sender.send("$PREFIX${ChatColor.GREEN}Running ${ChatColor.RED}${ChatColor.BOLD}${SpicyAzisaBan.instance.description.name}${ChatColor.RESET}${ChatColor.AQUA} v${SpicyAzisaBan.instance.description.version}${ChatColor.GREEN}.")
            sender.send("$PREFIX${ChatColor.GREEN}Use ${ChatColor.AQUA}/sab help${ChatColor.GREEN} to view commands.")
            sender.send("$PREFIX${ChatColor.GREEN}For other commands (such as /gban), please see ${ChatColor.AQUA}${ChatColor.UNDERLINE}https://github.com/acrylic-style/SpicyAzisaBan/issues/1${ChatColor.RESET}${ChatColor.GREEN}.")
            return
        }
        when (args[0]) {
            "creategroup" -> {
                if (args.size <= 1) return sender.sendHelp()
                val groupName = args[1]
                if (!groupName.matches(SpicyAzisaBan.GROUP_PATTERN)) {
                    return sender.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
                }
                SpicyAzisaBan.instance.connection.getAllGroups()
                    .then { list ->
                        if (list.any { it.equals(groupName, true) }) {
                            sender.send("$PREFIX${ChatColor.RED}この名前はすでに使用されています。")
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
                    .then { sender.send("${ChatColor.GREEN}グループ「${ChatColor.GOLD}$groupName${ChatColor.GREEN}」を作成しました。") }
                    .catch {
                        if (it::class.java == Exception::class.java) return@catch
                        sender.send("$PREFIX${ChatColor.RED}グループの作成に失敗しました。")
                        it.printStackTrace()
                    }
            }
            "deletegroup" -> {
                if (args.size <= 1) return sender.sendHelp()
                val groupName = args[1]
                if (!groupName.matches(SpicyAzisaBan.GROUP_PATTERN)) {
                    return sender.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
                }
                SpicyAzisaBan.instance.connection.getAllGroups()
                    .then { list ->
                        if (!list.any { it == groupName }) {
                            sender.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
                            throw Exception()
                        }
                        val a = args.joinToString()
                        if (groupRemoveConfirmation[sender.getUniqueId()] != a) {
                            sender.send("$PREFIX${ChatColor.GOLD}グループ「${ChatColor.YELLOW}$groupName${ChatColor.GOLD}」を削除しますか？関連付けられているすべての処罰が解除されます。")
                            sender.send("$PREFIX${ChatColor.GOLD}削除するには10秒以内に同じコマンドをもう一度打ってください。")
                            groupRemoveConfirmation[sender.getUniqueId()] = a
                            ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
                                if (groupRemoveConfirmation[sender.getUniqueId()] == a) groupRemoveConfirmation.remove(sender.getUniqueId())
                            }, 10, TimeUnit.SECONDS)
                            throw Exception()
                        }
                    }
                    .then(SpicyAzisaBan.instance.connection.groups.delete(FindOptions.Builder().addWhere("id", groupName).build()))
                    .then(SpicyAzisaBan.instance.connection.serverGroup.delete(FindOptions.Builder().addWhere("group", groupName).build()))
                    .thenDo {
                        SpicyAzisaBan.instance.connection.cachedGroups.set(null)
                        sender.send("$PREFIX${ChatColor.GREEN}グループに関連付けられた処罰を削除中...")
                        SpicyAzisaBan.instance.connection.punishments.delete(FindOptions.Builder().addWhere("server", groupName).build()).complete()
                        groupRemoveConfirmation.remove(sender.getUniqueId())
                    }
                    .then { sender.send("$PREFIX${ChatColor.GREEN}グループ「${ChatColor.GOLD}$groupName${ChatColor.GREEN}」を削除しました。") }
                    .catch {
                        if (it::class.java == Exception::class.java) return@catch
                        sender.send("$PREFIX${ChatColor.RED}グループの削除に失敗しました。")
                        SpicyAzisaBan.instance.logger.warning("Failed to delete group $groupName")
                        it.printStackTrace()
                    }
            }
            "group" -> {
                if (args.size <= 2 || !groupCommands.contains(args[2])) return sender.sendGroupHelp()
                val groupName = args[1]
                if (!groupName.matches(SpicyAzisaBan.GROUP_PATTERN)) {
                    return sender.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
                }
                SpicyAzisaBan.instance.connection.getAllGroups()
                    .then { list ->
                        if (!list.any { it == groupName }) {
                            return@then sender.send(SABMessages.Commands.General.invalidGroup.replaceVariables().translate())
                        }
                        if (args[2] == "add" || args[2] == "remove") {
                            if (args.size <= 2) {
                                return@then sender.sendGroupHelp()
                            } else if (!ProxyServer.getInstance().servers.map { it.value.name }.any { it == args[3] }) {
                                return@then sender.send(SABMessages.Commands.General.invalidServer.replaceVariables().translate())
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
                                sender.send("$PREFIX${ChatColor.GREEN}グループにサーバー($server)を追加しました。")
                            }
                            "remove" -> {
                                val server = args[3]
                                SpicyAzisaBan.instance.connection.serverGroup.delete(
                                    FindOptions.Builder()
                                        .addWhere("group", groupName)
                                        .addWhere("server", server)
                                        .build()
                                ).complete()
                                sender.send("$PREFIX${ChatColor.GREEN}グループからサーバーを(そのグループに入っている場合は)除外しました。")
                            }
                            "info" -> {
                                val servers = SpicyAzisaBan.instance.connection.getServersByGroup(args[1]).complete()
                                sender.send("$PREFIX${ChatColor.AQUA}グループ: ${ChatColor.RESET}$groupName")
                                sender.send("$PREFIX- ${ChatColor.AQUA}サーバー:")
                                servers.forEach { server ->
                                    sender.send("$PREFIX   - ${ChatColor.GREEN}${server.name}")
                                }
                            }
                            else -> sender.sendHelp()
                        }
                    }
            }
            "info" -> {
                Promise.create<Unit> { context ->
                    val dbVersion = SpicyAzisaBan.instance
                        .settings
                        .getDatabaseVersion()
                        .onCatch {}
                        .complete() ?: -1
                    sender.send("$PREFIX- ${ChatColor.AQUA}サーバーバージョン:")
                    sender.send("$PREFIX    ${ProxyServer.getInstance().version}")
                    sender.send("$PREFIX- ${ChatColor.AQUA}データベース:")
                    sender.send("$PREFIX    ${ChatColor.GOLD}接続済み: ${SpicyAzisaBan.instance.connection.isConnected().toMinecraft()}")
                    sender.send("$PREFIX    ${ChatColor.GOLD}バージョン: ${ChatColor.GREEN}$dbVersion")
                    sender.send("$PREFIX    ${ChatColor.GOLD}Failsafe: ${ChatColor.GREEN}${SABConfig.database.failsafe}")
                    sender.send("$PREFIX- ${ChatColor.AQUA}Uptime: ${ChatColor.GREEN}${SpicyAzisaBan.getUptime()}")
                    context.resolve()
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
                sender.send(SABMessages.Commands.Sab.setDebugLevel.replaceVariables().format(SpicyAzisaBan.debugLevel).translate())
            }
            "reload" -> {
                try {
                    ReloadableSABConfig.reload()
                } catch (e: Exception) {
                    sender.sendErrorMessage(e)
                    return
                }
                sender.send(SABMessages.Commands.Sab.reloadedConfiguration.replaceVariables().translate())
            }
            else -> sender.sendHelp()
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (!sender.hasPermission("sab.command.spicyazisaban")) return emptyList()
        if (args.isEmpty()) return emptyList()
        if (args.size == 1) return commands.filtr(args[0])
        if (args[0] == "deletegroup" && args.size == 2) {
            SpicyAzisaBan.instance.connection.getCachedGroups()?.let { return it.filtr(args[1]) }
        }
        if (args[0] == "group") {
            if (args.size == 2) SpicyAzisaBan.instance.connection.getCachedGroups()?.let { return it.filtr(args[1]) }
            if (args.size == 3) return groupCommands.filtr(args[2])
            if (args.size == 4 && (args[2] == "add" || args[2] == "remove"))
                return ProxyServer.getInstance().servers.values.map { it.name }.filtr(args[3])
        }
        return emptyList()
    }
}
