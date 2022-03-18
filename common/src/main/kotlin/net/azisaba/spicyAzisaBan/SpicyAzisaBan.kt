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
import net.azisaba.spicyAzisaBan.commands.LockdownCommand
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
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.common.scheduler.ScheduledTask
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.sql.migrations.DatabaseMigration
import net.azisaba.spicyAzisaBan.struct.EventType
import net.azisaba.spicyAzisaBan.util.TimerTasks
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.translate
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.UpsertOptions
import java.nio.file.Path
import java.util.Properties
import java.util.Timer
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.concurrent.scheduleAtFixedRate

@Suppress("LeakingThis")
abstract class SpicyAzisaBan {
    private val timer = Timer()
    lateinit var connection: SQLConnection
    lateinit var settings: Settings
    var lockdown = false
    
    companion object {
        val LOGGER: Logger = Logger.getLogger("SpicyAzisaBan")
        @JvmStatic
        val GROUP_PATTERN = "^[a-zA-Z0-9+_\\-]{1,32}$".toRegex()
        @JvmStatic
        lateinit var PREFIX: String
        @JvmStatic
        lateinit var instance: SpicyAzisaBan
        var wasInitialized = false

        private val startTime = System.currentTimeMillis()
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
            LOGGER.info(s)
            if (debugLevel >= 99999) Throwable("Debug").printStackTrace()
        }
    }

    init {
        if (wasInitialized) error("Cannot construct SpicyAzisaBan more than once")
        wasInitialized = true
        instance = this
        PREFIX = SABMessages.General.prefix.translate()
    }

    private fun initDatabase() {
        if (getPlatformType() != PlatformType.CLI) {
            LOGGER.info("Connecting to database...")
        }
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
        if (SABConfig.serverId != null) {
            // dismiss all events for this server
            val c = ",${SABConfig.serverId},"
            connection.execute("UPDATE `events` SET `seen` = CONCAT(`seen`, ?) WHERE `seen` NOT LIKE ?", c, "%$c%")
        }
        val version = settings.getDatabaseVersion().complete()
        if (version > SQLConnection.CURRENT_DATABASE_VERSION) {
            error("Cannot load the database that was used in a newer version of the plugin! Please update the plugin.\n" +
                    "Version stored in the database: $version\n" +
                    "Version stored in the plugin: ${SQLConnection.CURRENT_DATABASE_VERSION}")
        }
        lockdown = settings.isLockdown().complete()
        if (getPlatformType() != PlatformType.CLI) {
            LOGGER.info("Lockdown is " + if (lockdown) "enabled" else "disabled")
            LOGGER.info("Connected.")
        }
    }
    
    open fun doEnable() {
        if (SABConfig.debugBuild) debugLevel = 5
        if (SABConfig.prefix.contains("\\s+".toRegex())) throw IllegalArgumentException("prefix (in config.yml) contains whitespace")
        ReloadableSABConfig.reload()
        initDatabase()
        if (getPlatformType() != PlatformType.CLI) {
            DatabaseMigration.run().complete()
            LOGGER.info("Supported event types: ${EventType.values().joinToString(", ") { it.name.lowercase() }}")
        }
        val currentDatabaseVersion = settings.getDatabaseVersion().complete()
        if (currentDatabaseVersion != SQLConnection.CURRENT_DATABASE_VERSION) {
            throw IllegalStateException("Incompatible database version detected.\n" +
                    "Version stored in database: $currentDatabaseVersion\n" +
                    "Version stored in plugin: ${SQLConnection.CURRENT_DATABASE_VERSION}")
        }
        val timerTasks = TimerTasks(connection)
        if (SABConfig.serverId != null) {
            timer.scheduleAtFixedRate(10000, 10000) { timerTasks.checkEvents() }
        } else if (getPlatformType() != PlatformType.CLI) {
            LOGGER.warning("Disabled event check loop because serverId is null")
        }
        timer.scheduleAtFixedRate(SABConfig.Warning.sendTitleEvery, SABConfig.Warning.sendTitleEvery) { timerTasks.sendWarningTitle() }
        registerCommands()
    }

    private fun registerCommands() {
        instance.registerCommand(SABCommand)
        instance.registerCommand(GlobalBanCommand)
        instance.registerCommand(BanCommand)
        instance.registerCommand(GlobalTempBanCommand)
        instance.registerCommand(TempBanCommand)
        instance.registerCommand(GlobalIPBanCommand)
        instance.registerCommand(IPBanCommand)
        instance.registerCommand(GlobalTempIPBanCommand)
        instance.registerCommand(TempIPBanCommand)
        instance.registerCommand(GlobalMuteCommand)
        instance.registerCommand(MuteCommand)
        instance.registerCommand(GlobalTempMuteCommand)
        instance.registerCommand(TempMuteCommand)
        instance.registerCommand(GlobalIPMuteCommand)
        instance.registerCommand(IPMuteCommand)
        instance.registerCommand(GlobalTempIPMuteCommand)
        instance.registerCommand(TempIPMuteCommand)
        instance.registerCommand(GlobalWarningCommand)
        instance.registerCommand(WarningCommand)
        instance.registerCommand(GlobalCautionCommand)
        instance.registerCommand(CautionCommand)
        instance.registerCommand(GlobalKickCommand)
        instance.registerCommand(KickCommand)
        instance.registerCommand(GlobalNoteCommand)
        instance.registerCommand(NoteCommand)
        instance.registerCommand(UnBanCommand)
        instance.registerCommand(UnMuteCommand)
        instance.registerCommand(UnPunishCommand)
        instance.registerCommand(ChangeReasonCommand)
        instance.registerCommand(SeenCommand)
        instance.registerCommand(HistoryCommand)
        instance.registerCommand(CheckCommand)
        instance.registerCommand(BanListCommand)
        instance.registerCommand(WarnsCommand)
        instance.registerCommand(AddProofCommand)
        instance.registerCommand(DelProofCommand)
        instance.registerCommand(ProofsCommand)
        instance.registerCommand(LockdownCommand)
    }
    
    fun shutdownTimer() {
        timer.cancel()
    }

    abstract fun getPlatformType(): PlatformType
    abstract fun getPluginName(): String
    abstract fun getServerName(): String
    abstract fun getServerVersion(): String
    abstract fun getServers(): Map<String, ServerInfo>
    abstract fun getPlayers(): List<PlayerActor>
    abstract fun getPlayer(uuid: UUID): PlayerActor?
    abstract fun schedule(time: Long, unit: TimeUnit, runnable: () -> Unit): ScheduledTask
    abstract fun createTextComponentFromLegacyText(legacyText: String): Array<Component>
    abstract fun createTextComponent(content: String): Component
    abstract fun registerCommand(command: Command)
    abstract fun executeCommand(actor: Actor, command: String)
    abstract fun getConsoleActor(): Actor
    abstract fun getDataFolder(): Path

    class Settings {

        // Database version: Used when converting old database versions. Do not edit.

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

        // Lockdown state: Used when checking whether the lockdown is enabled (on startup)

        fun isLockdown(): Promise<Boolean> =
            instance.connection.settings.findOne(FindOptions.Builder().addWhere("key", "lockdown").build())
                .then { it?.getInteger("valueInt") == 1 }

        fun setLockdown(b: Boolean) =
            instance.connection.settings.upsert(
                UpsertOptions.Builder()
                    .addWhere("key", "lockdown")
                    .addValue("key", "lockdown")
                    .addValue("valueInt", if (b) 1 else 0)
                    .build()
            ).then { }
    }
}
