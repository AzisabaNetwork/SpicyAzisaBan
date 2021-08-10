package net.azisaba.spicyAzisaBan.punishment

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.TableData
import xyz.acrylicstyle.sql.options.FindOptions
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
    val server: String?,
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
            val server = td.getString("server")
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
            )
        }

        fun fetchActivePunishmentById(id: Long): Promise<Punishment?> =
            SpicyAzisaBan.instance.connection.punishments.findOne(FindOptions.Builder().addWhere("id", id).build())
                .then { it?.let { fromTableData(it) } }

        fun fetchActivePunishmentsByTarget(target: String): Promise<List<Punishment>> =
            SpicyAzisaBan.instance.connection.punishments.findAll(FindOptions.Builder().addWhere("target", target).build())
                .then { it.map { td -> fromTableData(td) } }

        fun fetchActivePunishmentsByTargetAndType(target: String, type: PunishmentType): Promise<List<Punishment>> =
            SpicyAzisaBan.instance.connection.punishments.findAll(FindOptions.Builder().addWhere("target", target).addWhere("type", type.name).build())
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
}
