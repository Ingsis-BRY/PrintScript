package com.printscript.parser

import com.printscript.token.Token

class TokenCursor(
    private val tokens: List<Token>
) {
    private var index: Int = 0

    fun peek(): Token? {
        return tokens.getOrNull(index)
    }

    fun consume(): Token? {
        if (!hasNext()) {
            return null
        }

        return tokens[index++]
    }

    fun hasNext(): Boolean {
        return index < tokens.size
    }
}