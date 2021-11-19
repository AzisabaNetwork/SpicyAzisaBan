package net.azisaba.spicyAzisaBan.velocity.command

import com.velocitypowered.api.command.RawCommand
import net.azisaba.spicyAzisaBan.common.command.Command
import net.azisaba.spicyAzisaBan.velocity.VelocityActor

class VelocityCommand(private val command: Command): RawCommand {
    override fun execute(invocation: RawCommand.Invocation) {
        val args = invocation.arguments().split(' ')
        command.execute(VelocityActor.of(invocation.source()), args.toTypedArray())
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
