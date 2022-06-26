package net.azisaba.spicyAzisaBan.common.command

import net.azisaba.spicyAzisaBan.common.Actor
import xyz.acrylicstyle.util.InvalidArgumentException

interface CommandExecutor {
    @Throws(InvalidArgumentException::class)
    fun execute(actor: Actor, args: Array<String>)
}
