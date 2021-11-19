package net.azisaba.spicyAzisaBan.bungee.util

import net.azisaba.spicyAzisaBan.bungee.util.BungeeUtil.toBungee
import net.azisaba.spicyAzisaBan.common.ChatColor
import net.azisaba.spicyAzisaBan.common.chat.ClickEvent
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.chat.HoverEvent
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.hover.content.Text

data class BungeeComponent(val component: BaseComponent): Component {
    override fun <T> setHoverEvent(action: HoverEvent.Action<T>, value: T) {
        if (action == HoverEvent.Action.SHOW_TEXT) {
            @Suppress("UNCHECKED_CAST")
            component.hoverEvent = net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                Text((value as Array<Component>).toBungee()),
            )
        }
    }

    override fun <T> setClickEvent(action: ClickEvent.Action<T>, value: T) {
        if (action == ClickEvent.Action.RUN_COMMAND) {
            component.clickEvent = net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                value as String,
            )
        }
    }

    override fun setColor(color: ChatColor) {
        color.color?.let {
            component.color = net.md_5.bungee.api.ChatColor.of(it)
        }
    }

    override fun addChildren(component: Component) {
        this.component.addExtra(component.toBungee())
    }
}
