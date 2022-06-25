package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Proof
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.WebhookUtil.sendWebhook
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.sql.options.FindOptions
import java.awt.Color

object DelProofCommand: Command() {
    override val name = "${SABConfig.prefix}delproof"
    override val permission = "sab.delproof"
    private val availableArguments = listOf("id=")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.delproof")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.DelProof.usage.replaceVariables().translate())
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
            actor.send(SABMessages.Commands.General.invalidNumber.replaceVariables().translate())
            return
        }
        val list = SpicyAzisaBan.instance.connection.proofs.delete(FindOptions.Builder().addWhere("id", id).setLimit(1).build()).complete()
        if (list.isEmpty()) return actor.send(SABMessages.Commands.General.proofNotFound.replaceVariables().format(id).translate())
        val proof = Proof.fromTableData(list[0]).complete()!!
        proof.sendWebhook(actor, "証拠が削除されました。", Color.RED)
        actor.send(
            SABMessages.Commands.DelProof.done
                .replaceVariables("id" to proof.id.toString(), "pid" to proof.punishment.id.toString(), "text" to proof.text)
                .replaceVariables(proof.punishment.getVariables().complete())
                .translate()
        )
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.delproof")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        return emptyList()
    }
}
