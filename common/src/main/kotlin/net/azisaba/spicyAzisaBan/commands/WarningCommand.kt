package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.ReloadableSABConfig
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
import net.azisaba.spicyAzisaBan.util.contexts.PlayerContext
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import util.ArgumentParser
import util.kt.promise.rewrite.catch

object WarningCommand: Command() {
    override val name = "${SABConfig.prefix}warning"
    override val aliases = arrayOf("${SABConfig.prefix}warn")

    private val availableArguments = listOf("player=", "reason=\"\"", "server=")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission(PunishmentType.WARNING.perm)) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.Warning.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        async<Unit> { context ->
            if (!arguments.parsedRawOptions.containsKey("server") && actor is PlayerActor) {
                val serverName = actor.getServerName()
                val group = SpicyAzisaBan.instance.connection.getGroupByServer(serverName).complete()
                arguments.parsedRawOptions["server"] = group ?: serverName
            }
            doWarning(actor, arguments)
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }

    internal fun doWarning(actor: Actor, arguments: ArgumentParser) {
        val player = arguments.get(Contexts.PLAYER, actor).complete().apply { if (!isSuccess) return }
        val server = arguments.get(Contexts.SERVER, actor).complete().apply { if (!isSuccess) return }
        val reason = arguments.get(Contexts.REASON, actor).complete()
        doWarning(actor, player, server, reason)
    }

    fun doWarning(actor: Actor, player: PlayerContext, server: ServerContext, reason: ReasonContext) {
        val p = Punishment
            .createByPlayer(player.profile, reason.text, actor.uniqueId, PunishmentType.WARNING, Expiration.NeverExpire, server.name)
            .insert(actor)
            .catch {
                SpicyAzisaBan.LOGGER.warning("Something went wrong while handling command from ${actor.name}!")
                actor.sendErrorMessage(it)
            }
            .complete() ?: return
        p.notifyToAll().complete()
        if (ReloadableSABConfig.BanOnWarning.threshold > 0) {
            val rs = SpicyAzisaBan.instance.connection.executeQuery(
                "SELECT COUNT(*) FROM `punishments` WHERE `target` = ? AND `server` = ? AND `type` = ?",
                player.profile.uniqueId.toString(),
                server.name,
                PunishmentType.WARNING.name,
            )
            rs.next()
            val count = rs.getInt(1)
            rs.statement.close()
            if (count >= ReloadableSABConfig.BanOnWarning.threshold) {
                TempBanCommand.doTempBan(
                    SpicyAzisaBan.instance.getConsoleActor(),
                    ArgumentParser(
                        "player=${player.profile.name} reason=\"${
                            ReloadableSABConfig.BanOnWarning.reason.replaceVariables(
                                "count" to count.toString()
                            ).translate()
                        }\" server=${server.name} time=${ReloadableSABConfig.BanOnWarning.time}"
                    )
                )
            }
        }
        actor.send(SABMessages.Commands.Warning.done.replaceVariables(p.getVariables().complete()).translate())
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission(PunishmentType.WARNING.perm)) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("player=")) return PlayerContext.tabComplete(s)
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.WARNING, args, actor.getServerName())
        return emptyList()
    }
}