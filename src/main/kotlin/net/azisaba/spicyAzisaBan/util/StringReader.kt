package net.azisaba.spicyAzisaBan.util

class StringReader(private val text: String) {
    var index = 0

    /**
     * Reads the next character. Does not update the current index.
     */
    fun peek(): Char = text[index]

    /**
     * Reads the remaining characters. Does not update the current index.
     */
    fun peekRemaining(): String = text.substring(index.coerceAtLeast(0), if (index < 0) -index else text.length)

    /**
     * Reads the first character. Updates the index by 1.
     */
    fun readFirst() = read(1)[0]

    /**
     * Reads the string by specified amount. Updates the index by `amount`.
     */
    fun read(amount: Int): String {
        val string = text.substring(index, index + amount)
        index += amount
        return string
    }

    /**
     * Checks if the remaining string starts with `prefix`.
     */
    fun startsWith(prefix: String): Boolean = peekRemaining().startsWith(prefix)

    /**
     * Updates the current index by `amount`.
     */
    fun skip(amount: Int = 1): StringReader {
        index += amount
        return this
    }

    /**
     * Checks if the reader has encountered EOF.
     */
    fun isEOF(): Boolean = index >= text.length
}
