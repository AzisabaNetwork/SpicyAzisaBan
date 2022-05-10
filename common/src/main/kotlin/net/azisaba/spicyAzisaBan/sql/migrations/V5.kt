package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.sql.SQLConnection

/**
 * v5 -> v6 converter
 */
object V5: DatabaseMigration {
    override val sourceDatabaseVersion = 5
    override val name = "Add events table"

    override fun execute(sql: SQLConnection) {
        // it is done at SQLConnection.kt
        SpicyAzisaBan.instance.settings.setDatabaseVersion(6).complete()
    }
}
