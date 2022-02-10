package net.azisaba.spicyAzisaBan.punishment

import java.util.UUID

data class PunishmentCacheKey(val uuid: UUID?, val address: String?, val server: String) {
    fun equalsAny(any: Any): Boolean {
        if (uuid == any) return true
        if (address == any) return true
        if (server == any) return true
        return false
    }

    fun equalsTarget(target: String): Boolean {
        if (uuid.toString() == target) return true
        if (address == target) return true
        return false
    }
}
