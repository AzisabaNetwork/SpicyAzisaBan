package net.azisaba.spicyAzisaBan.util.contexts

import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.isPunishableIP
import net.azisaba.spicyAzisaBan.util.Util.reconstructIPAddress
import net.azisaba.spicyAzisaBan.util.Util.send
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
    if (context == Contexts.SERVER) return getServer(sender) as Promise<T>
    if (context == Contexts.REASON) return getReason() as Promise<T>
    if (context == Contexts.TIME) return getTime(sender) as Promise<T>
    if (context == Contexts.IP_ADDRESS) return getIPAddress(sender) as Promise<T>
    return Promise.reject(IllegalArgumentException("Unknown context: " + context.key))
}

private fun ArgumentParser.getPlayer(sender: CommandSender): Promise<PlayerContext> = Promise.create { context ->
    val rawName = getString("player")
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

private fun ArgumentParser.getServer(sender: CommandSender): Promise<ServerContext> = Promise.create { context ->
    var isGroup = false
    val server = getString("server") ?: "global"
    if (server == "global") {
        if (!sender.hasPermission("sab.punish.global")) {
            sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
            return@create context.resolve(ServerContext(false, server, false))
        }
    } else {
        if (SpicyAzisaBan.instance.connection.getAllGroups().complete().contains(server)) {
            if (!sender.hasPermission("sab.punish.group.$server")) {
                sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
                return@create context.resolve(ServerContext(false, server, false))
            }
            isGroup = true
        } else {
            if (!ProxyServer.getInstance().servers.containsKey(server)) {
                sender.send(SABMessages.Commands.General.invalidServer.replaceVariables().translate())
                return@create context.resolve(ServerContext(false, server, false))
            }
            if (!sender.hasPermission("sab.punish.server.$server")) {
                sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
                return@create context.resolve(ServerContext(false, server, false))
            }
        }
    }
    return@create context.resolve(ServerContext(true, server, isGroup))
}

private fun ArgumentParser.getReason(): Promise<ReasonContext> = Promise.create { context ->
    val reason = getString("reason")
    return@create context.resolve(ReasonContext(if (reason.isNullOrBlank()) "none" else reason))
}

private fun ArgumentParser.getTime(sender: CommandSender): Promise<TimeContext> = Promise.create { context ->
    val time = getString("time")
    if (time.isNullOrBlank()) return@create context.resolve(TimeContext(true, -1L))
    try {
        return@create context.resolve(TimeContext(true, Util.processTime(time)))
    } catch (e: IllegalArgumentException) {
        sender.send(SABMessages.Commands.General.invalidTime.replaceVariables().translate())
        return@create context.resolve(TimeContext(false, -1L))
    }
}

private fun ArgumentParser.getIPAddress(sender: CommandSender): Promise<IPAddressContext> = Promise.create { context ->
    val target = getString("target")
    if (target == null) {
        sender.send(SABMessages.Commands.General.invalidIPAddress.replaceVariables().translate())
        return@create context.resolve(IPAddressContext(false, ""))
    }
    if (target.isPunishableIP()) return@create context.resolve(IPAddressContext(true, target.reconstructIPAddress()))
    val data = PlayerData.getByName(target)
        .catch { if (it is SQLException) it.printStackTrace() }
        .complete()
    if (data?.ip != null) {
        return@create context.resolve(IPAddressContext(true, data.ip))
    }
    sender.send(SABMessages.Commands.General.invalidIPAddress.replaceVariables().translate())
    context.resolve(IPAddressContext(false, ""))
}
