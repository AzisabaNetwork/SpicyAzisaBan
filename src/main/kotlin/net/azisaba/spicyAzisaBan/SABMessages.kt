package net.azisaba.spicyAzisaBan

import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.ChatColor
import util.ResourceLocator
import util.base.Bytes
import util.yaml.YamlConfiguration
import util.yaml.YamlObject
import java.io.File
import java.util.Calendar

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

    fun YamlObject.getMessage(key: String, def: List<String>): String {
        val raw = this.rawData[key] ?: def
        if (raw is String) return raw
        return this.getArray(key)?.mapNotNull { o -> o?.toString() }?.joinToString("${ChatColor.RESET}\n")
            ?: def.joinToString("${ChatColor.RESET}\n")
    }

    fun String.replaceVariables(variables: Map<String, String> = mapOf()): String {
        var s = replace("%PREFIX%", SpicyAzisaBan.PREFIX)
        variables.forEach { (key, value) -> s = s.replace("%${key.uppercase()}%", value) }
        return s
    }

    fun String.replaceVariables(vararg pairs: Pair<String, String>) = replaceVariables(mapOf(*pairs))

    fun getBannedMessage(server: String) =
        SABConfig.customBannedMessage[server] ?: Commands.General.removedFromServer

    fun formatDateTime(day: Int, hour: Int, minute: Int, second: Int): String {
        var s = ""
        if (day != 0) s += General.Time.day.format(day)
        if (hour != 0) s += General.Time.hour.format(hour)
        if (minute != 0) s += General.Time.minute.format(minute)
        if (second != 0 || s.isBlank()) s += General.Time.second.format(second)
        return s
    }

    fun formatDate(long: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = long
        return General.datetime
            .replaceVariables(
                "year" to Util.zero(4, c[Calendar.YEAR]),
                "month" to Util.zero(2, c[Calendar.MONTH] + 1),
                "day" to Util.zero(2, c[Calendar.DAY_OF_MONTH]),
                "hour" to Util.zero(2, c[Calendar.HOUR_OF_DAY]),
                "minute" to Util.zero(2, c[Calendar.MINUTE]),
                "second" to Util.zero(2, c[Calendar.SECOND]),
                "millis" to Util.zero(3, c[Calendar.MILLISECOND]),
            )
            .translate()
    }

    object General {
        private val obj = cfg.getObj("general")
        val prefix = obj.getMessage("prefix", "&c&lSpicyAzisaBan &8&l» &r")
        val missingPermissions = obj.getMessage("missingPermissions", "%PREFIX%&c権限がありません!")
        val error = obj.getMessage("error", "%PREFIX%&c処理中に不明なエラーが発生しました。")
        val global = obj.getMessage("global", "全サーバー")
        val permanent = obj.getMessage("permanent", "無期限")
        val online = obj.getMessage("online", "&aオンライン")
        val offline = obj.getMessage("offline", "&4オフライン")
        val previousPage = obj.getMessage("previousPage", "前のページ")
        val nextPage = obj.getMessage("nextPage", "次のページ")
        val datetime = obj.getMessage("datetime", "%YEAR%/%MONTH%/%DAY%-%HOUR%:%MINUTE%:%SECOND%")

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
            val invalidIPAddress = obj.getMessage("invalidIPAddress", "%PREFIX%&cIPアドレスの形式が正しくないか、処罰不可なIPアドレスです。")
            val invalidPunishmentType = obj.getMessage("invalidPunishmentType", "%PREFIX%&c無効な処罰タイプです。")
            val timeNotSpecified = obj.getMessage("timeNotSpecified", "%PREFIX%&c時間が指定されていません。")
            val samePunishmentAppliedToSameIPAddress = obj.getMessage("samePunishmentAppliedToSameIPAddress", "&7%d人の同じIPアドレスのプレイヤーにも同じ処罰が適用されました。")
            val alreadyPunished = obj.getMessage("alreadyPunished", "%PREFIX%&cこのアカウントはすでに(同じサーバー、同じ種類で)処罰されています!")
            val removedFromServer = obj.getMessage("removedFromServer", "&cプレイヤーがこのサーバーから抹消された。")
            val offlinePlayer = obj.getMessage("offlinePlayer", "%PREFIX%&cこのプレイヤーはオフラインです。")
            val notPunished = obj.getMessage("notPunished", "%PREFIX%&cこのアカウントは処罰されていません!")
            val noReasonSpecified = obj.getMessage("noReasonSpecified", "%PREFIX%&c理由が指定されていません!")
            val noProofSpecified = obj.getMessage("noProofSpecified", "%PREFIX%&c証拠が指定されていません!")
            val punishmentNotFound = obj.getMessage("punishmentNotFound", "%PREFIX%&c処罰#%dが見つかりません!")
            val proofNotFound = obj.getMessage("proofNotFound", "%PREFIX%&c証拠#%dが見つかりません!")
        }

        object Sab {
            private val obj = Commands.obj.getObj("sab")
            val setDebugLevel = obj.getMessage("setDebugLevel", "%PREFIX%&aデバッグログレベルを&e%d&aに設定しました。")
        }

        object Ban {
            private val obj = Commands.obj.getObj("ban")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /ban <player=...> <reason=\"...\"> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gban <player=...> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にBanされました!")
            val notify = obj.getMessage("notify", "&c&o%PLAYER%&r&7は、&e&o%SERVER%&r&7で&e&o%OPERATOR%&r&7からBanされました。&r\n&7理由 &8> &7&o%REASON%&r\n&7ID &8> &7&o#%ID%")
            val layout = obj.getMessage("layout", "%PREFIX%&7永久BANされました!\n\n&c対象サーバー &8&l» &7&o%SERVER%\n&c理由 &8&l» &7&o%REASON%")
        }

        object TempBan {
            private val obj = Commands.obj.getObj("tempban")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /tempban <player=...> <reason=\"...\"> <time=...> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gtempban <player=...> <reason=\"...\"> <time=...> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にTempBanされました!")
            val notify = obj.getMessage("notify", "&c&o%PLAYER%&r&7は、&5&o%SERVER%&r&7で&e&o%OPERATOR%&r&7からTempBanされました。&r\n&7理由 &8> &7&o%REASON%&r\n&7ID &8> &7&o#%ID%&r\n&7期間 &8> &7&o%TIME%")
            val layout = obj.getMessage("layout", "%PREFIX%&7永久BANされました!\n\n&c対象サーバー &8&l» &7全サーバー\n&c理由 &8&l» &7%REASON%\n&c期間 &8&l» &7&o%DURATION%")
        }

        object IPBan {
            private val obj = Commands.obj.getObj("ipban")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /ipban <target=Player/IP> <reason=\"...\"> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gipban <target=Player/IP> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%TARGET%&r&7は、正常にIPBanされました!")
            val notify = obj.getMessage("notify", "&c&o%PLAYER%&r&7は、&5&o%SERVER%&r&7で&e&o%OPERATOR%&r&7からIPBanされました。&r\n&7理由 &8> &7&o%REASON%&r\n&7ID &8> &7#&o%ID%")
            val layout = obj.getMessage("layout", "%PREFIX%&7永久IP BANされました!\n\n&c対象サーバー &8&l» &7全サーバー\n&c理由 &8&l» &7%REASON%")
        }

        object TempIPBan {
            private val obj = Commands.obj.getObj("tempipban")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /tempipban <target=Player/IP> <reason=\"...\"> <time=...> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gtempipban <target=Player/IP> <reason=\"...\"> <time=...> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%TARGET%&r&7は、正常にTempIPBanされました!")
            val notify = obj.getMessage("notify", "&c&o%PLAYER%&r&7は、&5&o%SERVER%&r&7で&e&o%OPERATOR%&r&7からTempIPBanされました。&r\n&7理由 &8> &7&o%REASON%&r\n&7ID &8> &7#&o%ID%&r\n&7期間 &8> &7&o%TIME%")
            val layout = obj.getMessage("layout", "%PREFIX%&7一時的にIP BANされました!\n\n&c対象サーバー &8&l» &7全サーバー&r\n&c理由 &8&l» &7%REASON%&r\n&c期間 &8&l» &7&o%DURATION%")
        }

        object Mute {
            private val obj = Commands.obj.getObj("mute")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /mute <player=...> <reason=\"...\"> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gmute <player=...> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にMuteされました!")
            val notify = obj.getMessage("notify", listOf("&c&o%PLAYER%&r&7は、&e&o%SERVER%&7で&e&o%OPERATOR%&r&7からMuteされました。", "&7理由 &8> &7&o%REASON%", "&7ID &8> &7&o#%ID%"))
            val layout1 = obj.getMessage("layout1", listOf("%PREFIX%&cあなたは永久ミュートされました!", "&7対象サーバー &8> &7&o%SERVER%", "&7理由 &8> &7&o%REASON%"))
            val layout2 = obj.getMessage("layout2", listOf("%PREFIX%&cあなたは永久ミュートされています!", "&7対象サーバー &8> &7&o%SERVER%", "&7理由 &8> &7&o%REASON%"))
        }

        object TempMute {
            private val obj = Commands.obj.getObj("tempmute")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /tempmute <player=...> <reason=\"...\\\"> <time=...> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gtempmute <player=...> <reason=\"...\"> <time=...> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にTempMuteされました!")
            val notify = obj.getMessage("notify", listOf("&c&o%PLAYER%&r&7は、&e&o%SERVER%&7で&e&o%OPERATOR%&r&7からTempMuteされました。", "&7理由 &8> &7&o%REASON%", "&7ID &8> &7&o#%ID%", "&7期間 &8> &7&o%TIME%"))
            val layout1 = obj.getMessage("layout1", listOf("%PREFIX%&cあなたは一時的にミュートされました!", "&7対象サーバー &8> &7&o%SERVER%", "&7理由 &8> &7&o%REASON%", "&7期間 &8> &7&o%DURATION%"))
            val layout2 = obj.getMessage("layout2", listOf("%PREFIX%&cあなたは一時的にミュートされています!", "&7対象サーバー &8> &7&o%SERVER%", "&7理由 &8> &7&o%REASON%", "&7期間 &8> &7&o%DURATION%"))
        }

        object IPMute {
            private val obj = Commands.obj.getObj("ipmute")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /ipmute <target=...> <reason=\"...\"> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gipmute <target=...> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にIPMuteされました!")
            val notify = obj.getMessage("notify", listOf("&c&o%PLAYER%&r&7は、&e&o%SERVER%&7で&e&o%OPERATOR%&r&7からIPMuteされました。", "&7理由 &8> &7&o%REASON%", "&7ID &8> &7&o#%ID%"))
            val layout1 = obj.getMessage("layout1", listOf("%PREFIX%&cあなたは永久IPミュートされました!", "&7対象サーバー &8> &7&o%SERVER%", "&7理由 &8> &7&o%REASON%"))
            val layout2 = obj.getMessage("layout2", listOf("%PREFIX%&cあなたは永久IPミュートされています!", "&7対象サーバー &8> &7&o%SERVER%", "&7理由 &8> &7&o%REASON%"))
        }

        object TempIPMute {
            private val obj = Commands.obj.getObj("tempipmute")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /tempipmute <target=...> <reason=\"...\\\"> <time=...> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gtempipmute <target=...> <reason=\"...\"> <time=...> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にTempIPMuteされました!")
            val notify = obj.getMessage("notify", listOf("&c&o%PLAYER%&r&7は、&e&o%SERVER%&7で&e&o%OPERATOR%&r&7からTempIPMuteされました。", "&7理由 &8> &7&o%REASON%", "&7ID &8> &7&o#%ID%", "&7期間 &8> &7&o%TIME%"))
            val layout1 = obj.getMessage("layout1", listOf("%PREFIX%&cあなたは一時的にIPミュートされました!", "&7対象サーバー &8> &7&o%SERVER%", "&7理由 &8> &7&o%REASON%", "&7期間 &8> &7&o%DURATION%"))
            val layout2 = obj.getMessage("layout2", listOf("%PREFIX%&cあなたは一時的にIPミュートされています!", "&7対象サーバー &8> &7&o%SERVER%", "&7理由 &8> &7&o%REASON%", "&7期間 &8> &7&o%DURATION%"))
        }

        object Warning {
            private val obj = Commands.obj.getObj("warning")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /warning <player=...> <reason=\"...\"> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gwarning <player=...> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にWarnされました!")
            val notify = obj.getMessage("notify", listOf("&c&o%PLAYER%&r&7は、&e&o%SERVER%&7で&e&o%OPERATOR%&r&7からWarnされました。", "&7理由 &8> &7&o%REASON%", "&7ID &8> &7&o#%ID%"))
            val layout = obj.getMessage("layout", listOf("%PREFIX%&c警告を受けました", "&7対象サーバー &8> &7&o%SERVER%", "&7理由 &8> &7&o%REASON%"))
            val title = obj.getMessage("title", "&cあなたは警告を受けました!")
            val subtitle = obj.getMessage("subtitle", "&6/warns&eで表示を解除することができます")
        }

        object Caution {
            private val obj = Commands.obj.getObj("caution")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /caution <player=...> <reason=\"...\"> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gcaution <player=...> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にCautionされました!")
            val notify = obj.getMessage("notify", listOf("&c&o%PLAYER%&r&7は、&e&o%SERVER%&7で&e&o%OPERATOR%&r&7からCautionされました。", "&7理由 &8> &7&o%REASON%", "&7ID &8> &7&o#%ID%"))
            val layout = obj.getMessage("layout", listOf("%PREFIX%&c注意を受けました", "&7対象サーバー &8> &7&o%SERVER%", "&7理由 &8> &7&o%REASON%"))
        }

        object Kick {
            private val obj = Commands.obj.getObj("kick")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /kick <player=...> <reason=\"...\"> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gkick <player=...> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にKickされました!")
            val notify = obj.getMessage("notify", listOf("&c&o%PLAYER%&r&7は、&e&o%SERVER%&7で&e&o%OPERATOR%&r&7からKickされました。", "&7理由 &8> &7&o%REASON%", "&7ID &8> &7&o#%ID%"))
            val layout = obj.getMessage("layout", listOf("%PREFIX%&cサーバーからキックされました", "&7理由 &8> &7&o%REASON%"))
        }

        object Note {
            private val obj = Commands.obj.getObj("note")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /note <player=...> <reason=\"...\"> [server=...]")
            val globalUsage = obj.getMessage("globalUsage", "%PREFIX%&a使用法: /gnote <player=...> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にNoteされました!")
            val notify = obj.getMessage("notify", listOf("&c&o%PLAYER%&r&7は、&e&o%SERVER%&7で&e&o%OPERATOR%&r&7からNoteされました。", "&7理由 &8> &7&o%REASON%", "&7ID &8> &7&o#%ID%"))
        }

        object Unpunish {
            private val obj = Commands.obj.getObj("unpunish")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /unpunish <id=...> <reason=\"...\"> [server=...]")
            val unbanUsage = obj.getMessage("unbanUsage", "%PREFIX%&a使用法: /unban <player=...> <reason=\"...\"> [server=...]")
            val unmuteUsage = obj.getMessage("unmuteUsage", "%PREFIX%&a使用法: /unmute <player=...> <reason=\"...\"> [server=...]")
            val done = obj.getMessage("done", "%PREFIX%&c&o%PLAYER%&r&7は、正常にUnpunishされました!")
            val notify = obj.getMessage("notify", listOf("&c&o%PLAYER%&r&7は、&e&o%SERVER%&7で&e&o%OPERATOR%&r&7からUnpunishされました。", "&7処罰タイプ &8> &7&o%TYPE%", "&7処罰理由 &8> &7&o%PREASON%", "&7処罰ID &8> &7&o#%PID%", "&7解除理由 &8> &7&o%REASON%", "&7ID &8> &7&o#%ID%"))
        }

        object ChangeReason {
            private val obj = Commands.obj.getObj("changereason")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /changereason <id=...> <reason=\"...\">")
            val done = obj.getMessage("done", "&7処罰&a#%ID%&7は正常に更新されました。")
        }

        object AddProof {
            private val obj = Commands.obj.getObj("addproof")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /addproof <id=...> <text=\"...\">")
            val done = obj.getMessage("done", "%PREFIX%&7処罰&a#%PID%&7に証拠を追加しました。&8(&7ID: &e%ID%&8)")
        }

        object DelProof {
            private val obj = Commands.obj.getObj("delproof")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /delproof <id=...>")
            val done = obj.getMessage("done", "%PREFIX%&7処罰&a#%PID%&7から証拠を削除しました。&8(&7ID: &e%ID%&8)")
        }

        object Seen {
            private val obj = Commands.obj.getObj("seen")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /seen <Player/IP>")
            val searching = obj.getMessage("searching", "%PREFIX%&eプレイヤーを検索中...")
            val layout = obj.getMessage("layout", listOf("%PREFIX%&c%PLAYER%&eは&c%SINCE%前&eから%STATUS%&eです。", "&7過去の名前 &8> &e&o%NAME_HISTORY%", "&7最近のIPアドレス &8> &e&o%IP% &7(&e%HOSTNAME%&7)", "&7過去のすべてのIPアドレス &8> &e&o%IP_HISTORY%"))
            val layoutIP = obj.getMessage("layoutIP", listOf("%PREFIX%&eこのプレイヤーは過去に%PLAYERS_COUNT%個のアカウントで接続しています:", "&6%PLAYERS%"))
        }

        object Check {
            private val obj = Commands.obj.getObj("check")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /check <target=Player/IP> [--ip] [--only]")
            val searching = obj.getMessage("searching", "%PREFIX%&eプレイヤーを検索中...")
            val layout = obj.getMessage("layout", listOf("&cプレイヤー &8> &e%NAME% &8(&e%UUID%&8)", "&cIPアドレス &8> &e%IP% &8(&e%HOSTNAME%&8)", "&cMute &8> &7%MUTE_COUNT%", "&cBan &8> &7%BAN_COUNT%", "&c警告数 &8> &7%WARNING_COUNT%", "&c注意数 &8> &7%CAUTION_COUNT%", "&cノート &8> &7%NOTE_COUNT%"))
            val layoutIP = obj.getMessage("layoutIP", listOf("&cIPアドレス &8> &e%IP% &8(&e%HOSTNAME%&8)", "&cMute &8> &7%MUTE_COUNT%", "&cBan &8> &7%BAN_COUNT%", "&c警告数 &8> &7%WARNING_COUNT%", "&c注意数 &8> &7%CAUTION_COUNT%", "&cノート &8> &7%NOTE_COUNT%"))
        }

        object History {
            private val obj = Commands.obj.getObj("history")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /history <target=Player/IP> [page=...] [--all] [--active] [--ip] [--only]")
            val header = obj.getMessage("header", "%PREFIX%&c&o%TARGET%&7の履歴:")
            val layout = obj.getMessage("layout", listOf("&8[&e%DATE%&8] &8(&e/proofs %ID%&7で証拠を表示&8)", "&c名前/IP &8> &7&o%PLAYER%", "&cタイプ &8> &7&o%TYPE%", "&c期間 &8> &7&o%STRIKETHROUGH_IF_UNPUNISHED%%TIME%", "&c理由 &8> &7&o%REASON% %UNPUNISH_REASON%", "&cID &8> &7&o#%ID% %UNPUNISH_ID%", "&cサーバー &8> &7&o%SERVER%", "&c執行者 &8> &7&o%OPERATOR% %UNPUNISH_OPERATOR%"))
            val footer = obj.getMessage("footer", "&7ページ &e&o%CURRENT_PAGE%&7/&e&o%MAX_PAGE% &8| &7処罰件数: &e&o%COUNT%")
            val unpunishReason = obj.getMessage("unpunishReason", "&8(&7解除理由: &e%REASON%&8)")
            val unpunishId = obj.getMessage("unpunishId", "&8(&7解除ID: &e%ID%&8)")
            val unpunishOperator = obj.getMessage("unpunishOperator", "&8(&7解除者: &e%OPERATOR%&8)")
            val invalidArguments = obj.getMessage("invalidArguments", "%PREFIX%&e--all&cと&e--active&cを同時に使用することはできません。")
        }

        object Proofs {
            private val obj = Commands.obj.getObj("proofs")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /proofs <ID>")
            val header = obj.getMessage("header", "%PREFIX%&e処罰&a#%PID%&eの証拠一覧:")
            val layout = obj.getMessage("layout", listOf("&c&o証拠ID #%ID% &8> &e&o%TEXT%"))
        }

        object Warns {
            private val obj = Commands.obj.getObj("warns")
            val notWarnedYet = obj.getMessage("notWarnedYet", "%PREFIX%&c&oまだ警告を受けていません。")
            val header = obj.getMessage("header", "%PREFIX%&c有効な警告一覧:")
            val layout = obj.getMessage("layout", listOf("&8[&e%DATE%&8]", "&cタイプ &8> &7&o%TYPE%", "&c対象サーバー &8> &7&o%SERVER%", "&c理由 &8> &7&o%REASON%"))
        }

        object BanList {
            private val obj = Commands.obj.getObj("banlist")
            val usage = obj.getMessage("usage", "%PREFIX%&a使用法: /banlist [page=...] [type=...] [server=...] [--all] [--active]")
            val header = obj.getMessage("header", "%PREFIX%&7処罰履歴:")
            val footer = obj.getMessage("footer", "&7ページ &e&o%CURRENT_PAGE%&7/&e&o%MAX_PAGE% &8| &7処罰件数: &e&o%COUNT%")
            val invalidArguments = obj.getMessage("invalidArguments", "%PREFIX%&e--all&cと&e--active&cを同時に使用することはできません。")
        }
    }
}
