package net.azisaba.spicyAzisaBan.commands

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SABMessages.replaceVariables
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.struct.EventType
import net.azisaba.spicyAzisaBan.util.Util
import net.azisaba.spicyAzisaBan.util.Util.filtr
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.translate
import org.json.JSONObject

object LockdownCommand: Command() {
    override val name = "${SABConfig.prefix}lockdown"

    override fun execute(actor: Actor, args: Array<String>) {
        if (!actor.hasPermission("sab.lockdown")) {
            return actor.send(SABMessages.General.missingPermissions.replaceVariables().translate())
        }
        if (args.isEmpty() || (args[0] != "true" && args[0] != "false")) {
            return actor.send(SABMessages.Commands.Lockdown.usage.replaceVariables().translate())
        }
        val enable = args[0].toBoolean()
        SpicyAzisaBan.instance.connection.sendEvent(EventType.LOCKDOWN, JSONObject().put("actor_name", actor.name).put("lockdown_enabled", enable))
        Util.setLockdownAndAnnounce(actor.name, enable)
    }

    override fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> {
        if (args.isEmpty()) return emptyList()
        if (actor.hasPermission("sab.lockdown")) return listOf("true", "false").filtr(args[0])
        return emptyList()
    }
}
