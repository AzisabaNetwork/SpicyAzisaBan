package net.azisaba.spicyAzisaBan

import net.md_5.bungee.api.plugin.Plugin

class SpicyAzisaBan: Plugin() {
    companion object {
        lateinit var instance: SpicyAzisaBan
    }

    init {
        instance = this
    }

    override fun onEnable() {
        logger.info("Hewwwwwwwwwoooooo!")
    }
}
