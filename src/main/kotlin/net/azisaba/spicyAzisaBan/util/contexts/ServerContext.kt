package net.azisaba.spicyAzisaBan.util.contexts

data class ServerContext(
    val isSuccess: Boolean,
    val name: String,
    val isGroup: Boolean,
) : Context
