package net.azisaba.spicyAzisaBan.cli.util

import net.azisaba.spicyAzisaBan.common.ChatColor
import net.azisaba.spicyAzisaBan.common.chat.ClickEvent
import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.chat.HoverEvent

class SimpleComponent(val text: String): Component {
    companion object {
        fun fromLegacyText(legacyText: String, char: Char = '\u00a7') = SimpleComponent(
            legacyText
                .replace("${char}0", Colors.BLACK.toString())
                .replace("${char}1", Colors.DARK_BLUE.toString())
                .replace("${char}2", Colors.DARK_GREEN.toString())
                .replace("${char}3", Colors.DARK_CYAN.toString())
                .replace("${char}4", Colors.DARK_RED.toString())
                .replace("${char}5", Colors.DARK_PURPLE.toString())
                .replace("${char}6", Colors.DARK_YELLOW.toString())
                .replace("${char}7", Colors.DARK_WHITE.toString())
                .replace("${char}8", Colors.GRAY.toString())
                .replace("${char}9", Colors.BLUE.toString())
                .replace("${char}[aA]".toRegex(), Colors.GREEN.toString())
                .replace("${char}[bB]".toRegex(), Colors.CYAN.toString())
                .replace("${char}[cC]".toRegex(), Colors.RED.toString())
                .replace("${char}[dD]".toRegex(), Colors.MAGENTA.toString())
                .replace("${char}[eE]".toRegex(), Colors.YELLOW.toString())
                .replace("${char}[fF]".toRegex(), Colors.WHITE.toString())
                .replace("${char}[rR]".toRegex(), Colors.RESET.toString())
                .replace("(?i)${char}[K-OX]".toRegex(), "")
        )
    }

    private val children = mutableListOf<SimpleComponent>()
    var color: Colors = Colors.RESET

    override fun <T> setHoverEvent(action: HoverEvent.Action<T>, value: T) {}

    override fun <T> setClickEvent(action: ClickEvent.Action<T>, value: T) {}

    override fun setColor(color: ChatColor) {
        this.color = if (color.color?.rgb == 0x000000) {
            Colors.BLACK
        } else if (color.color?.rgb == 0x0000AA) {
            Colors.DARK_BLUE
        } else if (color.color?.rgb == 0x00AA00) {
            Colors.DARK_GREEN
        } else if (color.color?.rgb == 0x00AAAA) {
            Colors.DARK_CYAN
        } else if (color.color?.rgb == 0xAA00AA) {
            Colors.DARK_PURPLE
        } else if (color.color?.rgb == 0xFFAA00) {
            Colors.DARK_YELLOW
        } else if (color.color?.rgb == 0xAAAAAA) {
            Colors.DARK_WHITE
        } else if (color.color?.rgb == 0x555555) {
            Colors.GRAY
        } else if (color.color?.rgb == 0x5555FF) {
            Colors.BLUE
        } else if (color.color?.rgb == 0x55FF55) {
            Colors.GREEN
        } else if (color.color?.rgb == 0x55FFFF) {
            Colors.CYAN
        } else if (color.color?.rgb == 0xFF5555) {
            Colors.RED
        } else if (color.color?.rgb == 0xFF55FF) {
            Colors.MAGENTA
        } else if (color.color?.rgb == 0xFFFF55) {
            Colors.YELLOW
        } else if (color.color?.rgb == 0xFFFFFF) {
            Colors.WHITE
        } else {
            Colors.RESET
        }
    }

    override fun addChildren(component: Component) {
        children.add(component as SimpleComponent)
    }

    fun addChildren(vararg component: Component) {
        children.addAll(component.map { it as SimpleComponent })
    }

    @JvmName("getTextRecursively")
    fun getText(): String = "${Colors.RESET}$color$text${children.joinToString("", transform = SimpleComponent::getText)}${Colors.RESET}"
}
