package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.ReloadableSABConfig
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
import net.azisaba.spicyAzisaBan.util.contexts.PlayerContext
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import net.azisaba.spicyAzisaBan.util.contexts.getServer
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.util.ArgumentParsedResult

object WarningCommand: Command() {
    override val name = "${SABConfig.prefix}warning"
    override val permission = PunishmentType.WARNING.perm
    override val aliases = arrayOf("${SABConfig.prefix}warn")

    private val availableArguments = listOf("player=", "reason=\"\"", "server=")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission(PunishmentType.WARNING.perm)) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.Warning.usage.replaceVariables().translate())
        val arguments = genericArgumentParser.parse(args.joinToString(" "))
        async<Unit> { context ->
            doWarning(actor, arguments, actor.getServerOrGroupName(arguments))
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }

    internal fun doWarning(actor: Actor, arguments: ArgumentParsedResult, serverName: String? = null) {
        val player = arguments.get(Contexts.PLAYER, actor).complete().apply { if (!isSuccess) return }
        val server = if (serverName == null) {
            arguments.get(Contexts.SERVER, actor)
        } else {
            arguments.getServer(actor, serverName, true)
        }.complete().apply { if (!isSuccess) return }
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
                val parsed = genericArgumentParser.parse("time=${ReloadableSABConfig.BanOnWarning.time}")
                val reasonContext =
                    ReasonContext(ReloadableSABConfig.BanOnWarning.reason
                        .replaceVariables(
                            "count" to count.toString(),
                            "original_reason" to reason.text,
                            "time" to ReloadableSABConfig.BanOnWarning.time,
                        )
                        .translate())
                val timeContext = parsed.get(Contexts.TIME, actor).complete().apply {
                    if (!isSuccess) {
                        SpicyAzisaBan.LOGGER.severe("Failed to parse time: ${ReloadableSABConfig.BanOnWarning.time}")
                        return
                    }
                }
                TempBanCommand.doTempBan(SpicyAzisaBan.instance.getConsoleActor(), player, server, reasonContext, timeContext, all = false, force = false)
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
