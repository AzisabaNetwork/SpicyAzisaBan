package net.azisaba.spicyAzisaBan

import net.azisaba.spicyAzisaBan.listener.PreloadPermissionsOnJoinListener
import net.azisaba.spicyAzisaBan.migrations.DatabaseMigration
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.api.ChatColor
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.UpsertOptions
import java.sql.SQLException
import java.util.Properties
import java.util.Timer
import java.util.TimerTask

class SpicyAzisaBan: Plugin() {
    companion object {
        val GROUP_PATTERN = "^[a-zA-Z0-9+_\\-]{1,32}$".toRegex()
        val PREFIX = "${ChatColor.RED}${ChatColor.BOLD}SpicyAzisaBan ${ChatColor.DARK_GRAY}${ChatColor.BOLD}≫ ${ChatColor.RESET}"
        lateinit var instance: SpicyAzisaBan
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
        proxy.pluginManager.registerCommand(this, SABCommand)
        logger.info("Hewwwwwwwwwoooooo!")
    }

    class Settings {
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
