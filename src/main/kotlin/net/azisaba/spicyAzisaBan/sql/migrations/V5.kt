package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan

/**
 * v5 -> v6 converter
 */
object V5: DatabaseMigration {
    override val targetDatabaseVersion = 5
    override val name = "Add events table"

    override fun execute() {
        // it is done at SQLConnection.kt
        SpicyAzisaBan.instance.settings.setDatabaseVersion(6).complete()
    }
}
