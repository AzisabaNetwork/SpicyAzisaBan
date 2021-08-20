package net.azisaba.spicyAzisaBan.struct

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.util.Util.getIPAddress
import net.azisaba.spicyAzisaBan.util.Util.insertNoId
import net.md_5.bungee.api.connection.ProxiedPlayer
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.mcutil.common.PlayerProfile
import xyz.acrylicstyle.mcutil.mojang.MojangAPI
import xyz.acrylicstyle.sql.TableData
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions
import xyz.acrylicstyle.sql.options.Sort
import xyz.acrylicstyle.sql.options.UpsertOptions
import java.util.UUID

data class PlayerData(
    val uuid: UUID,
    @get:JvmName("getName0")
    val name: String,
    val ip: String?,
    val lastSeen: Long,
): PlayerProfile {
    companion object {
        fun fromTableData(td: TableData): PlayerData {
            val uuid = UUID.fromString(td.getString("uuid")!!)
            val name = td.getString("name")!!
            val ip = td.getString("ip")!!
            val lastSeen = td.getLong("last_seen")!!
            return PlayerData(uuid, name, ip, lastSeen)
        }

        fun getByIP(ip: String): Promise<List<PlayerData>> =
            SpicyAzisaBan.instance.connection.players.findAll(FindOptions.Builder().addWhere("ip", ip).build())
                .then { list -> list.map { td -> fromTableData(td) } }

        fun getAllByIP(ip: String): Promise<List<PlayerData>> =
            SpicyAzisaBan.instance.connection.ipAddressHistory.findAll(FindOptions.Builder().addWhere("ip", ip).build())
                .then { list -> list.distinctBy { td -> td.getString("uuid") } }
                .then { list -> list.map { td -> UUID.fromString(td.getString("uuid")) } }
                .then { list -> list.map { uuid -> getByUUID(uuid).complete() } }

        fun getByUUID(uuid: UUID): Promise<PlayerData> =
            SpicyAzisaBan.instance.connection.players.findOne(FindOptions.Builder().addWhere("uuid", uuid.toString()).setLimit(1).build())
                .then { td -> td?.let { fromTableData(it) } ?: error("no player data found for $uuid") }

        fun getByUUID(uuid: String): Promise<PlayerData> =
            SpicyAzisaBan.instance.connection.players.findOne(FindOptions.Builder().addWhere("uuid", uuid).setLimit(1).build())
                .then { td -> td?.let { fromTableData(it) } ?: error("no player data found for $uuid") }

        fun getByName(name: String): Promise<PlayerData> = Promise.create { context ->
            val sql = "SELECT * FROM `players` WHERE LOWER(`name`) LIKE LOWER(?) ORDER BY `last_seen` DESC LIMIT 1"
            SQLConnection.logSql(sql)
            val ps = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
            ps.setString(1, "%$name%")
            val rs = ps.executeQuery()
            if (!rs.next()) {
                MojangAPI.getUniqueId(name)
                    .then { uuid ->
                        val sql2 = "INSERT INTO `players` (`uuid`, `name`, `ip`, `last_seen`) VALUES (?, ?, ?, ?)"
                        SQLConnection.logSql(sql2)
                        val ps2 = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql2)
                        ps2.setString(1, uuid.toString())
                        ps2.setString(2, name)
                        ps2.setString(3, "1.1.1.1")
                        ps2.setLong(4, System.currentTimeMillis())
                        insertNoId {
                            ps2.executeUpdate()
                        }
                        PlayerData(uuid, name, "1.1.1.1", System.currentTimeMillis())
                    }
                    .catch { context.reject(IllegalArgumentException("no player data found for $name")) }
                    .complete()
                    ?.let { return@create context.resolve(it) }
                return@create
            }
            val uuid = UUID.fromString(rs.getString("uuid"))
            val theName = rs.getString("name")
            val ip = rs.getString("ip")
            val lastSeen = rs.getLong("last_seen")
            return@create context.resolve(PlayerData(uuid, theName, ip, lastSeen))
        }

        /*
        fun getByName(name: String): Promise<PlayerData> =
            SpicyAzisaBan.instance.connection.players.findOne(FindOptions.Builder().addWhere("name", name).setOrderBy("last_seen").setOrder(Sort.DESC).setLimit(1).build())
                .then { td -> td?.let { fromTableData(it) } ?: error("no player data found for $name") }
        */

        fun createOrUpdate(player: ProxiedPlayer): Promise<PlayerData> = Promise.create { context ->
            val time = System.currentTimeMillis()
            val name = SpicyAzisaBan.instance.connection.usernameHistory
                .findOne(FindOptions.Builder().addWhere("uuid", player.uniqueId.toString()).setOrderBy("last_seen").setOrder(Sort.DESC).setLimit(1).build())
                .then { td -> td?.getString("name") }
                .complete()
            if (name != player.name.lowercase()) {
                SpicyAzisaBan.debug("Updating usernameHistory of ${player.name} (${player.uniqueId}) (old: $name)")
                insertNoId {
                    SpicyAzisaBan.instance.connection.usernameHistory.insert(
                        InsertOptions.Builder()
                            .addValue("uuid", player.uniqueId.toString())
                            .addValue("name", player.name.lowercase())
                            .addValue("last_seen", System.currentTimeMillis())
                            .build()
                    ).catch { it.printStackTrace() }.complete()
                }
            }
            val addr = player.socketAddress.getIPAddress()
            if (addr != null) {
                val ip = SpicyAzisaBan.instance.connection.ipAddressHistory
                    .findOne(FindOptions.Builder().addWhere("uuid", player.uniqueId.toString()).addWhere("ip", addr).build())
                    .then { td -> td?.getString("ip") }
                    .catch { it.printStackTrace() }
                    .complete()
                if (ip == null) {
                    SpicyAzisaBan.debug("Updating ipAddressHistory of ${player.name} (${player.uniqueId}) (old: $ip)")
                    insertNoId {
                        SpicyAzisaBan.instance.connection.ipAddressHistory.insert(
                            InsertOptions.Builder()
                                .addValue("uuid", player.uniqueId.toString())
                                .addValue("ip", addr)
                                .addValue("last_seen", System.currentTimeMillis())
                                .build()
                        ).catch { it.printStackTrace() }.complete()
                    }
                }
            }
            insertNoId {
                SpicyAzisaBan.instance.connection.players.upsert(
                    UpsertOptions.Builder()
                        .addWhere("uuid", player.uniqueId.toString())
                        .addValue("uuid", player.uniqueId.toString())
                        .addValue("name", player.name)
                        .addValue("ip", addr)
                        .addValue("last_seen", time)
                        .build()
                ).catch { it.printStackTrace() }.complete()
                context.resolve(PlayerData(player.uniqueId, player.name, addr, time))
            }
        }

        fun updateFromMojangAPI(uuid: UUID): Promise<Unit> {
            if (uuid.version() != 4) error("UUID must be Mojang-assigned (version 4)")
            return MojangAPI.getName(uuid).thenDo { name ->
                SpicyAzisaBan.instance.connection.players
                    .update("name", name, FindOptions.Builder().addWhere("uuid", uuid.toString()).build())
                    .complete()
            }.then {}
        }
    }

    fun getUsernameHistory(): Promise<List<String>> =
        SpicyAzisaBan.instance.connection.usernameHistory.findAll(
            FindOptions.Builder().addWhere("uuid", uuid.toString()).setOrderBy("last_seen").setOrder(Sort.DESC).build()
        ).then { list -> list.map { td -> td.getString("name") } }

    fun getIPAddressHistory(): Promise<List<String>> =
        SpicyAzisaBan.instance.connection.ipAddressHistory.findAll(
            FindOptions.Builder().addWhere("uuid", uuid.toString()).setOrderBy("last_seen").setOrder(Sort.DESC).build()
        ).then { list -> list.map { td -> td.getString("ip") } }

    override fun getUniqueId() = uuid
    override fun getName() = name
}
