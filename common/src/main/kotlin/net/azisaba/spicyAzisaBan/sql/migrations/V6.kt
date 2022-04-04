package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.sql.SQLConnection

/**
 * v6 -> v7 converter
 */
object V6: DatabaseMigration {
    override val targetDatabaseVersion = 6
    override val name = "Add (first|last)_login(_attempt)? to players table"

    override fun execute(sql: SQLConnection) {
        sql.execute("ALTER TABLE `players` ADD `first_login` BIGINT(255) NOT NULL DEFAULT 0")
        sql.execute("ALTER TABLE `players` ADD `first_login_attempt` BIGINT(255) NOT NULL DEFAULT 0")
        sql.execute("ALTER TABLE `players` ADD `last_login` BIGINT(255) NOT NULL DEFAULT 0")
        sql.execute("ALTER TABLE `players` ADD `last_login_attempt` BIGINT(255) NOT NULL DEFAULT 0")
        SpicyAzisaBan.instance.settings.setDatabaseVersion(7).complete()
    }
}
