package net.azisaba.spicyAzisaBan

import net.azisaba.spicyAzisaBan.common.ChatColor
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.translate
import util.yaml.YamlConfiguration
import util.yaml.YamlObject
import java.io.File
import java.util.Calendar

object SABMessages {
    private lateinit var defCfg: YamlObject
    private lateinit var cfg: YamlObject

    init {
        reload()
    }

    fun reload() {
        val dir = SpicyAzisaBan.instance.getDataFolder().toFile()
        dir.mkdir()
        val file = File(dir, "messages.yml")
        val input = SABMessages::class.java.getResourceAsStream("/spicyazisaban/messages.yml")
            ?: error("Could not find messages.yml in jar file!")
        if (!file.exists()) {
            file.outputStream().use { input.copyTo(it) }
            SpicyAzisaBan.LOGGER.info("Generated default messages.yml")
        }
        defCfg = YamlConfiguration(input).asObject()
        cfg = YamlConfiguration(file).asObject()
    }

    fun YamlObject.getObj(key: String) = this.getObject(key) ?: YamlObject()

    fun YamlObject.getMessage(key: String, def: String = "<key: $key>"): String {
        val raw = this.rawData[key] ?: def
        if (raw is String) return raw
        return this.getArray(key)?.mapNotNull { o -> o?.toString() }?.joinToString("${ChatColor.RESET}\n") ?: def
    }

    /**
     * Replaces the variables (%KEY_IN_UPPERCASE%) in string with value.
     */
    fun String.replaceVariables(variables: Map<String, String> = mapOf()): String {
        var s = replace("%PREFIX%", SpicyAzisaBan.PREFIX)
            .replace("%CMD_PREFIX%", SABConfig.prefix)
        variables.forEach { (key, value) -> s = s.replace("%${key.uppercase()}%", value) }
        return s
    }

    fun String.replaceVariables(vararg pairs: Pair<String, String>) = replaceVariables(mapOf(*pairs))

    /**
     * Get a custom "banned" message or default one if there is no custom message.
     */
    fun getBannedMessage(server: String) =
        ReloadableSABConfig.customBannedMessage[server] ?: Commands.General.removedFromServer

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
        private val defObj get() = defCfg.getObj("general")
        private val obj get() = cfg.getObj("general")
        val prefix get() = obj.getMessage("prefix", defObj.getMessage("prefix"))
        val missingPermissions get() = obj.getMessage("missingPermissions", defObj.getMessage("missingPermissions"))
        val error get() = obj.getMessage("error", defObj.getMessage("error"))
        val invalidSyntax get() = obj.getMessage("invalid-syntax", defObj.getMessage("invalid-syntax"))
        val errorDetailed get() = obj.getMessage("errorDetailed", defObj.getMessage("errorDetailed")) // similar to 'error', but with exception details
        val none get() = obj.getMessage("none", defObj.getMessage("none"))
        val global get() = obj.getMessage("global", defObj.getMessage("global"))
        val permanent get() = obj.getMessage("permanent", defObj.getMessage("permanent"))
        val online get() = obj.getMessage("online", defObj.getMessage("online"))
        val offline get() = obj.getMessage("offline", defObj.getMessage("offline"))
        val previousPage get() = obj.getMessage("previousPage", defObj.getMessage("previousPage"))
        val nextPage get() = obj.getMessage("nextPage", defObj.getMessage("nextPage"))
        val datetime get() = obj.getMessage("datetime", defObj.getMessage("datetime"))

        object Time {
            private val defObj get() = General.defObj.getObj("time")
            private val obj get() = General.obj.getObj("time")
            val day get() = obj.getMessage("day", defObj.getMessage("day"))
            val hour get() = obj.getMessage("hour", defObj.getMessage("hour"))
            val minute get() = obj.getMessage("minute", defObj.getMessage("minute"))
            val second get() = obj.getMessage("second", defObj.getMessage("second"))
        }
    }

    object Commands {
        private val defObj get() = defCfg.getObj("commands")
        private val obj get() = cfg.getObj("commands")

        object General {
            private val defObj get() = Commands.defObj.getObj("general")
            private val obj get() = Commands.obj.getObj("general")
            val invalidGroup get() = obj.getMessage("invalidGroup", defObj.getMessage("invalidGroup"))
            val invalidServer get() = obj.getMessage("invalidServer", defObj.getMessage("invalidServer"))
            val invalidPlayer get() = obj.getMessage("invalidPlayer", defObj.getMessage("invalidPlayer"))
            val invalidTime get() = obj.getMessage("invalidTime", defObj.getMessage("invalidTime"))
            val invalidIPAddress get() = obj.getMessage("invalidIPAddress", defObj.getMessage("invalidIPAddress"))
            val invalidPunishmentType get() = obj.getMessage("invalidPunishmentType", defObj.getMessage("invalidPunishmentType"))
            val invalidNumber get() = obj.getMessage("invalidNumber", defObj.getMessage("invalidNumber"))
            val timeNotSpecified get() = obj.getMessage("timeNotSpecified", defObj.getMessage("timeNotSpecified"))
            val samePunishmentAppliedToSameIPAddress get() = obj.getMessage("samePunishmentAppliedToSameIPAddress", defObj.getMessage("samePunishmentAppliedToSameIPAddress"))
            val alreadyPunished get() = obj.getMessage("alreadyPunished", defObj.getMessage("alreadyPunished"))
            val removedFromServer get() = obj.getMessage("removedFromServer", defObj.getMessage("removedFromServer"))
            val offlinePlayer get() = obj.getMessage("offlinePlayer", defObj.getMessage("offlinePlayer"))
            val notPunished get() = obj.getMessage("notPunished", defObj.getMessage("notPunished"))
            val noReasonSpecified get() = obj.getMessage("noReasonSpecified", defObj.getMessage("noReasonSpecified"))
            val noProofSpecified get() = obj.getMessage("noProofSpecified", defObj.getMessage("noProofSpecified"))
            val punishmentNotFound get() = obj.getMessage("punishmentNotFound", defObj.getMessage("punishmentNotFound"))
            val proofNotFound get() = obj.getMessage("proofNotFound", defObj.getMessage("proofNotFound"))
            val viewableProofs get() = obj.getMessage("viewable-proofs", defObj.getMessage("viewable-proofs"))
            val proofEntry get() = obj.getMessage("proof-entry", defObj.getMessage("proof-entry"))
        }

        object Sab {
            private val defObj get() = Commands.defObj.getObj("sab")
            private val obj get() = Commands.obj.getObj("sab")
            val setDebugLevel get() = obj.getMessage("setDebugLevel", defObj.getMessage("setDebugLevel"))
            val reloadedConfiguration get() = obj.getMessage("reloadedConfiguration", defObj.getMessage("reloadedConfiguration"))
            val clearedCache get() = obj.getMessage("clearedCache", defObj.getMessage("clearedCache"))
            val deleteGroupUnpunishReason get() = obj.getMessage("deleteGroupUnpunishReason", defObj.getMessage("deleteGroupUnpunishReason"))
            val removedFromPunishmentHistory get() = obj.getMessage("removedFromPunishmentHistory", defObj.getMessage("removedFromPunishmentHistory"))
            val removedFromPunishment get() = obj.getMessage("removedFromPunishment", defObj.getMessage("removedFromPunishment"))
            val info get() = obj.getMessage("info", defObj.getMessage("info"))
            val apiTableNotFound get() = obj.getMessage("apiTableNotFound", defObj.getMessage("apiTableNotFound"))
            val accountNoLinkCode get() = obj.getMessage("accountNoLinkCode", defObj.getMessage("accountNoLinkCode"))
            val accountLinking get() = obj.getMessage("accountLinking", defObj.getMessage("accountLinking"))
            val accountLinkComplete get() = obj.getMessage("accountLinkComplete", defObj.getMessage("accountLinkComplete"))
            val accountUnlinked get() = obj.getMessage("accountUnlinked", defObj.getMessage("accountUnlinked"))
        }

        object Ban {
            private val defObj get() = Commands.defObj.getObj("ban")
            private val obj get() = Commands.obj.getObj("ban")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
        }

        object TempBan {
            private val defObj get() = Commands.defObj.getObj("tempban")
            private val obj get() = Commands.obj.getObj("tempban")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
        }

        object IPBan {
            private val defObj get() = Commands.defObj.getObj("ipban")
            private val obj get() = Commands.obj.getObj("ipban")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
        }

        object TempIPBan {
            private val defObj get() = Commands.defObj.getObj("tempipban")
            private val obj get() = Commands.obj.getObj("tempipban")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
        }

        object Mute {
            private val defObj get() = Commands.defObj.getObj("mute")
            private val obj get() = Commands.obj.getObj("mute")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
            val layout1 get() = obj.getMessage("layout1", defObj.getMessage("layout1"))
            val layout2 get() = obj.getMessage("layout2", defObj.getMessage("layout2"))
        }

        object TempMute {
            private val defObj get() = Commands.defObj.getObj("tempmute")
            private val obj get() = Commands.obj.getObj("tempmute")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
            val layout1 get() = obj.getMessage("layout1", defObj.getMessage("layout1"))
            val layout2 get() = obj.getMessage("layout2", defObj.getMessage("layout2"))
        }

        object IPMute {
            private val defObj get() = Commands.defObj.getObj("ipmute")
            private val obj get() = Commands.obj.getObj("ipmute")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
            val layout1 get() = obj.getMessage("layout1", defObj.getMessage("layout1"))
            val layout2 get() = obj.getMessage("layout2", defObj.getMessage("layout2"))
        }

        object TempIPMute {
            private val defObj get() = Commands.defObj.getObj("tempipmute")
            private val obj get() = Commands.obj.getObj("tempipmute")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
            val layout1 get() = obj.getMessage("layout1", defObj.getMessage("layout1"))
            val layout2 get() = obj.getMessage("layout2", defObj.getMessage("layout2"))
        }

        object Warning {
            private val defObj get() = Commands.defObj.getObj("warning")
            private val obj get() = Commands.obj.getObj("warning")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
            val title get() = obj.getMessage("title", defObj.getMessage("title"))
            val subtitle get() = obj.getMessage("subtitle", defObj.getMessage("subtitle"))
        }

        object Caution {
            private val defObj get() = Commands.defObj.getObj("caution")
            private val obj get() = Commands.obj.getObj("caution")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
            val title get() = obj.getMessage("title", defObj.getMessage("title"))
            val subtitle get() = obj.getMessage("subtitle", defObj.getMessage("subtitle"))
        }

        object Kick {
            private val defObj get() = Commands.defObj.getObj("kick")
            private val obj get() = Commands.obj.getObj("kick")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
        }

        object Note {
            private val defObj get() = Commands.defObj.getObj("note")
            private val obj get() = Commands.obj.getObj("note")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val globalUsage get() = obj.getMessage("globalUsage", defObj.getMessage("globalUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
        }

        object Unpunish {
            private val defObj get() = Commands.defObj.getObj("unpunish")
            private val obj get() = Commands.obj.getObj("unpunish")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val unbanUsage get() = obj.getMessage("unbanUsage", defObj.getMessage("unbanUsage"))
            val unmuteUsage get() = obj.getMessage("unmuteUsage", defObj.getMessage("unmuteUsage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
            val notify get() = obj.getMessage("notify", defObj.getMessage("notify"))
        }

        object ChangeReason {
            private val defObj get() = Commands.defObj.getObj("changereason")
            private val obj get() = Commands.obj.getObj("changereason")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
        }

        object AddProof {
            private val defObj get() = Commands.defObj.getObj("addproof")
            private val obj get() = Commands.obj.getObj("addproof")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
        }

        object UpdateProof {
            private val defObj get() = Commands.defObj.getObj("updateproof")
            private val obj get() = Commands.obj.getObj("updateproof")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
        }

        object DelProof {
            private val defObj get() = Commands.defObj.getObj("delproof")
            private val obj get() = Commands.obj.getObj("delproof")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val done get() = obj.getMessage("done", defObj.getMessage("done"))
        }

        object Seen {
            private val defObj get() = Commands.defObj.getObj("seen")
            private val obj get() = Commands.obj.getObj("seen")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val searching get() = obj.getMessage("searching", defObj.getMessage("searching"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
            val layoutIP get() = obj.getMessage("layoutIP", defObj.getMessage("layoutIP"))
        }

        object Check {
            private val defObj get() = Commands.defObj.getObj("check")
            private val obj get() = Commands.obj.getObj("check")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val searching get() = obj.getMessage("searching", defObj.getMessage("searching"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
            val layoutIP get() = obj.getMessage("layoutIP", defObj.getMessage("layoutIP"))
            val banInfo get() = obj.getMessage("banInfo", defObj.getMessage("banInfo"))
            val muteInfo get() = obj.getMessage("muteInfo", defObj.getMessage("muteInfo"))
            val cannotUseTargetAndID get() = obj.getMessage("cannotUseTargetAndID", defObj.getMessage("cannotUseTargetAndID"))
        }

        object History {
            private val defObj get() = Commands.defObj.getObj("history")
            private val obj get() = Commands.obj.getObj("history")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val header get() = obj.getMessage("header", defObj.getMessage("header"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
            val footer get() = obj.getMessage("footer", defObj.getMessage("footer"))
            val unpunishReason get() = obj.getMessage("unpunishReason", defObj.getMessage("unpunishReason"))
            val unpunishId get() = obj.getMessage("unpunishId", defObj.getMessage("unpunishId"))
            val unpunishOperator get() = obj.getMessage("unpunishOperator", defObj.getMessage("unpunishOperator"))
            val invalidArguments get() = obj.getMessage("invalidArguments", defObj.getMessage("invalidArguments"))
        }

        object Proofs {
            private val defObj get() = Commands.defObj.getObj("proofs")
            private val obj get() = Commands.obj.getObj("proofs")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val header get() = obj.getMessage("header", defObj.getMessage("header"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
        }

        object Warns {
            private val defObj get() = Commands.defObj.getObj("warns")
            private val obj get() = Commands.obj.getObj("warns")
            val notWarnedYet get() = obj.getMessage("notWarnedYet", defObj.getMessage("notWarnedYet"))
            val header get() = obj.getMessage("header", defObj.getMessage("header"))
            val layout get() = obj.getMessage("layout", defObj.getMessage("layout"))
        }

        object BanList {
            private val defObj get() = Commands.defObj.getObj("banlist")
            private val obj get() = Commands.obj.getObj("banlist")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val header get() = obj.getMessage("header", defObj.getMessage("header"))
            val footer get() = obj.getMessage("footer", defObj.getMessage("footer"))
            val invalidArguments get() = obj.getMessage("invalidArguments", defObj.getMessage("invalidArguments"))
        }

        object Lockdown {
            private val defObj get() = Commands.defObj.getObj("lockdown")
            private val obj get() = Commands.obj.getObj("lockdown")
            val usage get() = obj.getMessage("usage", defObj.getMessage("usage"))
            val enabledLockdown get() = obj.getMessage("enabledLockdown", defObj.getMessage("enabledLockdown"))
            val disabledLockdown get() = obj.getMessage("disabledLockdown", defObj.getMessage("disabledLockdown"))
            val lockdown get() = obj.getMessage("lockdown", defObj.getMessage("lockdown"))
            val lockdownJoinAttempt get() = obj.getMessage("lockdownJoinAttempt", defObj.getMessage("lockdownJoinAttempt"))
        }
    }
}
