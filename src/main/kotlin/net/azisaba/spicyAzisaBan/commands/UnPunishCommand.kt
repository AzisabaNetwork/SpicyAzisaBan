package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.punishment.UnPunish
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.getUniqueId
import net.azisaba.spicyAzisaBan.util.Util.insert
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions

object UnPunishCommand: Command("${SABConfig.prefix}unpunish"), TabExecutor {
    private val availableArguments = listOf("id=", "reason=\"\"")

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("sab.unpunish")) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return sender.send(SABMessages.Commands.Unpunish.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        Promise.create<Unit> { context ->
            if (!arguments.containsKey("server") && sender is ProxiedPlayer) {
                val serverName = sender.server.info.name
                val group = SpicyAzisaBan.instance.connection.getGroupByServer(serverName).complete()
                arguments.parsedOptions["server"] = group ?: serverName
            }
            doUnPunish(sender, arguments)
            context.resolve()
        }.catch {
            sender.sendErrorMessage(it)
        }
    }

    private fun doUnPunish(sender: CommandSender, arguments: ArgumentParser) {
        val id = try {
            arguments.getString("id")?.toLong() ?: -1
        } catch (e: NumberFormatException) {
            sender.send(SABMessages.Commands.General.notPunished.replaceVariables().translate())
            return
        }
        val reason = arguments.get(Contexts.REASON, sender).complete()
        if (reason.text == "none") return sender.send(SABMessages.Commands.General.noReasonSpecified.replaceVariables().translate())
        val list = SpicyAzisaBan.instance.connection.punishments.delete(FindOptions.Builder().addWhere("id", id).build())
            .catch {
                SpicyAzisaBan.instance.logger.warning("Something went wrong while deleting punishment #${id}")
                sender.sendErrorMessage(it)
            }
            .complete() ?: return
        if (list.isEmpty()) {
            sender.send(SABMessages.Commands.General.punishmentNotFound.replaceVariables().format(id).translate())
            return
        }
        val p = Punishment.fromTableData(list[0])
        val permission = if (p.server == "global") {
            "sab.punish.global"
        } else if (SpicyAzisaBan.instance.connection.isGroupExists(p.server).complete()) {
            "sab.punish.group.${p.server}"
        } else {
            "sab.punish.server.${p.server}"
        }
        if (!sender.hasPermission(permission)) {
            sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
            return
        }
        val time = System.currentTimeMillis()
        val upid = try {
            insert {
                SpicyAzisaBan.instance.connection.unpunish
                    .insert(
                        InsertOptions.Builder()
                            .addValue("punish_id", p.id)
                            .addValue("reason", reason.text)
                            .addValue("timestamp", time)
                            .addValue("operator", sender.getUniqueId().toString())
                            .build()
                    )
                    .catch {
                        SpicyAzisaBan.instance.logger.warning("Something went wrong while inserting unpunish record")
                        sender.sendErrorMessage(it)
                    }
                    .complete() ?: error("cancel")
            }
        } catch (e: IllegalStateException) {
            if (e.message == "cancel") return
            throw e
        }
        UnPunish(upid, p, reason.text, time, sender.getUniqueId()).notifyToAll().complete()
        if (p.type.isMute()) {
            Punishment.muteCache.toList().forEach { (s) ->
                if (s.contains(p.target)) Punishment.muteCache.remove(s)
            }
        }
        sender.send(SABMessages.Commands.Unpunish.done.replaceVariables(p.getVariables().complete()).translate())
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (!sender.hasPermission("sab.unpunish")) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.BAN, args, sender.getServerName())
        return emptyList()
    }
}
