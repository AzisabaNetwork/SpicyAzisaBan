package net.azisaba.spicyAzisaBan

import net.azisaba.spicyAzisaBan.commands.BanCommand
import net.azisaba.spicyAzisaBan.commands.GlobalBanCommand
import net.azisaba.spicyAzisaBan.commands.GlobalIPBanCommand
import net.azisaba.spicyAzisaBan.commands.GlobalTempBanCommand
import net.azisaba.spicyAzisaBan.commands.IPBanCommand
import net.azisaba.spicyAzisaBan.commands.SABCommand
import net.azisaba.spicyAzisaBan.commands.TempBanCommand
import net.azisaba.spicyAzisaBan.listener.CheckBanListener
import net.azisaba.spicyAzisaBan.listener.CheckGlobalBanListener
import net.azisaba.spicyAzisaBan.listener.PostLoginListener
import net.azisaba.spicyAzisaBan.listener.PreloadPermissionsOnJoinListener
import net.azisaba.spicyAzisaBan.sql.migrations.DatabaseMigration
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.plugin.Plugin
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.UpsertOptions
import java.sql.SQLException
import java.util.Properties
import java.util.Timer
import java.util.TimerTask

class SpicyAzisaBan: Plugin() {
    companion object {
        private val startTime = System.currentTimeMillis()
        @JvmField
        val GROUP_PATTERN = "^[a-zA-Z0-9+_\\-]{1,32}$".toRegex()
        @JvmField
        val PREFIX = SABMessages.General.prefix.translate()
        @JvmStatic
        lateinit var instance: SpicyAzisaBan

        @JvmStatic
        fun getUptime(): String = Util.unProcessTime(System.currentTimeMillis() - startTime)

        // Range: 0 - 99999
        //     0: off
        //     1: on
        //     2: + additional data/info
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
    }

    override fun onEnable() {
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
        logger.info("Connected.")
        val version = settings.getDatabaseVersion().complete()
        if (version > SQLConnection.CURRENT_DATABASE_VERSION) {
            error("Cannot load the database that was used in a newer version of the plugin! Please update the plugin.\n" +
                    "Version stored in the database: $version\n" +
                    "Version stored in the plugin: ${SQLConnection.CURRENT_DATABASE_VERSION}")
        }
        DatabaseMigration.run().complete()
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                try {
                    val statement = connection.connection.createStatement()
                    statement.execute("SELECT 1;")
                    statement.close()
                } catch (e: SQLException) {
                    logger.warning("Could not execute keep-alive ping")
                    throw e
                }
            }
        }, SABConfig.database.keepAlive * 1000L, SABConfig.database.keepAlive * 1000L)
        proxy.pluginManager.registerListener(this, PreloadPermissionsOnJoinListener)
        proxy.pluginManager.registerListener(this, CheckGlobalBanListener)
        proxy.pluginManager.registerListener(this, CheckBanListener)
        proxy.pluginManager.registerListener(this, PostLoginListener)
        proxy.pluginManager.registerCommand(this, SABCommand)
        proxy.pluginManager.registerCommand(this, GlobalBanCommand)
        proxy.pluginManager.registerCommand(this, BanCommand)
        proxy.pluginManager.registerCommand(this, GlobalTempBanCommand)
        proxy.pluginManager.registerCommand(this, TempBanCommand)
        proxy.pluginManager.registerCommand(this, GlobalIPBanCommand)
        proxy.pluginManager.registerCommand(this, IPBanCommand)
        logger.info("Hewwwwwwwwwoooooo!")
    }

    override fun onDisable() {
        logger.info("Closing database connection")
        connection.close()
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
