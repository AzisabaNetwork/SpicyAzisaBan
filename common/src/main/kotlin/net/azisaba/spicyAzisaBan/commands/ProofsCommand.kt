package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import util.kt.promise.rewrite.catch

object ProofsCommand: Command() {
    override val name = "${SABConfig.prefix}proofs"
    override val permission = "sab.proofs"

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.proofs")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.Proofs.usage.replaceVariables().translate())
        val arguments = genericArgumentParser.parse(args.joinToString(" "))
        val id = try {
            arguments.getArgument("id")?.toLong() ?: arguments.unhandledArguments().getOrNull(0)?.toLong() ?: -1
        } catch (e: NumberFormatException) {
            actor.send(SABMessages.Commands.General.invalidNumber.replaceVariables().translate())
            return
        }
        async<Unit> { context ->
            val p = Punishment.fetchPunishmentById(id).complete()
            if (p == null) {
                actor.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().format(id).translate())
                return@async context.resolve()
            }
            val proofs = p.getProofs().complete()
            actor.send(
                SABMessages.Commands.Proofs.header
                    .replaceVariables("pid" to p.id.toString())
                    .replaceVariables(p.getVariables().complete())
                    .translate()
            )
            proofs.forEach { proof ->
                actor.send(SABMessages.Commands.Proofs.layout.replaceVariables(proof.variables).translate())
            }
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }
}
