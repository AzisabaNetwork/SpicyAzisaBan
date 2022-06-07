package net.azisaba.spicyAzisaBan.common

import net.azisaba.spicyAzisaBan.common.chat.Component
import java.util.UUID

interface Actor {
    val name: String
    val uniqueId: UUID
    fun sendMessage(component: Component)
    fun sendMessage(vararg components: Component)
    fun hasPermission(permission: String): Boolean

    object Dummy : Actor {
        override val name = "dummy player"
        override val uniqueId = UUID(0, 0)

        override fun sendMessage(component: Component) = Unit

        override fun sendMessage(vararg components: Component) = Unit

        override fun hasPermission(permission: String): Boolean = false
    }
}
