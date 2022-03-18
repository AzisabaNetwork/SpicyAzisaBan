package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import net.azisaba.spicyAzisaBan.util.Util.async
import util.promise.rewrite.Promise

interface DatabaseMigration {
    companion object {
        private val migrations = listOf(
            V1, V2, V3, V4, V5, V6,
        )

        fun run(): Promise<Unit> = async { context ->
            val start = System.currentTimeMillis()
            var currentVersion = SpicyAzisaBan.instance.settings.getDatabaseVersion().complete()
            val initialVersion = currentVersion
            SpicyAzisaBan.LOGGER.info("Running database migrations (current database version: $currentVersion)")
            SpicyAzisaBan.LOGGER.info("${migrations.size} migrations loaded.")
            migrations.forEach { migration ->
                if (migration.targetDatabaseVersion == currentVersion) {
                    SpicyAzisaBan.LOGGER.info("Migrating '${migration.name}' (database version $currentVersion)")
                    val sectionStart = System.currentTimeMillis()
                    try {
                        migration.execute(SpicyAzisaBan.instance.connection)
                    } catch (e: Throwable) {
                        SpicyAzisaBan.LOGGER.severe("Error migrating '${migration.name}' (database version $currentVersion)")
                        throw e
                    }
                    val sectionTime = System.currentTimeMillis() - sectionStart
                    SpicyAzisaBan.LOGGER.info("Migrated '${migration.name}' (database version $currentVersion, took ${sectionTime}ms)")
                    currentVersion = SpicyAzisaBan.instance.settings.getDatabaseVersion().complete()
                }
            }
            if (currentVersion != SQLConnection.CURRENT_DATABASE_VERSION) {
                SpicyAzisaBan.LOGGER.severe("Database migration did not upgrade the database version from $currentVersion to ${SQLConnection.CURRENT_DATABASE_VERSION}, this really should not happen")
            }
            val time = System.currentTimeMillis() - start
            SpicyAzisaBan.LOGGER.info("Completed database migrations (current database version: $currentVersion, took ${time}ms)")
            if (initialVersion != currentVersion) {
                SpicyAzisaBan.instance.settings.setDatabaseVersion(currentVersion)
            }
            context.resolve()
        }
    }

    val name: String
        get() = "Migration for database version $targetDatabaseVersion"
    val targetDatabaseVersion: Int

    fun execute(sql: SQLConnection)
}
