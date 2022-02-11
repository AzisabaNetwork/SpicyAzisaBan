package net.azisaba.spicyAzisaBan.punishment

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import util.promise.rewrite.Promise

enum class PunishmentType(
    val id: String,
    @Suppress("unused") val temp: Boolean,
    val perm: String,
    val exemptPermission: String? = null,
) {
    BAN("ban", false, "sab.ban.perm"),
    TEMP_BAN("tempban", true, "sab.ban.temp"),
    IP_BAN("ipban", false, "sab.ipban.perm"),
    TEMP_IP_BAN("tempipban", true, "sab.ipban.temp"),
    MUTE("mute", false, "sab.mute.perm", "sab.exempt.mute"),
    TEMP_MUTE("tempmute", true, "sab.mute.temp", "sab.exempt.temp_mute"),
    IP_MUTE("ipmute", false, "sab.ipmute.perm", "sab.exempt.ip_mute"),
    TEMP_IP_MUTE("tempipmute", true, "sab.ipmute.temp", "sab.exempt.temp_ip_mute"),
    WARNING("warn", false, "sab.warning"),
    CAUTION("caution", false, "sab.caution"),
    KICK("kick", false, "sab.kick"),
    NOTE("note", false, "sab.note"),
    ;

    fun getName() = id.replaceFirstChar { c -> c.uppercase() }
    fun isBan() = this == BAN || this == TEMP_BAN || this == IP_BAN || this == TEMP_IP_BAN
    fun isMute() = this == MUTE || this == TEMP_MUTE || this == IP_MUTE || this == TEMP_IP_MUTE
    fun isIPBased() = this == IP_BAN || this == TEMP_IP_BAN || this == IP_MUTE || this == TEMP_IP_MUTE

    fun hasExemptPermission(actor: Actor, server: String = actor.getServerName(), lookupGroup: Boolean = true): Promise<Boolean> {
        if (exemptPermission == null) return Promise.resolve(false)
        if (actor.hasPermission(exemptPermission)) return Promise.resolve(true)
        if (server.isEmpty()) return Promise.resolve(false)
        if (!lookupGroup) return Promise.resolve(actor.hasPermission("$exemptPermission.$server"))
        return SpicyAzisaBan.instance.connection.getCachedGroupByServerOrFetch(server).then { group ->
            if (group != null && actor.hasPermission("$exemptPermission.$group")) {
                return@then true
            }
            return@then actor.hasPermission("$exemptPermission.$server")
        }
    }
}
