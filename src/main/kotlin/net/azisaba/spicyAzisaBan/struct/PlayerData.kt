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

        fun getByUUID(uuid: UUID): Promise<PlayerData> =
            SpicyAzisaBan.instance.connection.players.findOne(FindOptions.Builder().addWhere("uuid", uuid.toString()).build())
                .then { td -> td?.let { fromTableData(it) } ?: error("no player data found for $uuid") }

        fun getByName(name: String): Promise<PlayerData> =
            SpicyAzisaBan.instance.connection.players.findOne(FindOptions.Builder().addWhere("name", name).setOrderBy("last_seen").setOrder(Sort.DESC).setLimit(1).build())
                .then { td -> td?.let { fromTableData(it) } ?: error("no player data found for $name") }

        fun createOrUpdate(player: ProxiedPlayer): Promise<PlayerData> = Promise.create { context ->
            val time = System.currentTimeMillis()
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

        fun updateFromMojangAPI(uuid: UUID) {
            if (uuid.version() != 4) error("UUID must be Mojang-assigned (version 4)")
            MojangAPI.getName(uuid).thenDo { name ->
                //
            }
        }
    }

    override fun getUniqueId() = uuid
    override fun getName() = name
}
