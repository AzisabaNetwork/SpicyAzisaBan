package net.azisaba.spicyAzisaBan.velocity.util

import net.azisaba.spicyAzisaBan.common.chat.Component
import net.kyori.adventure.text.Component as KComponent

object VelocityUtil {
    fun Component.toVelocity(): KComponent {
        if (this is VelocityComponent) return this.component
        throw IllegalArgumentException("${this::class.java.typeName} is not instance of ${VelocityComponent::class.java.typeName}")
    }

    fun KComponent.toCommon() = VelocityComponent(this)

    fun Array<out Component>.toVelocity(): Array<KComponent> = this.map { it.toVelocity() }.toTypedArray()

    fun Array<out KComponent>.toCommon() = this.map { it.toCommon() }.toTypedArray()
}
