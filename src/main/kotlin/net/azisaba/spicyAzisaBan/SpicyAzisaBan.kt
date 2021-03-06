package net.azisaba.spicyAzisaBan

import net.azisaba.spicyAzisaBan.commands.AddProofCommand
import net.azisaba.spicyAzisaBan.commands.BanCommand
import net.azisaba.spicyAzisaBan.commands.BanListCommand
import net.azisaba.spicyAzisaBan.commands.CautionCommand
import net.azisaba.spicyAzisaBan.commands.ChangeReasonCommand
import net.azisaba.spicyAzisaBan.commands.CheckCommand
import net.azisaba.spicyAzisaBan.commands.DelProofCommand
import net.azisaba.spicyAzisaBan.commands.GlobalBanCommand
import net.azisaba.spicyAzisaBan.commands.GlobalCautionCommand
import net.azisaba.spicyAzisaBan.commands.GlobalIPBanCommand
import net.azisaba.spicyAzisaBan.commands.GlobalIPMuteCommand
import net.azisaba.spicyAzisaBan.commands.GlobalKickCommand
import net.azisaba.spicyAzisaBan.commands.GlobalMuteCommand
import net.azisaba.spicyAzisaBan.commands.GlobalNoteCommand
import net.azisaba.spicyAzisaBan.commands.GlobalTempBanCommand
import net.azisaba.spicyAzisaBan.commands.GlobalTempIPBanCommand
import net.azisaba.spicyAzisaBan.commands.GlobalTempIPMuteCommand
import net.azisaba.spicyAzisaBan.commands.GlobalTempMuteCommand
import net.azisaba.spicyAzisaBan.commands.GlobalWarningCommand
import net.azisaba.spicyAzisaBan.commands.HistoryCommand
import net.azisaba.spicyAzisaBan.commands.IPBanCommand
import net.azisaba.spicyAzisaBan.commands.IPMuteCommand
import net.azisaba.spicyAzisaBan.commands.KickCommand
import net.azisaba.spicyAzisaBan.commands.MuteCommand
import net.azisaba.spicyAzisaBan.commands.NoteCommand
import net.azisaba.spicyAzisaBan.commands.ProofsCommand
import net.azisaba.spicyAzisaBan.commands.SABCommand
import net.azisaba.spicyAzisaBan.commands.SeenCommand
import net.azisaba.spicyAzisaBan.commands.TempBanCommand
import net.azisaba.spicyAzisaBan.commands.TempIPBanCommand
import net.azisaba.spicyAzisaBan.commands.TempIPMuteCommand
import net.azisaba.spicyAzisaBan.commands.TempMuteCommand
import net.azisaba.spicyAzisaBan.commands.UnBanCommand
import net.azisaba.spicyAzisaBan.commands.UnMuteCommand
import net.azisaba.spicyAzisaBan.commands.UnPunishCommand
import net.azisaba.spicyAzisaBan.commands.WarningCommand
import net.azisaba.spicyAzisaBan.commands.WarnsCommand
import net.azisaba.spicyAzisaBan.listener.CheckBanListener
import net.azisaba.spicyAzisaBan.listener.CheckGlobalBanListener
import net.azisaba.spicyAzisaBan.listener.CheckMuteListener
import net.azisaba.spicyAzisaBan.listener.PlayerDisconnectListener
import net.azisaba.spicyAzisaBan.listener.PostLoginListener
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.punishment.UnPunish
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.sql.migrations.DatabaseMigration
import net.azisaba.spicyAzisaBan.struct.EventType
import net.azisaba.spicyAzisaBan.struct.Events
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.removeIf
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.plugin.Plugin
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.UpsertOptions
import java.sql.SQLException
import java.util.Properties
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

class SpicyAzisaBan: Plugin() {
    companion object {
        private val startTime = System.currentTimeMillis()
        @JvmField
        val GROUP_PATTERN = "^[a-zA-Z0-9+_\\-]{1,32}$".toRegex()
        @JvmStatic
        lateinit var PREFIX: String
        @JvmStatic
        lateinit var instance: SpicyAzisaBan

        @JvmStatic
        fun getUptime(): String = Util.unProcessTime(System.currentTimeMillis() - startTime)

        // Range: 0 - 99999
        //     0: off
        //     1: on
        //     2: + additional data/info
        //     3: + sql queries/executes
        // 99999: + dump stacktrace with debug message
        @JvmStatic
        var debugLevel: Int = 0

        @JvmStatic
        fun debug(s: String, minLevel: Int = 1) {
            if (debugLevel < minLevel) return
            instance.logger.info(s)
            if (debugLevel >= 99999) Throwable("Debug").printStackTrace()
        }
    }

    private val timer = Timer()
    lateinit var connection: SQLConnection
    lateinit var settings: Settings

    init {
        instance = this
        PREFIX = SABMessages.General.prefix.translate()
    }

    override fun onEnable() {
        debugLevel = 3
        if (SABConfig.prefix.contains("\\s+".toRegex())) throw IllegalArgumentException("prefix (in config.yml) contains whitespace")
        ReloadableSABConfig.reload()
        logger.info("Connecting to database...")
        connection = SQLConnection(
            SABConfig.database.host,
            SABConfig.database.name,
            SABConfig.database.user,
            SABConfig.database.password,
        )
        val props = Properties()
        props.setProperty("verifyServerCertificate", SABConfig.database.verifyServerCertificate.toString())
        props.setProperty("useSSL", SABConfig.database.useSSL.toString())
        connection.connect(props)
        settings = Settings()
        // dismiss all events for this server
        val c = "%,${SABConfig.serverId},%"
        connection.execute("UPDATE `events` SET `seen` = CONCAT(`seen`, ?) WHERE `seen` NOT LIKE ?", ",${SABConfig.serverId},", c)
        logger.info("Connected.")
        val version = settings.getDatabaseVersion().complete()
        if (version > SQLConnection.CURRENT_DATABASE_VERSION) {
            error("Cannot load the database that was used in a newer version of the plugin! Please update the plugin.\n" +
                    "Version stored in the database: $version\n" +
                    "Version stored in the plugin: ${SQLConnection.CURRENT_DATABASE_VERSION}")
        }
        DatabaseMigration.run().complete()
        logger.info("Supported event types: ${EventType.values().joinToString(", ") { it.name.lowercase() }}")
        timer.scheduleAtFixedRate(10000, 10000) {
            try {
                val rs = connection.executeQuery("SELECT * FROM `events` WHERE `seen` NOT LIKE ?", c)
                while (rs.next()) {
                    logger.info("Received event id ${rs.getLong("id")} (${rs.getString("event_id")})")
                    val e = try {
                        Events.fromResultSet(rs)
                    } catch (e: IllegalArgumentException) {
                        logger.warning("Failed to process event data. You might need a newer version of the plugin.")
                        e.printStackTrace()
                        continue
                    }
                    try {
                        if (e.event == EventType.ADD_PUNISHMENT) {
                            val id = e.data.getLong("id")
                            val p = Punishment.fetchActivePunishmentById(id).complete()
                            if (p == null) {
                                debug("Ignoring event ${e.id} because the punishment #$id is no longer active")
                                continue
                            }
                            p.doSomethingIfOnline()
                            p.notifyToAll()
                        } else if (e.event == EventType.UPDATED_PUNISHMENT) {
                            val id = e.data.getLong("id")
                            if (id <= 0) {
                                logger.warning("Ignoring invalid event (invalid id): $e")
                                continue
                            }
                            debug("Removing cache of punishment $id (event received)")
                            Punishment.canJoinServerCachedData.removeIf { _, value -> value.get()?.id == id }
                            Punishment.muteCache.removeIf { _, value -> value.get()?.id == id }
                        } else if (e.event == EventType.REMOVED_PUNISHMENT) {
                            val id = e.data.getLong("punish_id")
                            if (id <= 0) {
                                logger.warning("Ignoring invalid event (invalid punish_id): $e")
                                continue
                            }
                            debug("Removing cache of punishment $id (event received)")
                            Punishment.canJoinServerCachedData.removeIf { _, value -> value.get()?.id == id }
                            Punishment.muteCache.removeIf { _, value -> value.get()?.id == id }
                            val punishment = Punishment.fetchPunishmentById(id).complete()
                            if (punishment == null) {
                                debug("Punishment #$id was removed entirely, skipping")
                                continue
                            }
                            val td = connection.unpunish.findOne(FindOptions.Builder().addWhere("punish_id", id).build()).complete()
                            if (td == null) {
                                logger.warning("Received removed_punishment event but there was no unpunish record")
                                continue
                            }
                            val unpunishRecord = UnPunish.fromTableData(punishment, td)
                            unpunishRecord.notifyToAll()
                        } else {
                            logger.warning("Unknown event: $e")
                        }
                    } catch (e: Exception) {
                        logger.warning("Received unprocessable event data")
                        e.printStackTrace()
                    }
                }
                rs.statement.close()
                connection.execute("UPDATE `events` SET `seen` = concat(`seen`, ?) WHERE `seen` NOT LIKE ?", ",${SABConfig.serverId},", c)
            } catch (e: SQLException) {
                logger.severe("Could not check for new events")
                e.printStackTrace()
            }
        }
        timer.scheduleAtFixedRate(SABConfig.Warning.sendTitleEvery, SABConfig.Warning.sendTitleEvery) {
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
                logger.severe("Could not fetch punishments")
                e.printStackTrace()
            }
        }
        if (!SABConfig.debugBuild) debugLevel = 0
        proxy.pluginManager.registerListener(this, CheckGlobalBanListener)
        proxy.pluginManager.registerListener(this, CheckBanListener)
        proxy.pluginManager.registerListener(this, PostLoginListener)
        proxy.pluginManager.registerListener(this, CheckMuteListener)
        proxy.pluginManager.registerListener(this, PlayerDisconnectListener)
        proxy.pluginManager.registerCommand(this, SABCommand)
        proxy.pluginManager.registerCommand(this, GlobalBanCommand)
        proxy.pluginManager.registerCommand(this, BanCommand)
        proxy.pluginManager.registerCommand(this, GlobalTempBanCommand)
        proxy.pluginManager.registerCommand(this, TempBanCommand)
        proxy.pluginManager.registerCommand(this, GlobalIPBanCommand)
        proxy.pluginManager.registerCommand(this, IPBanCommand)
        proxy.pluginManager.registerCommand(this, GlobalTempIPBanCommand)
        proxy.pluginManager.registerCommand(this, TempIPBanCommand)
        proxy.pluginManager.registerCommand(this, GlobalMuteCommand)
        proxy.pluginManager.registerCommand(this, MuteCommand)
        proxy.pluginManager.registerCommand(this, GlobalTempMuteCommand)
        proxy.pluginManager.registerCommand(this, TempMuteCommand)
        proxy.pluginManager.registerCommand(this, GlobalIPMuteCommand)
        proxy.pluginManager.registerCommand(this, IPMuteCommand)
        proxy.pluginManager.registerCommand(this, GlobalTempIPMuteCommand)
        proxy.pluginManager.registerCommand(this, TempIPMuteCommand)
        proxy.pluginManager.registerCommand(this, GlobalWarningCommand)
        proxy.pluginManager.registerCommand(this, WarningCommand)
        proxy.pluginManager.registerCommand(this, GlobalCautionCommand)
        proxy.pluginManager.registerCommand(this, CautionCommand)
        proxy.pluginManager.registerCommand(this, GlobalKickCommand)
        proxy.pluginManager.registerCommand(this, KickCommand)
        proxy.pluginManager.registerCommand(this, GlobalNoteCommand)
        proxy.pluginManager.registerCommand(this, NoteCommand)
        proxy.pluginManager.registerCommand(this, UnBanCommand)
        proxy.pluginManager.registerCommand(this, UnMuteCommand)
        proxy.pluginManager.registerCommand(this, UnPunishCommand)
        proxy.pluginManager.registerCommand(this, ChangeReasonCommand)
        proxy.pluginManager.registerCommand(this, SeenCommand)
        proxy.pluginManager.registerCommand(this, HistoryCommand)
        proxy.pluginManager.registerCommand(this, CheckCommand)
        proxy.pluginManager.registerCommand(this, BanListCommand)
        proxy.pluginManager.registerCommand(this, WarnsCommand)
        proxy.pluginManager.registerCommand(this, AddProofCommand)
        proxy.pluginManager.registerCommand(this, DelProofCommand)
        proxy.pluginManager.registerCommand(this, ProofsCommand)
        logger.info("Hewwwwwwwwwoooooo!")
    }

    override fun onDisable() {
        timer.cancel()
        logger.info("Closing database connection")
        connection.close()
        debugLevel = 0
        logger.info("Goodbye, World!")
    }

    class Settings {

        // Database version: Used to convert old database versions. Do not edit.

        fun getDatabaseVersion(): Promise<Int> =
            instance.connection.settings.findOne(FindOptions.Builder().addWhere("key", "database_version").build())
                .then { it?.getInteger("valueInt") ?: SQLConnection.CURRENT_DATABASE_VERSION }

        fun setDatabaseVersion(i: Int): Promise<Unit> =
            instance.connection.settings.upsert(
                UpsertOptions.Builder()
                    .addWhere("key", "database_version")
                    .addValue("key", "database_version")
                    .addValue("valueInt", i)
                    .build()
            ).then { }
    }
}
