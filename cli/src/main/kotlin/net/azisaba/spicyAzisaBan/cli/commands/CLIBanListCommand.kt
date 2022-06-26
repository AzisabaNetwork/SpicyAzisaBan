package net.azisaba.spicyAzisaBan.cli.commands

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.cli.util.CLIUtil
import net.azisaba.spicyAzisaBan.commands.BanListCommand
import net.azisaba.spicyAzisaBan.util.contexts.Contexts
import net.azisaba.spicyAzisaBan.util.contexts.get
import kotlin.math.max
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
object CLIBanListCommand: Subcommand("banlist", "Shows a punishment list") {
    private val type by option(ArgType.String, "type", "t", "Punishment type. Valid types are: BAN, TEMP_BAN, IP_BAN, TEMP_IP_BAN, MUTE, TEMP_MUTE, IP_MUTE, TEMP_IP_MUTE, WARNING, CAUTION, KICK, NOTE")
    private val active by option(ArgType.Boolean, "active", "e", "Only show active punishments. Conflicts with --all option.")
    private val all by option(ArgType.Boolean, "all", "a", "Show all punishments. Conflicts with --active option.")
    private val page by option(ArgType.Int, "page", "p", "# of page to show")
    private val server by option(ArgType.String, "server", "s", "Server to apply for")

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        val punishmentType = if (type == null)
            null
        else
            CLIUtil.genericArgumentParser.parse("type=$type")
                .get(Contexts.PUNISHMENT_TYPE, CLIActor)
                .complete()
                .apply { if (!isSuccess) exitProcess(1) }
                .type
        val newPage = max(1, page ?: 1)
        BanListCommand.execute(CLIActor, punishmentType, active ?: false, all ?: false, newPage, server).complete()
        exitProcess(0)
    }
}
