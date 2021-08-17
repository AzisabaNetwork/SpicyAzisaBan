package net.azisaba.spicyAzisaBan

import net.azisaba.spicyAzisaBan.SABMessages.getMessage
import net.azisaba.spicyAzisaBan.SABMessages.getObj
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.concat
import util.ResourceLocator
import util.base.Bytes
import util.yaml.YamlConfiguration
import util.yaml.YamlObject
import java.io.File
import java.util.Objects

object SABConfig {
    private val cfg: YamlObject

    init {
        val dir = File("./plugins/SpicyAzisaBan")
        dir.mkdir()
        val file = File(dir, "config.yml")
        if (!file.exists()) {
            val input = ResourceLocator.getInstance(SABConfig::class.java).getResourceAsStream("/config.yml")
            if (input == null) {
                SpicyAzisaBan.instance.logger.severe("Could not find config.yml in jar file!")
            } else {
                Bytes.copy(input, file)
                SpicyAzisaBan.instance.logger.info("Copied config.yml!")
            }
        }
        cfg = YamlConfiguration(file).asObject()
    }

    val prefix = cfg.getString("prefix", "")!!

    val database = DatabaseSettings(cfg.getObj("database"))

    class DatabaseSettings internal constructor(obj: YamlObject) {
        val host = obj.getString("host", "localhost")!!
        val name = obj.getString("name", "spicyazisaban")!!
        val user = obj.getString("user", "spicyazisaban")!!
        val password = obj.getString("password", "naetao")!!
        val verifyServerCertificate = obj.getBoolean("verifyServerCertificate", false)
        val useSSL = obj.getBoolean("useSSL", true)
        val keepAlive = obj.getInt("keepAlive", 300) // in seconds
        val failsafe = obj.getBoolean("failsafe", true)
    }

    val serverNames = cfg.getObject("serverNames")?.rawData?.mapKeys { it.key!! }?.mapValues { (_, value) -> value.toString() } ?: mapOf()

    val defaultReasons = PunishmentType.values().let { list ->
        val map = mutableMapOf<PunishmentType, Map<String, List<String>>>()
        val obj = cfg.getObj("defaultReasons")
        list.forEach { type ->
            val m2 = mutableMapOf<String, List<String>>()
            obj.getObj(type.name.lowercase()).rawData.forEach e@ { (server, any) ->
                if (any == null) return@e
                if (any is List<*>) {
                    m2[server] = any.map { a -> a.toString() }
                } else {
                    m2[server] = listOf(any.toString())
                }
            }
            map[type] = m2
        }
        SpicyAzisaBan.instance.logger.info("Loaded defaultReasons: $map")
        map.toMap()
    }

    val enableDebugFeatures = YamlConfiguration(ResourceLocator.getInstance(SABConfig::class.java).getResourceAsStream("/bungee.yml")!!).asObject().getBoolean("enableDebugFeatures", false)
    private val blockedCommandsWhenMuted = cfg.getObj("blockedCommandsWhenMuted").let { yaml ->
        val map = mutableMapOf<String, List<String>>()
        yaml.rawData.keys.forEach { key ->
            map[key] = yaml.getArray(key)?.mapNotNull { if (Objects.isNull(it)) null else it.toString() } ?: listOf()
        }
        map.toMap()
    }

    fun getBlockedCommandsWhenMuted(server: String): List<String> =
        (blockedCommandsWhenMuted["global"]?.toMutableList() ?: mutableListOf())
            .concat(blockedCommandsWhenMuted[server])

    object BanOnWarning {
        private val obj = cfg.getObj("banOnWarning")
        val threshold = obj.getInt("threshold", 3)
        val time = obj.getString("time", "1mo")!!
        val reason = obj.getString("reason", "You've got $threshold warnings")!!
    }

    object Warning {
        private val obj = cfg.getObj("warning")
        val sendTitleEvery = Util.processTime(obj.getString("sendTitleEvery", "10s"))
        val titleStayTime = Util.processTime(obj.getString("titleStayTime", "5s"))
    }

    val customBannedMessage = cfg.getObj("customBannedMessage").let {
        val map = mutableMapOf<String, String>()
        it.rawData.keys.forEach { key ->
            map[key] = it.getMessage(key)
        }
        map.toMap()
    }
}
