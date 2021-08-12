package net.azisaba.spicyAzisaBan.util.contexts

data class IPAddressContext(
    val isSuccess: Boolean,
    val profile: String,
): Context {
    companion object {
        fun tabComplete(s: String): List<String> = PlayerContext.tabComplete(s)
    }
}
