package net.azisaba.spicyAzisaBan.punishment

import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment.Flags.Companion.toDatabase
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.getIPAddress
import net.azisaba.spicyAzisaBan.util.Util.getProfile
import net.azisaba.spicyAzisaBan.util.Util.hasNotifyPermissionOf
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.TableData
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions
import java.sql.ResultSet
import java.util.UUID

data class Punishment(
    val id: Long,
    val name: String,
    val target: String,
    val reason: String,
    val operator: UUID,
    val type: PunishmentType,
    val start: Long,
    val end: Long,
    val server: String,
    val flags: List<Flags> = listOf(),
) {
    companion object {
        fun fromTableData(td: TableData): Punishment {
            val id = td.getLong("id")!!
            val name = td.getString("name")!!
            val target = td.getString("target")!!
            val reason = td.getString("reason")!!
            val operator = UUID.fromString(td.getString("operator")!!)!!
            val type = PunishmentType.valueOf(td.getString("type"))
            val start = td.getLong("start") ?: 0
            val end = td.getLong("end") ?: 0
            val server = td.getString("server")!!
            val flags = Flags.fromDatabase(td.getString("extra")!!)
            return Punishment(
                id,
                name,
                target,
                reason,
                operator,
                type,
                start,
                end,
                server,
                flags,
            )
        }

        fun fromResultSet(rs: ResultSet): Punishment {
            val id = rs.getLong("id")
            val name = rs.getString("name")!!
            val target = rs.getString("target")!!
            val reason = rs.getString("reason")!!
            val operator = UUID.fromString(rs.getString("operator")!!)!!
            val type = PunishmentType.valueOf(rs.getString("type")!!)
            val start = rs.getLong("start")
            val end = rs.getLong("end")
            val server = rs.getString("server")!!
            val flags = Flags.fromDatabase(rs.getString("flags")!!)
            return Punishment(
                id,
                name,
                target,
                reason,
                operator,
                type,
                start,
                end,
                server,
                flags,
            )
        }

        fun canJoinServer(player: ProxiedPlayer, server: String): Promise<Punishment?> = Promise.create { context ->
            val s = SpicyAzisaBan.instance.connection.connection.prepareStatement("SELECT * FROM `punishments` WHERE `target` = ? OR `target` = ?")
            s.setString(1, player.uniqueId.toString())
            s.setString(2, player.getIPAddress())
            val ps = mutableListOf<Punishment>()
            val rs = s.executeQuery()
            while (rs.next()) ps.add(fromResultSet(rs))
            s.close()
            if (ps.isEmpty()) return@create context.resolve()
            context.reject(IllegalArgumentException("no"))
        }

        fun fetchActivePunishmentById(id: Long): Promise<Punishment?> =
            SpicyAzisaBan.instance.connection.punishments.findOne(FindOptions.Builder().addWhere("id", id).build())
                .then { it?.let { fromTableData(it) } }

        fun fetchActivePunishmentsByTarget(target: String): Promise<List<Punishment>> =
            SpicyAzisaBan.instance.connection.punishments.findAll(FindOptions.Builder().addWhere("target", target).build())
                .then { it.map { td -> fromTableData(td) } }

        fun fetchActivePunishmentsByTarget(target: String, type: PunishmentType, server: String): Promise<List<Punishment>> =
            SpicyAzisaBan.instance
                .connection
                .punishments
                .findAll(FindOptions.Builder().addWhere("target", target).addWhere("type", type.name).addWhere("server", server).build())
                .then { it.map { td -> fromTableData(td) } }

        fun fetchActivePunishmentsByTarget(target: String, server: String): Promise<List<Punishment>> =
            SpicyAzisaBan.instance
                .connection
                .punishments
                .findAll(FindOptions.Builder().addWhere("target", target).addWhere("server", server).build())
                .then { it.map { td -> fromTableData(td) } }

        fun fetchPunishmentById(id: Long): Promise<Punishment?> =
            SpicyAzisaBan.instance.connection.punishmentHistory.findOne(FindOptions.Builder().addWhere("id", id).build())
                .then { it?.let { fromTableData(it) } }

        fun fetchPunishmentsByTarget(target: String): Promise<List<Punishment>> =
            SpicyAzisaBan.instance.connection.punishmentHistory.findAll(FindOptions.Builder().addWhere("target", target).build())
                .then { it.map { td -> fromTableData(td) } }
    }

    fun getTargetUUID() {
        if (type == PunishmentType.IP_BAN || type == PunishmentType.TEMP_IP_BAN) {
            error("This punishment ($id) is not UUID-based ban! (e.g. IP ban)")
        }
    }

    fun getVariables(): Promise<Map<String, String>> = operator.getProfile()
        .then { profile ->
            mapOf(
                "id" to id.toString(),
                "player" to name,
                "target" to target,
                "operator" to profile.name,
                "type" to type.id.replaceFirstChar { it.uppercase() },
                "reason" to reason,
                "server" to server,
            )
        }

    fun getBannedMessage() =
        getVariables()
            .then { variables ->
                return@then when (type) {
                    PunishmentType.BAN -> {
                        if (server == "global") {
                            SABMessages.Commands.GBan.layout.replaceVariables(variables).translate()
                        } else {
                            SABMessages.Commands.Ban.layout.replaceVariables(variables).translate()
                        }
                    }
                    else -> "undefined"
                }
            }
            .catch {
                SpicyAzisaBan.instance.logger.warning("Could not fetch player name of $operator")
                it.printStackTrace()
            }

    private fun getMessage() =
        getVariables()
            .then { variables ->
                return@then when (type) {
                    PunishmentType.BAN -> {
                        if (server == "global") {
                            SABMessages.Commands.GBan.notify.replaceVariables(variables).translate()
                        } else {
                            SABMessages.Commands.Ban.notify.replaceVariables(variables).translate()
                        }
                    }
                    else -> "undefined"
                }
            }
            .catch {
                SpicyAzisaBan.instance.logger.warning("Could not fetch player name of $operator")
                it.printStackTrace()
            }

    fun notifyToAll() =
        getMessage().thenDo { message ->
            ProxyServer.getInstance().players.filter { it.hasNotifyPermissionOf(type) }.forEach { player ->
                player.send(message)
            }
            ProxyServer.getInstance().console.send(message)
        }.then {}

    fun isExpired() = end < System.currentTimeMillis()

    fun updateFlags(): Promise<Unit> =
        SpicyAzisaBan.instance.connection.punishments.update("flags", flags.toDatabase(), FindOptions.Builder().addWhere("id", id).build())
            .then(SpicyAzisaBan.instance.connection.punishmentHistory.update("flags", flags.toDatabase(), FindOptions.Builder().addWhere("id", id).build()))
            .then {}

    /**
     * Inserts a new punishment into database.
     * @return new punishment with ID
     */
    fun insert(): Promise<Punishment> = Promise.create { context ->
        if (id != -1L) throw IllegalArgumentException("cannot insert existing punishment")
        val insertOptions = InsertOptions.Builder()
            .addValue("name", name)
            .addValue("target", target)
            .addValue("reason", reason)
            .addValue("operator", operator.toString())
            .addValue("type", type.name)
            .addValue("start", start)
            .addValue("end", end)
            .addValue("server", server)
            .addValue("flags", flags.toDatabase())
        try {
            val id = Util.insert {
                SpicyAzisaBan.instance.connection.punishmentHistory.insert(insertOptions.build()).complete()
            }
            Util.insert {
                SpicyAzisaBan.instance.connection.punishments.insert(insertOptions.addValue("id", id).build())
                    .complete()
            }
            return@create context.resolve(Punishment(id, name, target, reason, operator, type, start, end, server, flags))
        } catch (e: Throwable) {
            return@create context.reject(e)
        }
    }

    enum class Flags {
        SEEN,
        ;

        fun and(flags: Flags) = listOf(this, flags)

        companion object {
            fun fromDatabase(s: String): List<Flags> = s.split(",").map { valueOf(it) }.distinct()

            fun List<Flags>.toDatabase() = this.distinct().joinToString(",") { it.name }
            fun List<Flags>.and(flags: Flags) = this.toMutableList().apply { add(flags) }
        }
    }
}
