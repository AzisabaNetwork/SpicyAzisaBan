package net.azisaba.spicyAzisaBan.util

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.punishment.UnPunish
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.struct.EventType
import net.azisaba.spicyAzisaBan.struct.Events
import xyz.acrylicstyle.sql.options.FindOptions
import java.sql.SQLException

class TimerTasks(private val connection: SQLConnection) {
    fun checkEvents(server: String = "%,${SABConfig.serverId},%") {
        try {
            val rs = connection.executeQuery("SELECT * FROM `events` WHERE `seen` NOT LIKE ?", server)
            while (rs.next()) {
                SpicyAzisaBan.LOGGER.info("Received event id ${rs.getLong("id")} (${rs.getString("event_id")})")
                val e = try {
                    Events.fromResultSet(rs)
                } catch (e: IllegalArgumentException) {
                    SpicyAzisaBan.LOGGER.warning("Failed to process event data. You might need a newer version of the plugin.")
                    e.printStackTrace()
                    continue
                }
                try {
                    if (e.event == EventType.ADD_PUNISHMENT) {
                        val id = e.data.getLong("id")
                        val p = Punishment.fetchActivePunishmentById(id).complete()
                        if (p == null) {
                            SpicyAzisaBan.debug("Ignoring event ${e.id} because the punishment #$id is no longer active")
                            continue
                        }
                        p.doSomethingIfOnline()
                        p.notifyToAll()
                    } else if (e.event == EventType.UPDATED_PUNISHMENT) {
                        val id = e.data.getLong("id")
                        if (id <= 0) {
                            SpicyAzisaBan.LOGGER.warning("Ignoring invalid event (invalid id): $e")
                            continue
                        }
                        val p = Punishment.fetchActivePunishmentById(id).complete()
                        if (p == null) {
                            SpicyAzisaBan.debug("Received update event for #$id but it is no longer active")
                            continue
                        }
                        p.clearCache()
                    } else if (e.event == EventType.REMOVED_PUNISHMENT) {
                        val id = e.data.getLong("punish_id")
                        if (id <= 0) {
                            SpicyAzisaBan.LOGGER.warning("Ignoring invalid event (invalid punish_id): $e")
                            continue
                        }
                        val p = Punishment.fetchPunishmentById(id).complete()
                        if (p == null) {
                            SpicyAzisaBan.debug("Punishment #$id was removed entirely, skipping")
                            continue
                        }
                        p.clearCache()
                        val td = connection.unpunish.findOne(FindOptions.Builder().addWhere("punish_id", id).build())
                            .complete()
                        if (td == null) {
                            SpicyAzisaBan.LOGGER.warning("Received removed_punishment event but there was no unpunish record")
                            continue
                        }
                        val unpunishRecord = UnPunish.fromTableData(p, td)
                        unpunishRecord.notifyToAll()
                    } else if (e.event == EventType.LOCKDOWN) {
                        Util.setLockdownAndAnnounce(e.data.getString("actor_name"), e.data.getBoolean("lockdown_enabled"))
                    } else {
                        SpicyAzisaBan.LOGGER.warning("Event $e was not handled")
                    }
                } catch (e: Exception) {
                    SpicyAzisaBan.LOGGER.warning("Received unprocessable event data")
                    e.printStackTrace()
                }
            }
            rs.statement.close()
            connection.execute("UPDATE `events` SET `seen` = concat(`seen`, ?) WHERE `seen` NOT LIKE ?", ",${SABConfig.serverId},", server)
        } catch (e: SQLException) {
            SpicyAzisaBan.LOGGER.severe("Could not check for new events")
            e.printStackTrace()
        }
    }

    fun sendWarningTitle() {
        try {
            val rs = connection.executeQuery(
                "SELECT * FROM `punishments` WHERE `type` = ? OR `type` = ?",
                "WARNING",
                "CAUTION",
            )
            val ps = mutableListOf<Punishment>()
            while (rs.next()) {
                ps.add(Punishment.fromResultSet(rs))
            }
            ps.filter { it.type == PunishmentType.CAUTION }.distinctBy { it.target }.forEach { p -> p.sendTitle() }
            ps.filter { it.type == PunishmentType.WARNING }.distinctBy { it.target }.forEach { p -> p.sendTitle() }
        } catch (e: SQLException) {
            SpicyAzisaBan.LOGGER.severe("Could not fetch punishments")
            e.printStackTrace()
        }
    }
}
