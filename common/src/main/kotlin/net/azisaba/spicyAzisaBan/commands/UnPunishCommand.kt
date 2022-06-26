package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.UnPunish
import net.azisaba.spicyAzisaBan.struct.EventType
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.insert
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import org.json.JSONObject
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions
import xyz.acrylicstyle.util.ArgumentParsedResult

object UnPunishCommand: Command() {
    override val name = "${SABConfig.prefix}unpunish"
    override val permission = "sab.unpunish"
    private val availableArguments = listOf("id=", "reason=\"\"")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.unpunish")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.Unpunish.usage.replaceVariables().translate())
        val arguments = genericArgumentParser.parse(args.joinToString(" "))
        async<Unit> { context ->
            doUnPunish(actor, arguments)
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }

    private fun doUnPunish(actor: Actor, arguments: ArgumentParsedResult) {
        val id = try {
            arguments.getArgument("id")?.toLong() ?: -1
        } catch (e: NumberFormatException) {
            actor.send(SABMessages.Commands.General.notPunished.replaceVariables().translate())
            return
        }
        val reason = arguments.get(Contexts.REASON, actor).complete()
        if (reason.text == "none") return actor.send(SABMessages.Commands.General.noReasonSpecified.replaceVariables().translate())
        doUnPunish(actor, id, reason)
    }

    fun doUnPunish(actor: Actor, id: Long, reason: ReasonContext) {
        val p = Punishment.fetchActivePunishmentById(id).complete()
        if (p == null) {
            actor.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().format(id).translate())
            return
        }
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
        SpicyAzisaBan.instance.connection.punishments.delete(FindOptions.Builder().addWhere("id", id).build())
            .catch {
                SpicyAzisaBan.LOGGER.warning("Something went wrong while deleting punishment #${id}")
                actor.sendErrorMessage(it)
            }
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
        SpicyAzisaBan.instance.connection.sendEvent(EventType.REMOVED_PUNISHMENT, JSONObject().put("punish_id", p.id)).complete()
        UnPunish(upid, p, reason.text, time, actor.uniqueId).notifyToAll().complete()
        p.clearCache(sendEvent = true)
        actor.send(SABMessages.Commands.Unpunish.done.replaceVariables(p.getVariables().complete()).translate())
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.unpunish")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        //if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.BAN, args, actor.getServerName())
        return emptyList()
    }
}
