package net.azisaba.spicyAzisaBan.velocity.listener

import com.velocitypowered.api.event.EventTask
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.punishment.PunishmentChecker
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.velocity.VelocityPlayerActor
import net.azisaba.spicyAzisaBan.velocity.util.VelocityUtil.toVelocity
import net.kyori.adventure.text.TextComponent
import util.kt.promise.rewrite.catch

object EventListeners {
    @Subscribe
    fun onLogin(e: LoginEvent): EventTask = EventTask.async {
        var denied = false
        PunishmentChecker.checkLockdown(VelocityPlayerActor(e.player)) { reason ->
            e.result = ResultedEvent.ComponentResult.denied(TextComponent.ofChildren(*reason.toVelocity()))
            denied = true
        }
        if (!denied) {
            PlayerData.createOrUpdate(VelocityPlayerActor(e.player)).catch { it.printStackTrace() }
            PunishmentChecker.checkGlobalBan(VelocityPlayerActor(e.player)) { reason ->
                e.result = ResultedEvent.ComponentResult.denied(TextComponent.ofChildren(*reason.toVelocity()))
            }
        }
    }

    @Subscribe
    fun onServerPreConnect(e: ServerPreConnectEvent): EventTask = EventTask.async {
        PunishmentChecker.checkLocalBan(
            ServerInfo(e.originalServer.serverInfo.name, e.originalServer.serverInfo.address),
            VelocityPlayerActor(e.player)
        ) {
            e.result = ServerPreConnectEvent.ServerResult.denied()
        }
    }

    @Subscribe
    fun onPlayerChat(e: PlayerChatEvent): EventTask = EventTask.async {
        PunishmentChecker.checkMute(VelocityPlayerActor(e.player), e.message) {
            e.result = PlayerChatEvent.ChatResult.denied()
        }
    }

    @Subscribe
    fun onCommandExecute(e: CommandExecuteEvent): EventTask = EventTask.async {
        if (e.commandSource !is Player) return@async
        PunishmentChecker.checkMute(VelocityPlayerActor(e.commandSource as Player), "/${e.command}") {
            e.result = CommandExecuteEvent.CommandResult.denied()
        }
    }
}
