package net.azisaba.spicyAzisaBan.punishment

enum class PunishmentType(val text: String, base: PunishmentType?, temp: Boolean, perm: String) {
    BAN("Ban", null, false, "sab.ban.perm"),
    TEMP_BAN("Tempban", BAN, true, "sab.ban.temp"),
    IP_BAN("Ipban", null, false, "sab.ipban.perm"),
    TEMP_IP_BAN("Tempipban", IP_BAN, true, "sab.ipban.temp"),
    MUTE("Mute", null, false, "sab.mute.perm"),
    TEMP_MUTE("Tempmute", MUTE, true, "sab.mute.temp"),
    WARNING("Warn", null, false, "sab.warning.perm"),
    TEMP_WARNING("Tempwarn", WARNING, true, "sab.warning.temp"),
    KICK("Kick", null, false, "sab.kick"),
    NOTE("Note", null, false, "sab.note"),
    ;
}
