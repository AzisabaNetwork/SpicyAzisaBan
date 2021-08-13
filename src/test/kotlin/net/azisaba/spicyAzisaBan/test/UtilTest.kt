package net.azisaba.spicyAzisaBan.test

import net.azisaba.spicyAzisaBan.util.Util.isPunishableIP
import net.azisaba.spicyAzisaBan.util.Util.reconstructIPAddress
import org.junit.jupiter.api.Test

class UtilTest {
    @Test
    fun testPunishableIP() {
        assert(!"I am not an IP address".isPunishableIP())
        assert(!"255.255.255.255".isPunishableIP())
        assert(!"123.456.789.012".isPunishableIP())
        assert(!"0.0.0.0".isPunishableIP())
        assert("1.1.1.1".isPunishableIP())
        assert("1.1.1.1" == "001.001.01.1".reconstructIPAddress()) { "001.001.01.1".reconstructIPAddress() }
    }
}
