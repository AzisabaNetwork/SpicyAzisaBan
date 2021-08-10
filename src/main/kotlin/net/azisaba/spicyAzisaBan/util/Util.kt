package net.azisaba.spicyAzisaBan.util

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.Calendar
import kotlin.IllegalArgumentException
import kotlin.math.floor

object Util {
    /**
     * Sends a message to sender.
     */
    fun CommandSender.send(message: String) {
        sendMessage(*TextComponent.fromLegacyText(message.replace("  ", " ${ChatColor.RESET} ${ChatColor.RESET}")))
    }

    private var lastPreloadedPermissions = 0L

    /**
     * (Pre)load permissions so permissions will show up on LuckPerms suggestions.
     */
    fun preloadPermissions(sender: CommandSender) {
        if (System.currentTimeMillis() - lastPreloadedPermissions > 60000) return
        sender.hasPermission("sab.command.spicyazisaban")
        sender.hasPermission("sab.ban.perm")
        sender.hasPermission("sab.ban.temp")
        sender.hasPermission("sab.ipban.perm")
        sender.hasPermission("sab.ipban.temp")
        sender.hasPermission("sab.mute.perm")
        sender.hasPermission("sab.mute.temp")
        sender.hasPermission("sab.warning.perm")
        sender.hasPermission("sab.warning.temp")
        sender.hasPermission("sab.kick")
        sender.hasPermission("sab.note")
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
    private fun zero(length: Int, any: Any): String {
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

    fun InetSocketAddress.getIPAddress() = address.getIPAddress()

    fun InetAddress.getIPAddress() = hostAddress.replaceFirst("(.*)%.*".toRegex(), "$1")

    fun ProxiedPlayer.getIPAddress(): String {
        require(socketAddress is InetSocketAddress) { "Player $name is connecting via unix socket" }
        return (socketAddress as InetSocketAddress).getIPAddress()
    }

    fun <T> List<T>.concat(vararg another: List<T>) = this.toMutableList().apply { another.forEach { addAll(it) } }

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
        var s = ""
        var time = l
        if (time > day) {
            val t = floor(time / day.toDouble()).toLong()
            s += "${t}d "
            time -= t * day
        }
        if (time > hour) {
            val t = floor(time / hour.toDouble()).toLong()
            s += "${t}h "
            time -= t * hour
        }
        if (time > minute) {
            val t = floor(time / minute.toDouble()).toLong()
            s += "${t}m "
            time -= t * minute
        }
        if (time > second) {
            val t = floor(time / second.toDouble()).toLong()
            s += "${t}s"
            time -= t * second
        }
        return s
    }

    fun Boolean.toMinecraft() = if (this) "${ChatColor.GREEN}true" else "${ChatColor.RED}false"
}
