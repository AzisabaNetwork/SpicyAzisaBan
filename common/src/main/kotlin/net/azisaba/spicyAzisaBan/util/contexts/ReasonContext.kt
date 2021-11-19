package net.azisaba.spicyAzisaBan.util.contexts

import net.azisaba.spicyAzisaBan.ReloadableSABConfig
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.filtr
import util.ArgumentParser

data class ReasonContext(val text: String): Context {
    companion object {
        fun tabComplete(type: PunishmentType, args: Array<String>, defaultServer: String = "global"): List<String> {
            val arguments = ArgumentParser(args.joinToString(" "))
            val server: String? = arguments.parsedRawOptions["server"]
            if (server.isNullOrEmpty() && defaultServer.isEmpty()) return emptyList()
            // don't look for a group if specified explicitly by server parameter
            val groupOrServer = server
                ?: (SpicyAzisaBan.instance.connection.getCachedGroupByServer(defaultServer) ?: defaultServer)
            return ReloadableSABConfig.defaultReasons[type]!![groupOrServer]?.map { "reason=\"$it\"" }?.filtr(args.last()) ?: emptyList()
        }
    }
}
