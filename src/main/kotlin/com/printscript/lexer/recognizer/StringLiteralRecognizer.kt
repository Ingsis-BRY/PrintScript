package com.printscript.lexer.recognizer

import com.printscript.common.Position
import com.printscript.token.Token

/**
 * Recognizes string literals delimited by single or double quotes.
 *
 * There are no escape sequences: the body is taken literally up to the first
 * matching quote, so a double-quoted literal can hold apostrophes and a
 * single-quoted one can hold double quotes.
 *
 * A literal may not span lines. Meeting a line break before the closing quote
 * rejects the lexeme, and [diagnose] turns that into the unterminated-string
 * error. The same message covers a source that ends mid-literal, where the
 * automaton is still [RecognizerState.Pending] and never rejected anything.
 *
 * The body is walked by index rather than copied out, since the lexer calls
 * this once per character and a copy per call would cost a literal its length
 * squared in allocation.
 */
object StringLiteralRecognizer : TokenRecognizer {

    private const val UNTERMINATED = "Unterminated string literal"

    override fun recognize(lexeme: String): RecognizerState {
        if (lexeme.isEmpty()) {
            return RecognizerState.Pending
        }

        val quote = lexeme.first()

        if (!isQuote(quote)) {
            return RecognizerState.Rejected
        }

        for (index in 1 until lexeme.length) {
            val current = lexeme[index]

            when {
                current == quote ->
                    return if (index == lexeme.lastIndex) {
                        RecognizerState.Accepted
                    } else {
                        RecognizerState.Rejected
                    }

                endsLine(current) ->
                    return RecognizerState.Rejected
            }
        }

        return RecognizerState.Pending
    }

    /**
     * a lexeme that opened a quote can only have been meant as a string, so this
     * replaces the lexer's generic diagnosis whether the literal died on a line
     * break or on the end of the source
     */
    override fun diagnose(lexeme: String): String? =
        if (lexeme.isNotEmpty() && isQuote(lexeme.first())) UNTERMINATED else null

    /**
     * [Token.StringLiteralToken.lexeme] keeps the quotes, its value drops them
     */
    override fun tokenOf(lexeme: String, start: Position, end: Position): Token =
        Token.StringLiteralToken(
            lexeme = lexeme,
            value = lexeme.substring(1, lexeme.length - 1),
            start = start,
            end = end
        )

    private fun isQuote(value: Char): Boolean =
        value == '"' || value == '\''

    private fun endsLine(value: Char): Boolean =
        value == '\n' || value == '\r'
}
