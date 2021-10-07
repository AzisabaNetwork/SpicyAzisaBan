package net.azisaba.spicyAzisaBan.punishment

import net.azisaba.spicyAzisaBan.ReloadableSABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.getProfile
import net.azisaba.spicyAzisaBan.util.Util.hasNotifyPermissionOf
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.ProxyServer
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.TableData
import java.util.UUID

data class UnPunish(
    val id: Long,
    val punishment: Punishment,
    val reason: String,
    val timestamp: Long,
    val operator: UUID,
) {
    companion object {
        fun fromTableData(punishment: Punishment, td: TableData): UnPunish {
            val id = td.getLong("id")!!
            val punishId = td.getLong("punish_id")!!
            if (punishment.id != punishId) throw IllegalArgumentException("Wrong punishment ${punishment.id} != $punishId")
            val reason = td.getString("reason")!!
            val timestamp = td.getLong("timestamp")!!
            val operator = UUID.fromString(td.getString("operator"))!!
            return UnPunish(id, punishment, reason, timestamp, operator)
        }
    }

    fun getVariables(): Promise<Map<String, String>> = operator.getProfile()
        .then { profile ->
            mapOf(
                "id" to id.toString(),
                "pid" to punishment.id.toString(),
                "player" to punishment.name,
                "target" to punishment.target,
                "operator" to profile.name,
                "type" to punishment.type.getName(),
                "reason" to reason,
                "preason" to punishment.reason,
                "server" to if (punishment.server.lowercase() == "global") SABMessages.General.global else ReloadableSABConfig.serverNames.getOrDefault(punishment.server.lowercase(), punishment.server.lowercase()),
                "duration" to Util.unProcessTime(punishment.end - System.currentTimeMillis()),
                "time" to Util.unProcessTime(punishment.end - punishment.start),
            )
        }

    private fun getMessage() =
        getVariables()
            .then { variables -> SABMessages.Commands.Unpunish.notify.replaceVariables(variables).translate() }
            .catch {
                SpicyAzisaBan.instance.logger.warning("Could not fetch player name of $operator")
                it.printStackTrace()
            }

    fun notifyToAll() =
        getMessage().thenDo { message ->
            ProxyServer.getInstance().console.send(message)
            ProxyServer.getInstance().players.filter { it.hasNotifyPermissionOf(punishment.type, punishment.server) }.forEach { player ->
                player.send(message)
            }
        }.then {}
}
