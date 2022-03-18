package net.azisaba.spicyAzisaBan.util.contexts

import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.getNonParamStringAt
import net.azisaba.spicyAzisaBan.util.Util.isPunishableIP
import net.azisaba.spicyAzisaBan.util.Util.reconstructIPAddress
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import util.ArgumentParser
import util.UUIDUtil
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.mcutil.common.SimplePlayerProfile
import java.sql.SQLException

@Suppress("UNCHECKED_CAST")
fun <T : Context> ArgumentParser.get(context: Contexts<T>, actor: Actor): Promise<T> {
    if (context == Contexts.PLAYER) return getPlayer(actor) as Promise<T>
    if (context == Contexts.SERVER) return getServer(actor, true) as Promise<T>
    if (context == Contexts.SERVER_NO_PERM_CHECK) return getServer(actor, false) as Promise<T>
    if (context == Contexts.REASON) return getReason() as Promise<T>
    if (context == Contexts.TIME) return getTime(actor) as Promise<T>
    if (context == Contexts.IP_ADDRESS) return getIPAddress(actor) as Promise<T>
    if (context == Contexts.PUNISHMENT_TYPE) return Promise.resolve(getPunishmentType(actor) as T)
    return Promise.reject(IllegalArgumentException("Unknown context: " + context.key))
}

private fun ArgumentParser.getPlayer(actor: Actor): Promise<PlayerContext> = async { context ->
    val rawName = parsedRawOptions["player"] ?: getNonParamStringAt(0)
    if (rawName == null) {
        actor.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
        return@async context.resolve(PlayerContext(false, SimplePlayerProfile("", UUIDUtil.NIL)))
    }
    val profile = PlayerData.getByName(rawName)
        .catch {
            if (it is SQLException) it.printStackTrace()
            actor.send(SABMessages.Commands.General.invalidPlayer.replaceVariables().translate())
        }
        .complete()
        ?: return@async context.resolve(PlayerContext(false, SimplePlayerProfile("", UUIDUtil.NIL)))
    context.resolve(PlayerContext(true, profile))
}

private fun ArgumentParser.getServer(actor: Actor, checkPermission: Boolean): Promise<ServerContext> = async { context ->
    var isGroup = false
    val server = parsedRawOptions["server"] ?: "global"
    if (server == "global") {
        if (checkPermission && !actor.hasPermission("sab.punish.global")) {
            actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
            return@async context.resolve(ServerContext(false, server, false))
        }
    } else {
        if (SpicyAzisaBan.instance.connection.getAllGroups().complete().contains(server)) {
            if (checkPermission && !actor.hasPermission("sab.punish.group.$server")) {
                actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
                return@async context.resolve(ServerContext(false, server, false))
            }
            isGroup = true
        } else {
            if (checkPermission && !actor.hasPermission("sab.punish.server.$server")) {
                actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
                return@async context.resolve(ServerContext(false, server, false))
            }
        }
    }
    return@async context.resolve(ServerContext(true, server, isGroup))
}

private fun ArgumentParser.getReason(): Promise<ReasonContext> = async { context ->
    val reason = parsedRawOptions["reason"] ?: getNonParamStringAt(1)
    return@async context.resolve(ReasonContext(if (reason.isNullOrBlank()) "none" else reason))
}

private fun ArgumentParser.getTime(actor: Actor): Promise<TimeContext> = async { context ->
    val time = parsedRawOptions["time"] ?: getNonParamStringAt(2)
    if (time.isNullOrBlank()) return@async context.resolve(TimeContext(true, -1L))
    try {
        return@async context.resolve(TimeContext(true, Util.processTime(time)))
    } catch (e: IllegalArgumentException) {
        actor.send(SABMessages.Commands.General.invalidTime.replaceVariables().translate())
        return@async context.resolve(TimeContext(false, -1L))
    }
}

private fun ArgumentParser.getIPAddress(actor: Actor): Promise<IPAddressContext> = async { context ->
    val target = parsedRawOptions["target"] ?: getNonParamStringAt(0)
    if (target == null) {
        actor.send(SABMessages.Commands.General.invalidIPAddress.replaceVariables().translate())
        return@async context.resolve(IPAddressContext(false, ""))
    }
    try {
        if (target.isPunishableIP()) return@async context.resolve(
            IPAddressContext(
                true,
                target.reconstructIPAddress()
            )
        )
    } catch (ignored: IllegalArgumentException) {}
    val data = PlayerData.getByName(target)
        .catch { actor.sendErrorMessage(it) }
        .complete()
    if (data?.ip != null) {
        return@async context.resolve(IPAddressContext(true, data.ip))
    }
    actor.send(SABMessages.Commands.General.invalidIPAddress.replaceVariables().translate())
    context.resolve(IPAddressContext(false, ""))
}

private fun ArgumentParser.getPunishmentType(actor: Actor): PunishmentTypeContext {
    val type = parsedRawOptions["type"] ?: return PunishmentTypeContext(true, null)
    return try {
        val pType = PunishmentType.valueOf(type)
        PunishmentTypeContext(true, pType)
    } catch (e: IllegalArgumentException) {
        actor.send(SABMessages.Commands.General.invalidPunishmentType.replaceVariables().translate())
        PunishmentTypeContext(false, null)
    }
}
