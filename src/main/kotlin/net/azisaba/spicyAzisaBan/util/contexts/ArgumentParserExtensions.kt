package net.azisaba.spicyAzisaBan.util.contexts

import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.isPunishableIP
import net.azisaba.spicyAzisaBan.util.Util.reconstructIPAddress
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import util.ArgumentParser
import util.UUIDUtil
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.mcutil.common.SimplePlayerProfile
import java.sql.SQLException

@Suppress("UNCHECKED_CAST")
fun <T : Context> ArgumentParser.get(context: Contexts<T>, sender: CommandSender): Promise<T> {
    if (context == Contexts.PLAYER) return getPlayer(sender) as Promise<T>
    if (context == Contexts.SERVER) return getServer(sender, true) as Promise<T>
    if (context == Contexts.SERVER_NO_PERM_CHECK) return getServer(sender, false) as Promise<T>
    if (context == Contexts.REASON) return getReason() as Promise<T>
    if (context == Contexts.TIME) return getTime(sender) as Promise<T>
    if (context == Contexts.IP_ADDRESS) return getIPAddress(sender) as Promise<T>
    if (context == Contexts.PUNISHMENT_TYPE) return Promise.resolve(getPunishmentType(sender) as T)
    return Promise.reject(IllegalArgumentException("Unknown context: " + context.key))
}

private fun ArgumentParser.getPlayer(sender: CommandSender): Promise<PlayerContext> = Promise.create { context ->
    val rawName = parsedRawOptions["player"]
    if (rawName == null) {
        sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
        return@create context.resolve(PlayerContext(false, SimplePlayerProfile("", UUIDUtil.NIL)))
    }
    val profile = PlayerData.getByName(rawName)
        .catch {
            if (it is SQLException) it.printStackTrace()
            sender.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
        }
        .complete()
        ?: return@create context.resolve(PlayerContext(false, SimplePlayerProfile("", UUIDUtil.NIL)))
    context.resolve(PlayerContext(true, profile))
}

private fun ArgumentParser.getServer(sender: CommandSender, checkPermission: Boolean): Promise<ServerContext> = Promise.create { context ->
    var isGroup = false
    val server = parsedRawOptions["server"] ?: "global"
    if (server == "global") {
        if (checkPermission && !sender.hasPermission("sab.punish.global")) {
            sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
            return@create context.resolve(ServerContext(false, server, false))
        }
    } else {
        if (SpicyAzisaBan.instance.connection.getAllGroups().complete().contains(server)) {
            if (checkPermission && !sender.hasPermission("sab.punish.group.$server")) {
                sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
                return@create context.resolve(ServerContext(false, server, false))
            }
            isGroup = true
        } else {
            if (!ProxyServer.getInstance().servers.containsKey(server)) {
                sender.send(SABMessages.Commands.General.invalidServer.replaceVariables().translate())
                return@create context.resolve(ServerContext(false, server, false))
            }
            if (checkPermission && !sender.hasPermission("sab.punish.server.$server")) {
                sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
                return@create context.resolve(ServerContext(false, server, false))
            }
        }
    }
    return@create context.resolve(ServerContext(true, server, isGroup))
}

private fun ArgumentParser.getReason(): Promise<ReasonContext> = Promise.create { context ->
    val reason = parsedRawOptions["reason"]
    return@create context.resolve(ReasonContext(if (reason.isNullOrBlank()) "none" else reason))
}

private fun ArgumentParser.getTime(sender: CommandSender): Promise<TimeContext> = Promise.create { context ->
    val time = parsedRawOptions["time"]
    if (time.isNullOrBlank()) return@create context.resolve(TimeContext(true, -1L))
    try {
        return@create context.resolve(TimeContext(true, Util.processTime(time)))
    } catch (e: IllegalArgumentException) {
        sender.send(SABMessages.Commands.General.invalidTime.replaceVariables().translate())
        return@create context.resolve(TimeContext(false, -1L))
    }
}

private fun ArgumentParser.getIPAddress(sender: CommandSender): Promise<IPAddressContext> = Promise.create { context ->
    val target = parsedRawOptions["target"]
    if (target == null) {
        sender.send(SABMessages.Commands.General.invalidIPAddress.replaceVariables().translate())
        return@create context.resolve(IPAddressContext(false, ""))
    }
    try {
        if (target.isPunishableIP()) return@create context.resolve(
            IPAddressContext(
                true,
                target.reconstructIPAddress()
            )
        )
    } catch (ignored: IllegalArgumentException) {}
    val data = PlayerData.getByName(target)
        .catch { sender.sendErrorMessage(it) }
        .complete()
    if (data?.ip != null) {
        return@create context.resolve(IPAddressContext(true, data.ip))
    }
    sender.send(SABMessages.Commands.General.invalidIPAddress.replaceVariables().translate())
    context.resolve(IPAddressContext(false, ""))
}

private fun ArgumentParser.getPunishmentType(sender: CommandSender): PunishmentTypeContext {
    val type = parsedRawOptions["type"] ?: return PunishmentTypeContext(true, null)
    return try {
        val pType = PunishmentType.valueOf(type)
        PunishmentTypeContext(true, pType)
    } catch (e: IllegalArgumentException) {
        sender.send(SABMessages.Commands.General.invalidPunishmentType.replaceVariables().translate())
        PunishmentTypeContext(false, null)
    }
}
