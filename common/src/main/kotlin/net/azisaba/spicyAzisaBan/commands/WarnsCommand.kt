package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import util.kt.promise.rewrite.catch
import xyz.acrylicstyle.sql.options.FindOptions

object WarnsCommand: Command() {
    override val name = "${SABConfig.prefix}warns"

    override fun execute(actor: Actor, args: Array<String>) {
        if (actor !is PlayerActor) return actor.send("e^1")
        async<Unit> { context ->
            val ps = SpicyAzisaBan.instance.connection.punishments
                .findAll(FindOptions.Builder().addWhere("target", actor.uniqueId.toString()).build())
                .then { list -> list.map { td -> Punishment.fromTableData(td) } }
                .complete()
                .toList()
                .filter { it.type == PunishmentType.WARNING || it.type == PunishmentType.CAUTION }
            if (ps.isEmpty()) {
                actor.send(SABMessages.Commands.Warns.notWarnedYet.replaceVariables().translate())
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
            actor.clearTitle()
            val variables = ps.map { it.getVariables().complete() }
            actor.send(SABMessages.Commands.Warns.header.replaceVariables().translate())
            variables.forEach { s ->
                actor.send(SABMessages.Commands.Warns.layout.replaceVariables(s).translate())
            }
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }
}