package net.azisaba.spicyAzisaBan.sql

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.struct.EventType
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.async
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import util.concurrent.ref.DataCache
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.DataType
import xyz.acrylicstyle.sql.Sequelize
import xyz.acrylicstyle.sql.Table
import xyz.acrylicstyle.sql.TableDefinition
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.SQLNonTransientConnectionException
import java.sql.Statement
import java.util.Properties

class SQLConnection(host: String, name: String, user:String, password: String): Sequelize(host, name, user, password) {
    companion object {
        const val CURRENT_DATABASE_VERSION = 8

        fun logSql(s: String, time: Long) {
            SpicyAzisaBan.debug("Executed SQL: $s (took $time ms)", 3)
        }

        fun logSql(s: String, params: Array<out Any>) {
            SpicyAzisaBan.debug("Executing SQL: '$s' with params: ${params.toList()}", 3)
        }

        fun Statement.executeAndLog(@Language("SQL") sql: String): Boolean {
            val start = System.currentTimeMillis()
            val result = this.execute(sql)
            logSql(sql, System.currentTimeMillis() - start)
            return result
        }
    }

    internal val eventsByUs = mutableListOf<Long>()
    lateinit var properties: Properties
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
    lateinit var events: Table

    fun isConnected() =
        try {
            connection != null && !connection.isClosed && connection.isValid(1000)
        } catch (e: SQLException) {
            false
        }

    fun connect(properties: Properties) {
        if (isConnected()) return
        this.properties = properties
        this.authenticate(getMariaDBDriver(), properties)
        this.definitions.clear()
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
                TableDefinition.Builder("ip", DataType.STRING).setAllowNull(true).build(),
                TableDefinition.Builder("last_seen", DataType.BIGINT).setAllowNull(false).setDefaultValue(0L).build(),
                TableDefinition.Builder("first_login", DataType.BIGINT).setAllowNull(false).setDefaultValue(0L).build(),
                TableDefinition.Builder("first_login_attempt", DataType.BIGINT).setAllowNull(false).setDefaultValue(0L).build(),
                TableDefinition.Builder("last_login", DataType.BIGINT).setAllowNull(false).setDefaultValue(0L).build(),
                TableDefinition.Builder("last_login_attempt", DataType.BIGINT).setAllowNull(false).setDefaultValue(0L).build(),
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
        events = this.define(
            "events",
            arrayOf(
                TableDefinition.Builder("id", DataType.BIGINT).setPrimaryKey(true).setAutoIncrement(true).build(),
                TableDefinition.Builder("event_id", DataType.STRING).setAllowNull(false).build(),
                TableDefinition.Builder("data", DataType.TEXT).setAllowNull(false).build(),
                TableDefinition.Builder("handled", DataType.BOOL).setAllowNull(false).setDefaultValue(0).build(),
            ),
        ).setupEventListener()
        this.sync()
    }

    fun execute(@Language("SQL") sql: String, vararg params: Any): Boolean {
        logSql(sql, params)
        connection.prepareStatement(sql).use { statement ->
            params.forEachIndexed { index, any -> statement.setObject(index + 1, any) }
            return statement.execute()
        }
    }

    fun executeQuery(@Language("SQL") sql: String, vararg params: Any): ResultSet {
        return executeQuery(sql, *params, retry = false)
    }

    private fun executeQuery(@Language("SQL") sql: String, vararg params: Any, retry: Boolean): ResultSet {
        logSql(sql, params)
        val statement = connection.prepareStatement(sql)
        params.forEachIndexed { index, any -> statement.setObject(index + 1, any) }
        return try {
            statement.executeQuery()
        } catch (e: SQLNonTransientConnectionException) {
            if (retry) throw e
            connect(properties)
            executeQuery(sql, *params, retry = true)
        } finally {
            statement.close()
        }
    }

    fun sendEvent(eventType: EventType, data: JSONObject, handled: Boolean = true): Promise<Unit> = async {
        val id = try {
            Util.insert {
                events.insert(
                    InsertOptions.Builder()
                        .addValue("event_id", eventType.name.lowercase())
                        .addValue("data", data.toString())
                        .addValue("handled", handled)
                        .build()
                ).complete()
            }
        } catch (e: Exception) {
            SpicyAzisaBan.LOGGER.warning("Failed to send event with type $eventType and data $data")
            e.printStackTrace()
            it.resolve()
            return@async
        }
        eventsByUs.add(id)
        SpicyAzisaBan.debug("Sending event with id $id")
        SpicyAzisaBan.debug("Event type: $eventType")
        SpicyAzisaBan.debug("Event data: ${data.toString(2)}")
        it.resolve()
    }

    private fun Table.setupEventListener(): Table {
        eventEmitter.on(Table.Events.EXECUTED) {
            val sql = it[0] as String
            logSql(sql, it[1] as Long)
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

    private val cachedGroupByServer = mutableMapOf<String, DataCache<String>>()
    private var updatingCacheGroupByServer = false
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

    fun getCachedGroupByServerOrFetch(server: String): Promise<String?> {
        if (server == "global") return Promise.resolve(null)
        val group = cachedGroupByServer[server]
        if (group == null || group.ttl - System.currentTimeMillis() < 10000) {
            updatingCacheGroupByServer = true
            return serverGroup.findOne(FindOptions.Builder().addWhere("server", server).setLimit(1).build())
                .then {
                    val g = it?.getString("group")
                    cachedGroupByServer[server] = DataCache(g, System.currentTimeMillis() + 1000 * 60)
                    updatingCacheGroupByServer = false
                    return@then g
                }
        }
        return Promise.resolve(group.get())
    }

    fun isGroupExists(group: String): Promise<Boolean> =
        groups.findOne(FindOptions.Builder().addWhere("id", group).build()).then { it != null }

    fun getGroupByServer(server: String): Promise<String?> {
        if (server == "global") return Promise.resolve(null)
        return serverGroup.findOne(FindOptions.Builder().addWhere("server", server).setLimit(1).build())
            .then { it?.getString("group") }
    }

    fun getServersByGroup(groupId: String): Promise<List<String>> =
        serverGroup.findAll(FindOptions.Builder().addWhere("group", groupId).build())
            .then { it.mapNotNull { td -> td.getString("server") } }

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

    fun isTableExists(table: String): Promise<Boolean> = async {
        val rs = executeQuery("SHOW TABLES LIKE ?", table)
        val exists = rs.next()
        rs.statement.close()
        it.resolve(exists)
    }
}
