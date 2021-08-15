package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise

object ProofsCommand: Command("${SABConfig.prefix}proofs"), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.proofs")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return sender.send(SABMessages.Commands.Proofs.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        val id = try {
            arguments.getString("id")?.toLong() ?: -1
        } catch (e: NumberFormatException) {
            sender.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().translate())
            return
        }
        Promise.create<Unit> { context ->
            val p = Punishment.fetchPunishmentById(id).complete()
            if (p == null) {
                sender.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().format(id).translate())
                return@create context.resolve()
            }
            val proofs = p.getProofs().complete()
            sender.send(
                SABMessages.Commands.Proofs.header
                    .replaceVariables("pid" to p.id.toString())
                    .replaceVariables(p.getVariables().complete())
                    .translate()
            )
            proofs.forEach { proof ->
                sender.send(
                    SABMessages.Commands.Proofs.layout
                        .replaceVariables(
                            "id" to proof.id.toString(),
                            "text" to proof.text,
                        )
                        .translate()
                )
            }
            context.resolve()
        }.catch {
            sender.sendErrorMessage(it)
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        return emptyList()
    }
}
