package com.printscript.lexer.recognizer

import com.printscript.common.Position
import com.printscript.token.Token

/**
 * Recognizes number literals: `digit+ ( '.' digit+ )?`.
 *
 * A trailing dot leaves the automaton [RecognizerState.Pending] instead of
 * accepted, which is what makes `5.` fall back to `5`: the lexer keeps the last
 * accepted length and pushes the dot back for the next scan.
 *
 * The sign is never part of the literal; `-` is always an operator.
 *
 * Digits are ASCII on purpose, so `Char.isDigit` is not used: it would also
 * admit digits from other scripts, which `toDouble` cannot read back.
 *
 * The whole and fraction parts are checked in place rather than cut out, since
 * the lexer calls this once per character.
 */
object NumberLiteralRecognizer : TokenRecognizer {
    override fun recognize(lexeme: String): RecognizerState {
        if (lexeme.isEmpty()) {
            return RecognizerState.Pending
        }

        val dot = lexeme.indexOf('.')

        if (dot < 0) {
            return if (allDigits(lexeme, from = 0, toExclusive = lexeme.length)) {
                RecognizerState.Accepted
            } else {
                RecognizerState.Rejected
            }
        }

        return when {
            dot == 0 || !allDigits(lexeme, from = 0, toExclusive = dot) ->
                RecognizerState.Rejected

            dot == lexeme.lastIndex ->
                RecognizerState.Pending

            allDigits(lexeme, from = dot + 1, toExclusive = lexeme.length) ->
                RecognizerState.Accepted

            else ->
                RecognizerState.Rejected
        }
    }

    /**
     * the strict format is a subset of what `toDouble` reads, so [Token.NumberLiteralToken.value]
     * is the lexeme itself
     */
    override fun tokenOf(
        lexeme: String,
        start: Position,
        end: Position,
    ): Token =
        Token.NumberLiteralToken(
            lexeme = lexeme,
            value = lexeme,
            start = start,
            end = end,
        )

    private fun allDigits(
        lexeme: String,
        from: Int,
        toExclusive: Int,
    ): Boolean {
        for (index in from until toExclusive) {
            if (!isDigit(lexeme[index])) {
                return false
            }
        }

        return true
    }

    private fun isDigit(value: Char): Boolean = value in '0'..'9'
}
