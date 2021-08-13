package net.azisaba.spicyAzisaBan.util.contexts

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.util.Util.concat
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.md_5.bungee.api.ProxyServer

data class ServerContext(
    val isSuccess: Boolean,
    val name: String,
    val isGroup: Boolean,
) : Context {
    companion object {
        fun tabComplete(s: String) = ProxyServer.getInstance()
            .servers
            .keys
            .concat(SpicyAzisaBan.instance.connection.getCachedGroups())
            .apply { add("global") }
            .map { "server=${it}" }
            .distinct()
            .filtr(s)
    }
}
