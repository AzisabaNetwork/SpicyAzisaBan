package net.azisaba.spicyAzisaBan.util.contexts

import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.filtr

data class PunishmentTypeContext(
    val isSuccess: Boolean,
    val type: PunishmentType?,
): Context {
    companion object {
        fun tabComplete(s: String): List<String> =
            PunishmentType
                .values()
                .map { "type=${it.name}" }
                .distinct()
                .filtr(s)
    }
}
