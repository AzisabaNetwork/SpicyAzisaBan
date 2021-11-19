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
import net.azisaba.spicyAzisaBan.util.Util.insert
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.sql.options.InsertOptions

object AddProofCommand: Command() {
    override val name = "${SABConfig.prefix}addproof"

    private val availableArguments = listOf("id=", "text=\"\"")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.addproof")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.AddProof.usage.replaceVariables().translate())
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
        val text = arguments.getString("text") ?: return actor.send(SABMessages.Commands.General.noProofSpecified.replaceVariables().translate())
        val p = Punishment.fetchActivePunishmentById(id).complete()
            ?: return actor.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().format(id).translate())
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
        actor.send(
            SABMessages.Commands.AddProof.done
                .replaceVariables("id" to proofId.toString(), "pid" to id.toString(), "text" to text)
                .replaceVariables(p.getVariables().complete())
                .translate()
        )
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.addproof")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.BAN, args, actor.getServerName())
        return emptyList()
    }
}
