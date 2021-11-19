package net.azisaba.spicyAzisaBan.common.command

abstract class Command: TabExecutor {
    abstract val name: String
    open val permission: String? = null
    open val aliases: Array<String> = emptyArray()
}
