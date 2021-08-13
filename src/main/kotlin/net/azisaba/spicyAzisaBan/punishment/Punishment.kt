package net.azisaba.spicyAzisaBan.punishment

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment.Flags.Companion.toDatabase
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.getProfile
import net.azisaba.spicyAzisaBan.util.Util.hasNotifyPermissionOf
import net.azisaba.spicyAzisaBan.util.Util.isPunishableIP
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.ProxyServer
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.mcutil.common.PlayerProfile
import xyz.acrylicstyle.mcutil.common.SimplePlayerProfile
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
    val flags: MutableList<Flags> = mutableListOf(),
) {
    companion object {
        val recentPunishedPlayers = mutableSetOf<SimplePlayerProfile>()

        fun createByPlayer(
            playerProfile: PlayerProfile,
            reason: String,
            operator: UUID,
            type: PunishmentType,
            end: Long,
            server: String,
        ): Punishment {
            if (type.isIPBased()) error("Wrong type for #createByPlayer: ${type.name}")
            return Punishment(
                -1,
                playerProfile.name,
                playerProfile.uniqueId.toString(),
                reason,
                operator,
                type,
                System.currentTimeMillis(),
                end,
                server,
            )
        }

        fun createByIPAddress(ip: String, reason: String, operator: UUID, type: PunishmentType, end: Long, server: String): Punishment {
            if (!type.isIPBased()) error("Wrong type for #createByIPAddress: ${type.name}")
            return Punishment(-1, ip, ip, reason, operator, type, System.currentTimeMillis(), end, server)
        }

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
            val flags = Flags.fromDatabase(rs.getString("extra")!!)
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

        fun canJoinServer(uuid: UUID, address: String?, server: String): Promise<Punishment?> = Promise.create { context ->
            SpicyAzisaBan.debug("Checking for $uuid (trigger: joining server $server)")
            val group = if (server == "global") server else SpicyAzisaBan.instance.connection.getGroupByServer(server).complete()
            val s = SpicyAzisaBan.instance.connection.connection.prepareStatement("SELECT * FROM `punishments` WHERE (`target` = ? OR `target` = ?) AND (`server` = \"global\" OR `server` = ? OR `server` = ?)")
            s.setString(1, uuid.toString())
            s.setString(2, address ?: uuid.toString())
            s.setString(3, server)
            s.setString(4, group ?: server)
            val ps = mutableListOf<Punishment>()
            val rs = s.executeQuery()
            while (rs.next()) ps.add(fromResultSet(rs))
            s.close()
            if (ps.isEmpty()) return@create context.resolve(null)
            var punishment: Punishment? = null
            ps.filter { p -> p.type.isBan() }.forEach { p ->
                if (p.isExpired()) {
                    SpicyAzisaBan.debug("Removing punishment #${p.id} (reason: expired)")
                    SpicyAzisaBan.debug(p.toString(), 2)
                    SpicyAzisaBan.instance.connection.punishments.delete(FindOptions.Builder().addWhere("id", p.id).build()).thenDo {
                        SpicyAzisaBan.debug("Removed punishment #${p.id} (reason: expired)")
                    }
                } else {
                    punishment = p
                }
            }
            context.resolve(punishment)
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

    init {
        if (type.isIPBased() && !target.isPunishableIP()) error("This IP address ($target) cannot be banned")
        if (!type.isIPBased()) {
            try {
                val uuid = getTargetUUID()
                recentPunishedPlayers.add(SimplePlayerProfile(name, uuid))
            } catch (e: IllegalArgumentException) {}
        }
    }

    fun getProofs(): Promise<List<Proof>> =
        SpicyAzisaBan.instance.connection.proofs.findAll(FindOptions.Builder().addWhere("punish_id", id).build())
            .then { list -> list.map { td -> Proof.fromTableData(this, td) } }

    fun getTargetUUID(): UUID {
        if (type.isIPBased()) {
            throw IllegalArgumentException("This punishment ($id) is not UUID-based ban! (e.g. IP ban)")
        }
        return UUID.fromString(target)
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
                "server" to if (server.lowercase() == "global") SABMessages.General.global else SABConfig.serverNames.getOrDefault(server.lowercase(), server.lowercase()),
                "duration" to Util.unProcessTime(end - System.currentTimeMillis()),
                "time" to Util.unProcessTime(end - start),
            )
        }

    fun getBannedMessage() =
        getVariables()
            .then { variables ->
                return@then when (type) {
                    PunishmentType.BAN -> SABMessages.Commands.Ban.layout.replaceVariables(variables).translate()
                    PunishmentType.TEMP_BAN -> SABMessages.Commands.TempBan.layout.replaceVariables(variables).translate()
                    PunishmentType.IP_BAN -> SABMessages.Commands.IPBan.layout.replaceVariables(variables).translate()
                    PunishmentType.TEMP_IP_BAN -> SABMessages.Commands.TempIPBan.layout.replaceVariables(variables).translate()
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
                    PunishmentType.BAN -> SABMessages.Commands.Ban.notify.replaceVariables(variables).translate()
                    PunishmentType.TEMP_BAN -> SABMessages.Commands.TempBan.notify.replaceVariables(variables).translate()
                    PunishmentType.IP_BAN -> SABMessages.Commands.IPBan.notify.replaceVariables(variables).translate()
                    PunishmentType.TEMP_IP_BAN -> SABMessages.Commands.TempIPBan.notify.replaceVariables(variables).translate()
                    else -> "undefined"
                }
            }
            .catch {
                SpicyAzisaBan.instance.logger.warning("Could not fetch player name of $operator")
                it.printStackTrace()
            }

    fun notifyToAll() =
        getMessage().thenDo { message ->
            ProxyServer.getInstance().console.send(message)
            ProxyServer.getInstance().players.filter { it.hasNotifyPermissionOf(type) }.forEach { player ->
                player.send(message)
            }
        }.then {}

    fun isExpired() = end != -1L && end < System.currentTimeMillis()

    fun updateFlags(): Promise<Unit> =
        SpicyAzisaBan.instance.connection.punishments.update("extra", flags.toDatabase(), FindOptions.Builder().addWhere("id", id).build())
            .then(SpicyAzisaBan.instance.connection.punishmentHistory.update("extra", flags.toDatabase(), FindOptions.Builder().addWhere("id", id).build()))
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
            .addValue("server", server.lowercase())
            .addValue("extra", flags.toDatabase())
        var cancel = false
        val id = Util.insert {
            cancel = SpicyAzisaBan.instance.connection.punishmentHistory.insert(insertOptions.build())
                .catch { it.printStackTrace();context.reject(it) }
                .complete() == null
        }
        if (cancel) return@create
        Util.insert u@{
            cancel = SpicyAzisaBan.instance.connection.punishments.insert(insertOptions.addValue("id", id).build())
                .catch { it.printStackTrace();context.reject(it) }
                .complete() == null
        }
        if (cancel) return@create
        return@create context.resolve(
            Punishment(
                id,
                name,
                target,
                reason,
                operator,
                type,
                start,
                end,
                server.lowercase(),
                flags,
            )
        )
    }

    enum class Flags {
        SEEN,
        ;

        fun and(flags: Flags) = listOf(this, flags)

        companion object {
            fun fromDatabase(s: String): MutableList<Flags> = if (s.isEmpty()) mutableListOf() else s.split(",").map { valueOf(it) }.distinct().toMutableList()

            fun List<Flags>.toDatabase() = this.distinct().joinToString(",") { it.name }
            fun List<Flags>.and(flags: Flags) = this.toMutableList().apply { add(flags) }
        }
    }
}
