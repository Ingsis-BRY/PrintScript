package com.printscript.lexer.recognizer

import com.printscript.common.Position
import com.printscript.token.Token

/**
 * Recognizes one fixed lexeme, such as ";" or "(".
 *
 * The automaton has no loop: it walks [expected] character by character and
 * accepts only on an exact match. Anything that stops being a prefix of
 * [expected] is rejected, which includes overshooting it.
 */
class FixedLexemeRecognizer(
    private val expected: String,
    private val build: (String, Position, Position) -> Token
) : TokenRecognizer {

    override fun recognize(lexeme: String): RecognizerState =
        when {
            lexeme == expected -> RecognizerState.Accepted
            expected.startsWith(lexeme) -> RecognizerState.Pending
            else -> RecognizerState.Rejected
        }

    override fun tokenOf(lexeme: String, start: Position, end: Position): Token =
        build(lexeme, start, end)
}
