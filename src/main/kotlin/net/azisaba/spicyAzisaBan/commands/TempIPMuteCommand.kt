package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.getUniqueId
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.IPAddressContext
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import net.azisaba.spicyAzisaBan.util.contexts.TimeContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.ArgumentParser
import util.kt.promise.rewrite.catch

object TempIPMuteCommand: Command("${SABConfig.prefix}tempipmute"), TabExecutor {
    private val availableArguments = listOf("target=", "reason=\"\"", "time=", "server=")

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission(PunishmentType.TEMP_IP_MUTE.perm)) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return sender.send(SABMessages.Commands.TempIPMute.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        async<Unit> { context ->
            if (!arguments.parsedRawOptions.containsKey("server") && sender is ProxiedPlayer) {
                val serverName = sender.server.info.name
                val group = SpicyAzisaBan.instance.connection.getGroupByServer(serverName).complete()
                arguments.parsedRawOptions["server"] = group ?: serverName
            }
            doTempIPMute(sender, arguments)
            context.resolve()
        }.catch {
            sender.sendErrorMessage(it)
        }
    }

    internal fun doTempIPMute(sender: CommandSender, arguments: ArgumentParser) {
        val ip = arguments.get(Contexts.IP_ADDRESS, sender).complete().apply { if (!isSuccess) return }.ip
        val server = arguments.get(Contexts.SERVER, sender).complete().apply { if (!isSuccess) return }
        val reason = arguments.get(Contexts.REASON, sender).complete()
        val time = arguments.get(Contexts.TIME, sender).complete().apply { if (!isSuccess) return }.time
        if (time == -1L) {
            sender.send(SABMessages.Commands.General.timeNotSpecified.replaceVariables().translate())
            return
        }
        if (Punishment.canSpeak(null, ip, server.name).complete() != null) {
            sender.send(SABMessages.Commands.General.alreadyPunished.replaceVariables().translate())
            return
        }
        val p = Punishment
            .createByIPAddress(ip, reason.text, sender.getUniqueId(), PunishmentType.TEMP_IP_MUTE, System.currentTimeMillis() + time, server.name)
            .insert(sender)
            .catch {
                SpicyAzisaBan.instance.logger.warning("Something went wrong while handling command from ${sender.name}!")
                sender.sendErrorMessage(it)
            }
            .complete() ?: return
        p.notifyToAll().complete()
        sender.send(SABMessages.Commands.TempIPMute.done.replaceVariables(p.getVariables().complete()).translate())
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (!sender.hasPermission(PunishmentType.TEMP_IP_MUTE.perm)) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("target=")) return IPAddressContext.tabComplete(s)
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.TEMP_IP_MUTE, args, sender.getServerName())
        if (s.startsWith("time=")) return TimeContext.tabComplete(s)
        return emptyList()
    }
}
