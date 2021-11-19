package net.azisaba.spicyAzisaBan.common

import net.azisaba.spicyAzisaBan.common.chat.Component
import java.util.UUID

interface Actor {
    val name: String
    val uniqueId: UUID
    fun sendMessage(component: Component)
    fun sendMessage(vararg components: Component)
    fun hasPermission(permission: String): Boolean
}
