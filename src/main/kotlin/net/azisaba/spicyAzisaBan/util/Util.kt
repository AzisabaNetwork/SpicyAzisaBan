package net.azisaba.spicyAzisaBan.util

import com.google.common.net.InetAddresses
import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.connection.ProxiedPlayer
import util.UUIDUtil
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import util.ref.DataCache
import xyz.acrylicstyle.mcutil.common.PlayerProfile
import xyz.acrylicstyle.mcutil.common.SimplePlayerProfile
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.UUID
import java.util.concurrent.TimeUnit
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

    /**
     * method name says that.
     */
    fun CommandSender.sendDelayed(timeInMillis: Long, message: String) {
        ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, { send(message) }, timeInMillis, TimeUnit.MILLISECONDS)
    }

    /**
     * Prints stacktrace to console and sends "error" message to sender.
     */
    fun CommandSender.sendErrorMessage(throwable: Throwable) {
        if (throwable::class.java.canonicalName.endsWith("Cancel")) return // intended to block throwable like Promise$Cancel
        throwable.printStackTrace()
        send(SABMessages.General.error.replaceVariables().translate())
    }

    /**
     * Prints stacktrace to console and sends "error" message to sender.
     * Ignores the throwable with type T.
     */
    inline fun <reified T : Throwable> CommandSender.sendOrSuppressErrorMessage(throwable: Throwable) {
        if (throwable::class.java.canonicalName.endsWith("Cancel")) return
        if (throwable is T) return
        throwable.printStackTrace()
        send(SABMessages.General.error.replaceVariables().translate())
    }

    /**
     * Add missing zeros to beginning of the string.
     */
    fun zero(length: Int, any: Any): String {
        val s = any.toString()
        if (s.length >= length) return s
        return "0".repeat(length - s.length) + s
    }

    /**
     * Try to get IP address of the socket address. null if this isn't instance of InetSocketAddress.
     */
    fun SocketAddress?.getIPAddress() = if (this is InetSocketAddress) this.getIPAddress() else null

    /**
     * Returns the IP address of the socket address.
     */
    fun InetSocketAddress.getIPAddress() = address.getIPAddress()

    /**
     * Returns the IP address of the InetAddress.
     */
    fun InetAddress.getIPAddress() = hostAddress.replaceFirst("(.*)%.*".toRegex(), "$1")

    /**
     * Try to get the IP address of the player.
     * @throws IllegalArgumentException if player is connecting via something other than InetSocketAddress (e.g. unix socket)
     */
    fun ProxiedPlayer.getIPAddress(): String {
        require(socketAddress is InetSocketAddress) { "Player $name is connecting via unix socket" }
        return (socketAddress as InetSocketAddress).getIPAddress()
    }

    /**
     * Concatenates one or more lists.
     */
    fun <T> Iterable<T>.concat(vararg another: List<T>?) = this.toMutableList().apply { another.filterNotNull().forEach { addAll(it) } }

    /**
     * Time format in string -> Time in milliseconds
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

    /**
     * Time in milliseconds -> formatted time text
     */
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

    /**
     * Returns colored string of the boolean.
     */
    fun Boolean.toMinecraft() = if (this) "${ChatColor.GREEN}true" else "${ChatColor.RED}false"

    /**
     * Translates '&' into section char.
     */
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

    /**
     * Returns list that matches/starts with specified string.
     */
    fun List<String>.filtr(s: String): List<String> = distinct().filter { s1 -> s1.lowercase().startsWith(s.lowercase()) }

    private val insertLock = Object()

    /**
     * Executes `fn` and returns the result of `SELECT LAST_INSERT_ID()`.
     */
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
            return r.toLong()
        }
    }

    /**
     * #insert but without return value
     */
    fun insertNoId(fn: () -> Unit) {
        synchronized(insertLock) {
            fn()
        }
    }

    /**
     * Try to get the uuid of the sender.
     * @return Player's uuid if sender is ProxiedPlayer, NIL otherwise
     */
    fun CommandSender.getUniqueId(): UUID = when (this) {
        is ProxiedPlayer -> this.uniqueId
        else -> UUIDUtil.NIL
    }

    /**
     * Checks if sender has "notify" permission for a punishment type.
     */
    fun CommandSender.hasNotifyPermissionOf(type: PunishmentType) = hasPermission("sab.notify.${type.id}")

    private val profileCache = mutableMapOf<UUID, DataCache<PlayerProfile>>()

    /**
     * Fetches the PlayerProfile of player's uuid.
     * The result may be cached. and always returns `CONSOLE` for NIL uuid.
     */
    fun UUID.getProfile(): Promise<PlayerProfile> = Promise.create { context ->
        if (this == UUIDUtil.NIL) return@create context.resolve(SimplePlayerProfile("CONSOLE", this))
        val cache = profileCache[this]
        val profile = cache?.get()
        if (cache == null || profile == null || cache.ttl - System.currentTimeMillis() <= 10000) {
            PlayerData.getByUUID(this)
                .thenDo {
                    context.resolve(it)
                    profileCache[this] = DataCache(profile, System.currentTimeMillis() + 1000L * 60L * 60L)
                }
                .catch { context.reject(it) }
        } else {
            context.resolve(profile)
        }
    }

    /**
     * Kicks the player with specified reason. The string will be automatically converted into TextComponent.
     */
    fun ProxiedPlayer.kick(reason: String) {
        this.disconnect(*TextComponent.fromLegacyText(reason.replace("  ", " ${ChatColor.RESET} ${ChatColor.RESET}")))
    }

    /**
     * Reconstructs(Formats) IP address.
     * For example, this method converts `0001.001.01.1` into `1.1.1.1`.
     */
    @Suppress("UnstableApiUsage")
    fun String.reconstructIPAddress(): String {
        if (!isValidIPAddress()) error("not a valid ip address")
        return InetAddresses.forString(this).getIPAddress()
    }

    /**
     * Checks if the ip address (v4 only) is punishable.
     * The IP address must NOT be:
     * - Private address (e.g. 127.0.0.1, 192.168.xxx.xxx)
     * - Reserved address
     * IPv6 is not checked thus always returns true for IPv6 addresses.
     */
    fun String.isPunishableIP(): Boolean {
        if (!isValidIPAddress()) throw IllegalArgumentException("Invalid IP address: $this")
        if (!isValidIPv4Address()) return true // skip IPv6 checks
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

    /**
     * Checks if the string is valid IP address.
     */
    @Suppress("UnstableApiUsage")
    fun String.isValidIPAddress(): Boolean = InetAddresses.isInetAddress(this)

    /**
     * Checks if the string is valid IPv4 address.
     */
    private fun String.isValidIPv4Address(): Boolean {
        val numbers = this.split(".").mapNotNull {
            try {
                Integer.parseInt(it, 10)
            } catch (e: NumberFormatException) { null }
        }
        if (numbers.size != 4) return false
        return numbers.all { it in 0..255 }
    }

    /**
     * Try to get the server name of the server they're in. May be null if server is null or the sender isn't player.
     */
    fun CommandSender.getServerName() = if (this is ProxiedPlayer) this.server?.info?.name ?: "" else ""

    /**
     * Sends the message to specified server after 100 + (random time in range 0-300) seconds.
     */
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

    /**
     * Try to parse int or fallbacks to `def` if fails.
     */
    fun String.toIntOr(def: Int, radix: Int = 10) = try {
        Integer.parseInt(this, radix)
    } catch (e: NumberFormatException) {
        def
    }

    /**
     * Extension method to allow `ChatColor.XXXX + string`.
     */
    operator fun ChatColor.plus(s: String) = "$this$s"

    fun String.getCurrentColor(char: Char = '§'): ChatColor {
        val reader = StringReader(this.reversed())
        while (!reader.isEOF()) {
            val first = reader.readFirst()
            if (reader.peek() == char) {
                ChatColor.getByChar(first)?.let { return it }
            }
        }
        return ChatColor.WHITE
    }

    fun String.toUUID() = try {
        UUID.fromString(this)!!
    } catch (e: IllegalArgumentException) {
        null
    }

    fun getPlayerNamesWithRecentPunishedPlayers() =
        ProxyServer.getInstance().players
            .filterIndexed { i, _ -> i < 500 }
            .map { it.name }
            .concat(Punishment.recentPunishedPlayers.map { it.name })
            .distinct()

    fun ProxiedPlayer.connectToLobbyOrKick(from: ServerContext, reason: Array<BaseComponent>) {
        if (from.name == "global") {
            return this.disconnect(*reason)
        }
        val servers = if (from.isGroup) {
            SpicyAzisaBan.instance.connection.getServersByGroup(from.name).complete()
        } else {
            listOf(ProxyServer.getInstance().servers[from.name]!!)
        }
        this.connectToLobbyOrKick(servers.toTypedArray(), reason)
    }

    fun ProxiedPlayer.connectToLobbyOrKick(from: Array<ServerInfo>, reason: Array<BaseComponent>) {
        if (this.server?.info?.name?.startsWith("lobby") == true) {
            this.disconnect(*reason)
            return
        }
        if (from.isEmpty() || from.contains(this.server?.info)) {
            val lobby = ProxyServer.getInstance().servers.values
                .filter { it.name.startsWith("lobby") }
                .filterNotNull()
                .random()
            this.connect(lobby)
            ProxyServer.getInstance().scheduler.schedule(SpicyAzisaBan.instance, {
                this.sendMessage(*reason)
            }, 2, TimeUnit.SECONDS)
        }
    }
}
