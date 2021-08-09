package net.azisaba.spicyAzisaBan

import net.azisaba.spicyAzisaBan.SpicyAzisaBan.Companion.PREFIX
import net.azisaba.spicyAzisaBan.util.Util.send
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.kt.promise.rewrite.catch
import util.ref.DataCache
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions
import xyz.acrylicstyle.sql.options.UpsertOptions

object SABCommand: Command("spicyazisaban", null, "sab"), TabExecutor {
    private val commands = listOf("creategroup", "deletegroup", "group")
    private val groupCommands = listOf("add", "remove")
    private var cachedGroups = DataCache<List<String>>()
    @Volatile
    private var updatingCache = false

    private fun CommandSender.sendHelp() {
        send("${ChatColor.AQUA}/sab creategroup <group>")
        send("${ChatColor.AQUA}/sab deletegroup <group>")
        send("${ChatColor.AQUA}/sab group <group> add <server>")
        send("${ChatColor.AQUA}/sab group <group> remove <server>")
    }

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.command.spicyazisaban")) {
            sender.send("$PREFIX${ChatColor.GREEN}Running ${ChatColor.RED}${ChatColor.BOLD}${SpicyAzisaBan.instance.description.name}${ChatColor.RESET}${ChatColor.GREEN} v${ChatColor.AQUA}${SpicyAzisaBan.instance.description.version}${ChatColor.GREEN}.")
            sender.send("$PREFIX${ChatColor.GREEN}You do not have permission to run commands.")
            return
        }
        if (args.isEmpty()) {
            sender.send("$PREFIX${ChatColor.GREEN}Running ${ChatColor.RED}${ChatColor.BOLD}${SpicyAzisaBan.instance.description.name}${ChatColor.RESET}${ChatColor.GREEN} v${ChatColor.AQUA}${SpicyAzisaBan.instance.description.version}${ChatColor.GREEN}.")
            sender.send("$PREFIX${ChatColor.GREEN}Use ${ChatColor.AQUA}/sab help${ChatColor.GREEN} to view commands.")
            return
        }
        when (args[0]) {
            "creategroup" -> {
                if (args.size <= 1) return sender.sendHelp()
                val groupName = args[1]
                if (!groupName.matches(SpicyAzisaBan.GROUP_PATTERN)) {
                    return sender.send("$PREFIX${ChatColor.RED}この名前は使用できません。")
                }
                SpicyAzisaBan.instance.connection.getAllGroups()
                    .then { list ->
                        if (list.any { it.equals(groupName, true) }) {
                            sender.send("$PREFIX${ChatColor.RED}この名前はすでに使用されています。")
                            throw Exception()
                        }
                    }
                    .then(SpicyAzisaBan.instance.connection.groups.insert(InsertOptions.Builder().addValue("id", groupName).build()))
                    .thenDo { cachedGroups.set(null) }
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
                    return sender.send("$PREFIX${ChatColor.RED}無効なグループ名です。")
                }
                SpicyAzisaBan.instance.connection.getAllGroups()
                    .then { list ->
                        if (!list.any { it == groupName }) {
                            sender.send("$PREFIX${ChatColor.RED}無効なグループ名です。")
                            throw Exception()
                        }
                    }
                    .then(SpicyAzisaBan.instance.connection.groups.delete(FindOptions.Builder().addWhere("id", groupName).build()))
                    .then(SpicyAzisaBan.instance.connection.serverGroup.delete(FindOptions.Builder().addWhere("group", groupName).build()))
                    .thenDo { cachedGroups.set(null) }
                    .then { sender.send("$PREFIX${ChatColor.GREEN}グループ「${ChatColor.GOLD}$groupName${ChatColor.GREEN}」を削除しました。") }
                    .catch {
                        if (it::class.java == Exception::class.java) return@catch
                        sender.send("$PREFIX${ChatColor.RED}グループの削除に失敗しました。")
                        it.printStackTrace()
                    }
            }
            "group" -> {
                if (args.size <= 3 || !groupCommands.contains(args[2])) return sender.sendHelp()
                val groupName = args[1]
                if (!groupName.matches(SpicyAzisaBan.GROUP_PATTERN)) {
                    return sender.send("$PREFIX${ChatColor.RED}無効なグループ名です。")
                }
                SpicyAzisaBan.instance.connection.getAllGroups()
                    .then { list ->
                        if (!list.any { it == groupName }) {
                            sender.send("$PREFIX${ChatColor.RED}無効なグループ名です。")
                            throw Exception()
                        }
                        if (!ProxyServer.getInstance().servers.map { it.value.name }.any { it == args[3] }) {
                            sender.send("$PREFIX${ChatColor.RED}無効なサーバー名です。")
                            throw Exception()
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
                            else -> sender.sendHelp()
                        }
                    }.catch {}
            }
            else -> sender.sendHelp()
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.isEmpty()) return emptyList()
        if (args.size == 1) return commands.filter(args[0])
        if (args[0] == "deletegroup" && args.size == 2) {
            getGroups()?.let { return it.filter(args[1]) }
        }
        if (args[0] == "group") {
            if (args.size == 2) getGroups()?.let { return it.filter(args[1]) }
            if (args.size == 3) return groupCommands.filter(args[2])
            if (args.size == 4) return ProxyServer.getInstance().servers.values.map { it.name }.filter(args[3])
        }
        return emptyList()
    }

    private fun getGroups(): List<String>? {
        val groups = cachedGroups.get()
        if (groups == null || cachedGroups.ttl - System.currentTimeMillis() < 10000) { // update if cache is expired or expiring in under 10 seconds
            if (!updatingCache) {
                updatingCache = true
                SpicyAzisaBan.instance.connection.getAllGroups().then {
                    cachedGroups = DataCache(groups, System.currentTimeMillis() + 1000 * 60)
                    updatingCache = false
                }.catch {
                    it.printStackTrace()
                    updatingCache = false
                }
            }
        }
        return groups
    }

    private fun List<String>.filter(s: String): List<String> = filter { s1 -> s1.lowercase().startsWith(s.lowercase()) }
}
