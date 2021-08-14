package net.azisaba.spicyAzisaBan.struct

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
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

        fun getByUUID(uuid: UUID): Promise<PlayerData> =
            SpicyAzisaBan.instance.connection.players.findOne(FindOptions.Builder().addWhere("uuid", uuid.toString()).setLimit(1).build())
                .then { td -> td?.let { fromTableData(it) } ?: error("no player data found for $uuid") }

        fun getByUUID(uuid: String): Promise<PlayerData> =
            SpicyAzisaBan.instance.connection.players.findOne(FindOptions.Builder().addWhere("uuid", uuid).setLimit(1).build())
                .then { td -> td?.let { fromTableData(it) } ?: error("no player data found for $uuid") }

        fun getByName(name: String): Promise<PlayerData> =
            SpicyAzisaBan.instance.connection.players.findOne(FindOptions.Builder().addWhere("name", name).setOrderBy("last_seen").setOrder(Sort.DESC).setLimit(1).build())
                .then { td -> td?.let { fromTableData(it) } ?: error("no player data found for $name") }

        fun createOrUpdate(player: ProxiedPlayer): Promise<PlayerData> = Promise.create { context ->
            val time = System.currentTimeMillis()
            val name = SpicyAzisaBan.instance.connection.usernameHistory
                .findOne(FindOptions.Builder().addWhere("uuid", player.uniqueId).setOrderBy("last_seen").setOrder(Sort.DESC).setLimit(1).build())
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
            if (player.socketAddress.getIPAddress() != null) {
                val ip = SpicyAzisaBan.instance.connection.ipAddressHistory
                    .findOne(FindOptions.Builder().addWhere("uuid", player.uniqueId).addWhere("ip", player.getIPAddress()).build())
                    .then { td -> td?.getString("ip") }
                    .complete()
                if (ip == null) {
                    SpicyAzisaBan.debug("Updating ipAddressHistory of ${player.name} (${player.uniqueId}) (old: $ip)")
                    insertNoId {
                        SpicyAzisaBan.instance.connection.ipAddressHistory.insert(
                            InsertOptions.Builder()
                                .addValue("uuid", player.uniqueId.toString())
                                .addValue("ip", player.socketAddress.getIPAddress())
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
                        .addValue("ip", player.socketAddress.getIPAddress())
                        .addValue("last_seen", time)
                        .build()
                ).catch { it.printStackTrace() }.complete()
                context.resolve(PlayerData(player.uniqueId, player.name, player.socketAddress.getIPAddress(), time))
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
