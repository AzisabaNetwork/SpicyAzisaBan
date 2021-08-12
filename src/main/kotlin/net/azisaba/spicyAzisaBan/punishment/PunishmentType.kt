package net.azisaba.spicyAzisaBan.punishment

enum class PunishmentType(val id: String, val base: PunishmentType?, val temp: Boolean, val perm: String) {
    BAN("ban", null, false, "sab.ban.perm"),
    TEMP_BAN("tempban", BAN, true, "sab.ban.temp"),
    IP_BAN("ipban", null, false, "sab.ipban.perm"),
    TEMP_IP_BAN("tempipban", IP_BAN, true, "sab.ipban.temp"),
    MUTE("mute", null, false, "sab.mute.perm"),
    TEMP_MUTE("tempmute", MUTE, true, "sab.mute.temp"),
    WARNING("warn", null, false, "sab.warning.perm"),
    TEMP_WARNING("tempwarn", WARNING, true, "sab.warning.temp"),
    KICK("kick", null, false, "sab.kick"),
    NOTE("note", null, false, "sab.note"),
    ;

    fun isBan() = this == BAN || this == TEMP_BAN || this == IP_BAN || this == TEMP_IP_BAN

    fun isIPBased() = this == IP_BAN || this.base == IP_BAN
}
