package net.azisaba.spicyAzisaBan.cli.commands

import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.common.Actor
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
class CLISimpleCommand(name: String, actionDescription: String, private val executor: (Actor) -> Unit):
    Subcommand(name, actionDescription) {

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        executor(CLIActor)
        exitProcess(0)
    }
}
