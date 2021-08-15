package net.azisaba.spicyAzisaBan.punishment

enum class PunishmentType(val id: String, val base: PunishmentType?, val temp: Boolean, val perm: String) {
    BAN("ban", null, false, "sab.ban.perm"),
    TEMP_BAN("tempban", BAN, true, "sab.ban.temp"),
    IP_BAN("ipban", null, false, "sab.ipban.perm"),
    TEMP_IP_BAN("tempipban", IP_BAN, true, "sab.ipban.temp"),
    MUTE("mute", null, false, "sab.mute.perm"),
    TEMP_MUTE("tempmute", MUTE, true, "sab.mute.temp"),
    IP_MUTE("ipmute", null, false, "sab.ipmute.perm"),
    TEMP_IP_MUTE("tempipmute", IP_MUTE, true, "sab.ipmute.temp"),
    WARNING("warn", null, false, "sab.warning"),
    CAUTION("caution", null, false, "sab.caution"),
    KICK("kick", null, false, "sab.kick"),
    NOTE("note", null, false, "sab.note"),
    ;

    fun getName() = id.replaceFirstChar { c -> c.uppercase() }
    fun isBan() = this == BAN || this == TEMP_BAN || this == IP_BAN || this == TEMP_IP_BAN
    fun isMute() = this == MUTE || this == TEMP_MUTE || this == IP_MUTE || this == TEMP_IP_MUTE
    fun isIPBased() = this == IP_BAN || this == TEMP_IP_BAN || this == IP_MUTE || this == TEMP_IP_MUTE
}
