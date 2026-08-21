package com.printscript.parser

import com.printscript.common.Position
import com.printscript.common.Span
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

    fun endOfInput(): Span {
        val last = tokens.lastOrNull()
            ?: return Span.at(Position(1, 1))

        return Span.at(last.end)
    }
}
