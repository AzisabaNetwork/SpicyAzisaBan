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
import net.azisaba.spicyAzisaBan.util.Util.insert
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.options.InsertOptions

object AddProofCommand: Command("${SABConfig.prefix}addproof"), TabExecutor {
    private val availableArguments = listOf("id=", "text=\"\"")

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.addproof")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return sender.send(SABMessages.Commands.AddProof.usage.replaceVariables().translate())
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
            arguments.getString("id")?.toLong() ?: -1
        } catch (e: NumberFormatException) {
            sender.send(SABMessages.Commands.General.notPunished.replaceVariables().translate())
            return
        }
        val text = arguments.getString("text") ?: return sender.send(SABMessages.Commands.General.noProofSpecified.replaceVariables().translate())
        val p = Punishment.fetchActivePunishmentById(id).complete()
            ?: return sender.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().format(id).translate())
        val proofId = try {
            insert {
                SpicyAzisaBan.instance.connection.proofs.insert(
                    InsertOptions.Builder()
                        .addValue("punish_id", p.id)
                        .addValue("text", text)
                        .build()
                ).complete()
            }
        } catch (e: IllegalStateException) {
            if (e.message == "cancel") return
            throw e
        }
        sender.send(
            SABMessages.Commands.AddProof.done
                .replaceVariables("id" to proofId.toString(), "pid" to id.toString(), "text" to text)
                .replaceVariables(p.getVariables().complete())
                .translate()
        )
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (!sender.hasPermission("sab.addproof")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.BAN, args, sender.getServerName())
        return emptyList()
    }
}
