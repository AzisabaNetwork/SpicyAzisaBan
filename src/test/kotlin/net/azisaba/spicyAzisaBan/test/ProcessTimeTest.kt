package net.azisaba.spicyAzisaBan.test

import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.day
import net.azisaba.spicyAzisaBan.util.hour
import net.azisaba.spicyAzisaBan.util.minute
import net.azisaba.spicyAzisaBan.util.month
import net.azisaba.spicyAzisaBan.util.second
import net.azisaba.spicyAzisaBan.util.year
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProcessTimeTest {
    @Test
    fun testProcessTime() {
        assert(Util.processTime("1y7mo5d1h2m5s") == 1 * year + 7 * month + 5 * day + 1 * hour + 2 * minute + 5 * second) {
            "${Util.processTime("1y7mo5d1h2m5s")} != ${1 * year + 7 * month + 5 * day + 1 * hour + 2 * minute + 5 * second}"
        }
        assert(Util.processTime("1y1y1y7mo5d1h2m5s") == 3 * year + 7 * month + 5 * day + 1 * hour + 2 * minute + 5 * second) {
            "${Util.processTime("1y1y1y7mo5d1h2m5s")} != ${3 * year + 7 * month + 5 * day + 1 * hour + 2 * minute + 5 * second}"
        }
        assert(Util.processTime("1000y") == 1000 * year) {
            "${Util.processTime("1000y")} != ${1000 * year}"
        }
        assert(Util.processTime("27.2d") == (27.2 * day).toLong()) {
            "${Util.processTime("27.2d")} != ${(27.2 * day).toLong()}"
        }
        assert(Util.processTime("27.d") == 27 * day) {
            "${Util.processTime("27.d")} != ${27 * day}"
        }
        assert(Util.processTime("27.0d") == 27 * day) {
            "${Util.processTime("27.0d")} != ${27 * day}"
        }
    }

    @Test
    fun testProcessTimeButFails() {
        assertThrows<IllegalArgumentException> { Util.processTime("ymodhms") }
        assertThrows<IllegalArgumentException> { Util.processTime("1MO") }
    }
}
