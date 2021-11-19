package net.azisaba.spicyAzisaBan.common.chat

class HoverEvent {
    class Action<@Suppress("unused") T> {
        companion object {
            val SHOW_TEXT = Action<Array<Component>>()
        }
    }
}
