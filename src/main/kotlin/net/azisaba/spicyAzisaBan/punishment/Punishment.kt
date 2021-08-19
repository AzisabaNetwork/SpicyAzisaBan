package net.azisaba.spicyAzisaBan.punishment

import net.azisaba.spicyAzisaBan.ReloadableSABConfig
import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment.Flags.Companion.toDatabase
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.getProfile
import net.azisaba.spicyAzisaBan.util.Util.hasNotifyPermissionOf
import net.azisaba.spicyAzisaBan.util.Util.isPunishableIP
import net.azisaba.spicyAzisaBan.util.Util.kick
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import util.ref.DataCache
import xyz.acrylicstyle.mcutil.common.PlayerProfile
import xyz.acrylicstyle.mcutil.common.SimplePlayerProfile
import xyz.acrylicstyle.sql.TableData
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions
import java.sql.ResultSet
import java.util.UUID

data class Punishment(
    val id: Long,
    val name: String,
    val target: String,
    val reason: String,
    val operator: UUID,
    val type: PunishmentType,
    val start: Long,
    val end: Long,
    val server: String,
    val flags: MutableList<Flags> = mutableListOf(),
) {
    companion object {
        private val pendingRemoval = mutableListOf<Long>()
        val recentPunishedPlayers = mutableSetOf<SimplePlayerProfile>()

        fun createByPlayer(
            playerProfile: PlayerProfile,
            reason: String,
            operator: UUID,
            type: PunishmentType,
            end: Long,
            server: String,
        ): Punishment {
            if (type.isIPBased()) error("Wrong type for #createByPlayer: ${type.name}")
            return Punishment(
                -1,
                playerProfile.name,
                playerProfile.uniqueId.toString(),
                reason,
                operator,
                type,
                System.currentTimeMillis(),
                end,
                server,
            )
        }

        fun createByIPAddress(ip: String, reason: String, operator: UUID, type: PunishmentType, end: Long, server: String): Punishment {
            if (!type.isIPBased()) error("Wrong type for #createByIPAddress: ${type.name}")
            return Punishment(-1, ip, ip, reason, operator, type, System.currentTimeMillis(), end, server)
        }

        fun fromTableData(td: TableData): Punishment {
            val id = td.getLong("id")!!
            val name = td.getString("name")!!
            val target = td.getString("target")!!
            val reason = td.getString("reason")!!
            val operator = UUID.fromString(td.getString("operator")!!)!!
            val type = PunishmentType.valueOf(td.getString("type"))
            val start = td.getLong("start") ?: 0
            val end = td.getLong("end") ?: 0
            val server = td.getString("server")!!
            val flags = Flags.fromDatabase(td.getString("extra")!!)
            return Punishment(
                id,
                name,
                target,
                reason,
                operator,
                type,
                start,
                end,
                server,
                flags,
            )
        }

        fun fromResultSet(rs: ResultSet): Punishment {
            val id = rs.getLong("id")
            val name = rs.getString("name")!!
            val target = rs.getString("target")!!
            val reason = rs.getString("reason")!!
            val operator = UUID.fromString(rs.getString("operator")!!)!!
            val type = PunishmentType.valueOf(rs.getString("type")!!)
            val start = rs.getLong("start")
            val end = rs.getLong("end")
            val server = rs.getString("server")!!
            val flags = Flags.fromDatabase(rs.getString("extra")!!)
            return Punishment(
                id,
                name,
                target,
                reason,
                operator,
                type,
                start,
                end,
                server,
                flags,
            )
        }

        fun readAllFromResultSet(rs: ResultSet): List<Punishment> {
            val list = mutableListOf<Punishment>()
            while (rs.next()) list.add(fromResultSet(rs))
            return list
        }

        fun canJoinServer(uuid: UUID?, address: String?, server: String, noLookupGroup: Boolean = false): Promise<Punishment?> = Promise.create { context ->
            if (uuid == null && address == null) return@create context.reject(IllegalArgumentException("Either uuid or address must not be null"))
            val group = if (server == "global" || noLookupGroup) server else SpicyAzisaBan.instance.connection.getGroupByServer(server).complete()
            val ps = fetchActivePunishmentsBy(uuid, address, server, group)
            if (ps.isEmpty()) return@create context.resolve(null)
            var punishment: Punishment? = null
            ps.filter { p -> p.type.isBan() }.forEach { p ->
                if (p.isExpired()) {
                    p.removeIfExpired()
                } else {
                    punishment = p
                }
            }
            canJoinServerCachedData[Triple(uuid, address, server)] = DataCache(punishment, System.currentTimeMillis() + 1000L * 60L * 120L) // 2 hours
            context.resolve(punishment)
        }

        private val canJoinServerCachedData = mutableMapOf<Triple<UUID?, String?, String>, DataCache<Punishment>>()
        fun canJoinServerCached(uuid: UUID?, address: String?, server: String): Pair<Boolean, Punishment?> =
            canJoinServerCachedData[Triple(uuid, address, server)]?.let { Pair(true, it.get()) } ?: Pair(false, null)

        private val muteCache = mutableMapOf<String, DataCache<Punishment>>()

        fun canSpeak(uuid: UUID?, address: String?, server: String, noCache: Boolean = false, noLookupGroup: Boolean = false): Promise<Punishment?> = Promise.create { context ->
            if (uuid == null && address == null) return@create context.reject(IllegalArgumentException("Either uuid or address must not be null"))
            val punish = muteCache["$uuid,$address"]
            val punishValue = punish?.get()
            if (noCache || punish == null || punish.ttl - System.currentTimeMillis() < 1000 * 60 * 5) {
                SpicyAzisaBan.debug("Checking for $uuid, $address (trigger: ChatEvent on $server)")
                muteCache["$uuid,$address"] = DataCache(punishValue, System.currentTimeMillis() + 1000L * 60L * 30L) // prevent spam to database
                val group = if (server == "global" || noLookupGroup) server else SpicyAzisaBan.instance.connection.getGroupByServer(server).complete()
                val ps = fetchActivePunishmentsBy(uuid, address, server, group)
                if (ps.isEmpty()) return@create context.resolve(null)
                var punishment: Punishment? = null
                ps.filter { p -> p.type.isMute() }.forEach { p ->
                    if (p.isExpired()) {
                        p.removeIfExpired()
                    } else {
                        punishment = p
                    }
                }
                muteCache["$uuid,$address"] = DataCache(punishment, System.currentTimeMillis() + 1000L * 60L * 30L)
                return@create context.resolve(punishment)
            }
            if (punishValue?.isExpired() == true) {
                punishValue.removeIfExpired()
                return@create context.resolve(null)
            }
            context.resolve(punishValue)
        }

        private fun fetchActivePunishmentsBy(uuid: UUID?, ip: String?, server: String, group: String?): List<Punishment> {
            if (uuid == null && ip == null) throw IllegalArgumentException("Either uuid or ip must not be null")
            val sql = "SELECT * FROM `punishments` WHERE (`target` = ? OR `target` = ?) AND (`server` = \"global\" OR `server` = ? OR `server` = ?)"
            SQLConnection.logSql(sql)
            val s = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
            s.setString(1, uuid?.toString() ?: ip)
            s.setString(2, ip ?: uuid.toString())
            s.setString(3, server)
            s.setString(4, group ?: server)
            val ps = mutableListOf<Punishment>()
            val rs = s.executeQuery()
            while (rs.next()) ps.add(fromResultSet(rs))
            s.close()
            return ps
        }

        fun fetchActivePunishmentById(id: Long): Promise<Punishment?> =
            SpicyAzisaBan.instance.connection.punishments.findOne(FindOptions.Builder().addWhere("id", id).setLimit(1).build())
                .then { it?.let { fromTableData(it) } }

        fun fetchPunishmentById(id: Long): Promise<Punishment?> =
            SpicyAzisaBan.instance.connection.punishmentHistory.findOne(FindOptions.Builder().addWhere("id", id).setLimit(1).build())
                .then { it?.let { fromTableData(it) } }
    }

    init {
        if (type.isIPBased() && !target.isPunishableIP()) error("This IP address ($target) cannot be banned")
        if (!type.isIPBased()) {
            try {
                val uuid = getTargetUUID()
                recentPunishedPlayers.add(SimplePlayerProfile(name, uuid))
            } catch (e: IllegalArgumentException) {}
        }
    }

    fun sendTitle() {
        if (type != PunishmentType.WARNING && type != PunishmentType.CAUTION) return
        if (flags.contains(Flags.SEEN)) return
        val player = ProxyServer.getInstance().getPlayer(getTargetUUID()) ?: return
        val title = ProxyServer.getInstance().createTitle()
        title.fadeIn(0)
        title.fadeOut(0)
        title.stay((SABConfig.Warning.titleStayTime / 50L).toInt())
        title.title(*TextComponent.fromLegacyText(SABMessages.Commands.Warning.title.translate()))
        title.subTitle(*TextComponent.fromLegacyText(SABMessages.Commands.Warning.subtitle.translate()))
        player.sendTitle(title)
    }

    fun removeIfExpired(): Promise<Unit> = Promise.create { context ->
        if (isExpired() && !pendingRemoval.contains(id)) {
            pendingRemoval.add(id)
            SpicyAzisaBan.debug("Removing punishment #${id} (reason: expired)")
            SpicyAzisaBan.debug(toString(), 2)
            SpicyAzisaBan.instance.connection.punishments.delete(
                FindOptions.Builder().addWhere("id", id).build()
            ).thenDo {
                SpicyAzisaBan.debug("Removed punishment #${id} (reason: expired)")
            }.complete()
            clearCache()
            pendingRemoval.remove(id)
        }
        context.resolve()
    }

    fun clearCache() {
        if (type.isBan()) {
            val toRemove = mutableListOf<Triple<UUID?, String?, String>>()
            canJoinServerCachedData.keys.forEach { triple ->
                if (triple.first.toString() == target || triple.second == target) {
                    toRemove.add(triple)
                }
            }
            toRemove.forEach { canJoinServerCachedData.remove(it) }
        }
        if (type.isMute()) {
            muteCache.toList().forEach { (s) ->
                if (s.contains(target)) muteCache.remove(s)
            }
        }
    }

    fun getProofs(): Promise<List<Proof>> =
        SpicyAzisaBan.instance.connection.proofs.findAll(FindOptions.Builder().addWhere("punish_id", id).build())
            .then { list -> list.map { td -> Proof.fromTableData(this, td) } }

    fun getTargetUUID(): UUID {
        if (type.isIPBased()) {
            throw IllegalArgumentException("This punishment ($id) is not UUID-based ban! (e.g. IP ban)")
        }
        return UUID.fromString(target)
    }

    fun getVariables(): Promise<Map<String, String>> = operator.getProfile()
        .then { profile ->
            mapOf(
                "id" to id.toString(),
                "player" to name,
                "target" to target,
                "operator" to profile.name,
                "type" to type.id.replaceFirstChar { it.uppercase() },
                "reason" to reason,
                "server" to if (server.lowercase() == "global") SABMessages.General.global else ReloadableSABConfig.serverNames.getOrDefault(server.lowercase(), server.lowercase()),
                "duration" to Util.unProcessTime(end - System.currentTimeMillis()),
                "time" to Util.unProcessTime(end - start),
                "date" to SABMessages.formatDate(start),
                "end_date" to if (end == -1L) "N/A" else SABMessages.formatDate(end)
            )
        }

    fun getBannedMessage() =
        getVariables()
            .then { variables ->
                return@then when (type) {
                    PunishmentType.BAN -> SABMessages.Commands.Ban.layout.replaceVariables(variables).translate()
                    PunishmentType.TEMP_BAN -> SABMessages.Commands.TempBan.layout.replaceVariables(variables).translate()
                    PunishmentType.IP_BAN -> SABMessages.Commands.IPBan.layout.replaceVariables(variables).translate()
                    PunishmentType.TEMP_IP_BAN -> SABMessages.Commands.TempIPBan.layout.replaceVariables(variables).translate()
                    PunishmentType.MUTE -> SABMessages.Commands.Mute.layout2.replaceVariables(variables).translate()
                    PunishmentType.TEMP_MUTE -> SABMessages.Commands.TempMute.layout2.replaceVariables(variables).translate()
                    PunishmentType.IP_MUTE -> SABMessages.Commands.IPMute.layout2.replaceVariables(variables).translate()
                    PunishmentType.TEMP_IP_MUTE -> SABMessages.Commands.TempIPMute.layout2.replaceVariables(variables).translate()
                    PunishmentType.WARNING -> SABMessages.Commands.Warning.layout.replaceVariables(variables).translate()
                    PunishmentType.CAUTION -> SABMessages.Commands.Caution.layout.replaceVariables(variables).translate()
                    PunishmentType.KICK -> SABMessages.Commands.Kick.layout.replaceVariables(variables).translate()
                    PunishmentType.NOTE -> "RIP Note 2021-2021." // it should not be shown
                }
            }
            .catch {
                SpicyAzisaBan.instance.logger.warning("Could not fetch player name of $operator")
                it.printStackTrace()
            }

    private fun getMessage() =
        getVariables()
            .then { variables ->
                return@then when (type) {
                    PunishmentType.BAN -> SABMessages.Commands.Ban.notify.replaceVariables(variables).translate()
                    PunishmentType.TEMP_BAN -> SABMessages.Commands.TempBan.notify.replaceVariables(variables).translate()
                    PunishmentType.IP_BAN -> SABMessages.Commands.IPBan.notify.replaceVariables(variables).translate()
                    PunishmentType.TEMP_IP_BAN -> SABMessages.Commands.TempIPBan.notify.replaceVariables(variables).translate()
                    PunishmentType.MUTE -> SABMessages.Commands.Mute.notify.replaceVariables(variables).translate()
                    PunishmentType.TEMP_MUTE -> SABMessages.Commands.TempMute.notify.replaceVariables(variables).translate()
                    PunishmentType.IP_MUTE -> SABMessages.Commands.IPMute.notify.replaceVariables(variables).translate()
                    PunishmentType.TEMP_IP_MUTE -> SABMessages.Commands.TempIPMute.notify.replaceVariables(variables).translate()
                    PunishmentType.WARNING -> SABMessages.Commands.Warning.notify.replaceVariables(variables).translate()
                    PunishmentType.CAUTION -> SABMessages.Commands.Caution.notify.replaceVariables(variables).translate()
                    PunishmentType.KICK -> SABMessages.Commands.Kick.notify.replaceVariables(variables).translate()
                    PunishmentType.NOTE -> SABMessages.Commands.Note.notify.replaceVariables(variables).translate()
                }
            }
            .catch {
                SpicyAzisaBan.instance.logger.warning("Could not fetch player name of $operator")
                it.printStackTrace()
            }

    fun notifyToAll() =
        getMessage().thenDo { message ->
            ProxyServer.getInstance().console.send(message)
            ProxyServer.getInstance().players.filter { it.hasNotifyPermissionOf(type) }.forEach { player ->
                player.send(message)
            }
        }.then {}

    fun isExpired() = (end != -1L && end < System.currentTimeMillis()) || pendingRemoval.contains(id)

    fun updateFlags(): Promise<Unit> =
        SpicyAzisaBan.instance.connection.punishments.update("extra", flags.toDatabase(), FindOptions.Builder().addWhere("id", id).build())
            .then(SpicyAzisaBan.instance.connection.punishmentHistory.update("extra", flags.toDatabase(), FindOptions.Builder().addWhere("id", id).build()))
            .then {}

    /**
     * Inserts a new punishment into database.
     * @return new punishment with ID
     */
    fun insert(): Promise<Punishment> = Promise.create { context ->
        if (id != -1L) throw IllegalArgumentException("cannot insert existing punishment")
        val insertOptions = InsertOptions.Builder()
            .addValue("name", name)
            .addValue("target", target)
            .addValue("reason", reason)
            .addValue("operator", operator.toString())
            .addValue("type", type.name)
            .addValue("start", start)
            .addValue("end", end)
            .addValue("server", server.lowercase())
            .addValue("extra", flags.toDatabase())
        var cancel = false
        val id = Util.insert {
            cancel = SpicyAzisaBan.instance.connection.punishmentHistory.insert(insertOptions.build())
                .catch { it.printStackTrace();context.reject(it) }
                .complete() == null
        }
        if (cancel) return@create
        Util.insert u@{
            cancel = SpicyAzisaBan.instance.connection.punishments.insert(insertOptions.addValue("id", id).build())
                .catch { it.printStackTrace();context.reject(it) }
                .complete() == null
        }
        if (cancel) return@create
        clearCache()
        return@create context.resolve(
            Punishment(
                id,
                name,
                target,
                reason,
                operator,
                type,
                start,
                end,
                server.lowercase(),
                flags,
            )
        )
    }

    private fun alsoApplyToPlayer(profile: PlayerProfile): Promise<Punishment> =
        createByPlayer(profile, reason, operator, type, end, server).insert()

    fun applyToSameIPs(uuid: UUID): Promise<List<Punishment>> = Promise.create<List<Punishment>?> { context ->
        val data = PlayerData.getByUUID(uuid).catch { context.reject(it) }.complete() ?: return@create
        if (data.ip == null) return@create context.resolve(emptyList())
        val punishments = mutableListOf<Punishment>()
        PlayerData.getByIP(data.ip)
            .catch { context.reject(it) }
            .complete()
            ?.forEach { pd ->
                if (pd.uuid == uuid) return@forEach
                val p = alsoApplyToPlayer(pd).catch { context.reject(it) }.complete() ?: return@create
                punishments.add(p)
                if (p.type.isBan()) {
                    p.getBannedMessage().thenDo { ProxyServer.getInstance().getPlayer(pd.uuid)?.kick(it) }
                } else {
                    p.getBannedMessage().thenDo { ProxyServer.getInstance().getPlayer(pd.uuid)?.send(it) }
                }
            }
            ?: return@create
        context.resolve(punishments)
    }.thenDo { list ->
        val message = SABMessages.Commands.General.samePunishmentAppliedToSameIPAddress.replaceVariables().format(list.size).translate()
        ProxyServer.getInstance().console.send(message)
        ProxyServer.getInstance().players.filter { it.hasNotifyPermissionOf(type) }.forEach { player ->
            player.send(message)
        }
    }

    fun getHistoryMessage(): Promise<String> = Promise.create { context ->
        val unpunish: UnPunish? = SpicyAzisaBan.instance.connection.unpunish
            .findOne(FindOptions.Builder().addWhere("punish_id", this.id).setLimit(1).build())
            .then { it?.let { UnPunish.fromTableData(this, it) } }
            .complete()
        val strikethroughIfUnpunished = if (unpunish == null) "" else "${ChatColor.STRIKETHROUGH}"
        val unpunishReason = if (unpunish == null) "" else SABMessages.Commands.History.unpunishReason.replaceVariables("reason" to unpunish.reason).translate()
        val unpunishId = if (unpunish == null) "" else SABMessages.Commands.History.unpunishId.replaceVariables("id" to unpunish.id.toString()).translate()
        val unpunishOperator = if (unpunish == null) "" else {
            val opName = unpunish.operator.getProfile().catch {}.complete()?.name ?: "Unknown"
            SABMessages.Commands.History.unpunishOperator.replaceVariables("operator" to opName).translate()
        }
        context.resolve(
            SABMessages.Commands.History.layout.replaceVariables()
                .replaceVariables(getVariables().complete())
                .replaceVariables(
                    "date" to SABMessages.formatDate(start),
                    "unpunish_reason" to unpunishReason,
                    "unpunish_id" to unpunishId,
                    "unpunish_operator" to unpunishOperator,
                    "strikethrough_if_unpunished" to strikethroughIfUnpunished,
                )
                .translate()
        )
    }

    enum class Flags {
        SEEN,
        ;

        fun and(flags: Flags) = listOf(this, flags)

        companion object {
            fun fromDatabase(s: String): MutableList<Flags> = if (s.isEmpty()) mutableListOf() else s.split(",").map { valueOf(it) }.distinct().toMutableList()

            fun List<Flags>.toDatabase() = this.distinct().joinToString(",") { it.name }
            fun List<Flags>.and(flags: Flags) = this.toMutableList().apply { add(flags) }
        }
    }
}
