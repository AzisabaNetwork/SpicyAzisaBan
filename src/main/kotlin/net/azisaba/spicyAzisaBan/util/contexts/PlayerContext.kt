package net.azisaba.spicyAzisaBan.util.contexts

import xyz.acrylicstyle.mcutil.common.PlayerProfile

data class PlayerContext(
    val isSuccess: Boolean,
    val profile: PlayerProfile,
): Context
