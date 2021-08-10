package net.azisaba.spicyAzisaBan.sql

import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.config.ServerInfo
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.DataType
import xyz.acrylicstyle.sql.Sequelize
import xyz.acrylicstyle.sql.Table
import xyz.acrylicstyle.sql.TableDefinition
import xyz.acrylicstyle.sql.options.FindOptions
import java.sql.SQLException
import java.util.Properties

class SQLConnection(host: String, name: String, user:String, password: String): Sequelize(host, name, user, password) {
    companion object {
        const val CURRENT_DATABASE_VERSION = 1
    }

    lateinit var punishments: Table
    lateinit var punishmentHistory: Table
    lateinit var groups: Table
    lateinit var serverGroup: Table
    lateinit var settings: Table

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
            TableDefinition.Builder("server", DataType.STRING).build(), // null = global; can be group name
        )
        // remove punishments associated with group when group disappears after the punishment
        punishments = this.define("punishments", dupe)
        punishmentHistory = this.define("punishmentHistory", dupe)
        groups = this.define(
            "groups",
            arrayOf(
                TableDefinition.Builder("id", DataType.STRING).setAllowNull(false).setPrimaryKey(true).build(),
            ),
        )
        serverGroup = this.define(
            "serverGroup",
            arrayOf(
                TableDefinition.Builder("server", DataType.STRING).setAllowNull(false).setPrimaryKey(true).build(),
                TableDefinition.Builder("group", DataType.STRING).setAllowNull(false).build(),
            ),
        )
        settings = this.define(
            "settings",
            arrayOf(
                TableDefinition.Builder("key", DataType.STRING).setPrimaryKey(true).build(),
                TableDefinition.Builder("valueString", DataType.STRING).build(),
                TableDefinition.Builder("valueInt", DataType.INT).build(),
            ),
        )
        this.sync()
    }

    fun getGroupByServer(server: String): Promise<String?> =
        serverGroup.findOne(FindOptions.Builder().addWhere("server", server).setLimit(1).build())
            .then { it?.getString("group") }

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