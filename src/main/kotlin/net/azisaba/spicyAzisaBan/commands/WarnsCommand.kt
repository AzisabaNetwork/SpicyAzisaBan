package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.sql.options.FindOptions

object WarnsCommand: Command("${SABConfig.prefix}warns"), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (sender !is ProxiedPlayer) return sender.send("e^1")
        async<Unit> { context ->
            val ps = SpicyAzisaBan.instance.connection.punishments
                .findAll(FindOptions.Builder().addWhere("target", sender.uniqueId.toString()).build())
                .then { list -> list.map { td -> Punishment.fromTableData(td) } }
                .complete()
                .toList()
                .filter { it.type == PunishmentType.WARNING || it.type == PunishmentType.CAUTION }
            if (ps.isEmpty()) {
                sender.send(SABMessages.Commands.Warns.notWarnedYet.replaceVariables().translate())
                return@async context.resolve()
            }
            ps.forEach { p ->
                if (!p.flags.contains(Punishment.Flags.SEEN)) {
                    p.flags.add(Punishment.Flags.SEEN)
                    p.updateFlags().thenDo {
                        SpicyAzisaBan.debug("Updated flags for punishment #${p.id} (${p.flags.joinToString(", ") { it.name }})")
                    }
                }
            }
            ProxyServer.getInstance().createTitle().also { title ->
                title.fadeIn(0)
                title.fadeOut(0)
                title.stay(10)
                title.title(TextComponent(" "))
                title.subTitle(TextComponent(" "))
                title.send(sender)
            }
            val variables = ps.map { it.getVariables().complete() }
            sender.send(SABMessages.Commands.Warns.header.replaceVariables().translate())
            variables.forEach { s ->
                sender.send(SABMessages.Commands.Warns.layout.replaceVariables(s).translate())
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
