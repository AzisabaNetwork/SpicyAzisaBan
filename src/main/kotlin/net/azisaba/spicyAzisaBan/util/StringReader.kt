package net.azisaba.spicyAzisaBan.util

class StringReader(private val text: String) {
    var index = 0

    fun peek(): Char = text[index]

    fun peekString(): String = peek().toString()

    fun peekRemaining(): String = text.substring(index.coerceAtLeast(0), if (index < 0) -index else text.length)

    fun readFirst() = read(1)[0]

    fun read(amount: Int): String {
        val string = text.substring(index, index + amount)
        index += amount
        return string
    }

    fun startsWith(prefix: String): Boolean = peekRemaining().startsWith(prefix)

    fun skip(amount: Int = 1): StringReader {
        index += amount
        return this
    }

    fun isEOF(): Boolean = index >= text.length
}
