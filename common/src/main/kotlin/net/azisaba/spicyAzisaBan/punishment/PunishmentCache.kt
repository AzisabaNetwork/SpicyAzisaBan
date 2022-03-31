package net.azisaba.spicyAzisaBan.punishment

import net.azisaba.spicyAzisaBan.util.Util.removeIf
import util.concurrent.ref.DataCache
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PunishmentCache {
    private val map = ConcurrentHashMap<PunishmentCacheKey, DataCache<Punishment>>()

    fun find(key: PunishmentCacheKey) = map.filter { (k, _) -> k == key }.values.firstOrNull()

    fun find(uuid: UUID?, address: String?, server: String) = find(PunishmentCacheKey(uuid, address, server))

    operator fun set(key: PunishmentCacheKey, value: DataCache<Punishment>) {
        map[key] = value
    }

    operator fun set(uuid: UUID?, address: String?, server: String, value: DataCache<Punishment>) {
        set(PunishmentCacheKey(uuid, address, server), value)
    }

    fun removeIf(predicate: (key: PunishmentCacheKey, value: DataCache<Punishment>) -> Boolean) {
        map.removeIf(predicate)
    }

    fun clear() {
        map.clear()
    }
}
