package net.azisaba.spicyAzisaBan.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.velocity.listener.EventListeners
import net.azisaba.spicyAzisaBan.velocity.listener.LockdownListener
import net.azisaba.spicyAzisaBan.velocity.listener.PlayerDataUpdaterListener
import org.slf4j.Logger
import util.maven.Dependency
import util.maven.JarUtils
import util.maven.MavenRepository
import util.maven.Repository
import java.io.File
import java.nio.file.Path

@Plugin(
    id = "spicyazisaban",
    name = "SpicyAzisaBan",
    version = "some-version",
    authors = ["AzisabaNetwork"],
    url = "https://github.com/AzisabaNetwork/SpicyAzisaBan"
)
class VelocityPlugin @Inject constructor(val server: ProxyServer, private val logger: Logger) {
    companion object {
        lateinit var instance: VelocityPlugin
    }

    private val urlsToBeAdded: List<Path>

    init {
        instance = this
        val dataFolder = File("plugins/SpicyAzisaBan")
        logger.info("Data folder: ${dataFolder.absolutePath}")
        val maven = MavenRepository()
        maven.addRepository(Repository.mavenLocal())
        maven.addRepository(Repository.mavenCentral())
        maven.addDependency(Dependency.resolve('c' + "om.google.guava:guava:31.0.1-jre"))
        maven.addDependency(Dependency.resolve("org.reflections:reflections:0.10.2"))
        maven.addDependency(Dependency.resolve('o' + "rg.json:json:20210307"))
        maven.addDependency(Dependency.resolve("org.yaml:snakeyaml:1.29"))
        maven.addDependency(Dependency.resolve('o' + "rg.mariadb.jdbc:mariadb-java-client:2.7.3"))
        maven.addExclude("log4j", "log4j")
        var hasError = false
        val files = maven.newFetcher(File(dataFolder, "libraries")).withMessageReporter { msg, throwable ->
            if (throwable == null) {
                logger.info(msg)
            } else {
                logger.warn(msg)
            }
            throwable?.let {
                hasError = true
                it.printStackTrace()
            }
        }.downloadAllDependencies()
        if (hasError) logger.warn("Failed to download some dependencies.")
        urlsToBeAdded = files
            .filterNotNull()
            .map { file -> JarUtils.remapJarWithClassPrefix(file, "-remapped", "net.azisaba.spicyAzisaBan.libs") }
            .mapNotNull { file -> file.toPath() }
    }

    @Subscribe
    fun onProxyInitialization(@Suppress("UNUSED_PARAMETER") e: ProxyInitializeEvent) {
        try {
            for (path in urlsToBeAdded) {
                server.pluginManager.addToClasspath(this, path)
                logger.info("Loaded library $path")
            }
            SpicyAzisaBan.debugLevel = 5
            SpicyAzisaBanVelocity(server).doEnable()
            if (!SABConfig.debugBuild) SpicyAzisaBan.debugLevel = 0
            server.eventManager.register(this, EventListeners)
            server.eventManager.register(this, PlayerDataUpdaterListener)
        } catch (e: Exception) {
            logger.error("Fatal error occurred while initializing the plugin", e)
            if (SABConfig.database.failsafe) {
                logger.info("Failsafe is enabled, locking down the server")
                server.eventManager.register(this, LockdownListener)
            }
        }
    }
}
