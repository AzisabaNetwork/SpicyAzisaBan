package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import util.kt.promise.rewrite.catch
import util.kt.promise.rewrite.component1
import util.kt.promise.rewrite.component2
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.options.FindOptions
import java.util.UUID

object NameHistoryCommand : Command() {
    override val name = "namehistory"
    override val permission = "sab.namehistory"

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission(permission)) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.NameHistory.usage.replaceVariables().translate())
        getUUIDs(args[0])
            .then { pair ->
                pair.first to pair.second.map { uuid ->
                    SpicyAzisaBan.instance
                        .connection
                        .usernameHistory
                        .findAll(FindOptions.Builder().addWhere("uuid", uuid.toString()).build())
                        .then { it.sortedBy { td -> td.getLong("last_seen") }.map { td -> td.getString("name") } }
                }.map(Promise<List<String>>::complete)
            }
            .thenDo { pair ->
                pair.second.forEach { names ->
                    names.mapIndexed { i, s ->
                        val isLast = names.lastIndex == i
                        if (pair.first.contains(s)) {
                            "§${if (isLast) 'a' else 'b'}§n$s§r"
                        } else {
                            if (isLast) {
                                "§a$s"
                            } else {
                                "§e$s"
                            }
                        }
                    }.joinToString("§7 -> ").let { actor.send(it) }
                }
            }
            .catch { actor.sendErrorMessage(it) }
            .complete()
    }

    private fun getUUIDs(name: String): Promise<Pair<List<String>, List<UUID>>> = Promise.create { (resolve, reject) ->
        try {
            val sql = "SELECT `name`, `uuid` FROM `usernameHistory` WHERE LOWER(`name`) LIKE LOWER(?)"
            val start = System.currentTimeMillis()
            SpicyAzisaBan.instance.connection.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, name)
                SQLConnection.logSql(sql, start, name)
                val names = mutableListOf<String>()
                val uuids = mutableListOf<UUID>()
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        names.add(rs.getString("name"))
                        uuids.add(UUID.fromString(rs.getString("uuid")))
                    }
                }
                resolve(names.distinct() to uuids.distinct())
            }
        } catch (t: Throwable) {
            reject(t)
        }
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (args.isEmpty()) return emptyList()
        return SpicyAzisaBan.instance.getPlayers().map { it.name }.filtr(args[0])
    }
}
