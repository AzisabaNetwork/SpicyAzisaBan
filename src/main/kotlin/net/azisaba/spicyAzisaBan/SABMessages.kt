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

    fun String.replaceVariables() =
        replace("%PREFIX%", SpicyAzisaBan.PREFIX)

    object General {
        private val obj = cfg.getObject("general")
        val prefix = obj.getString("prefix", "&c&lSpicyAzisaBan &8&l≫ &r")!!
        val missingPermissions = obj.getString("missingPermissions", "%PREFIX%&c権限がありません!")!!
    }

    object Commands {
        private val obj = cfg.getObject("commands")

        object General {
            private val obj = Commands.obj.getObject("general")
            val invalidGroup = obj.getString("invalidGroup", "%PREFIX%&c無効なグループ名です。")!!
            val invalidServer = obj.getString("invalidServer", "%PREFIX%&c無効なサーバー名です。")!!
            val invalidPlayer = obj.getString("invalidPlayer", "%PREFIX%&cプレイヤーが見つかりません。")!!
        }

        object Gban {
            private val obj = Commands.obj.getObject("gban")
            val usage = obj.getString("usage", "%PREFIX%&a使用法: /gban <Name>[+silent] <Reason...> [server=...]")!!
        }
    }
}
