package net.azisaba.spicyAzisaBan.punishment

interface Expiration {
    companion object {
        private val DESERIALIZERS = mutableListOf<Deserializer>()

        fun addDeserializer(deserializer: Deserializer) =
            DESERIALIZERS.add(deserializer)

        fun deserializeFromLong(value: Long): Expiration? =
            DESERIALIZERS.find { it.canDeserialize(value) }?.deserialize(value)

        init {
            addDeserializer(NeverExpire.deserializer)
            addDeserializer(ExpireAt.deserializer)
        }
    }

    fun isExpired(): Boolean

    fun serializeAsLong(): Long

    interface Deserializer {
        fun canDeserialize(serialized: Long): Boolean

        fun deserialize(serialized: Long): Expiration
    }

    object NeverExpire : Expiration {
        val deserializer = object : Deserializer {
            override fun canDeserialize(serialized: Long): Boolean = serialized == -1L

            override fun deserialize(serialized: Long): Expiration = NeverExpire
        }

        override fun isExpired(): Boolean = false

        override fun serializeAsLong(): Long = -1L
    }

    class ExpireAt private constructor(private val expireAt: Long) : Expiration {
        companion object {
            fun of(expireAt: Long): ExpireAt = ExpireAt(expireAt)

            val deserializer = object : Deserializer {
                override fun canDeserialize(serialized: Long): Boolean = serialized > 0L

                override fun deserialize(serialized: Long): Expiration = ExpireAt(serialized)
            }
        }

        override fun isExpired(): Boolean = System.currentTimeMillis() >= expireAt

        override fun serializeAsLong(): Long = expireAt
    }
}
