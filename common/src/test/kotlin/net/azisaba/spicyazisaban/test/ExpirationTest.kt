package net.azisaba.spicyazisaban.test

import net.azisaba.spicyAzisaBan.punishment.Expiration
import org.junit.jupiter.api.Test

class ExpirationTest {
    @Test
    fun deserialize() {
        Expiration.deserializeFromLong(-1).let {
            assert(it is Expiration.NeverExpire) { "-1 deserialized as $it" }
        }
    }
}