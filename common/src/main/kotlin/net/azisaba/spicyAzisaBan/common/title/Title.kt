package net.azisaba.spicyAzisaBan.common.title

import net.azisaba.spicyAzisaBan.common.chat.Component

data class Title(
    var title: Array<Component> = emptyArray(),
    var subTitle: Array<Component> = emptyArray(),
    var fadeIn: Int = 10,
    var stay: Int = 70,
    var fadeOut: Int = 20,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Title
        if (!title.contentEquals(other.title)) return false
        if (!subTitle.contentEquals(other.subTitle)) return false
        if (fadeIn != other.fadeIn) return false
        if (stay != other.stay) return false
        if (fadeOut != other.fadeOut) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.contentHashCode()
        result = 31 * result + subTitle.contentHashCode()
        result = 31 * result + fadeIn
        result = 31 * result + stay
        result = 31 * result + fadeOut
        return result
    }
}
