package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.WebhookUtil.sendReasonChangedWebhook
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.sql.options.FindOptions

object ChangeReasonCommand: Command() {
    override val name = "${SABConfig.prefix}changereason"
    override val aliases = arrayOf("${SABConfig.prefix}change-reason")
    private val availableArguments = listOf("id=", "reason=\"\"")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.changereason")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.ChangeReason.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        async<Unit> { context ->
            execute(actor, arguments)
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }

    private fun execute(actor: Actor, arguments: ArgumentParser) {
        val id = try {
            arguments.parsedRawOptions["id"]?.toLong() ?: -1
        } catch (e: NumberFormatException) {
            actor.send(SABMessages.Commands.General.notPunished.replaceVariables().translate())
            return
        }
        val reason = arguments.get(Contexts.REASON, actor).complete()
        val p = Punishment.fetchActivePunishmentById(id).complete()
            ?: return actor.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().format(id).translate())
        val findOptions = FindOptions.Builder()
            .addWhere("id", p.id)
            .build()
        SpicyAzisaBan.instance.connection.punishments
            .update("reason", reason.text, findOptions)
            .catch { actor.sendErrorMessage(it) }
            .complete() ?: return
        SpicyAzisaBan.instance.connection.punishmentHistory
            .update("reason", reason.text, findOptions)
            .catch { actor.sendErrorMessage(it) }
            .complete() ?: return
        p.clearCache(sendEvent = true)
        p.sendReasonChangedWebhook(actor, reason.text)
        actor.send(SABMessages.Commands.ChangeReason.done.replaceVariables(p.getVariables().complete()).translate())
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.changereason")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.BAN, args, actor.getServerName())
        return emptyList()
    }
}
