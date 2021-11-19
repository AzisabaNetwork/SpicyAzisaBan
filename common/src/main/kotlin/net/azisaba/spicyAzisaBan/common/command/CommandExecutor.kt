package net.azisaba.spicyAzisaBan.common.command

import net.azisaba.spicyAzisaBan.common.Actor

interface CommandExecutor {
    fun execute(actor: Actor, args: Array<String>)
}
