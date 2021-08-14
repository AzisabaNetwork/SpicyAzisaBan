package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan

/**
 * v4 -> v5 converter
 */
object V4: DatabaseMigration {
    override val targetDatabaseVersion = 4
    override val name = "Add ipAddressHistory table"

    override fun execute() {
        // it is done via SQLConnection.kt (by adding new table and #sync there)
        SpicyAzisaBan.instance.settings.setDatabaseVersion(4).complete()
    }
}
