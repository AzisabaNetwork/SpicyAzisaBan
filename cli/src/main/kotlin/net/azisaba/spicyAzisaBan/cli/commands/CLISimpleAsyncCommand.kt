package net.azisaba.spicyAzisaBan.cli.commands

import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import net.azisaba.spicyAzisaBan.cli.SpicyAzisaBanCLI
import net.azisaba.spicyAzisaBan.cli.actor.CLIActor
import net.azisaba.spicyAzisaBan.common.Actor
import util.promise.rewrite.Promise
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
class CLISimpleAsyncCommand(name: String, actionDescription: String, private val executor: (Actor) -> Promise<Unit>):
    Subcommand(name, actionDescription) {

    override fun execute() {
        SpicyAzisaBanCLI().doEnable()
        executor(CLIActor).complete()
        exitProcess(0)
    }
}
