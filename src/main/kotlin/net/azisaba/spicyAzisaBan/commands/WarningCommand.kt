package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.PunishmentType
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.util.Util.filterArgKeys
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.getUniqueId
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendErrorMessage
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.PlayerContext
import net.azisaba.spicyAzisaBan.util.contexts.ReasonContext
import net.azisaba.spicyAzisaBan.util.contexts.ServerContext
import net.azisaba.spicyAzisaBan.util.contexts.get
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.ArgumentParser
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise

object WarningCommand: Command("${SABConfig.prefix}warning", null, "warn"), TabExecutor {
    private val availableArguments = listOf("player=", "reason=\"\"", "server=")

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission(PunishmentType.WARNING.perm)) {
            return sender.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty()) return sender.send(SABMessages.Commands.Warning.usage.replaceVariables().translate())
        val arguments = ArgumentParser(args.joinToString(" "))
        Promise.create<Unit> { context ->
            if (!arguments.containsKey("server") && sender is ProxiedPlayer) {
                val serverName = sender.server.info.name
                val group = SpicyAzisaBan.instance.connection.getGroupByServer(serverName).complete()
                arguments.parsedOptions["server"] = group ?: serverName
            }
            doWarning(sender, arguments)
            context.resolve()
        }.catch {
            sender.sendErrorMessage(it)
        }
    }

    internal fun doWarning(sender: CommandSender, arguments: ArgumentParser) {
        val player = arguments.get(Contexts.PLAYER, sender).complete().apply { if (!isSuccess) return }
        val server = arguments.get(Contexts.SERVER, sender).complete().apply { if (!isSuccess) return }
        val reason = arguments.get(Contexts.REASON, sender).complete()
        val p = Punishment
            .createByPlayer(player.profile, reason.text, sender.getUniqueId(), PunishmentType.WARNING, -1, server.name)
            .insert()
            .catch {
                SpicyAzisaBan.instance.logger.warning("Something went wrong while handling command from ${sender.name}!")
                sender.sendErrorMessage(it)
            }
            .complete() ?: return
        p.getBannedMessage().thenDo { msg ->
            ProxyServer.getInstance().getPlayer(player.profile.uniqueId)?.send(msg)
        }
        p.notifyToAll().complete()
        p.sendTitle()
        if (SABConfig.BanOnWarning.threshold > 0) {
            val sql = "SELECT COUNT(*) FROM `punishments` WHERE `target` = ? AND `server` = ?"
            SQLConnection.logSql(sql)
            val st = SpicyAzisaBan.instance.connection.connection.prepareStatement(sql)
            st.setString(1, player.profile.uniqueId.toString())
            st.setString(2, server.name)
            val rs = st.executeQuery()
            rs.next()
            val count = rs.getInt(1)
            st.close()
            if (count >= SABConfig.BanOnWarning.threshold) {
                ProxyServer.getInstance().pluginManager.dispatchCommand(
                    ProxyServer.getInstance().console,
                    "${SABConfig.prefix}gtempban player=${player.profile.name} reason=\"${SABConfig.BanOnWarning.reason.replaceVariables("count" to count.toString()).translate()}\" server=${server.name} time=${SABConfig.BanOnWarning.time}",
                )
            }
        }
        sender.send(SABMessages.Commands.Warning.done.replaceVariables(p.getVariables().complete()).translate())
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.isEmpty()) return emptyList()
        val s = args.last()
        if (!s.contains("=")) return availableArguments.filterArgKeys(args).filtr(s)
        if (s.startsWith("player=")) return PlayerContext.tabComplete(s)
        if (s.startsWith("server=")) return ServerContext.tabComplete(s)
        if (s.startsWith("reason=")) return ReasonContext.tabComplete(PunishmentType.WARNING, args, sender.getServerName())
        return emptyList()
    }
}
