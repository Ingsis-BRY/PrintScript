package com.printscript.linter.rules.identifier

import com.printscript.linter.config.identifier.IdentifierStyle

class IdentifierValidator(
    private val style: IdentifierStyle,
) {
    fun isValid(name: String): Boolean =
        when (style) {
            IdentifierStyle.CAMEL_CASE -> CAMEL_CASE.matches(name)
            IdentifierStyle.SNAKE_CASE -> SNAKE_CASE.matches(name)
        }

    private companion object {
        val CAMEL_CASE = Regex("[a-z][a-zA-Z0-9]*")
        val SNAKE_CASE = Regex("[a-z][a-z0-9]*(?:_[a-z0-9]+)*")
    }
}
