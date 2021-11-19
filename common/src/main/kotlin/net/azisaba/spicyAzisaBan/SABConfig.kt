package net.azisaba.spicyAzisaBan

import net.azisaba.spicyAzisaBan.SABMessages.getObj
import net.azisaba.spicyAzisaBan.util.Util
import util.ResourceLocator
import util.base.Bytes
import util.yaml.YamlConfiguration
import util.yaml.YamlObject
import java.io.File

object SABConfig {
    private val cfg: YamlObject
    private val bungee = YamlConfiguration(ResourceLocator.getInstance(SpicyAzisaBan::class.java).getResourceAsStream("/spicyazisaban/version.yml")!!)
        .asObject()

    init {
        println("Loaded version.yml: ${bungee.rawData}")
        val dir = File("./plugins/SpicyAzisaBan")
        dir.mkdir()
        val file = File(dir, "config.yml")
        if (!file.exists()) {
            val input = ResourceLocator.getInstance(SpicyAzisaBan::class.java).getResourceAsStream("/spicyazisaban/config.yml")
            if (input == null) {
                SpicyAzisaBan.LOGGER.severe("Could not find config.yml in jar file!")
            } else {
                Bytes.copy(input, file)
                SpicyAzisaBan.LOGGER.info("Copied config.yml!")
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
        val failsafe = obj.getBoolean("failsafe", true)
    }

    val serverId = cfg.getString("serverId") ?: "bungee"

    val version = bungee.getString("version", "undefined")!!
    val debugBuild = bungee.getBoolean("debugBuild", false)
    val devBuild = bungee.getBoolean("devBuild", false)
    val enableDebugFeatures = bungee.getBoolean("enableDebugFeatures", false)

    object Warning {
        private val obj
            get() = cfg.getObj("warning")
        val sendTitleEvery
            get() = Util.processTime(obj.getString("sendTitleEvery", "10s"))
        val titleStayTime
            get() = Util.processTime(obj.getString("titleStayTime", "5s"))
    }
}
