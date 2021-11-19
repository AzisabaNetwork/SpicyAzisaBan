package net.azisaba.spicyAzisaBan.common

import java.awt.Color
import java.util.Objects
import java.util.regex.Pattern

@Suppress("unused")
class ChatColor {
    private val toString: String
    val name: String
    val color: Color?

    private constructor(code: Char, name: String, color: Color? = null) {
        this.name = name
        toString = String(charArrayOf(COLOR_CHAR, code))
        this.color = color
        BY_CHAR[code] = this
        BY_NAME[name.uppercase()] = this
    }

    private constructor(name: String, toString: String, rgb: Int) {
        this.name = name
        this.toString = toString
        color = Color(rgb)
    }

    override fun hashCode(): Int {
        return 53 * 7 + Objects.hashCode(toString)
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other != null && this.javaClass == other.javaClass) {
            val cc = other as ChatColor
            toString == cc.toString
        } else {
            false
        }
    }

    override fun toString(): String {
        return toString
    }

    companion object {
        const val COLOR_CHAR = '§'
        const val ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx"
        private val STRIP_COLOR_PATTERN: Pattern = Pattern.compile("(?i)$COLOR_CHAR[0-9A-FK-ORX]")
        private val BY_CHAR: MutableMap<Char, ChatColor> = HashMap()
        private val BY_NAME: MutableMap<String, ChatColor> = HashMap()
        val BLACK = ChatColor('0', "black", Color(0x000000))
        val DARK_BLUE = ChatColor('1', "dark_blue", Color(0x0000AA))
        val DARK_GREEN = ChatColor('2', "dark_green", Color(0x00AA00))
        val DARK_AQUA = ChatColor('3', "dark_aqua", Color(0x00AAAA))
        val DARK_RED = ChatColor('4', "dark_red", Color(0xAA0000))
        val DARK_PURPLE = ChatColor('5', "dark_purple", Color(0xAA00AA))
        val GOLD = ChatColor('6', "gold", Color(0xFFAA00))
        val GRAY = ChatColor('7', "gray", Color(0xAAAAAA))
        val DARK_GRAY = ChatColor('8', "dark_gray", Color(0x555555))
        val BLUE = ChatColor('9', "blue", Color(0x5555FF))
        val GREEN = ChatColor('a', "green", Color(0x55FF55))
        val AQUA = ChatColor('b', "aqua", Color(0x55FFFF))
        val RED = ChatColor('c', "red", Color(0xFF5555))
        val LIGHT_PURPLE = ChatColor('d', "light_purple", Color(0xFF55FF))
        val YELLOW = ChatColor('e', "yellow", Color(0xFFFF55))
        val WHITE = ChatColor('f', "white", Color(0xFFFFFF))
        val MAGIC = ChatColor('k', "obfuscated")
        val BOLD = ChatColor('l', "bold")
        val STRIKETHROUGH = ChatColor('m', "strikethrough")
        val UNDERLINE = ChatColor('n', "underline")
        val ITALIC = ChatColor('o', "italic")
        val RESET = ChatColor('r', "reset")
        fun stripColor(input: String?): String? {
            return if (input == null) null else STRIP_COLOR_PATTERN.matcher(input).replaceAll("")
        }

        fun translateAlternateColorCodes(altColorChar: Char, textToTranslate: String): String {
            val b = textToTranslate.toCharArray()
            for (i in 0 until b.size - 1) {
                if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                    b[i] = COLOR_CHAR
                    b[i + 1] = b[i + 1].lowercaseChar()
                }
            }
            return String(b)
        }

        fun getByChar(code: Char): ChatColor? {
            return BY_CHAR[code]
        }

        fun of(color: Color): ChatColor {
            return of("#" + String.format("%08x", color.rgb).substring(2))
        }

        fun of(string: String): ChatColor {
            return if (string.startsWith("#") && string.length == 7) {
                val rgb = try {
                    string.substring(1).toInt(16)
                } catch (var7: NumberFormatException) {
                    throw IllegalArgumentException("Illegal hex string $string")
                }
                val magic = StringBuilder(COLOR_CHAR.toString() + "x")
                for (c in string.substring(1).toCharArray()) {
                    magic.append(COLOR_CHAR).append(c)
                }
                ChatColor(string, magic.toString(), rgb)
            } else {
                val defined = BY_NAME[string.uppercase()]
                defined ?: throw IllegalArgumentException("Could not parse ChatColor $string")
            }
        }
    }
}