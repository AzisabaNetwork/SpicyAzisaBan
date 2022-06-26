package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Proof
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.insert
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.toMinecraft
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.WebhookUtil.sendWebhook
import net.azisaba.spicyAzisaBan.util.contexts.getFlag
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.sql.options.InsertOptions
import xyz.acrylicstyle.util.ArgumentParsedResult
import java.awt.Color

object AddProofCommand: Command() {
    override val name = "${SABConfig.prefix}addproof"
    override val permission = "sab.addproof"

    private val availableArguments = listOf("id=", "text=\"\"", "public")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.addproof")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.AddProof.usage.replaceVariables().translate())
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
        val text = arguments.getArgument("text")
        if (text.isNullOrBlank()) {
            return actor.send(SABMessages.Commands.General.noProofSpecified.replaceVariables().translate())
        }
        execute(actor, id, text, arguments.getFlag("public"))
    }

    fun execute(actor: Actor, id: Long, text: String, public: Boolean) {
        val p = Punishment.fetchActivePunishmentById(id).complete()
            ?: return actor.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().format(id).translate())
        val proofId = try {
            insert {
                SpicyAzisaBan.instance.connection.proofs.insert(
                    InsertOptions.Builder()
                        .addValue("punish_id", p.id)
                        .addValue("text", text)
                        .addValue("public", public)
                        .build()
                ).complete()
            }
        } catch (e: IllegalStateException) {
            if (e.message == "cancel") return
            throw e
        }
        Proof(proofId, p, text, public).sendWebhook(actor, "証拠が追加されました。", Color.GREEN)
        actor.send(
            SABMessages.Commands.AddProof.done
                .replaceVariables(
                    "id" to proofId.toString(),
                    "pid" to id.toString(),
                    "text" to text,
                    "public" to public.toMinecraft(),
                )
                .replaceVariables(p.getVariables().complete())
                .translate()
        )
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission("sab.addproof")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("public=")) return listOf("public=true", "public=false").filtr(args.last())
        return emptyList()
    }
}
