package net.azisaba.spicyAzisaBan

import net.md_5.bungee.api.ChatColor
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

    fun YamlObject.getMessage(key: String, def: String = "<key: $key>"): String {
        val raw = this.rawData[key] ?: def
        if (raw is String) return raw
        return this.getArray(key)?.mapNotNull { o -> o?.toString() }?.joinToString("${ChatColor.RESET}\n") ?: def
    }

    fun String.replaceVariables(variables: Map<String, String> = mapOf()): String {
        var s = replace("%PREFIX%", SpicyAzisaBan.PREFIX)
        variables.forEach { (key, value) -> s = s.replace("%${key.uppercase()}%", value) }
        return s
    }

    fun String.replaceVariables(vararg pairs: Pair<String, String>) = replaceVariables(mapOf(*pairs))

    fun formatDateTime(day: Int, hour: Int, minute: Int, second: Int): String {
        var s = ""
        if (day != 0) s += General.Time.day.format(day)
        if (hour != 0) s += General.Time.hour.format(hour)
        if (minute != 0) s += General.Time.minute.format(minute)
        if (second != 0) s += General.Time.second.format(second)
        return s
    }

    object General {
        private val obj = cfg.getObject("general")
        val prefix = obj.getMessage("prefix", "&c&lSpicyAzisaBan &8&l» &r")
        val missingPermissions = obj.getMessage("missingPermissions", "%PREFIX%&c権限がありません!")
        val error = obj.getMessage("error", "%PREFIX%&c処理中に不明なエラーが発生しました。")

        object Time {
            private val obj = General.obj.getObject("time")
            val day = obj.getMessage("day", "%d日")
            val hour = obj.getMessage("hour", "%d時間")
            val minute = obj.getMessage("minute", "%d分")
            val second = obj.getMessage("second", "%d秒")
        }
    }

    object Commands {
        private val obj = cfg.getObject("commands")

        object General {
            private val obj = Commands.obj.getObject("general")
            val invalidGroup = obj.getMessage("invalidGroup", "%PREFIX%&c無効なグループ名です。")
            val invalidServer = obj.getMessage("invalidServer", "%PREFIX%&c無効なサーバー名です。")
            val invalidPlayer = obj.getMessage("invalidPlayer", "%PREFIX%&cプレイヤーが見つかりません。")
        }

        object GBan {
            private val obj = Commands.obj.getObject("gban")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /gban [-s] <player=...> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にGlobal Banされました!")
            val notify = obj.getMessage("notify", "&c&o%PLAYER%&r&7は、&e&o%OPERATOR%&r&7からGlobal Banされました。\n&7理由 &8> &7%REASON%\n&7ID &8> &7#%ID%")
            val layout = obj.getMessage("layout", "%PREFIX%&7永久BANされました!\n\n&c対象サーバー &8&l» &7全サーバー\n&c理由 &8&l» &7%REASON%")
        }

        object Ban {
            private val obj = Commands.obj.getObject("ban")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /ban [-s] <player=...> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にBanされました!")
            val notify = obj.getMessage("notify", "&c&o%PLAYER%&r&7は、&5&o%SERVER%&r&7で&e&o%OPERATOR%&r&7からBanされました。&r\n&7理由 &8> &7&o%REASON%&r\n&7ID &8> &7&o#%ID%")
            val layout = obj.getMessage("layout", "%PREFIX%&7永久BANされました!\n\n&c対象サーバー &8&l» &7全サーバー\n&c理由 &8&l» &7%REASON%")
        }

        object IPBan {
            private val obj = Commands.obj.getObject("ipban")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /ipban [-s] <target=Player/IP> <reason=\\\"...\\\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にIP Banされました!")
            val notify = obj.getMessage("notify", "&c&o%PLAYER%&r&7は、&5&o%SERVER%&r&7で&e&o%OPERATOR%&r&7からBanされました。&r\n&7理由 &8> &7&o%REASON%&r\n&7ID &8> &7#&o%ID%")
            val layout = obj.getMessage("layout", "%PREFIX%&7永久BANされました!\n\n&c対象サーバー &8&l» &7全サーバー\n&c理由 &8&l» &7%REASON%")
        }
    }
}
