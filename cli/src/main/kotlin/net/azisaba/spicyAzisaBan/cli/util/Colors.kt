package net.azisaba.spicyAzisaBan.cli.util

enum class Colors(private val code: String) {
    RESET("\u001B[0m"),
    BLACK("\u001B[30m"),
    DARK_RED("\u001B[31m"),
    DARK_GREEN("\u001B[32m"),
    DARK_YELLOW("\u001B[33m"),
    DARK_BLUE("\u001B[34m"),
    DARK_PURPLE("\u001B[35m"),
    DARK_CYAN("\u001B[36m"),
    DARK_WHITE("\u001B[37m"),
    GRAY("\u001B[90m"),
    RED("\u001B[91m"),
    GREEN("\u001B[92m"),
    YELLOW("\u001B[93m"),
    BLUE("\u001B[94m"),
    MAGENTA("\u001B[95m"),
    CYAN("\u001B[96m"),
    WHITE("\u001B[97m"),
    ;

    override fun toString(): String = code
}