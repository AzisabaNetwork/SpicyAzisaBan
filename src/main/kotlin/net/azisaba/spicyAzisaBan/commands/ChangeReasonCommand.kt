package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.options.FindOptions

object ChangeReasonCommand: Command("${SABConfig.prefix}changereason", null, "${SABConfig.prefix}change-reason"), TabExecutor {
    private val availableArguments = listOf("id=", "reason=\"\"")

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.changereason")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return sender.send(SABMessages.Commands.ChangeReason.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        Promise.create<Unit> { context ->
            execute(sender, arguments)
            context.resolve()
        }.catch {
            sender.sendErrorMessage(it)
        }
    }

    private fun execute(sender: CommandSender, arguments: ArgumentParser) {
        val id = try {
            arguments.parsedRawOptions["id"]?.toLong() ?: -1
        } catch (e: NumberFormatException) {
            sender.send(SABMessages.Commands.General.notPunished.replaceVariables().translate())
            return
        }
        val reason = arguments.get(Contexts.REASON, sender).complete()
        val p = Punishment.fetchActivePunishmentById(id).complete()
            ?: return sender.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().format(id).translate())
        val findOptions = FindOptions.Builder()
            .addWhere("id", p.id)
            .build()
        SpicyAzisaBan.instance.connection.punishments
            .update("reason", reason.text, findOptions)
            .catch { sender.sendErrorMessage(it) }
            .complete() ?: return
        SpicyAzisaBan.instance.connection.punishmentHistory
            .update("reason", reason.text, findOptions)
            .catch { sender.sendErrorMessage(it) }
            .complete() ?: return
        p.clearCache()
        sender.send(SABMessages.Commands.ChangeReason.done.replaceVariables(p.getVariables().complete()).translate())
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (!sender.hasPermission("sab.changereason")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.BAN, args, sender.getServerName())
        return emptyList()
    }
}
