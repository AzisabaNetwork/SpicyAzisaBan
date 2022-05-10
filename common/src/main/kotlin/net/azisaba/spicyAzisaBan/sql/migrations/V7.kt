package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.sql.SQLConnection

/**
 * v7 -> v8 converter
 */
object V7: DatabaseMigration {
    override val sourceDatabaseVersion = 7
    override val name = "events table: drop seen column, add handled column"

    override fun execute(sql: SQLConnection) {
        sql.execute("ALTER TABLE `events` DROP `seen`")
        sql.execute("ALTER TABLE `events` ADD `handled` TINYINT(1) NOT NULL DEFAULT 0")
        SpicyAzisaBan.instance.settings.setDatabaseVersion(8).complete()
    }
}
