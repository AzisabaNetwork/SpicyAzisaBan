package net.azisaba.spicyAzisaBan.test

import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.*
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
    }

    @Test
    fun testProcessTimeButFails() {
        assertThrows<IllegalArgumentException> { Util.processTime("ymodhms") }
        assertThrows<IllegalArgumentException> { Util.processTime("1MO") }
    }
}
