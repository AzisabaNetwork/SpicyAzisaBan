package net.azisaba.spicyAzisaBan.punishment

import net.azisaba.spicyAzisaBan.ReloadableSABConfig
import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.PlayerActor
import net.azisaba.spicyAzisaBan.common.PlayerConnection
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.getIPAddress
import net.azisaba.spicyAzisaBan.util.Util.getServerName
import net.azisaba.spicyAzisaBan.util.Util.kick
import net.azisaba.spicyAzisaBan.util.Util.reconstructIPAddress
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.sendDelayed
import net.azisaba.spicyAzisaBan.util.Util.translate
import util.kt.promise.rewrite.catch
import java.util.concurrent.TimeUnit

object PunishmentChecker {
    fun checkLockdown(connection: PlayerConnection, deny: (reason: Array<Component>) -> Unit) {
        if (!SpicyAzisaBan.instance.lockdown) return
        Util.async<Boolean> { context ->
            Thread {
                val start = System.currentTimeMillis()
                SpicyAzisaBan.instance.schedule(800, TimeUnit.MILLISECONDS) { // single SELECT should not take long time
                    context.resolve(false)
                }
                val exists = PlayerData.isExists(connection.uniqueId).complete()
                if (!exists) {
                    deny(Component.fromLegacyText(SABMessages.Commands.Lockdown.lockdown.replaceVariables().translate()))
                    val message = SABMessages.Commands.Lockdown.lockdownJoinAttempt
                        .replaceVariables("player" to (connection.name ?: connection.uniqueId).toString(), "IP_ADDRESS" to connection.getRemoteAddress().getIPAddress().toString()).translate()
                        .translate()
                    SpicyAzisaBan.instance.getPlayers().filter { it.hasPermission("sab.lockdown") }.forEach { it.send(message) }
                    SpicyAzisaBan.instance.getConsoleActor().send(message)
                }
                val time = System.currentTimeMillis() - start
                if (time > 500) {
                    SpicyAzisaBan.LOGGER.warning("PunishmentChecker#checkGlobalBan took $time ms to process!")
                }
                context.resolve(true)
            }.start()
        }.thenDo { res ->
            if (!res) {
                SpicyAzisaBan.LOGGER.warning("Could not check lockdown state for ${connection.uniqueId} (Timed out, > 800 ms)")
                if (SABConfig.database.failsafe) {
                    deny(Component.fromLegacyText(SABMessages.General.error.replaceVariables().translate()))
                }
            }
        }.catch {
            SpicyAzisaBan.LOGGER.warning("Could not check lockdown state for ${connection.uniqueId}")
            it.printStackTrace()
            if (SABConfig.database.failsafe) {
                deny(Component.fromLegacyText(SABMessages.General.errorDetailed.replaceVariables(
                    "EXCEPTION_CLASS_NAME" to it.javaClass.name,
                    "EXCEPTION_MESSAGE" to (it.message ?: "null"),
                ).translate()))
            }
        }.complete()
    }

    fun checkGlobalBan(connection: PlayerConnection, deny: (reason: Array<Component>) -> Unit) {
        Util.async<Boolean> { context ->
            Thread {
                val start = System.currentTimeMillis()
                SpicyAzisaBan.instance.schedule(1500, TimeUnit.MILLISECONDS) {
                    context.resolve(false)
                }
                val p = Punishment.canJoinServer(
                    connection.uniqueId,
                    connection.getRemoteAddress().getIPAddress()?.reconstructIPAddress(),
                    "global"
                ).complete()
                if (p != null) {
                    deny(Component.fromLegacyText(p.getBannedMessage().complete()))
                }
                val time = System.currentTimeMillis() - start
                if (time > 1000) {
                    SpicyAzisaBan.LOGGER.warning("PunishmentChecker#checkGlobalBan took $time ms to process!")
                }
                context.resolve(true)
            }.start()
        }.thenDo { res ->
            if (!res) {
                SpicyAzisaBan.LOGGER.warning("Could not check punishments for ${connection.uniqueId} (Timed out, > 1500 ms)")
                if (SABConfig.database.failsafe) {
                    deny(Component.fromLegacyText(SABMessages.General.error.replaceVariables().translate()))
                }
            }
        }.catch {
            SpicyAzisaBan.LOGGER.warning("Could not check punishments for ${connection.uniqueId}")
            it.printStackTrace()
            if (SABConfig.database.failsafe) {
                deny(Component.fromLegacyText(SABMessages.General.errorDetailed.replaceVariables(
                    "EXCEPTION_CLASS_NAME" to it.javaClass.name,
                    "EXCEPTION_MESSAGE" to (it.message ?: "null"),
                ).translate()))
            }
        }.complete()
    }

    /**
     * Checks whether if the player is allowed to join the target server.
     * - Creates async context and fetch punishments asynchronously
     * - Reject as "timed out" if punishment could not be fetched in 1500 milliseconds
     */
    fun checkLocalBan(target: ServerInfo, player: PlayerActor, cancel: () -> Unit) {
        /**
         * the server before connecting to target server
         */
        val currentServer = player.getServer()

        /**
         * formatted ip address
         */
        val ipAddress = player.getRemoteAddress().getIPAddress()?.reconstructIPAddress()
        val pair = Punishment.canJoinServerCached(player.uniqueId, ipAddress, target.name.lowercase())
        if (pair.first) { // true = cached, false = not cached
            SpicyAzisaBan.debug("Found cached punishment for ${player.name} at ${target.name.lowercase()}")
            val p = pair.second // punishment
            if (p == null) {
                SpicyAzisaBan.debug("(null punishment)", 2)
                return
            }
            SpicyAzisaBan.debug(p.toString(), 2)
            if (p.isExpired()) {
                SpicyAzisaBan.debug("but it is expired, removing it.")
                // if expired, remove the punishment and check for new punishments asynchronously
                p.removeIfExpired()
            } else {
                // if active, cancel the event and kick the player from the server
                SpicyAzisaBan.debug("Kicking ${player.name} from ${player.getServer()?.name} asynchronously (reason: banned from ${p.server}; cached)")
                cancel()
                if (currentServer == null || player.getServer() == null) {
                    player.kick(p.getBannedMessage().complete())
                } else {
                    player.sendDelayed(100, p.getBannedMessage().complete())
                }
                return
            }
        } else {
            SpicyAzisaBan.debug("Punishment is not cached for ${player.name} at ${target.name}")
        }
        Util.async<Boolean> { context ->
            Thread({
                SpicyAzisaBan.instance.schedule(1500, TimeUnit.MILLISECONDS) {
                    context.resolve(false)
                }
                val p = Punishment.canJoinServer(player.uniqueId, ipAddress, target.name.lowercase()).complete()
                if (p != null) {
                    SpicyAzisaBan.debug("Kicking ${player.name} from ${player.getServer()?.name} asynchronously (reason: banned from ${p.server})")
                    SpicyAzisaBan.debug(p.toString(), 2)
                    cancel()
                    if (currentServer == null || player.getServer() == null) {
                        player.kick(p.getBannedMessage().complete())
                    } else if (player.getServer() != currentServer) {
                        player.plsConnect(currentServer, target)
                        player.sendDelayed(2000, p.getBannedMessage().complete())
                    } else {
                        SpicyAzisaBan.debug("CheckBanListener - else branch (server: ${player.getServer()?.name}, server before check: $currentServer)")
                        player.plsConnect(currentServer, target)
                        player.send(p.getBannedMessage().complete())
                    }
                } else {
                    SpicyAzisaBan.debug("${player.name} is not banned on ${target.name}")
                }
                context.resolve(true)
            }, "SpicyAzisaBan - CheckBanListener Thread").start()
        }.thenDo { res ->
            if (!res) {
                SpicyAzisaBan.LOGGER.warning("Could not check punishments for ${player.uniqueId} (Timed out, > 1500 ms)")
                if (SABConfig.database.failsafe) {
                    cancel()
                    player.kick(SABMessages.General.error.replaceVariables().translate())
                }
            }
        }.catch {
            SpicyAzisaBan.LOGGER.warning("Could not check punishments for ${player.uniqueId}")
            it.printStackTrace()
            if (SABConfig.database.failsafe) {
                cancel()
                player.kick(SABMessages.General.errorDetailed.replaceVariables(
                    "EXCEPTION_CLASS_NAME" to it.javaClass.name,
                    "EXCEPTION_MESSAGE" to (it.message ?: "null"),
                ).translate())
            }
        }
    }

    fun checkMute(actor: Actor, message: String, cancel: () -> Unit) {
        if (actor !is PlayerActor) return
        val isCommand = message.startsWith("/")
        if (isCommand &&
            !ReloadableSABConfig.getBlockedCommandsWhenMuted(actor.getServerName()).any { s -> message.matches("^/(.*:)?$s(\$|\\s+.*)".toRegex()) }
        ) {
            return
        }
        val res = Util.async<Boolean> { context ->
            SpicyAzisaBan.instance.schedule(3, TimeUnit.SECONDS) {
                context.resolve(false)
            }
            val p = Punishment.canSpeak(
                actor.uniqueId,
                actor.getRemoteAddress().getIPAddress()?.reconstructIPAddress(),
                actor.getServerName()
            ).complete()
            if (p != null && !p.type.hasExemptPermission(actor).complete()) {
                cancel()
                actor.send(p.getBannedMessage().complete())
            }
            context.resolve(true)
        }.catch {
            SpicyAzisaBan.LOGGER.warning("Could not check punishments for ${actor.uniqueId}")
            it.printStackTrace()
            if (SABConfig.database.failsafe) {
                cancel()
                actor.send(SABMessages.General.errorDetailed.replaceVariables(
                    "EXCEPTION_CLASS_NAME" to it.javaClass.name,
                    "EXCEPTION_MESSAGE" to (it.message ?: "null"),
                ).translate())
            }
        }.complete()
        if (!res) {
            SpicyAzisaBan.LOGGER.warning("Could not check punishments for ${actor.uniqueId} (Timed out)")
            if (SABConfig.database.failsafe) {
                cancel()
                actor.send(SABMessages.General.error.replaceVariables().translate())
            }
        }
    }

    private fun PlayerActor.plsConnect(server: ServerInfo, tryAgainIf: ServerInfo? = null) {
        connect(server)
        if (tryAgainIf != null) {
            SpicyAzisaBan.instance.schedule(1, TimeUnit.SECONDS) {
                if (this.getServer() == tryAgainIf) {
                    connect(server)
                }
            }
        }
    }
}
