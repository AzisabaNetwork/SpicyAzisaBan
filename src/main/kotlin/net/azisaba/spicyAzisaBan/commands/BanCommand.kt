package net.azisaba.spicyAzisaBan.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor

class BanCommand(
    // compile error without @get:JvmName, but warning with @get:JvmName... now what?
    @Suppress("ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR")
    @get:JvmName("getName0")
    private val name: String,
    private val temp: Boolean,
    private val global: Boolean,
): Command(name), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        //
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        return emptyList()
    }
}
