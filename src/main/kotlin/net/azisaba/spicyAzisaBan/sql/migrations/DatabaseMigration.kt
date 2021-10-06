package net.azisaba.spicyAzisaBan.sql.migrations

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.sql.SQLConnection
import util.promise.rewrite.Promise

interface DatabaseMigration {
    companion object {
        private val migrations = listOf(
            V1, V2, V3, V4, V5, V6,
        )

        fun run(): Promise<Unit> = Promise.create { context ->
            val start = System.currentTimeMillis()
            val initialVersion = SpicyAzisaBan.instance.settings.getDatabaseVersion().complete()
            SpicyAzisaBan.instance.logger.info("Running database migrations (current database version: $initialVersion)")
            SpicyAzisaBan.instance.logger.info("${migrations.size} migrations loaded.")
            migrations.forEach { migration ->
                val version = SpicyAzisaBan.instance.settings.getDatabaseVersion().complete()
                if (migration.targetDatabaseVersion == version) {
                    SpicyAzisaBan.instance.logger.info("Migrating '${migration.name}' (database version $version)")
                    val sectionStart = System.currentTimeMillis()
                    try {
                        migration.execute(SpicyAzisaBan.instance.connection)
                    } catch (e: Throwable) {
                        SpicyAzisaBan.instance.logger.severe("Error migrating '${migration.name}' (database version $version)")
                        throw e
                    }
                    val sectionTime = System.currentTimeMillis() - sectionStart
                    SpicyAzisaBan.instance.logger.info("Migrated '${migration.name}' (database version $version, took ${sectionTime}ms)")
                }
            }
            val version = SpicyAzisaBan.instance.settings.getDatabaseVersion().complete()
            if (version != SQLConnection.CURRENT_DATABASE_VERSION) {
                SpicyAzisaBan.instance.logger.severe("Database migration did not upgrade the database version from $version to ${SQLConnection.CURRENT_DATABASE_VERSION}, this really should not happen")
            }
            val time = System.currentTimeMillis() - start
            SpicyAzisaBan.instance.logger.info("Completed database migrations (current database version: $version, took ${time}ms)")
            SpicyAzisaBan.instance.settings.setDatabaseVersion(version)
            context.resolve()
        }
    }

    val name: String
        get() = "Migration for database version $targetDatabaseVersion"
    val targetDatabaseVersion: Int

    fun execute(sql: SQLConnection)
}
