package net.azisaba.spicyAzisaBan.velocity.util

import net.azisaba.spicyAzisaBan.common.ChatColor
import net.azisaba.spicyAzisaBan.common.chat.ClickEvent
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.chat.HoverEvent
import net.azisaba.spicyAzisaBan.velocity.util.VelocityUtil.toVelocity
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.Component as KComponent

data class VelocityComponent(var component: KComponent): Component {
    override fun <T> setHoverEvent(action: HoverEvent.Action<T>, value: T) {
        if (action == HoverEvent.Action.SHOW_TEXT) {
            @Suppress("UNCHECKED_CAST")
            component = component.hoverEvent(TextComponent.ofChildren(*(value as Array<Component>).toVelocity()))
        }
    }

    override fun <T> setClickEvent(action: ClickEvent.Action<T>, value: T) {
        if (action == ClickEvent.Action.RUN_COMMAND) {
            component = component.clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(value as String))
        }
    }

    override fun setColor(color: ChatColor) {
        color.color?.let {
            component = component.color(TextColor.color(it.rgb))
        }
    }

    override fun addChildren(component: Component) {
        this.component = this.component.children(this.component.children().toMutableList().apply { add(component.toVelocity()) })
    }
}
