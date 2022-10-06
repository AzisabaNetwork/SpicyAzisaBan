package net.azisaba.spicyAzisaBan.bungee

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.bungee.listener.EventListeners
import net.azisaba.spicyAzisaBan.bungee.listener.LockdownListener
import net.azisaba.spicyAzisaBan.bungee.listener.PlayerDataUpdaterListener
import net.blueberrymc.nativeutil.NativeUtil
import net.md_5.bungee.api.plugin.Plugin
import util.maven.Dependency
import util.maven.JarUtils
import util.maven.MavenRepository
import util.maven.Repository
import java.io.File
import java.net.URLClassLoader
import java.util.logging.Logger

class BungeePlugin: Plugin() {
    companion object {
        private val LOGGER = Logger.getLogger("SpicyAzisaBan")
        lateinit var instance: BungeePlugin

        init {
            val dataFolder = File("plugins/SpicyAzisaBan")
            LOGGER.info("Data folder: ${dataFolder.absolutePath}")
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
                    LOGGER.info(msg)
                } else {
                    LOGGER.warning(msg)
                }
                throwable?.let {
                    hasError = true
                    it.printStackTrace()
                }
            }.downloadAllDependencies()
            if (hasError) LOGGER.warning("Failed to download some dependencies.")
            val cl = BungeePlugin::class.java.classLoader
            val urls = files.filterNotNull()
                .map { file -> JarUtils.remapJarWithClassPrefix(file, "-remapped", "net.azisaba.spicyAzisaBan.libs") }
                .map { file -> file.toURI().toURL() }
                .toTypedArray()
            val libraryLoader = URLClassLoader(urls)
            NativeUtil.setObject(cl::class.java.getDeclaredField("libraryLoader"), cl, libraryLoader)
            LOGGER.info("Loaded libraries (" + files.size + "):")
            urls.forEach { url ->
                LOGGER.info(" - ${url.path}")
            }
        }
    }

    init {
        instance = this
    }

    override fun onEnable() {
        try {
            SpicyAzisaBanBungee().doEnable()
            proxy.pluginManager.registerListener(this, EventListeners)
            proxy.pluginManager.registerListener(this, PlayerDataUpdaterListener)
            logger.info("Hewwwwwwwwwoooooo!")
        } catch (e: Exception) {
            logger.severe("Fatal error occurred while initializing the plugin")
            e.printStackTrace()
            if (SABConfig.database.failsafe) {
                logger.info("Failsafe is enabled, locking down the server")
                proxy.pluginManager.registerListener(this, LockdownListener)
            }
        }
    }

    override fun onDisable() {
        SpicyAzisaBan.instance.shutdownTimer()
        logger.info("Closing database connection")
        SpicyAzisaBan.instance.connection.close()
        SpicyAzisaBan.debugLevel = 0
        logger.info("Goodbye, World!")
    }
}
