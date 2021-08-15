package net.azisaba.spicyAzisaBan.util

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.connection.ProxiedPlayer
import util.UUIDUtil
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.mcutil.common.PlayerProfile
import xyz.acrylicstyle.mcutil.common.SimplePlayerProfile
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.NumberFormatException
import kotlin.math.floor

object Util {
    /**
     * Sends a message to sender.
     */
    fun CommandSender.send(message: String) {
        message.split("\\n|\\\\n".toRegex()).forEach { msg ->
            sendMessage(*TextComponent.fromLegacyText(msg.replace("  ", " ${ChatColor.RESET} ${ChatColor.RESET}")))
        }
    }

    fun CommandSender.sendErrorMessage(throwable: Throwable) {
        throwable.printStackTrace()
        send(SABMessages.General.error.replaceVariables().translate())
    }

    private var lastPreloadedPermissions = 0L

    /**
     * (Pre)load permissions so permissions will show up on LuckPerms suggestions.
     */
    fun preloadPermissions(sender: CommandSender) {
        if (System.currentTimeMillis() - lastPreloadedPermissions > 60000) return
        sender.hasPermission("sab.command.spicyazisaban")
        sender.hasPermission("sab.check")
        sender.hasPermission("sab.history")
        sender.hasPermission("sab.seen")
        sender.hasPermission("sab.banlist")
        sender.hasPermission("sab.proofs")
        sender.hasPermission("sab.delproof")
        sender.hasPermission("sab.addproof")
        sender.hasPermission("sab.changereason")
        sender.hasPermission("sab.unban")
        sender.hasPermission("sab.unmute")
        sender.hasPermission("sab.unpunish")
        sender.hasPermission("sab.punish.global")
        PunishmentType.values().forEach { type ->
            sender.hasPermission(type.perm)
            sender.hasNotifyPermissionOf(type)
        }
        SpicyAzisaBan.instance.connection.getAllGroups().then {
            it.forEach { group -> sender.hasPermission("sab.punish.group.$group") }
        }
        ProxyServer.getInstance().servers.keys.forEach { server -> sender.hasPermission("sab.punish.server.$server") }
        lastPreloadedPermissions = System.currentTimeMillis()
    }

    /**
     * Formats timestamp to "year/month/day hour:minute:second.millisecond"
     */
    fun formatDateTime(millis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return cal.formatDateTime()
    }

    /**
     * Add zeros (if missing) to beginning of the string.
     */
    fun zero(length: Int, any: Any): String {
        val s = any.toString()
        if (s.length >= length) return s
        return "0".repeat(length - s.length) + s
    }

    /**
     * Formats calendar to "year/month/day hour:minute:second.millisecond"
     */
    fun Calendar.formatDateTime(): String {
        val year = zero(4, this[Calendar.YEAR])
        val month = zero(2, this[Calendar.MONTH] + 1)
        val day = zero(2, this[Calendar.DAY_OF_MONTH])
        val hour = zero(2, this[Calendar.HOUR_OF_DAY])
        val minute = zero(2, this[Calendar.MINUTE])
        val second = zero(2, this[Calendar.SECOND])
        val millisecond = zero(3, this[Calendar.MILLISECOND])
        return "$year/$month/$day $hour:$minute:$second.$millisecond"
    }

    /**
     * you know what this method does (unless you're drunk).
     */
    fun Calendar.getBeginAndEndOfMonth(): Pair<Long, Long> {
        val c = this.clone() as Calendar
        c[Calendar.DAY_OF_MONTH] = 1
        c[Calendar.HOUR_OF_DAY] = 0
        c[Calendar.MINUTE] = 0
        c[Calendar.SECOND] = 0
        c[Calendar.MILLISECOND] = 0
        val first = c.timeInMillis
        var newMonth = c[Calendar.MONTH] + 1
        if (newMonth > 11) {
            c.set(Calendar.YEAR, c[Calendar.YEAR] + 1)
            newMonth -= 12
        }
        c[Calendar.MONTH] = newMonth
        val second = c.timeInMillis - 1
        return first to second
    }

    /**
     * this method gets current month. nothing else.
     */
    fun getCurrentMonth() = Calendar.getInstance()[Calendar.MONTH]

    /**
     * Sets the month, but it makes sure that the month is not future.
     */
    fun Calendar.convertMonth(month: Int) {
        if (month < 0 || month > 11) error("Invalid month (must be 0-11 inclusive): $month")
        val backToThePast = month > getCurrentMonth()
        if (backToThePast) {
            this[Calendar.YEAR]--
            this[Calendar.MONTH] = month
        }
        this[Calendar.MONTH] = month
    }

    fun SocketAddress.getIPAddress() = if (this is InetSocketAddress) this.getIPAddress() else null

    fun InetSocketAddress.getIPAddress() = address.getIPAddress()

    fun InetAddress.getIPAddress() = hostAddress.replaceFirst("(.*)%.*".toRegex(), "$1")

    fun ProxiedPlayer.getIPAddress(): String {
        require(socketAddress is InetSocketAddress) { "Player $name is connecting via unix socket" }
        return (socketAddress as InetSocketAddress).getIPAddress()
    }

    fun <T> Iterable<T>.concat(vararg another: List<T>?) = this.toMutableList().apply { another.filterNotNull().forEach { addAll(it) } }

    /**
     * @sample net.azisaba.spicyAzisaBan.test.ProcessTimeTest
     */
    @Throws(IllegalArgumentException::class)
    fun processTime(s: String): Long {
        var time = 0L
        var rawNumber = ""
        val reader = StringReader(s)
        while (!reader.isEOF()) {
            val c = reader.readFirst()
            if (c.isDigit()) {
                rawNumber += c
            } else {
                if (rawNumber.isEmpty()) throw IllegalArgumentException("Unexpected non-digit character: '$c' at index ${reader.index}")
                // mo
                if (c == 'm' && !reader.isEOF() && reader.peek() == 'o') {
                    reader.skip()
                    time += month * rawNumber.toLong()
                    rawNumber = ""
                    continue
                }
                // y(ear), d(ay), h(our), m(inute), s(econd)
                time += when (c) {
                    'y' -> year * rawNumber.toLong()
                    // mo is not here
                    'd' -> day * rawNumber.toLong()
                    'h' -> hour * rawNumber.toLong()
                    'm' -> minute * rawNumber.toLong()
                    's' -> second * rawNumber.toLong()
                    else -> throw IllegalArgumentException("Unexpected character: '$c' at index ${reader.index}")
                }
                rawNumber = ""
            }
        }
        return time
    }

    fun unProcessTime(l: Long): String {
        if (l < 0L) return SABMessages.General.permanent
        var time = l
        var d = 0
        var h = 0
        var m = 0
        var s = 0
        if (time > day) {
            val t = floor(time / day.toDouble()).toLong()
            d = t.toInt()
            time -= t * day
        }
        if (time > hour) {
            val t = floor(time / hour.toDouble()).toLong()
            h = t.toInt()
            time -= t * hour
        }
        if (time > minute) {
            val t = floor(time / minute.toDouble()).toLong()
            m = t.toInt()
            time -= t * minute
        }
        if (time > second) {
            val t = floor(time / second.toDouble()).toLong()
            s = t.toInt()
            time -= t * second
        }
        return SABMessages.formatDateTime(d, h, m, s)
    }

    fun Boolean.toMinecraft() = if (this) "${ChatColor.GREEN}true" else "${ChatColor.RED}false"

    fun String.translate() = ChatColor.translateAlternateColorCodes('&', this)!!

    fun List<String>.filterArgKeys(args: Array<String>): List<String> {
        val list = args.map { it.replace("(=.*)".toRegex(), "") }
        return filter { s -> !list.contains(s.replace("(=.*)".toRegex(), "")) }
    }

    @JvmName("listListFilterArgKeysString")
    fun List<List<String>>.filterArgKeys(args: Array<String>): List<String> {
        val arguments = args.map { it.replace("(=.*)".toRegex(), "") }
        val output = mutableListOf<String>()
        this.forEach { list ->
            if (list.all { s -> !arguments.contains(s.replace("(=.*)".toRegex(), "")) }) output.addAll(list)
        }
        return output
    }

    fun List<String>.filtr(s: String): List<String> = distinct().filter { s1 -> s1.lowercase().startsWith(s.lowercase()) }

    private val insertLock = Object()

    fun insert(fn: () -> Unit): Long {
        synchronized(insertLock) {
            fn()
            val statement = SpicyAzisaBan.instance.connection.connection.createStatement()
            val sql = "SELECT LAST_INSERT_ID()"
            SQLConnection.logSql(sql)
            val result = statement.executeQuery(sql)
            if (!result.next()) return -1L
            val r = result.getObject(1) as Number
            statement.close()
            return r.toLong() + 1L
        }
    }

    fun insertNoId(fn: () -> Unit) {
        synchronized(insertLock) {
            fn()
        }
    }

    fun CommandSender.getUniqueId(): UUID = when (this) {
        is ProxiedPlayer -> this.uniqueId
        else -> UUIDUtil.NIL
    }

    fun CommandSender.hasNotifyPermissionOf(type: PunishmentType) = hasPermission("sab.notify.${type.id}")

    fun UUID.getProfile(): Promise<PlayerProfile> = Promise.create { context ->
        if (this == UUIDUtil.NIL) return@create context.resolve(SimplePlayerProfile("CONSOLE", this))
        PlayerData.getByUUID(this)
            .thenDo { context.resolve(it) }
            .catch { context.reject(it) }
    }

    fun ProxiedPlayer.kick(reason: String) {
        this.disconnect(*TextComponent.fromLegacyText(reason.replace("  ", " ${ChatColor.RESET} ${ChatColor.RESET}")))
    }

    fun String.reconstructIPAddress(): String {
        if (!isPunishableIP()) error("not a valid ip address")
        val numbers = this.split(".").map { Integer.parseInt(it, 10) }
        return "${numbers[0]}.${numbers[1]}.${numbers[2]}.${numbers[3]}"
    }

    fun String.isPunishableIP(): Boolean {
        val numbers = this.split(".").mapNotNull {
            try {
                Integer.parseInt(it, 10)
            } catch (e: NumberFormatException) { null }
        }
        if (numbers.size != 4) return false
        if (numbers.any { it !in 0..255 }) return false
        if (SABConfig.enableDebugFeatures && (this == "127.0.0.1" || (numbers[0] == 192 && numbers[1] == 168))) return true
        // Reserved IP addresses
        // 0.0.0.0/8 (0.0.0.0 - 0.255.255.255)
        if (numbers[0] == 0) return false
        // 10.0.0.0/8 (10.0.0.0 - 10.255.255.255)
        if (numbers[0] == 10) return false
        // 100.64.0.0/10 (100.64.0.0 - 100.127.255.255)
        if (numbers[0] == 100 && numbers[1] >= 64 && numbers[1] <= 127) return false
        // 127.0.0.0/8 (127.0.0.0 - 127.255.255.255)
        if (numbers[0] == 127) return false
        // 169.254.0.0/16 (169.254.0.0 - 169.254.255.255)
        if (numbers[0] == 169 && numbers[1] == 254) return false
        // 192.0.0.0/24 (192.0.0.0 - 192.0.0.255)
        if (numbers[0] == 192 && numbers[1] == 0 && numbers[2] == 0) return false
        // 192.0.2.0/24 (192.0.2.0 - 192.0.2.255)
        if (numbers[0] == 192 && numbers[1] == 0 && numbers[2] == 2) return false
        // 192.88.99.0/24 (192.88.99.0 - 192.88.99.255)
        if (numbers[0] == 192 && numbers[1] == 88 && numbers[2] == 99) return false
        // 192.168.0.0/16 (192.168.0.0 - 192.168.255.255)
        if (numbers[0] == 192 && numbers[1] == 168) return false
        // 198.18.0.0/15 (192.18.0.0 - 192.19.255.255)
        if (numbers[0] == 198 && (numbers[1] == 18 || numbers[1] == 19)) return false
        // 203.0.133.0/24 (203.0.133.0 - 203.0.133.255)
        if (numbers[0] == 203 && numbers[1] == 0 && numbers[2] == 133) return false
        // 224.0.0.0/4 (224.0.0.0 - 239.255.255.255)
        if (numbers[0] in 224..239) return false
        // 233.252.0.0/24
        if (numbers[0] == 233 && numbers[1] == 252 && numbers[2] == 0) return false
        // 240.0.0.0/4 (240.0.0.0 - 255.255.255.254)
        // 255.255.255.255/32 (255.255.255.255)
        if (numbers[0] >= 240) return false
        return true
    }

    fun String.isValidIPAddress(): Boolean {
        val numbers = this.split(".").mapNotNull {
            try {
                Integer.parseInt(it, 10)
            } catch (e: NumberFormatException) { null }
        }
        if (numbers.size != 4) return false
        return numbers.all { it in 0..255 }
    }

    fun CommandSender.getServerName() = if (this is ProxiedPlayer) this.server?.info?.name ?: "" else ""

    fun ServerInfo.broadcastMessageAfterRandomTime(server: String) {
        val s = SABMessages.getBannedMessage(server).replaceVariables().translate()
        val random = 100 + (Math.random() * 300).toLong()
        ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
            ProxyServer.getInstance().players.forEach { p ->
                if (p.getServerName() == this.name) {
                    p.send(s)
                }
            }
        }, random, TimeUnit.SECONDS)
    }

    fun String.toIntOr(def: Int, radix: Int = 10) = try {
        Integer.parseInt(this, radix)
    } catch (e: NumberFormatException) {
        def
    }

    operator fun ChatColor.plus(s: String) = "$this$s"
}
