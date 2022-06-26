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
import net.azisaba.spicyAzisaBan.util.Util.toMinecraft
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.WebhookUtil.sendWebhook
import net.azisaba.spicyAzisaBan.util.contexts.getFlag
import net.azisaba.spicyAzisaBan.util.contexts.isFlagNotSpecified
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.UpsertOptions
import xyz.acrylicstyle.util.ArgumentParsedResult
import java.awt.Color

object UpdateProofCommand: Command() {
    override val name = "${SABConfig.prefix}updateproof"
    override val permission = "sab.updateproof"

    private val availableArguments = listOf("id=", "text=\"\"", "public=")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.updateproof")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.UpdateProof.usage.replaceVariables().translate())
        val arguments = genericArgumentParser.parse(args.joinToString(" "))
        async<Unit> { context ->
            execute(actor, arguments)
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }

    private fun execute(actor: Actor, arguments: ArgumentParsedResult) {
        val id = try {
            arguments.getArgument("id")?.toLong() ?: -1
        } catch (e: NumberFormatException) {
            actor.send(SABMessages.Commands.General.invalidNumber.replaceVariables().translate())
            return
        }
        val text: String? = arguments.getArgument("text")
        execute(actor, id, text, if (arguments.isFlagNotSpecified("public")) null else arguments.getFlag("public"))
    }

    fun execute(actor: Actor, id: Long, text: String?, public: Boolean?) {
        if (text.isNullOrBlank() && public == null) {
            return actor.send(SABMessages.Commands.UpdateProof.usage.replaceVariables().translate())
        }
        val proof = SpicyAzisaBan.instance.connection.proofs.findOne(FindOptions.Builder().addWhere("id", id).build())
            .then { if (it != null) Proof.fromTableData(it).complete() else null }
            .complete()
            ?: return actor.send(SABMessages.Commands.General.proofNotFound.replaceVariables().format(id).translate())
        val builder = UpsertOptions.Builder().addWhere("id", id)
        if (text != null) builder.addValue("text", text)
        if (public != null) builder.addValue("public", public)
        SpicyAzisaBan.instance.connection.proofs.update(builder.build()).complete()
        Proof(proof.id, proof.punishment, text ?: proof.text, public ?: proof.public)
            .sendWebhook(actor, "証拠が更新されました。", Color.YELLOW)
        actor.send(
            SABMessages.Commands.UpdateProof.done
                .replaceVariables(
                    "id" to proof.id.toString(),
                    "pid" to proof.punishment.id.toString(),
                    "text" to (text ?: proof.text),
                    "public" to (public ?: proof.public).toMinecraft(),
                )
                .translate()
        )
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.updateproof")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("public=")) return listOf("public=true", "public=false").filtr(args.last())
        return emptyList()
    }
}
