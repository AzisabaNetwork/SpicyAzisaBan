package net.azisaba.spicyAzisaBan.bungee.util

import net.azisaba.spicyAzisaBan.common.chat.Component
import net.md_5.bungee.api.chat.BaseComponent

object BungeeUtil {
    fun Component.toBungee(): BaseComponent {
        if (this is BungeeComponent) return this.component
        throw IllegalArgumentException("${this::class.java.typeName} is not instance of ${BungeeComponent::class.java.typeName}")
    }

    fun BaseComponent.toCommon() = BungeeComponent(this)

    fun Array<out Component>.toBungee(): Array<BaseComponent> = this.map { it.toBungee() }.toTypedArray()

    fun Array<out BaseComponent>.toCommon(): Array<Component> = this.map { it.toCommon() }.toTypedArray()
}
