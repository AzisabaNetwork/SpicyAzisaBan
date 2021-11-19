package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.sql.SQLConnection.Companion.executeAndLog

/**
 * v1 -> v2 converter
 */
object V1: DatabaseMigration {
    override val targetDatabaseVersion = 1
    override val name = "Add NOT NULL to 'server' in punishments and punishmentHistory table"

    override fun execute(sql: SQLConnection) {
        val statement = sql.connection.createStatement()
        listOf("punishments", "punishmentHistory").forEach { table ->
            statement.executeAndLog("UPDATE `$table` SET `server` = \"global\" WHERE `server` IS NULL")
            statement.executeAndLog("ALTER TABLE `$table` MODIFY `server` VARCHAR(255) NOT NULL")
        }
        SpicyAzisaBan.instance.settings.setDatabaseVersion(2).complete()
        statement.close()
    }
}
