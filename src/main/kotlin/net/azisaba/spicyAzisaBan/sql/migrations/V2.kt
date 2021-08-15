package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.sql.SQLConnection.Companion.executeAndLog

/**
 * v2 -> v3 converter
 */
object V2: DatabaseMigration {
    override val targetDatabaseVersion = 2
    override val name = "Add `extra` column to punishments and punishmentHistory table"

    override fun execute() {
        val statement = SpicyAzisaBan.instance.connection.connection.createStatement()
        listOf("punishments", "punishmentHistory").forEach { table ->
            statement.executeAndLog("ALTER TABLE `$table` ADD `extra` VARCHAR(255) NOT NULL DEFAULT \"\"")
        }
        SpicyAzisaBan.instance.settings.setDatabaseVersion(3).complete()
        statement.close()
    }
}
