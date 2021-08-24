package net.azisaba.spicyAzisaBan.sql

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.config.ServerInfo
import org.intellij.lang.annotations.Language
import util.promise.rewrite.Promise
import util.ref.DataCache
import xyz.acrylicstyle.sql.DataType
import xyz.acrylicstyle.sql.Sequelize
import xyz.acrylicstyle.sql.Table
import xyz.acrylicstyle.sql.TableDefinition
import xyz.acrylicstyle.sql.options.FindOptions
import java.sql.SQLException
import java.sql.Statement
import java.util.Properties

class SQLConnection(host: String, name: String, user:String, password: String): Sequelize(host, name, user, password) {
    companion object {
        const val CURRENT_DATABASE_VERSION = 5

        fun logSql(s: String) {
            SpicyAzisaBan.debug("Executing SQL: $s", 3)
        }

        fun Statement.executeAndLog(@Language("SQL") sql: String): Boolean {
            logSql(sql)
            return this.execute(sql)
        }
    }

    lateinit var punishments: Table
    lateinit var punishmentHistory: Table
    lateinit var groups: Table
    lateinit var serverGroup: Table
    lateinit var settings: Table
    lateinit var unpunish: Table
    lateinit var proofs: Table
    lateinit var players: Table
    lateinit var usernameHistory: Table
    lateinit var ipAddressHistory: Table

    fun isConnected() =
        try {
            connection != null && !connection.isClosed && connection.isValid(1000)
        } catch (e: SQLException) {
            false
        }

    fun connect(properties: Properties) {
        if (isConnected()) return
        this.authenticate(getMariaDBDriver(), properties)
        val dupe = arrayOf(
            TableDefinition.Builder("id", DataType.BIGINT).setAutoIncrement(true).setPrimaryKey(true).build(), // punish id
            TableDefinition.Builder("name", DataType.STRING).setAllowNull(false).build(), // player name
            TableDefinition.Builder("target", DataType.STRING).setAllowNull(false).build(), // player uuid or IP if ip ban
            TableDefinition.Builder("reason", DataType.STRING).setAllowNull(false).build(), // punish reason
            TableDefinition.Builder("operator", DataType.STRING).setAllowNull(false).build(), // operator uuid
            TableDefinition.Builder("type", DataType.STRING).setAllowNull(false).build(), // type (see PunishmentType)
            TableDefinition.Builder("start", DataType.BIGINT).setAllowNull(false).build(),
            TableDefinition.Builder("end", DataType.BIGINT).setAllowNull(false).build(), // -1 means permanent, otherwise temporary
            TableDefinition.Builder("server", DataType.STRING).setAllowNull(false).build(), // "global", server or group name
            TableDefinition.Builder("extra", DataType.STRING).setDefaultValue("").setAllowNull(false).build(), // Punishment.Flags
        )
        // remove punishments associated with group when group disappears after the punishment
        punishments = this.define("punishments", dupe).setupEventListener()
        punishmentHistory = this.define("punishmentHistory", dupe).setupEventListener()
        groups = this.define(
            "groups",
            arrayOf(
                TableDefinition.Builder("id", DataType.STRING).setAllowNull(false).setPrimaryKey(true).build(),
            ),
        ).setupEventListener()
        serverGroup = this.define(
            "serverGroup",
            arrayOf(
                TableDefinition.Builder("server", DataType.STRING).setAllowNull(false).setPrimaryKey(true).build(),
                TableDefinition.Builder("group", DataType.STRING).setAllowNull(false).build(),
            ),
        ).setupEventListener()
        settings = this.define(
            "settings",
            arrayOf(
                TableDefinition.Builder("key", DataType.STRING).setPrimaryKey(true).build(),
                TableDefinition.Builder("valueString", DataType.STRING).build(),
                TableDefinition.Builder("valueInt", DataType.INT).build(),
            ),
        ).setupEventListener()
        unpunish = this.define(
            "unpunish",
            arrayOf(
                TableDefinition.Builder("id", DataType.BIGINT).setAutoIncrement(true).setPrimaryKey(true).build(), // unpunish id
                TableDefinition.Builder("punish_id", DataType.BIGINT).setAllowNull(false).build(),
                TableDefinition.Builder("reason", DataType.STRING).setAllowNull(false).build(),
                TableDefinition.Builder("timestamp", DataType.BIGINT).setAllowNull(false).build(),
                TableDefinition.Builder("operator", DataType.STRING).setAllowNull(false).build(),
            ),
        ).setupEventListener()
        proofs = this.define(
            "proofs",
            arrayOf(
                TableDefinition.Builder("id", DataType.BIGINT).setAutoIncrement(true).setPrimaryKey(true).build(),
                TableDefinition.Builder("punish_id", DataType.BIGINT).setAllowNull(false).build(),
                TableDefinition.Builder("text", DataType.STRING).setAllowNull(false).build(),
            ),
        ).setupEventListener()
        players = this.define(
            "players",
            arrayOf(
                TableDefinition.Builder("uuid", DataType.STRING).setPrimaryKey(true).build(),
                TableDefinition.Builder("name", DataType.STRING).setAllowNull(false).build(),
                TableDefinition.Builder("ip", DataType.STRING).setAllowNull(true).build(), // could be unix socket
                TableDefinition.Builder("last_seen", DataType.BIGINT).setAllowNull(false).setDefaultValue(0L).build(),
            ),
        ).setupEventListener()
        usernameHistory = this.define(
            "usernameHistory",
            arrayOf(
                TableDefinition.Builder("uuid", DataType.STRING).setAllowNull(false).build(),
                TableDefinition.Builder("name", DataType.STRING).setAllowNull(false).build(),
                TableDefinition.Builder("last_seen", DataType.BIGINT).setAllowNull(false).build(),
            ),
        ).setupEventListener()
        ipAddressHistory = this.define(
            "ipAddressHistory",
            arrayOf(
                TableDefinition.Builder("uuid", DataType.STRING).setAllowNull(false).build(),
                TableDefinition.Builder("ip", DataType.STRING).setAllowNull(false).build(),
                TableDefinition.Builder("last_seen", DataType.BIGINT).setAllowNull(false).build(),
            ),
        ).setupEventListener()
        this.sync()
    }

    private fun Table.setupEventListener(): Table {
        eventEmitter.on(Table.Events.EXECUTE) {
            val sql = it[0] as String
            logSql(sql)
        }
        return this
    }

    var cachedGroups = DataCache<List<String>>()
        private set
    @Volatile private var updatingCache = false
    fun getCachedGroups(): List<String>? {
        val groups = cachedGroups.get()
        if (groups == null || cachedGroups.ttl - System.currentTimeMillis() < 10000) { // update if cache is expired or expiring in under 10 seconds
            if (!updatingCache) {
                updatingCache = true
                getAllGroups().then {
                    cachedGroups = DataCache(it, System.currentTimeMillis() + 1000 * 60)
                    updatingCache = false
                }
            }
        }
        return groups
    }

    val cachedGroupByServer = mutableMapOf<String, DataCache<String>>()
    var updatingCacheGroupByServer = false
    fun getCachedGroupByServer(server: String): String? {
        if (server == "global") return null
        val group = cachedGroupByServer[server]
        if (group == null || group.ttl - System.currentTimeMillis() < 10000) {
            if (!updatingCacheGroupByServer) {
                updatingCacheGroupByServer = true
                serverGroup.findOne(FindOptions.Builder().addWhere("server", server).setLimit(1).build())
                    .then {
                        val g = it?.getString("group")
                        cachedGroupByServer[server] = DataCache(g, System.currentTimeMillis() + 1000 * 60)
                        updatingCacheGroupByServer = false
                    }
            }
        }
        return group?.get()
    }

    fun isGroupExists(group: String): Promise<Boolean> =
        SpicyAzisaBan.instance.connection.groups
            .findOne(FindOptions.Builder().addWhere("id", group).build())
            .then { it != null }

    fun getGroupByServer(server: String): Promise<String?> {
        if (server == "global") return Promise.resolve(null)
        return serverGroup.findOne(FindOptions.Builder().addWhere("server", server).setLimit(1).build())
            .then { it?.getString("group") }
    }

    fun getServersByGroup(groupId: String): Promise<List<ServerInfo>> =
        serverGroup.findAll(FindOptions.Builder().addWhere("group", groupId).build())
            .then { it.mapNotNull { td -> ProxyServer.getInstance().getServerInfo(td.getString("server")) } }

    fun getAllServerGroups(): Promise<Map<String, List<String>>> =
        serverGroup.findAll(FindOptions.ALL).then {
            val map = mutableMapOf<String, MutableList<String>>()
            it.forEach { td ->
                val group = td.getString("group")
                val l = map.getOrPut(group) { mutableListOf() }
                l.add(td.getString("server"))
            }
            return@then map
        }

    fun getAllGroups(): Promise<List<String>> =
        groups.findAll(FindOptions.ALL).then { it.map { td -> td.getString("id") } }
}
