package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.punishment.Expiration
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.PlayerContext
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import net.azisaba.spicyAzisaBan.util.contexts.TimeContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import util.ArgumentParser
import util.kt.promise.rewrite.catch

object TempMuteCommand: Command() {
    override val name = "${SABConfig.prefix}tempmute"
    override val permission = PunishmentType.TEMP_MUTE.perm
    private val availableArguments = listOf("player=", "reason=\"\"", "time=", "server=", "--all")

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission(PunishmentType.TEMP_MUTE.perm)) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return actor.send(SABMessages.Commands.TempMute.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        async<Unit> { context ->
            if (!arguments.parsedRawOptions.containsKey("server") && actor is PlayerActor) {
                val serverName = actor.getServerName()
                val group = SpicyAzisaBan.instance.connection.getGroupByServer(serverName).complete()
                arguments.parsedRawOptions["server"] = group ?: serverName
            }
            doTempMute(actor, arguments)
            context.resolve()
        }.catch {
            actor.sendErrorMessage(it)
        }
    }
    
    internal fun doTempMute(actor: Actor, arguments: ArgumentParser) {
        val player = arguments.get(Contexts.PLAYER, actor).complete().apply { if (!isSuccess) return }
        val server = arguments.get(Contexts.SERVER, actor).complete().apply { if (!isSuccess) return }
        val reason = arguments.get(Contexts.REASON, actor).complete()
        val time = arguments.get(Contexts.TIME, actor).complete().apply { if (!isSuccess) return }
        doTempMute(actor, player, server, reason, time, arguments.contains("all"))
    }

    fun doTempMute(
        actor: Actor,
        player: PlayerContext,
        server: ServerContext,
        reason: ReasonContext,
        time: TimeContext,
        all: Boolean,
    ) {
        if (time.time == -1L) {
            actor.send(SABMessages.Commands.General.timeNotSpecified.replaceVariables().translate())
            return
        }
        if (Punishment.canSpeak(player.profile.uniqueId, null, server.name).complete() != null) {
            actor.send(SABMessages.Commands.General.alreadyPunished.replaceVariables().translate())
            return
        }
        val p = Punishment
            .createByPlayer(
                player.profile,
                reason.text,
                actor.uniqueId,
                PunishmentType.TEMP_MUTE,
                Expiration.ExpireAt.of(System.currentTimeMillis() + time.time),
                server.name
            )
            .insert(actor)
            .catch {
                SpicyAzisaBan.LOGGER.warning("Something went wrong while handling command from ${actor.name}!")
                actor.sendErrorMessage(it)
            }
            .complete() ?: return
        p.notifyToAll().complete()
        if (all) {
            p.applyToSameIPs(player.profile.uniqueId).catch { actor.sendErrorMessage(it) }.complete()
        }
        actor.send(SABMessages.Commands.TempMute.done.replaceVariables(p.getVariables().complete()).translate())
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (!actor.hasPermission(PunishmentType.TEMP_MUTE.perm)) return emptyList()
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("player=")) return PlayerContext.tabComplete(s)
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.TEMP_MUTE, args, actor.getServerName())
        if (s.startsWith("time=")) return TimeContext.tabComplete(s)
        return emptyList()
    }
}
