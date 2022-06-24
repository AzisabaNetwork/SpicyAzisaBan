package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.sql.SQLConnection

/**
 * v8 -> v9 converter
 */
object V8: DatabaseMigration {
    override val sourceDatabaseVersion = 8
    override val name = "proofs table: Add public field"

    override fun execute(sql: SQLConnection) {
        sql.execute("ALTER TABLE `proofs` ADD `public` TINYINT(1) NOT NULL DEFAULT 0")
        SpicyAzisaBan.instance.settings.setDatabaseVersion(9).complete()
    }
}
