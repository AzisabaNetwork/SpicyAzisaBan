package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Expiration
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.getServerOrGroupName
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.IPAddressContext
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import net.azisaba.spicyAzisaBan.util.contexts.getServer
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.util.ArgumentParsedResult

object IPBanCommand: Command() {
    override val name = "${SABConfig.prefix}ipban"
    override val permission = PunishmentType.IP_BAN.perm
    override val aliases = arrayOf("${SABConfig.prefix}ban-ip", "${SABConfig.prefix}banip")
    private val availableArguments = listOf("target=", "reason=\"\"", "server=")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission(PunishmentType.IP_BAN.perm)) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.IPBan.usage.replaceVariables().translate())
        val arguments = genericArgumentParser.parse(args.joinToString(" "))
        async<Unit> { context ->
            doIPBan(actor, arguments, actor.getServerOrGroupName(arguments))
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }

    internal fun doIPBan(actor: Actor, arguments: ArgumentParsedResult, serverName: String? = null) {
        val ip = arguments.get(Contexts.IP_ADDRESS, actor).complete().apply { if (!isSuccess) return }.ip
        val server = if (serverName == null) {
            arguments.get(Contexts.SERVER, actor)
        } else {
            arguments.getServer(actor, serverName, true)
        }.complete().apply { if (!isSuccess) return }
        val reason = arguments.get(Contexts.REASON, actor).complete()
        if (Punishment.canJoinServer(null, ip, server.name).complete() != null) {
            actor.send(SABMessages.Commands.General.alreadyPunished.replaceVariables().translate())
            return
        }
        val p = Punishment
            .createByIPAddress(ip, reason.text, actor.uniqueId, PunishmentType.IP_BAN, Expiration.NeverExpire, server.name)
            .insert(actor)
            .catch {
                SpicyAzisaBan.LOGGER.warning("Something went wrong while handling command from ${actor.name}!")
                actor.sendErrorMessage(it)
            }
            .complete() ?: return
        p.notifyToAll().complete()
        actor.send(SABMessages.Commands.IPBan.done.replaceVariables(p.getVariables().complete()).translate())
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission(PunishmentType.IP_BAN.perm)) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("target=")) return IPAddressContext.tabComplete(s)
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.IP_BAN, args, actor.getServerName())
        return emptyList()
    }
}
