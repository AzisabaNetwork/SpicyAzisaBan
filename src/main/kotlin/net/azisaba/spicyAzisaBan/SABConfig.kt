package net.azisaba.spicyAzisaBan

import util.ResourceLocator
import util.base.Bytes
import util.yaml.YamlConfiguration
import util.yaml.YamlObject
import java.io.File

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

    val database = DatabaseSettings(cfg.getObject("database"))

    class DatabaseSettings internal constructor(obj: YamlObject) {
        val host = obj.getString("host") ?: "localhost"
        val name = obj.getString("name") ?: "spicyazisaban"
        val user = obj.getString("user") ?: "spicyazisaban"
        val password = obj.getString("password") ?: "naetao"
        val verifyServerCertificate = obj.getBoolean("verifyServerCertificate", false)
        val useSSL = obj.getBoolean("useSSL", true)
        val keepAlive = obj.getInt("keepAlive", 300) // in seconds
        val failsafe = obj.getBoolean("failsafe", true)
    }
}
