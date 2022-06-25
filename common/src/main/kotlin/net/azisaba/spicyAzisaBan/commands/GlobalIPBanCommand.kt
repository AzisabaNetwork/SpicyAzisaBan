package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.IPAddressContext
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import util.ArgumentParser
import util.kt.promise.rewrite.catch

object GlobalIPBanCommand: Command() {
    override val name = "${SABConfig.prefix}gipban"
    override val permission = PunishmentType.IP_BAN.perm
    private val availableArguments = listOf("target=", "reason=\"\"", "server=")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission(PunishmentType.IP_BAN.perm)) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.IPBan.globalUsage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        async<Unit> { context ->
            IPBanCommand.doIPBan(actor, arguments)
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission(PunishmentType.IP_BAN.perm)) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("target=")) return IPAddressContext.tabComplete(s)
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.IP_BAN, args, "global")
        return emptyList()
    }
}
