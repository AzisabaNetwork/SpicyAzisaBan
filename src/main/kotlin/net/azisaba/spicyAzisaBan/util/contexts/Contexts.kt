package net.azisaba.spicyAzisaBan.util.contexts

class Contexts<T : Context> private constructor(val key: String) {
    companion object {
        /**
         * "player" context. You may want to execute Promise asynchronously because we have to get player profile
         * asynchronously using database.
         */
        val PLAYER = Contexts<PlayerContext>("player")

        /**
         * "server" context. You may want to execute Promise asynchronously because there is database operation within
         * server context. (to check whether the server is group)
         */
        val SERVER = Contexts<ServerContext>("server")

        val SERVER_NO_PERM_CHECK = Contexts<ServerContext>("server")

        /**
         * Returns reason but returns "none" if blank or null and can never fail.
         */
        val REASON = Contexts<ReasonContext>("reason")

        val TIME = Contexts<TimeContext>("time")

        val IP_ADDRESS = Contexts<IPAddressContext>("target")

        val PUNISHMENT_TYPE = Contexts<PunishmentTypeContext>("type")
    }
}
