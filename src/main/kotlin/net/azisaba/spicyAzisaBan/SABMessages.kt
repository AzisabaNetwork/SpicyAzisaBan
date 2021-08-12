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

    fun YamlObject.getObj(key: String) = this.getObject(key) ?: YamlObject()

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
        private val obj = cfg.getObj("general")
        val prefix = obj.getMessage("prefix", "&c&lSpicyAzisaBan &8&l» &r")
        val missingPermissions = obj.getMessage("missingPermissions", "%PREFIX%&c権限がありません!")
        val error = obj.getMessage("error", "%PREFIX%&c処理中に不明なエラーが発生しました。")
        val global = obj.getMessage("global", "全サーバー")

        object Time {
            private val obj = General.obj.getObj("time")
            val day = obj.getMessage("day", "%d日")
            val hour = obj.getMessage("hour", "%d時間")
            val minute = obj.getMessage("minute", "%d分")
            val second = obj.getMessage("second", "%d秒")
        }
    }

    object Commands {
        private val obj = cfg.getObj("commands")

        object General {
            private val obj = Commands.obj.getObj("general")
            val invalidGroup = obj.getMessage("invalidGroup", "%PREFIX%&c無効なグループ名です。")
            val invalidServer = obj.getMessage("invalidServer", "%PREFIX%&c無効なサーバー名です。")
            val invalidPlayer = obj.getMessage("invalidPlayer", "%PREFIX%&cプレイヤーが見つかりません。")
            val invalidTime = obj.getMessage("invalidTime", "%PREFIX%&c時間(&etime=&c)の形式が正しくありません。")
            val timeNotSpecified = obj.getMessage("timeNotSpecified", "%PREFIX%&c時間が指定されていません。")
        }

        object Ban {
            private val obj = Commands.obj.getObj("ban")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /ban [-s] <player=...> <reason=\"...\"> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gban [-s] <player=...> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にBanされました!")
            val notify = obj.getMessage("notify", "&c&o%PLAYER%&r&7は、&5&o%SERVER%&r&7で&e&o%OPERATOR%&r&7からBanされました。&r\n&7理由 &8> &7&o%REASON%&r\n&7ID &8> &7&o#%ID%")
            val layout = obj.getMessage("layout", "%PREFIX%&7永久BANされました!\n\n&c対象サーバー &8&l» &7&o%SERVER%\n&c理由 &8&l» &7&o%REASON%")
        }

        object TempBan {
            private val obj = Commands.obj.getObj("tempban")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /tempban [-s] <player=...> <reason=\"...\"> <time=...> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gtempban [-s] <player=...> <reason=\"...\"> <time=...> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にTempBanされました!")
            val notify = obj.getMessage("notify", "&c&o%PLAYER%&r&7は、&5&o%SERVER%&r&7で&e&o%OPERATOR%&r&7からTempBanされました。&r\n&7理由 &8> &7&o%REASON%&r\n&7ID &8> &7&o#%ID%&r\n&7期間 &8> &7&o%DURATION%")
            val layout = obj.getMessage("layout", "%PREFIX%&7永久BANされました!\n\n&c対象サーバー &8&l» &7全サーバー\n&c理由 &8&l» &7%REASON%\n&c期間 &8&l» &7&o%DURATION%")
        }

        object IPBan {
            private val obj = Commands.obj.getObj("ipban")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /ipban [-s] <target=Player/IP> <reason=\\\"...\\\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にIP Banされました!")
            val notify = obj.getMessage("notify", "&c&o%PLAYER%&r&7は、&5&o%SERVER%&r&7で&e&o%OPERATOR%&r&7からBanされました。&r\n&7理由 &8> &7&o%REASON%&r\n&7ID &8> &7#&o%ID%")
            val layout = obj.getMessage("layout", "%PREFIX%&7永久BANされました!\n\n&c対象サーバー &8&l» &7全サーバー\n&c理由 &8&l» &7%REASON%")
        }
    }
}
