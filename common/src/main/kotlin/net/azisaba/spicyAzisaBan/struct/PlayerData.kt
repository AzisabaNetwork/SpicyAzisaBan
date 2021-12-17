package net.azisaba.spicyAzisaBan.struct

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.PlayerConnection
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.getIPAddress
import net.azisaba.spicyAzisaBan.util.Util.insertNoId
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
    val firstLogin: Long?,
    val firstLoginAttempt: Long?,
    val lastLogin: Long?,
    val lastLoginAttempt: Long?,
): PlayerProfile {
    companion object {
        fun fromTableData(td: TableData): PlayerData {
            val uuid = UUID.fromString(td.getString("uuid")!!)
            val name = td.getString("name")!!
            val ip = td.getString("ip")!!
            val lastSeen = td.getLong("last_seen")!!
            val firstLogin = td.getLong("first_login")!!
            val firstLoginAttempt = td.getLong("first_login_attempt")!!
            val lastLogin = td.getLong("last_login")!!
            val lastLoginAttempt = td.getLong("last_login_attempt")!!
            return PlayerData(uuid, name, ip, lastSeen, firstLogin, firstLoginAttempt, lastLogin, lastLoginAttempt)
        }

        fun getByIP(ip: String): Promise<List<PlayerData>> =
            SpicyAzisaBan.instance.connection.players.findAll(FindOptions.Builder().addWhere("ip", ip).setOrderBy("last_seen").setOrder(Sort.DESC).build())
                .then { list -> list.map { td -> fromTableData(td) } }

        fun getAllByIP(ip: String): Promise<List<PlayerData>> =
            SpicyAzisaBan.instance.connection.ipAddressHistory.findAll(FindOptions.Builder().addWhere("ip", ip).setOrderBy("last_seen").setOrder(Sort.DESC).build())
                .then { list -> list.distinctBy { td -> td.getString("uuid") } }
                .then { list -> list.map { td -> UUID.fromString(td.getString("uuid")) } }
                .then { list -> list.map { uuid -> getByUUID(uuid).complete() } }

        fun isExists(uuid: UUID) = async<Boolean> { context ->
            getByUUID(uuid).then { context.resolve(true) }.catch { context.resolve(false) }
        }

        fun getByUUID(uuid: UUID): Promise<PlayerData> =
            SpicyAzisaBan.instance.connection.players.findOne(FindOptions.Builder().addWhere("uuid", uuid.toString()).setLimit(1).build())
                .then { td -> td?.let { fromTableData(it) } ?: error("no player data found for $uuid") }

        fun getByUUID(uuid: String): Promise<PlayerData> =
            SpicyAzisaBan.instance.connection.players.findOne(FindOptions.Builder().addWhere("uuid", uuid).setLimit(1).build())
                .then { td -> td?.let { fromTableData(it) } ?: error("no player data found for $uuid") }

        fun getByName(name: String, ambiguousSearch: Boolean = false): Promise<PlayerData> = async { context ->
            val sql = "SELECT * FROM `players` WHERE LOWER(`name`) LIKE LOWER(?) ORDER BY `last_seen` DESC LIMIT 1"
            val start = System.currentTimeMillis()
            val ps = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
            ps.setString(1, if (ambiguousSearch) "%$name%" else name)
            val rs = ps.executeQuery()
            SQLConnection.logSql(sql, System.currentTimeMillis() - start)
            if (!rs.next()) {
                MojangAPI.getUniqueId(name)
                    .then { uuid ->
                        insertNoId {
                            SpicyAzisaBan.instance.connection.players.insert(
                                InsertOptions.Builder()
                                    .addValue("uuid", uuid.toString())
                                    .addValue("name", name)
                                    .addValue("ip", "1.1.1.1")
                                    .addValue("last_seen", System.currentTimeMillis())
                                    .build()
                            ).complete()
                        }
                        PlayerData(uuid, name, "1.1.1.1", System.currentTimeMillis(), -1, -1, -1, -1)
                    }
                    .catch { context.reject(IllegalArgumentException("no player data found for $name")) }
                    .complete()
                    ?.let { return@async context.resolve(it) }
                return@async
            }
            val uuid = UUID.fromString(rs.getString("uuid"))
            val theName = rs.getString("name")
            val ip = rs.getString("ip")
            val lastSeen = rs.getLong("last_seen")
            val firstLogin = rs.getLong("first_login")
            val firstLoginAttempt = rs.getLong("first_login_attempt")
            val lastLogin = rs.getLong("last_login")
            val lastLoginAttempt = rs.getLong("last_login_attempt")
            return@async context.resolve(PlayerData(uuid, theName, ip, lastSeen, firstLogin, firstLoginAttempt, lastLogin, lastLoginAttempt))
        }

        /*
        fun getByName(name: String): Promise<PlayerData> =
            SpicyAzisaBan.instance.connection.players.findOne(FindOptions.Builder().addWhere("name", name).setOrderBy("last_seen").setOrder(Sort.DESC).setLimit(1).build())
                .then { td -> td?.let { fromTableData(it) } ?: error("no player data found for $name") }
        */

        fun createOrUpdate(connection: PlayerConnection): Promise<PlayerData> = async { context ->
            val time = System.currentTimeMillis()
            val rs = SpicyAzisaBan.instance.connection.executeQuery(
                "SELECT `uuid`, `first_login_attempt` FROM `players` WHERE `uuid` = ?",
                connection.uniqueId.toString(),
            )
            if (rs.next() && rs.getLong("first_login_attempt") > 0) {
                SpicyAzisaBan.instance.connection.players.update(
                    UpsertOptions.Builder()
                        .addWhere("uuid", connection.uniqueId.toString())
                        .addValue("uuid", connection.uniqueId.toString())
                        .addValue("name", connection.name ?: connection.uniqueId.toString())
                        .addValue("ip", connection.getRemoteAddress().getIPAddress())
                        .addValue("last_login_attempt", time)
                        .build()
                ).catch { it.printStackTrace() }.complete()
            } else {
                insertNoId {
                    SpicyAzisaBan.instance.connection.players.upsert(
                        UpsertOptions.Builder()
                            .addWhere("uuid", connection.uniqueId.toString())
                            .addValue("uuid", connection.uniqueId.toString())
                            .addValue("name", connection.name ?: connection.uniqueId.toString())
                            .addValue("ip", connection.getRemoteAddress().getIPAddress())
                            .addValue("first_login_attempt", time)
                            .addValue("last_login_attempt", time)
                            .build()
                    ).catch { it.printStackTrace() }.complete()
                }
            }
            context.resolve()
        }

        fun updatePlayerDataAsync(player: PlayerActor, login: Boolean) =
            isExists(player.uniqueId).then { exists ->
                if (!exists) return@then
                SpicyAzisaBan.debug("Updating player data of ${player.uniqueId} (${player.name})")
                createOrUpdate(player, login).thenDo {
                    SpicyAzisaBan.debug("Updated player data of ${player.uniqueId} (${player.name})")
                    SpicyAzisaBan.debug(it.toString(), 2)
                }.catch { it.printStackTrace() }
            }

        fun createOrUpdate(player: PlayerActor, login: Boolean): Promise<PlayerData> = async { context ->
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
            val addr = player.getRemoteAddress().getIPAddress()
            if (addr != null) {
                val ip = SpicyAzisaBan.instance.connection.ipAddressHistory
                    .findOne(FindOptions.Builder().addWhere("uuid", player.uniqueId.toString()).setOrderBy("last_seen").setOrder(Sort.DESC).build())
                    .then { td -> td?.getString("ip") }
                    .catch { it.printStackTrace() }
                    .complete()
                if (ip != addr) {
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
            val exists = SpicyAzisaBan.instance.connection.players
                .findOne(FindOptions.Builder().addWhere("uuid", player.uniqueId.toString()).build())
                .complete()
                .let { it != null && it.getLong("first_login") > 0 }
            insertNoId {
                val builder = UpsertOptions.Builder()
                    .addWhere("uuid", player.uniqueId.toString())
                    .addValue("uuid", player.uniqueId.toString())
                    .addValue("name", player.name)
                    .addValue("ip", addr)
                    .addValue("last_seen", time)
                if (!exists) builder.addValue("first_login", time)
                if (login) builder.addValue("last_login", time)
                SpicyAzisaBan.instance.connection.players
                    .upsert(builder.build())
                    .catch { it.printStackTrace() }
                    .complete()
                context.resolve(PlayerData(player.uniqueId, player.name, addr, time, -1, -1, -1, -1))
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
