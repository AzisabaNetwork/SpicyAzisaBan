package net.azisaba.spicyAzisaBan.common.command

import xyz.acrylicstyle.util.ArgumentParserBuilder

abstract class Command: TabExecutor {
    companion object {
        @JvmStatic
        val genericArgumentParser =
            ArgumentParserBuilder.builder()
                .parseOptionsWithoutDash()
                .disallowEscapedLineTerminators()
                .disallowEscapedTabCharacter()
                .create()
    }

    abstract val name: String
    open val permission: String? = null
    open val aliases: Array<String> = emptyArray()
}
