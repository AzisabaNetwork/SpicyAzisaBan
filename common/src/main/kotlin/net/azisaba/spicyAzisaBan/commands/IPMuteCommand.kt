package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Expiration
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.IPAddressContext
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import util.ArgumentParser
import util.kt.promise.rewrite.catch

object IPMuteCommand: Command() {
    override val name = "${SABConfig.prefix}ipmute"
    override val permission = PunishmentType.IP_MUTE.perm
    private val availableArguments = listOf("target=", "reason=\"\"", "server=")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission(PunishmentType.IP_MUTE.perm)) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.IPMute.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        async<Unit> { context ->
            if (!arguments.parsedRawOptions.containsKey("server") && actor is PlayerActor) {
                val serverName = actor.getServerName()
                val group = SpicyAzisaBan.instance.connection.getGroupByServer(serverName).complete()
                arguments.parsedRawOptions["server"] = group ?: serverName
            }
            doIPMute(actor, arguments)
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }

    internal fun doIPMute(actor: Actor, arguments: ArgumentParser) {
        val ip = arguments.get(Contexts.IP_ADDRESS, actor).complete().apply { if (!isSuccess) return }.ip
        val server = arguments.get(Contexts.SERVER, actor).complete().apply { if (!isSuccess) return }
        val reason = arguments.get(Contexts.REASON, actor).complete()
        if (Punishment.canSpeak(null, ip, server.name).complete() != null) {
            actor.send(SABMessages.Commands.General.alreadyPunished.replaceVariables().translate())
            return
        }
        val p = Punishment
            .createByIPAddress(ip, reason.text, actor.uniqueId, PunishmentType.IP_MUTE, Expiration.NeverExpire, server.name)
            .insert(actor)
            .catch {
                SpicyAzisaBan.LOGGER.warning("Something went wrong while handling command from ${actor.name}!")
                actor.sendErrorMessage(it)
            }
            .complete() ?: return
        p.notifyToAll().complete()
        actor.send(SABMessages.Commands.IPMute.done.replaceVariables(p.getVariables().complete()).translate())
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission(PunishmentType.IP_MUTE.perm)) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("target=")) return IPAddressContext.tabComplete(s)
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.IP_MUTE, args, actor.getServerName())
        return emptyList()
    }
}
