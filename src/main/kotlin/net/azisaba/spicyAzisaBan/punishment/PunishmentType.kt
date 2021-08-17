package net.azisaba.spicyAzisaBan.punishment

enum class PunishmentType(val id: String, @Suppress("unused") val temp: Boolean, val perm: String) {
    BAN("ban", false, "sab.ban.perm"),
    TEMP_BAN("tempban", true, "sab.ban.temp"),
    IP_BAN("ipban", false, "sab.ipban.perm"),
    TEMP_IP_BAN("tempipban", true, "sab.ipban.temp"),
    MUTE("mute", false, "sab.mute.perm"),
    TEMP_MUTE("tempmute", true, "sab.mute.temp"),
    IP_MUTE("ipmute", false, "sab.ipmute.perm"),
    TEMP_IP_MUTE("tempipmute", true, "sab.ipmute.temp"),
    WARNING("warn", false, "sab.warning"),
    CAUTION("caution", false, "sab.caution"),
    KICK("kick", false, "sab.kick"),
    NOTE("note", false, "sab.note"),
    ;

    fun getName() = id.replaceFirstChar { c -> c.uppercase() }
    fun isBan() = this == BAN || this == TEMP_BAN || this == IP_BAN || this == TEMP_IP_BAN
    fun isMute() = this == MUTE || this == TEMP_MUTE || this == IP_MUTE || this == TEMP_IP_MUTE
    fun isIPBased() = this == IP_BAN || this == TEMP_IP_BAN || this == IP_MUTE || this == TEMP_IP_MUTE
}
