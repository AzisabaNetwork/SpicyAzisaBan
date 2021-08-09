package net.azisaba.spicyAzisaBan

import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.api.ChatColor
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
        logger.info("Connected.")
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
        proxy.pluginManager.registerCommand(this, SABCommand)
        logger.info("Hewwwwwwwwwoooooo!")
    }
}
