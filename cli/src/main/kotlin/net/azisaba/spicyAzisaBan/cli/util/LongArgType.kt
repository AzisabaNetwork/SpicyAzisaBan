package net.azisaba.spicyAzisaBan.cli.util

import kotlinx.cli.ArgType
import kotlinx.cli.ParsingException

object LongArgType : ArgType<Long>(true) {
    override val description: kotlin.String
        get() = "{ Long }"

    override fun convert(value: kotlin.String, name: kotlin.String): Long =
        value.toLongOrNull()
            ?: throw ParsingException("Option $name is expected to be long number. $value is provided.")
}
