package net.azisaba.spicyAzisaBan

import util.ResourceLocator
import util.base.Bytes
import util.yaml.YamlConfiguration
import util.yaml.YamlObject
import java.io.File

object SABMessages {
    private val cfg: YamlObject

    init {
        val dir = File("./plugins/SpicyAzisaBan")
        dir.mkdir()
        val file = File(dir, "messages.yml")
        if (!file.exists()) {
            val input = ResourceLocator.getInstance(SABMessages::class.java).getResourceAsStream("/messages.yml")
            if (input == null) {
                println("[SpicyAzisaBan] Could not find messages.yml in jar file!")
            } else {
                Bytes.copy(input, file)
            }
        }
        cfg = YamlConfiguration(file).asObject()
    }

    object General {
        private val obj = cfg.getObject("General")
        val Prefix = obj.getString("Prefix", "&c&lSpicyAzisaBan &8&l≫ &r")!!
        val NoPerms = obj.getString("NoPerms", "&cYou don't have permission for that!")!!
    }
}
