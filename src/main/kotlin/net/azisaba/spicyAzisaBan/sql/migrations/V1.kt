package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan

object V1: DatabaseMigration {
    override val targetDatabaseVersion = 1
    override val name = "Add NOT NULL to 'server' in punishments and punishmentHistory table"

    override fun execute() {
        val statement = SpicyAzisaBan.instance.connection.connection.createStatement()
        listOf("punishments", "punishmentHistory").forEach { table ->
            statement.execute("UPDATE `$table` SET `server` = \"global\" WHERE `server` IS NULL")
            statement.execute("ALTER TABLE `$table` MODIFY `server` VARCHAR(255) NOT NULL")
        }
        SpicyAzisaBan.instance.settings.setDatabaseVersion(2).complete()
        statement.close()
    }
}
