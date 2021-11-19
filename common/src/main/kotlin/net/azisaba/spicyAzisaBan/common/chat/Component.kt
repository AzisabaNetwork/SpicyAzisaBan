package net.azisaba.spicyAzisaBan.common.chat

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.ChatColor

interface Component {
    companion object {
        @JvmStatic
        fun text(content: String) = SpicyAzisaBan.instance.createTextComponent(content)

        @JvmStatic
        fun fromLegacyText(legacyText: String) = SpicyAzisaBan.instance.createTextComponentFromLegacyText(legacyText)
    }

    fun <T> setHoverEvent(action: HoverEvent.Action<T>, value: T)

    fun <T> setClickEvent(action: ClickEvent.Action<T>, value: T)

    fun setColor(color: ChatColor)

    fun addChildren(component: Component)
}
