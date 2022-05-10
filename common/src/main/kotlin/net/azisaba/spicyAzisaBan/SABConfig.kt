package net.azisaba.spicyAzisaBan

import net.azisaba.spicyAzisaBan.SABMessages.getObj
import net.azisaba.spicyAzisaBan.util.Util
import util.yaml.YamlConfiguration
import util.yaml.YamlObject
import java.io.File

object SABConfig {
    private val cfg: YamlObject
    private val versionFile = YamlConfiguration(SpicyAzisaBan::class.java.getResourceAsStream("/spicyazisaban/version.yml")!!)
        .asObject()

    init {
        //SpicyAzisaBan.LOGGER.info("Loaded version.yml: ${versionFile.rawData}")
        val dir = SpicyAzisaBan.instance.getDataFolder().toFile()
        dir.mkdir()
        val file = File(dir, "config.yml")
        if (!file.exists()) {
            val input = SpicyAzisaBan::class.java.getResourceAsStream("/spicyazisaban/config.yml")
            if (input == null) {
                SpicyAzisaBan.LOGGER.severe("Could not find config.yml in jar file!")
            } else {
                file.outputStream().use { input.copyTo(it) }
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

    val serverId: String?
        get() = if (SpicyAzisaBan.instance.getPlatformType() == PlatformType.CLI) null else cfg.getString("serverId")

    val version = versionFile.getString("version", "undefined")!!
    val debugBuild = false//versionFile.getBoolean("debugBuild", false)
    val devBuild = versionFile.getBoolean("devBuild", false)
    val enableDebugFeatures = versionFile.getBoolean("enableDebugFeatures", false)

    object Warning {
        private val obj
            get() = cfg.getObj("warning")
        val sendTitleEvery
            get() = Util.processTime(obj.getString("sendTitleEvery", "10s"))
        val titleStayTime
            get() = Util.processTime(obj.getString("titleStayTime", "5s"))
    }
}
