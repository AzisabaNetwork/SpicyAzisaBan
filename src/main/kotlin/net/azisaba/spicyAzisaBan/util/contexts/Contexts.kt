package net.azisaba.spicyAzisaBan.util.contexts

class Contexts<T : Context> private constructor(val key: String) {
    companion object {
        /**
         * "player" context. You may want to execute Promise asynchronously because we have to get player profile
         * asynchronously using Mojang's API.
         */
        val PLAYER = Contexts<PlayerContext>("player")

        /**
         * "server" context. You may want to execute Promise asynchronously because there is database operation within
         * server context. (to check whether the server is group)
         */
        val SERVER = Contexts<ServerContext>("server")
    }
}
