package net.azisaba.spicyAzisaBan.util.contexts

import net.azisaba.spicyAzisaBan.util.Util.filtr

data class TimeContext(
    val isSuccess: Boolean,
    val time: Long,
): Context {
    companion object {
        private val chars = listOf("y", "mo", "d", "h", "m", "s")

        fun tabComplete(s: String): List<String> {
            if (!s.matches("^time=.*\\d+$".toRegex())) return emptyList()
            return chars.map { c -> s + c }.filtr(s)
        }
    }
}
