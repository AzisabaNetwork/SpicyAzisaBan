package net.azisaba.spicyAzisaBan.util

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import java.util.Calendar

object Util {
    /**
     * Sends a message to sender.
     */
    fun CommandSender.send(message: String) {
        sendMessage(*TextComponent.fromLegacyText(message))
    }

    private var preloadedPermissions = false

    /**
     * (Pre)load permissions so permissions will show up on LuckPerms suggestions.
     */
    fun preloadPermissions(sender: CommandSender) {
        if (preloadedPermissions) return
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
        preloadedPermissions = true
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
}
