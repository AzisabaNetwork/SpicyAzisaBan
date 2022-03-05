package net.azisaba.spicyAzisaBan

import net.azisaba.spicyAzisaBan.SABMessages.getMessage
import net.azisaba.spicyAzisaBan.SABMessages.getObj
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.concat
import util.yaml.YamlConfiguration
import util.yaml.YamlObject
import java.io.File
import java.util.Objects

object ReloadableSABConfig {
    private lateinit var cfg: YamlObject

    fun reload() {
        cfg = YamlConfiguration(File("./plugins/SpicyAzisaBan/config.yml")).asObject()
    }

    val serverNames
        get() = cfg.getObject("serverNames")?.rawData?.mapKeys { it.key!! }?.mapValues { (_, value) -> value.toString() } ?: mapOf()

    val defaultReasons
        get() = PunishmentType.values().let { list ->
            val map = mutableMapOf<PunishmentType, Map<String, List<String>>>()
            val obj = cfg.getObj("defaultReasons")
            list.forEach { type ->
                val m2 = mutableMapOf<String, List<String>>()
                obj.getObj(type.name.lowercase()).rawData.forEach e@{ (server, any) ->
                    if (any == null) return@e
                    if (any is List<*>) {
                        m2[server] = any.map { a -> a.toString() }
                    } else {
                        m2[server] = listOf(any.toString())
                    }
                }
                map[type] = m2
            }
            map.toMap()
        }

    private val blockedCommandsWhenMuted
        get() = cfg.getObj("blockedCommandsWhenMuted").let { yaml ->
            val map = mutableMapOf<String, List<String>>()
            yaml.rawData.keys.forEach { key ->
                map[key] =
                    yaml.getArray(key)?.mapNotNull { if (Objects.isNull(it)) null else it.toString() } ?: listOf()
            }
            map.toMap()
        }

    fun getBlockedCommandsWhenMuted(server: String): List<String> =
        (blockedCommandsWhenMuted["global"]?.toMutableList() ?: mutableListOf())
            .concat(blockedCommandsWhenMuted[server])

    object BanOnWarning {
        private val obj
            get() = cfg.getObj("banOnWarning")
        val threshold
            get() = obj.getInt("threshold", 3)
        val time
            get() = obj.getString("time", "1mo")!!
        val reason
            get() = obj.getString("reason", "You've got $threshold warnings")!!
    }

    val customBannedMessage
        get() = cfg.getObj("customBannedMessage").let {
            val map = mutableMapOf<String, String>()
            it.rawData.keys.forEach { key ->
                map[key] = it.getMessage(key)
            }
            map.toMap()
        }

    private val webhookURLs: Map<String, Map<String, String?>>
        get() = cfg.getObj("webhookURLs").rawData.mapValues { (_, value) ->
            (value as Map<*, *>)
                .mapKeys { (punishmentType, _) -> punishmentType.toString().uppercase() }
                .mapValues { (_, url) -> url?.toString() }
        }.toMap()

    fun getWebhookURL(server: String, type: PunishmentType): String? =
        (webhookURLs[server] ?: webhookURLs["__fallback__"])?.let { it[type.name.uppercase()] ?: it["default"] }

    fun getWebhookURL(server: String): String? =
        (webhookURLs[server] ?: webhookURLs["__fallback__"])?.get("default")
}
