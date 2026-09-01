package com.printscript.parser

import com.printscript.token.Token

object PrecedenceTable {
    private const val MIN_PRECEDENCE = 0
    private const val ADDITIVE_PRECEDENCE = 1
    private const val MULTIPLICATIVE_PRECEDENCE = 2

    fun precedenceOf(token: Token): Int =
        when (token) {
            is Token.PlusToken,
            is Token.MinusToken,
            -> ADDITIVE_PRECEDENCE

            is Token.StarToken,
            is Token.SlashToken,
            -> MULTIPLICATIVE_PRECEDENCE

            else -> MIN_PRECEDENCE
        }
}
