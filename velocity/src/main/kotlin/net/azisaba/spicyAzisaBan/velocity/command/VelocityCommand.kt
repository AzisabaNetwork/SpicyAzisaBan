package net.azisaba.spicyAzisaBan.velocity.command

import com.velocitypowered.api.command.RawCommand
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.util.Util.toComponent
import net.azisaba.spicyAzisaBan.velocity.VelocityActor
import net.azisaba.spicyAzisaBan.velocity.util.VelocityUtil.toVelocity
import xyz.acrylicstyle.util.InvalidArgumentException

class VelocityCommand(private val command: Command): RawCommand {
    override fun execute(invocation: RawCommand.Invocation) {
        val args = invocation.arguments().split(' ')
        try {
            command.execute(VelocityActor.of(invocation.source()), args.toTypedArray())
        } catch (e: InvalidArgumentException) {
            invocation.source().sendMessage(e.toComponent().toVelocity())
        }
    }

    override fun suggest(invocation: RawCommand.Invocation): List<String> {
        val args = invocation.arguments().split(' ')
        return command.onTabComplete(VelocityActor.of(invocation.source()), args.toTypedArray()).toList()
    }

    override fun hasPermission(invocation: RawCommand.Invocation): Boolean {
        if (command.permission == null) return true
        return invocation.source().hasPermission(command.permission)
    }
}
