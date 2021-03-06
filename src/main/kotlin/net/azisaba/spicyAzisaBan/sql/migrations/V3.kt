package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan

/**
 * v3 -> v4 converter
 */
object V3: DatabaseMigration {
    override val targetDatabaseVersion = 3
    override val name = "Add unpunish, proofs, players, usernameHistory tables"

    override fun execute() {
        // it is done via SQLConnection.kt (by adding new table and #sync there)
        SpicyAzisaBan.instance.settings.setDatabaseVersion(4).complete()
    }
}
