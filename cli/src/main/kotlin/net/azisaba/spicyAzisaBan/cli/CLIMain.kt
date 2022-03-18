package net.azisaba.spicyAzisaBan.cli

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.SingleNullableOption
import net.azisaba.spicyAzisaBan.cli.commands.CLIAddProofCommand
import net.azisaba.spicyAzisaBan.cli.commands.CLIBanCommand
import net.azisaba.spicyAzisaBan.cli.commands.CLIBanListCommand
import net.azisaba.spicyAzisaBan.cli.commands.CLICautionCommand
import net.azisaba.spicyAzisaBan.cli.commands.CLIGroupCommand
import net.azisaba.spicyAzisaBan.cli.commands.CLIKickCommand
import net.azisaba.spicyAzisaBan.cli.commands.CLIMuteCommand
import net.azisaba.spicyAzisaBan.cli.commands.CLISeenCommand
import net.azisaba.spicyAzisaBan.cli.commands.CLISimpleAsyncCommand
import net.azisaba.spicyAzisaBan.cli.commands.CLIUnpunishCommand
import net.azisaba.spicyAzisaBan.cli.commands.CLIWarningCommand
import net.azisaba.spicyAzisaBan.commands.SABCommand
import kotlin.system.exitProcess

object CLIMain {
    lateinit var debugLevel: SingleNullableOption<Int>

    @OptIn(ExperimentalCli::class)
    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1\$tT] [%4\$s] %5\$s%6\$s%n")
        try {
            val parser = ArgParser("SpicyAzisaBan", prefixStyle = ArgParser.OptionPrefixStyle.GNU)
            debugLevel = parser.option(ArgType.Int, "debug", "d", "Debug Level (0-99999)")
            parser.subcommands(
                CLISimpleAsyncCommand("cli-info", "Shows an information about this tool.") { SABCommand.executeInfo(it) },
                CLIAddProofCommand,
                CLIBanCommand,
                CLIBanListCommand,
                CLICautionCommand,
                CLIGroupCommand,
                CLIKickCommand,
                CLIMuteCommand,
                CLISeenCommand,
                CLIUnpunishCommand,
                CLIWarningCommand,
            )
            parser.parse(args)
        } catch (e: Throwable) {
            e.printStackTrace()
            exitProcess(1)
        }
    }
}
