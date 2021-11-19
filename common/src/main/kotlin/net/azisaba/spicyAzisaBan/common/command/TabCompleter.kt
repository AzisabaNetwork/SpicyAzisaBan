package net.azisaba.spicyAzisaBan.common.command

import net.azisaba.spicyAzisaBan.common.Actor

interface TabCompleter {
    fun onTabComplete(actor: Actor, args: Array<String>): Collection<String> = emptyList()
}
