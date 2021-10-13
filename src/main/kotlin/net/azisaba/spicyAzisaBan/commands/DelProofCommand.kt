package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Proof
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.sql.options.FindOptions

object DelProofCommand: Command("${SABConfig.prefix}delproof"), TabExecutor {
    private val availableArguments = listOf("id=")

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.delproof")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return sender.send(SABMessages.Commands.DelProof.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        async<Unit> { context ->
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
        val list = SpicyAzisaBan.instance.connection.proofs.delete(FindOptions.Builder().addWhere("id", id).setLimit(1).build()).complete()
        if (list.isEmpty()) return sender.send(SABMessages.Commands.General.proofNotFound.replaceVariables().format(id).translate())
        val proof = Proof.fromTableData(list[0]).complete()!!
        sender.send(
            SABMessages.Commands.DelProof.done
                .replaceVariables("id" to proof.id.toString(), "pid" to proof.punishment.id.toString(), "text" to proof.text)
                .replaceVariables(proof.punishment.getVariables().complete())
                .translate()
        )
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (!sender.hasPermission("sab.delproof")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        return emptyList()
    }
}
