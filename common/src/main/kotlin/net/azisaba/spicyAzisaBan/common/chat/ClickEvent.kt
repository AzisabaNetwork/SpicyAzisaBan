package net.azisaba.spicyAzisaBan.common.chat

class ClickEvent {
    class Action<@Suppress("unused") T> {
        companion object {
            val RUN_COMMAND = Action<String>()
        }
    }
}
