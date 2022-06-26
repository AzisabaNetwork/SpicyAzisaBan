package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.punishment.UnPunish
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.getServerOrGroupName
import net.azisaba.spicyAzisaBan.util.Util.insert
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
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions
import xyz.acrylicstyle.util.ArgumentParsedResult

object UnBanCommand: Command() {
    override val name = "${SABConfig.prefix}unban"
    override val permission = "sab.unban"
    private val availableArguments = listOf("player=", "reason=\"\"", "server=")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.unban")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.Unpunish.unbanUsage.replaceVariables().translate())
        val arguments = genericArgumentParser.parse(args.joinToString(" "))
        async<Unit> { context ->
            doUnPunish(actor, arguments, actor.getServerOrGroupName(arguments))
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }

    private fun doUnPunish(actor: Actor, arguments: ArgumentParsedResult, serverName: String? = null) {
        val player = arguments.get(Contexts.PLAYER, actor).complete().apply { if (!isSuccess) return }.profile
        val reason = arguments.get(Contexts.REASON, actor).complete()
        if (reason.text == "none") return actor.send(SABMessages.Commands.General.noReasonSpecified.replaceVariables().translate())
        val server = if (serverName == null) {
            arguments.get(Contexts.SERVER, actor)
        } else {
            arguments.getServer(actor, serverName, true)
        }.complete().apply { if (!isSuccess) return }.name
        val p = Punishment.canJoinServer(player.uniqueId, null, server, noLookupGroup = true)
            .catch { actor.sendErrorMessage(it) }
            .complete()
            ?: return actor.send(SABMessages.Commands.General.notPunished.replaceVariables().translate())
        val permission = if (p.server == "global") {
            "sab.punish.global"
        } else if (SpicyAzisaBan.instance.connection.isGroupExists(p.server).complete()) {
            "sab.punish.group.${p.server}"
        } else {
            "sab.punish.server.${p.server}"
        }
        if (!actor.hasPermission(permission)) {
            actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
            return
        }
        SpicyAzisaBan.instance.connection.punishments
            .delete(FindOptions.Builder().addWhere("id", p.id).build())
            .catch { e -> actor.sendErrorMessage(e) }
            .complete() ?: return
        val time = System.currentTimeMillis()
        val upid = try {
            insert {
                SpicyAzisaBan.instance.connection.unpunish
                    .insert(
                        InsertOptions.Builder()
                            .addValue("punish_id", p.id)
                            .addValue("reason", reason.text)
                            .addValue("timestamp", time)
                            .addValue("operator", actor.uniqueId.toString())
                            .build()
                    )
                    .catch {
                        SpicyAzisaBan.LOGGER.warning("Something went wrong while inserting unpunish record")
                        actor.sendErrorMessage(it)
                    }
                    .complete() ?: error("cancel")
            }
        } catch (e: IllegalStateException) {
            if (e.message == "cancel") return
            throw e
        }
        UnPunish(upid, p, reason.text, time, actor.uniqueId).notifyToAll().complete()
        p.clearCache(sendEvent = true)
        actor.send(SABMessages.Commands.Unpunish.done.replaceVariables(p.getVariables().complete()).translate())
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.unban")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("player=")) return PlayerContext.tabComplete(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.BAN, args, actor.getServerName())
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        return emptyList()
    }
}
